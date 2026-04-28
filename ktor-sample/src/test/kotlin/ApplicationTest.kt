package com.example

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApplicationTest {

    private val validPayload = """
        {
            "siteId": "site_upstream",
            "timeStamp": "2025-05-01T08:00:00",
            "pH": 7.2,
            "turbidityNtu": 2.0,
            "conductivityPerCm": 300.0,
            "waterTempC": 15.0,
            "waterLvlCm": 45.0,
            "lightLux": 800.0
        }
    """.trimIndent()

    /**
     * Runs before every single test.
     * Clears the two tables that tests write to, so each test
     * starts with a clean slate. Sites are NOT cleared because
     * they are seeded once at startup and tests depend on them existing.
     */
    @BeforeTest
    fun clearTables() {
        // We need the app to have initialised the DB at least once
        // before we can clear it. testApplication handles that, but
        // @BeforeTest runs outside of it — so we guard with a try/catch.
        try {
            transaction {
                AlertsLog.deleteAll()
                WaterQualityReadings.deleteAll()
            }
        } catch (e: Exception) {
            // Tables may not exist yet on the very first run — that is fine
        }
    }

    // ─── Root ────────────────────────────────────────────────────

    @Test
    fun `GET root returns 200 or redirect`() = testApplication {
        application { module() }
        val response = client.get("/")
        assertTrue(
            response.status == HttpStatusCode.OK || response.status == HttpStatusCode.Found,
            "Expected 200 or 302 but got ${response.status}"
        )
    }

    // ─── POST /api/ingest — happy path ───────────────────────────

    @Test
    fun `POST ingest with valid payload returns 201`() = testApplication {
        application { module() }

        val response = client.post("/api/ingest") {
            contentType(ContentType.Application.Json)
            setBody(validPayload)
        }

        assertEquals(HttpStatusCode.Created, response.status)
        assertTrue(response.bodyAsText().contains("saved"))
    }

    @Test
    fun `POST ingest normal reading returns derived status normal`() = testApplication {
        application { module() }

        val response = client.post("/api/ingest") {
            contentType(ContentType.Application.Json)
            setBody(validPayload)
        }

        assertTrue(response.bodyAsText().contains("normal"))
    }

    @Test
    fun `POST ingest critical pH returns derived status critical`() = testApplication {
        application { module() }

        val criticalPayload = validPayload.replace("\"pH\": 7.2", "\"pH\": 5.0")

        val response = client.post("/api/ingest") {
            contentType(ContentType.Application.Json)
            setBody(criticalPayload)
        }

        assertEquals(HttpStatusCode.Created, response.status)
        assertTrue(response.bodyAsText().contains("critical"))
    }

    @Test
    fun `POST ingest warning pH returns derived status warning`() = testApplication {
        application { module() }

        val warningPayload = validPayload.replace("\"pH\": 7.2", "\"pH\": 6.3")

        val response = client.post("/api/ingest") {
            contentType(ContentType.Application.Json)
            setBody(warningPayload)
        }

        assertEquals(HttpStatusCode.Created, response.status)
        assertTrue(response.bodyAsText().contains("warning"))
    }

    // ─── POST /api/ingest — validation failures ───────────────────

    @Test
    fun `POST ingest with negative pH returns 400`() = testApplication {
        application { module() }

        val response = client.post("/api/ingest") {
            contentType(ContentType.Application.Json)
            setBody(validPayload.replace("\"pH\": 7.2", "\"pH\": -1.0"))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `POST ingest with pH above 14 returns 400`() = testApplication {
        application { module() }

        val response = client.post("/api/ingest") {
            contentType(ContentType.Application.Json)
            setBody(validPayload.replace("\"pH\": 7.2", "\"pH\": 15.0"))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `POST ingest with negative turbidity returns 400`() = testApplication {
        application { module() }

        val response = client.post("/api/ingest") {
            contentType(ContentType.Application.Json)
            setBody(validPayload.replace("\"turbidityNtu\": 2.0", "\"turbidityNtu\": -5.0"))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `POST ingest with unknown siteId returns 400`() = testApplication {
        application { module() }

        val response = client.post("/api/ingest") {
            contentType(ContentType.Application.Json)
            setBody(validPayload.replace("\"siteId\": \"site_upstream\"", "\"siteId\": \"site_ghost\""))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("Unknown siteId"))
    }

    @Test
    fun `POST ingest with malformed JSON returns 400`() = testApplication {
        application { module() }

        val response = client.post("/api/ingest") {
            contentType(ContentType.Application.Json)
            setBody("this is not json at all {{{")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `POST ingest with missing fields returns 400`() = testApplication {
        application { module() }

        val response = client.post("/api/ingest") {
            contentType(ContentType.Application.Json)
            setBody("""{"siteId": "site_upstream"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    // ─── GET /api/alerts ─────────────────────────────────────────

    @Test
    fun `GET alerts returns 200 and a JSON array`() = testApplication {
        application { module() }

        val response = client.get("/api/alerts")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.startsWith("["), "Expected JSON array, got: $body")
    }

    @Test
    fun `GET alerts after ingest with critical reading contains an alert`() = testApplication {
        application { module() }

        client.post("/api/ingest") {
            contentType(ContentType.Application.Json)
            setBody(validPayload.replace("\"pH\": 7.2", "\"pH\": 5.0"))
        }

        val body = client.get("/api/alerts").bodyAsText()
        assertTrue(body.contains("critical"))
        assertTrue(body.contains("site_upstream"))
    }

    @Test
    fun `GET alerts filtered by site returns only that site`() = testApplication {
        application { module() }

        // Ingest a critical reading for site_upstream only
        client.post("/api/ingest") {
            contentType(ContentType.Application.Json)
            setBody(validPayload.replace("\"pH\": 7.2", "\"pH\": 5.0"))
        }

        // Filter to site_downstream — should return empty array
        val response = client.get("/api/alerts?site=site_downstream")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("[]", response.bodyAsText().trim())
    }

    @Test
    fun `GET alerts filtered by severity=critical only shows critical`() = testApplication {
        application { module() }

        client.post("/api/ingest") {
            contentType(ContentType.Application.Json)
            setBody(validPayload.replace("\"pH\": 7.2", "\"pH\": 5.0"))
        }

        val body = client.get("/api/alerts?severity=critical").bodyAsText()
        assertTrue(body.contains("critical"))
    }

    @Test
    fun `GET alerts with no matching filter returns empty array`() = testApplication {
        application { module() }

        // Clear any alerts that other tests may have written to the shared DB
        transaction {
            AlertsLog.deleteAll()
        }

        val response = client.get("/api/alerts?severity=warning")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("[]", response.bodyAsText().trim())
    }

    // ─── GET /api/readings ────────────────────────────────────────

    @Test
    fun `GET readings without site param returns 400`() = testApplication {
        application { module() }

        val response = client.get("/api/readings")
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("site"))
    }

    @Test
    fun `GET readings for unknown site returns 404`() = testApplication {
        application { module() }

        val response = client.get("/api/readings?site=site_ghost")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `GET readings for valid site returns 200 and array`() = testApplication {
        application { module() }

        transaction {
            AlertsLog.deleteAll()
            WaterQualityReadings.deleteAll()
        }

        val ingestResponse = client.post("/api/ingest") {
            contentType(ContentType.Application.Json)
            setBody(validPayload)
        }

        // Print what the ingest actually returned so we can see it in test output
        val ingestBody = ingestResponse.bodyAsText()
        println("INGEST STATUS: ${ingestResponse.status}")
        println("INGEST BODY: $ingestBody")

        val response = client.get("/api/readings?site=site_upstream")
        val body = response.bodyAsText()

        println("READINGS STATUS: ${response.status}")
        println("READINGS BODY: $body")

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(body.startsWith("["), "Expected JSON array but got: $body")
        assertTrue(body.contains("site_upstream"), "Expected siteId in body but got: $body")
    }

    @Test
    fun `GET readings for valid site with no data returns empty array`() = testApplication {
        application { module() }

        // site_reservoir exists but we cleared all readings in @BeforeTest
        val response = client.get("/api/readings?site=site_reservoir")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("[]", response.bodyAsText().trim())
    }

    @Test
    fun `GET readings with invalid from date returns 400`() = testApplication {
        application { module() }

        val response = client.get("/api/readings?site=site_upstream&from=not-a-date")
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("from"))
    }

    // ─── GET /api/sites ───────────────────────────────────────────

    @Test
    fun `GET sites returns 200 and contains seeded sites`() = testApplication {
        application { module() }

        val response = client.get("/api/sites")
        assertEquals(HttpStatusCode.OK, response.status)

        val body = response.bodyAsText()
        assertTrue(body.contains("site_upstream"))
        assertTrue(body.contains("site_downstream"))
        assertTrue(body.contains("site_reservoir"))
    }

    // ─── AlertEngine unit tests ───────────────────────────────────

    @Test
    fun `AlertEngine normal reading returns normal status and no alerts`() {
        val data = WaterQualityPayload(
            siteId = "site_upstream", timeStamp = "2025-05-01T08:00:00",
            pH = 7.2, turbidityNtu = 1.0, conductivityPerCm = 200.0,
            waterTempC = 15.0, waterLvlCm = 45.0, lightLux = 500.0
        )
        val result = AlertEngine.evaluateWaterQuality(data)
        assertEquals("normal", result.status)
        assertTrue(result.alerts.isEmpty())
    }

    @Test
    fun `AlertEngine pH below 6_0 returns critical`() {
        val data = WaterQualityPayload(
            siteId = "site_upstream", timeStamp = "2025-05-01T08:00:00",
            pH = 5.5, turbidityNtu = 1.0, conductivityPerCm = 200.0,
            waterTempC = 15.0, waterLvlCm = 45.0, lightLux = 500.0
        )
        val result = AlertEngine.evaluateWaterQuality(data)
        assertEquals("critical", result.status)
        assertTrue(result.alerts.any { it.parameter == "pH" && it.severity == "critical" })
    }

    @Test
    fun `AlertEngine pH between 6_0 and 6_5 returns warning`() {
        val data = WaterQualityPayload(
            siteId = "site_upstream", timeStamp = "2025-05-01T08:00:00",
            pH = 6.3, turbidityNtu = 1.0, conductivityPerCm = 200.0,
            waterTempC = 15.0, waterLvlCm = 45.0, lightLux = 500.0
        )
        val result = AlertEngine.evaluateWaterQuality(data)
        assertEquals("warning", result.status)
    }

    @Test
    fun `AlertEngine turbidity above 10 returns critical`() {
        val data = WaterQualityPayload(
            siteId = "site_upstream", timeStamp = "2025-05-01T08:00:00",
            pH = 7.0, turbidityNtu = 11.0, conductivityPerCm = 200.0,
            waterTempC = 15.0, waterLvlCm = 45.0, lightLux = 500.0
        )
        val result = AlertEngine.evaluateWaterQuality(data)
        assertEquals("critical", result.status)
        assertTrue(result.alerts.any { it.parameter == "turbidity" && it.severity == "critical" })
    }

    @Test
    fun `AlertEngine combined turbidity and conductivity spike adds contamination alert`() {
        val data = WaterQualityPayload(
            siteId = "site_upstream", timeStamp = "2025-05-01T08:00:00",
            pH = 7.0, turbidityNtu = 6.0, conductivityPerCm = 600.0,
            waterTempC = 15.0, waterLvlCm = 45.0, lightLux = 500.0
        )
        val result = AlertEngine.evaluateWaterQuality(data)
        assertTrue(result.alerts.any { it.parameter == "contamination" })
    }
}
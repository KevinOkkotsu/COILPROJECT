package com.example

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

fun Application.configureRouting() {
    routing {

        // serve frontend html pages in static
        staticResources("/static", "static")

        // root will show the dashboard
        get("/") {
            call.respondRedirect("/static/index.html")
        }

        // ─────────────────────────────────────────────
        // POST /api/ingest
        // looks out for a JSON structure for WaterQualityPayload and accepts it
        // runs the alert engine once validated and saves it
        // readings or any triggered alerts get sent to the db
        // ─────────────────────────────────────────────
        post("/api/ingest") {
            // try/catch means if ANYTHING goes wrong we return a clean error message instead of crashing the server
            try {
                // call.receive() reads the JSON body and converts it into kotlin object
                val payload = call.receive<WaterQualityPayload>()

                // server side validation
                // all validation occurs in backend

                // if siteId is empty
                if ( payload.siteId.isBlank() ) {
                    call.respond(HttpStatusCode.BadRequest,mapOf("error" to "siteId is blank. Cannot be blank"))
                    return@post
                }

                // pH must be a real-world value between 0 and 14
                if ( payload.pH < 0.0 || payload.pH > 14.0 ) {
                    call.respond(HttpStatusCode.BadRequest,mapOf("error" to "pH must be between 0.0 and 14.0"))
                    return@post
                }

                // turbidity cannot be negative (no such thing as negative cloudiness)
                if ( payload.turbidityNtu < 0.0 ) {
                    call.respond( HttpStatusCode.BadRequest,mapOf("error" to "turbidityNtu is negative. Cannot be negative.") )
                    return@post
                }

                // conductivity cannot be negative
                if ( payload.conductivityPerCm < 0.0 ) {
                    call.respond(HttpStatusCode.BadRequest,mapOf("error" to "conductivityPerCm is negative. Cannot be negative"))
                    return@post
                }

                // water level cannot be negative
                if ( payload.waterLvlCm < 0.0 ) {
                    call.respond(HttpStatusCode.BadRequest,mapOf("error" to "waterLvlCm is negative. Cannot be negative") )
                    return@post
                }

                // check if site exists in our Sites table
                // this prevents phantom readings from unknown locations
                val siteExists = transaction {
                    // eq cannot be replaced for '==' compares database definitions
                    Sites.selectAll().where { Sites.id eq payload.siteId }.count() > 0
                }
                if (!siteExists) {
                    call.respond(HttpStatusCode.BadRequest,mapOf("error" to "Unknown siteId: ${payload.siteId}"))
                    return@post
                }

                // Parse timeStamp
                // LocalDateTime.parse() throws if the string is not a valid ISO datetime — we catch that below
                val parsedTime = LocalDateTime.parse(payload.timeStamp)

                // Running the Alert Engine
                // returns an EvaluationResult with an overall status string and a list of individual AlertTriggers
                val evaluation = AlertEngine.evaluateWaterQuality(payload)

                // Database transaction structure
                // transaction { } is Exposed's way of grouping DB
                // operations — if one fails, the whole block is reverted
                // can never have partially complete transactions
                transaction {
                    // insert the sensor reading row
                    // get back the auto-generated integer ID for the AlertsLog foreign key
                    val insertedReadingId = WaterQualityReadings.insert {
                        it[siteId]           = payload.siteId
                        it[timeStamp]        = parsedTime
                        it[pH]               = payload.pH
                        it[turbidityNtu]     = payload.turbidityNtu
                        it[conductivityPerCm]= payload.conductivityPerCm
                        it[waterTempC]       = payload.waterTempC
                        it[waterLvlCm]       = payload.waterLvlCm
                        it[lightLux]         = payload.lightLux
                        it[status]           = evaluation.status
                    } get WaterQualityReadings.id

                    // for each alert the engine raised, facilitate a row to AlertsLog
                    // forEach is like a for-loop but written as a lambda
                    evaluation.alerts.forEach { alert ->
                        AlertsLog.insert {
                            it[readingId]  = insertedReadingId
                            it[siteId]     = payload.siteId
                            it[parameter]  = alert.parameter
                            it[severity]   = alert.severity
                            it[message]    = alert.message
                            it[timeStamp]  = parsedTime
                        }
                    }
                }

                // 201 Created = standard HTTP response for "new resource saved"
                call.respond(HttpStatusCode.Created,mapOf("status" to "saved", "derived_status" to evaluation.status))

            } catch (e: Exception) {
                // catches: bad JSON shape, unparseable timestamp, DB errors, etc.
                // we never expose the raw exception message to the client (security)
                call.respond(HttpStatusCode.BadRequest,mapOf("error" to "Invalid payload"))
            }
        }

        // ─────────────────────────────────────────────
        // GET /api/alerts
        // returns the last 50 alerts.
        // both query parameters are optional filters.
        // ─────────────────────────────────────────────
        get("/api/alerts") {
            // call.request.queryParameters["site"] returns null if not provided
            val siteFilter = call.request.queryParameters["site"]
            val severityFilter = call.request.queryParameters["severity"]

            val alerts = transaction {
                // start with all rows, then progressively narrow down
                // using Exposed's query
                var query = AlertsLog.selectAll()

                // .andWhere adds an extra SQL WHERE condition only if the client wanted that filter
                if (siteFilter != null) {
                    query = query.andWhere { AlertsLog.siteId eq siteFilter }
                }
                if (severityFilter != null) {
                    query = query.andWhere { AlertsLog.severity eq severityFilter }
                }

                // preparing database data from backend to move to frontend
                // orders alerts from newest (top) to oldest (bottom)
                // chooses the first 50
                query
                    .orderBy(AlertsLog.timeStamp to SortOrder.DESC)
                    .limit(50)
                    .map { row ->
                        AlertDTO(
                            id        = row[AlertsLog.id],
                            siteId    = row[AlertsLog.siteId],
                            parameter = row[AlertsLog.parameter],
                            severity  = row[AlertsLog.severity],
                            message   = row[AlertsLog.message],
                            timeStamp = row[AlertsLog.timeStamp].toString()
                        )
                    }
            }

            // returns [] (empty JSON array) if no alerts match — not an error
            call.respond(alerts)
        }

        // ─────────────────────────────────────────────
        // GET /api/readings
        // returns readings for a given site, optionally.
        // filtered by a date-time range.
        // ─────────────────────────────────────────────
        get("/api/readings") {
            val siteParameter = call.request.queryParameters["site"]
            val fromParameter = call.request.queryParameters["from"]
            val toParameter = call.request.queryParameters["to"]

            // site is required for this endpoint
            if ( siteParameter == null ) {
                call.respond(HttpStatusCode.BadRequest,mapOf("error" to "Required query parameter missing: site"))
                return@get
            }

            // check the site exists before querying readings
            val siteExists = transaction {
                // at least one site exists
                Sites.selectAll().where { Sites.id eq siteParameter }.count() > 0
            }
            if (!siteExists) {
                call.respond(HttpStatusCode.NotFound,mapOf("error" to "No site found: $siteParameter"))
                return@get
            }

            // Parse optional date range — if the format is wrong, return a clear error
            // if fromTime has an invalid format
            val fromTime = try {
                if ( fromParameter != null ) {
                    LocalDateTime.parse(fromParameter)
                } else null
            }
            catch ( e: Exception ) {
                call.respond(HttpStatusCode.BadRequest,mapOf("error" to "Invalid 'from' datetime format"))
                return@get
            }
            // if toTime has an invalid format
            val toTime = try {
                if ( toParameter != null ) {
                    LocalDateTime.parse(toParameter)
                } else null }
            catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid 'to' datetime format"))
                return@get
            }


            val readings = transaction {
                // grabs all the readings for specific site
                var query = WaterQualityReadings.selectAll()
                    .andWhere { WaterQualityReadings.siteId eq siteParameter }
                // grabs all readings after specific time
                if ( fromTime != null ) {
                    query = query.andWhere { WaterQualityReadings.timeStamp greaterEq fromTime }
                }
                // grabs all readings before specific time
                if ( toTime != null ) {
                    query = query.andWhere { WaterQualityReadings.timeStamp lessEq toTime }
                }

                // now mapping to ReadingDTO instead of mapOf — kotlinx.serialization
                // knows how to handle this because of the @Serializable annotation
                query.orderBy(WaterQualityReadings.timeStamp to SortOrder.ASC).map { row ->
                    ReadingDTO(
                        id                = row[WaterQualityReadings.id],
                        siteId            = row[WaterQualityReadings.siteId],
                        timeStamp         = row[WaterQualityReadings.timeStamp].toString(),
                        pH                = row[WaterQualityReadings.pH],
                        turbidityNtu      = row[WaterQualityReadings.turbidityNtu],
                        conductivityPerCm = row[WaterQualityReadings.conductivityPerCm],
                        waterTempC        = row[WaterQualityReadings.waterTempC],
                        waterLvlCm        = row[WaterQualityReadings.waterLvlCm],
                        lightLux          = row[WaterQualityReadings.lightLux],
                        status            = row[WaterQualityReadings.status]
                    )
                }
            }

            call.respond(readings)
        }

        // ─────────────────────────────────────────────
        // GET /api/sites
        // returns all registered monitoring sites.
        // ─────────────────────────────────────────────
        get("/api/sites") {
            val sites = transaction {
                Sites.selectAll().map { row ->
                    mapOf(
                        "id"          to row[Sites.id],
                        "description" to row[Sites.description]
                    )
                }
            }
            call.respond(sites)
        }
    }
}

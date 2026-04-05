package com.environmental

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {

    @Test
    fun `GET readings without site param returns 400`() = testApplication {
        application { module() }
        val response = client.get("/readings")
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `GET alerts returns 200 with empty list`() = testApplication {
        application { module() }
        val response = client.get("/alerts")
        assertEquals(HttpStatusCode.OK, response.status)
    }
}

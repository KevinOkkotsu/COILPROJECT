package com.environmental.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Registers routes for sensor reading operations.
 *
 * GET /readings?site={siteId}&from={timestamp}&to={timestamp}
 *   Returns all sensor readings for the given site within the specified date range.
 *   Query params:
 *     - site (required): ID of the site to query
 *     - from (optional): ISO 8601 start timestamp (inclusive)
 *     - to   (optional): ISO 8601 end timestamp (inclusive)
 *   Responses:
 *     200 OK           — list of SensorReading objects
 *     400 Bad Request  — missing or invalid query parameters
 *     404 Not Found    — site ID does not exist
 */
fun Routing.readingRoutes() {
    get("/readings") {
        val siteId = call.request.queryParameters["site"]

        if (siteId.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing required query parameter: site"))
            return@get
        }

        // TODO: call ReadingService to fetch and return filtered readings
        call.respond(HttpStatusCode.OK, emptyList<Any>())
    }
}

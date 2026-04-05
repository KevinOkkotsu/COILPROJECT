package com.environmental.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Registers routes for alert event operations.
 *
 * GET /alerts?site={siteId}&severity={severity}
 *   Returns all alert events, optionally filtered by site and/or severity.
 *   Query params:
 *     - site     (optional): ID of the site to filter by
 *     - severity (optional): Severity level to filter by — NORMAL | WARNING | CRITICAL
 *   Responses:
 *     200 OK           — list of AlertEvent objects (empty list if none match)
 *     400 Bad Request  — invalid severity value
 */
fun Routing.alertRoutes() {
    get("/alerts") {
        // TODO: call AlertService to fetch and return filtered alert events
        call.respond(HttpStatusCode.OK, emptyList<Any>())
    }
}

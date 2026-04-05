package com.environmental

import com.environmental.routes.alertRoutes
import com.environmental.routes.readingRoutes
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.routing.*

/**
 * Registers all application routes.
 * Static frontend assets are served from /static.
 * API routes are mounted under their respective paths.
 */
fun Application.configureRouting() {
    routing {
        // Serve frontend static files (dashboard, trends, alerts, portal)
        staticResources("/", "static")

        // API routes
        readingRoutes()
        alertRoutes()
    }
}

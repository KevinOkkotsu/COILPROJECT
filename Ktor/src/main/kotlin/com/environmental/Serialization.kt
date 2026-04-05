package com.environmental

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*

/**
 * Configures JSON serialisation for all API responses.
 * Uses kotlinx.serialization under the hood.
 */
fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json()
    }
}

package com.environmental.database

import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Configures and initialises the application database connection.
 * Uses H2 in-memory for development/testing; swap the connection string for production.
 */
fun Application.configureDatabases() {
    val database = Database.connect(
        url = "jdbc:h2:mem:environmental;DB_CLOSE_DELAY=-1",
        user = "root",
        driver = "org.h2.Driver",
        password = ""
    )

    transaction(database) {
        SchemaUtils.create(
            SensorReadingTable,
            AlertEventTable,
            SiteTable
        )
    }
}

package com.environmental.database

import com.environmental.models.AlertEvent
import com.environmental.models.AlertSeverity
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

/**
 * Exposed table definition for alert events.
 */
object AlertEventTable : Table("alert_events") {
    val id           = integer("id").autoIncrement()
    val siteId       = varchar("site_id", 64)
    val sensorType   = varchar("sensor_type", 64)
    val readingValue = double("reading_value")
    val severity     = enumerationByName("severity", 16, AlertSeverity::class)
    val message      = text("message")
    val timestamp    = varchar("timestamp", 64)
    val resolved     = bool("resolved").default(false)

    override val primaryKey = PrimaryKey(id)
}

/**
 * Data access service for [AlertEvent] persistence.
 *
 * @property database The Exposed [Database] instance to run queries against.
 */
class AlertEventService(private val database: Database) {

    /**
     * Persists a triggered alert event.
     *
     * @param event The alert event to store.
     * @return The auto-generated ID of the newly inserted row.
     */
    suspend fun create(event: AlertEvent): Int = dbQuery {
        AlertEventTable.insert {
            it[siteId]       = event.siteId
            it[sensorType]   = event.sensorType
            it[readingValue] = event.readingValue
            it[severity]     = event.severity
            it[message]      = event.message
            it[timestamp]    = event.timestamp
            it[resolved]     = event.resolved
        }[AlertEventTable.id]
    }

    /**
     * Retrieves alert events, optionally filtered by site and/or severity.
     *
     * @param siteId   Optional site ID to filter by.
     * @param severity Optional severity level to filter by.
     * @return List of matching [AlertEvent] objects. Returns empty list if none found.
     */
    suspend fun find(siteId: String? = null, severity: AlertSeverity? = null): List<AlertEvent> = dbQuery {
        AlertEventTable
            .selectAll()
            .apply {
                if (siteId != null) andWhere { AlertEventTable.siteId eq siteId }
                if (severity != null) andWhere { AlertEventTable.severity eq severity }
            }
            .map { row ->
                AlertEvent(
                    id           = row[AlertEventTable.id],
                    siteId       = row[AlertEventTable.siteId],
                    sensorType   = row[AlertEventTable.sensorType],
                    readingValue = row[AlertEventTable.readingValue],
                    severity     = row[AlertEventTable.severity],
                    message      = row[AlertEventTable.message],
                    timestamp    = row[AlertEventTable.timestamp],
                    resolved     = row[AlertEventTable.resolved]
                )
            }
    }

    private suspend fun <T> dbQuery(block: () -> T): T =
        newSuspendedTransaction(Dispatchers.IO, database) { block() }
}

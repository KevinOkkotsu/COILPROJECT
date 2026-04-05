package com.environmental.database

import com.environmental.models.AlertSeverity
import com.environmental.models.SensorReading
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

/**
 * Exposed table definition for sensor readings.
 */
object SensorReadingTable : Table("sensor_readings") {
    val id         = integer("id").autoIncrement()
    val siteId     = varchar("site_id", 64)
    val sensorType = varchar("sensor_type", 64)
    val value      = double("value")
    val unit       = varchar("unit", 32)
    val timestamp  = varchar("timestamp", 64)
    val severity   = enumerationByName("severity", 16, AlertSeverity::class)

    override val primaryKey = PrimaryKey(id)
}

/**
 * Data access service for [SensorReading] persistence.
 *
 * @property database The Exposed [Database] instance to run queries against.
 */
class SensorReadingService(private val database: Database) {

    /**
     * Inserts a new sensor reading into the database.
     *
     * @param reading The reading to persist.
     * @return The auto-generated ID of the newly inserted row.
     */
    suspend fun create(reading: SensorReading): Int = dbQuery {
        SensorReadingTable.insert {
            it[siteId]     = reading.siteId
            it[sensorType] = reading.sensorType
            it[value]      = reading.value
            it[unit]       = reading.unit
            it[timestamp]  = reading.timestamp
            it[severity]   = reading.severity
        }[SensorReadingTable.id]
    }

    /**
     * Retrieves all readings for a given site, optionally filtered by date range.
     *
     * @param siteId The site to query.
     * @param from   Optional ISO 8601 start timestamp (inclusive).
     * @param to     Optional ISO 8601 end timestamp (inclusive).
     * @return List of [SensorReading] matching the criteria.
     */
    suspend fun findBySite(siteId: String, from: String? = null, to: String? = null): List<SensorReading> = dbQuery {
        SensorReadingTable
            .selectAll()
            .where { SensorReadingTable.siteId eq siteId }
            .map { row ->
                SensorReading(
                    id         = row[SensorReadingTable.id],
                    siteId     = row[SensorReadingTable.siteId],
                    sensorType = row[SensorReadingTable.sensorType],
                    value      = row[SensorReadingTable.value],
                    unit       = row[SensorReadingTable.unit],
                    timestamp  = row[SensorReadingTable.timestamp],
                    severity   = row[SensorReadingTable.severity]
                )
            }
    }

    private suspend fun <T> dbQuery(block: () -> T): T =
        newSuspendedTransaction(Dispatchers.IO, database) { block() }
}

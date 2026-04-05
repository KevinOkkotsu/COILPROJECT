package com.environmental.models

import kotlinx.serialization.Serializable

/**
 * Represents a single sensor reading from a monitoring site.
 *
 * @property id Unique identifier for this reading.
 * @property siteId ID of the site this reading belongs to.
 * @property sensorType Type of sensor (e.g. "soil_moisture", "water_level", "air_quality").
 * @property value The numeric sensor reading.
 * @property unit Unit of measurement (e.g. "mm", "%", "ppm").
 * @property timestamp ISO 8601 timestamp of when the reading was recorded.
 * @property severity Evaluated severity at the time of reading.
 */
@Serializable
data class SensorReading(
    val id: Int = 0,
    val siteId: String,
    val sensorType: String,
    val value: Double,
    val unit: String,
    val timestamp: String,
    val severity: AlertSeverity = AlertSeverity.NORMAL
)

package com.environmental.models

import kotlinx.serialization.Serializable

/**
 * Represents a triggered alert event logged by the alert engine.
 *
 * @property id Unique identifier for this alert event.
 * @property siteId ID of the site where the alert was triggered.
 * @property sensorType The sensor type that triggered the alert.
 * @property readingValue The value that caused the alert to fire.
 * @property severity The severity level assigned to this alert.
 * @property message Plain-language explanation of the alert (shown to end users).
 * @property timestamp ISO 8601 timestamp of when the alert was triggered.
 * @property resolved Whether the alert has been resolved.
 */
@Serializable
data class AlertEvent(
    val id: Int = 0,
    val siteId: String,
    val sensorType: String,
    val readingValue: Double,
    val severity: AlertSeverity,
    val message: String,
    val timestamp: String,
    val resolved: Boolean = false
)

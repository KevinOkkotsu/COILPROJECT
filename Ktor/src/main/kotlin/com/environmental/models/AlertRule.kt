package com.environmental.models

import kotlinx.serialization.Serializable

/**
 * Defines a threshold rule that triggers an alert for a specific sensor type.
 *
 * @property id Unique identifier for this rule.
 * @property sensorType The sensor type this rule applies to (e.g. "soil_moisture").
 * @property warningThreshold Value at or above which a WARNING alert is raised.
 * @property criticalThreshold Value at or above which a CRITICAL alert is raised.
 * @property unit Unit of measurement for the thresholds.
 * @property description Plain-language description of what this rule monitors.
 */
@Serializable
data class AlertRule(
    val id: Int = 0,
    val sensorType: String,
    val warningThreshold: Double,
    val criticalThreshold: Double,
    val unit: String,
    val description: String
)

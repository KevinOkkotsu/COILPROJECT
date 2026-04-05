package com.environmental.services

import com.environmental.models.AlertRule
import com.environmental.models.AlertSeverity
import com.environmental.models.SensorReading

/**
 * Evaluates sensor readings against defined alert rules and produces alert events.
 * This is a pure, stateless service — no DB access; all I/O handled by the caller.
 */
class AlertEngine {

    /**
     * Evaluates a single sensor reading against a list of alert rules.
     *
     * @param reading The sensor reading to evaluate.
     * @param rules The list of rules applicable to the reading's sensor type.
     * @return The [AlertSeverity] assigned based on the reading value and matching rule.
     *         Returns [AlertSeverity.NORMAL] if no matching rule exists.
     */
    fun evaluateReading(reading: SensorReading, rules: List<AlertRule>): AlertSeverity {
        val matchingRule = rules.firstOrNull { it.sensorType == reading.sensorType }
            ?: return AlertSeverity.NORMAL

        return when {
            reading.value >= matchingRule.criticalThreshold -> AlertSeverity.CRITICAL
            reading.value >= matchingRule.warningThreshold  -> AlertSeverity.WARNING
            else                                            -> AlertSeverity.NORMAL
        }
    }

    /**
     * Builds a plain-language alert message suitable for display to end users.
     *
     * @param reading The reading that triggered the alert.
     * @param severity The evaluated severity level.
     * @param rule The rule that was matched.
     * @return A human-readable alert message string.
     */
    fun buildAlertMessage(reading: SensorReading, severity: AlertSeverity, rule: AlertRule): String {
        return when (severity) {
            AlertSeverity.WARNING  ->
                "${rule.description} is approaching a critical level: ${reading.value}${reading.unit}. Monitor closely."
            AlertSeverity.CRITICAL ->
                "${rule.description} has exceeded a critical threshold: ${reading.value}${reading.unit}. Immediate action required."
            AlertSeverity.NORMAL   -> ""
        }
    }
}

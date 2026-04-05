package com.environmental.models

/**
 * Severity levels for environmental sensor alerts.
 *
 * @property NORMAL Reading is within safe bounds — no action required.
 * @property WARNING Reading is approaching a critical threshold — monitor closely.
 * @property CRITICAL Reading has exceeded a critical threshold — immediate action required.
 */
enum class AlertSeverity {
    NORMAL,
    WARNING,
    CRITICAL
}

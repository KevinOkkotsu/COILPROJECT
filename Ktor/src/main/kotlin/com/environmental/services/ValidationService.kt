package com.environmental.services

import com.environmental.models.SensorReading

/**
 * Validates incoming sensor reading data before it is stored.
 * All validation is server-side — never trust client input.
 */
object ValidationService {

    private val VALID_SENSOR_TYPES = setOf("soil_moisture", "water_level", "air_quality", "temperature")
    private const val MAX_SITE_ID_LENGTH = 64
    private const val MAX_VALUE = 100_000.0
    private const val MIN_VALUE = -1_000.0

    /**
     * Validates a [SensorReading] before storage.
     *
     * @param reading The reading to validate.
     * @return [ValidationResult.Valid] if the reading passes all checks,
     *         [ValidationResult.Invalid] with a descriptive error message otherwise.
     */
    fun validate(reading: SensorReading): ValidationResult {
        if (reading.siteId.isBlank()) return ValidationResult.Invalid("siteId must not be blank")
        if (reading.siteId.length > MAX_SITE_ID_LENGTH) return ValidationResult.Invalid("siteId exceeds max length of $MAX_SITE_ID_LENGTH")
        if (reading.sensorType !in VALID_SENSOR_TYPES) return ValidationResult.Invalid("Unknown sensorType: ${reading.sensorType}")
        if (reading.value < MIN_VALUE || reading.value > MAX_VALUE) return ValidationResult.Invalid("value ${reading.value} is out of accepted range ($MIN_VALUE–$MAX_VALUE)")
        if (reading.timestamp.isBlank()) return ValidationResult.Invalid("timestamp must not be blank")

        return ValidationResult.Valid
    }

    /** Result type for validation outcomes. */
    sealed class ValidationResult {
        /** The reading passed all validation checks. */
        object Valid : ValidationResult()

        /**
         * The reading failed validation.
         * @property reason Human-readable description of the failure.
         */
        data class Invalid(val reason: String) : ValidationResult()
    }
}

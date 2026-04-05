package com.environmental.models

import kotlinx.serialization.Serializable

/**
 * Represents a monitoring site (e.g. a specific farm or field location).
 *
 * @property id Unique identifier for the site.
 * @property name Human-readable name of the site (e.g. "Hargreaves Farm — North Field").
 * @property location Geographic description or coordinates of the site.
 * @property description Optional additional context about the site.
 */
@Serializable
data class Site(
    val id: String,
    val name: String,
    val location: String,
    val description: String = ""
)

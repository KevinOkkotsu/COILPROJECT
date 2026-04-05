package com.environmental.database

import com.environmental.models.Site
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

/**
 * Exposed table definition for monitoring sites.
 */
object SiteTable : Table("sites") {
    val id          = varchar("id", 64)
    val name        = varchar("name", 128)
    val location    = varchar("location", 256)
    val description = text("description").default("")

    override val primaryKey = PrimaryKey(id)
}

/**
 * Data access service for [Site] persistence.
 *
 * @property database The Exposed [Database] instance to run queries against.
 */
class SiteService(private val database: Database) {

    /**
     * Inserts a new monitoring site.
     *
     * @param site The site to persist.
     * @return The ID of the newly inserted site.
     */
    suspend fun create(site: Site): String = dbQuery {
        SiteTable.insert {
            it[id]          = site.id
            it[name]        = site.name
            it[location]    = site.location
            it[description] = site.description
        }[SiteTable.id]
    }

    /**
     * Retrieves a site by its ID.
     *
     * @param siteId The ID of the site to look up.
     * @return The [Site] if found, or null if no site exists with the given ID.
     */
    suspend fun findById(siteId: String): Site? = dbQuery {
        SiteTable
            .selectAll()
            .where { SiteTable.id eq siteId }
            .singleOrNull()
            ?.let { row ->
                Site(
                    id          = row[SiteTable.id],
                    name        = row[SiteTable.name],
                    location    = row[SiteTable.location],
                    description = row[SiteTable.description]
                )
            }
    }

    /**
     * Returns all registered monitoring sites.
     *
     * @return List of all [Site] objects.
     */
    suspend fun findAll(): List<Site> = dbQuery {
        SiteTable.selectAll().map { row ->
            Site(
                id          = row[SiteTable.id],
                name        = row[SiteTable.name],
                location    = row[SiteTable.location],
                description = row[SiteTable.description]
            )
        }
    }

    private suspend fun <T> dbQuery(block: () -> T): T =
        newSuspendedTransaction(Dispatchers.IO, database) { block() }
}

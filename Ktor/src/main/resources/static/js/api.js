/**
 * api.js — Centralised API client for the Environmental Monitoring Dashboard.
 * All fetch calls to the Ktor backend go through this module.
 */

const BASE_URL = "";

/**
 * Fetches sensor readings for a given site and optional date range.
 * @param {string} siteId
 * @param {string|null} from - ISO 8601 start timestamp
 * @param {string|null} to   - ISO 8601 end timestamp
 * @returns {Promise<Array>}
 */
export async function fetchReadings(siteId, from = null, to = null) {
    const params = new URLSearchParams({ site: siteId });
    if (from) params.append("from", from);
    if (to)   params.append("to", to);
    const res = await fetch(`${BASE_URL}/readings?${params}`);
    if (!res.ok) throw new Error(`Failed to fetch readings: ${res.status}`);
    return res.json();
}

/**
 * Fetches alert events, optionally filtered by site and/or severity.
 * @param {string|null} siteId
 * @param {string|null} severity - "NORMAL" | "WARNING" | "CRITICAL"
 * @returns {Promise<Array>}
 */
export async function fetchAlerts(siteId = null, severity = null) {
    const params = new URLSearchParams();
    if (siteId)   params.append("site", siteId);
    if (severity) params.append("severity", severity);
    const res = await fetch(`${BASE_URL}/alerts?${params}`);
    if (!res.ok) throw new Error(`Failed to fetch alerts: ${res.status}`);
    return res.json();
}

/**
 * Fetches all registered monitoring sites.
 * @returns {Promise<Array>}
 */
export async function fetchSites() {
    const res = await fetch(`${BASE_URL}/sites`);
    if (!res.ok) throw new Error(`Failed to fetch sites: ${res.status}`);
    return res.json();
}

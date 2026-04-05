/**
 * main.js — Shared utilities used across all pages.
 */

import { fetchSites } from "./api.js";

/**
 * Returns the CSS class suffix for a given severity string.
 * @param {string} severity
 * @returns {string}
 */
export function severityClass(severity) {
    return (severity || "NORMAL").toLowerCase();
}

/**
 * Renders a user-friendly error message into a container element.
 * @param {HTMLElement} container
 * @param {string} message
 */
export function showError(container, message) {
    container.innerHTML = `<p class="state-error" role="alert">${message}</p>`;
}

/**
 * Renders an empty-state message into a container element.
 * @param {HTMLElement} container
 * @param {string} message
 */
export function showEmpty(container, message) {
    container.innerHTML = `<p class="state-empty">${message}</p>`;
}

/**
 * Populates a <select> element with site options from the API.
 * @param {HTMLSelectElement} select
 * @returns {Promise<void>}
 */
export async function populateSiteSelector(select) {
    try {
        const sites = await fetchSites();
        select.innerHTML = `<option value="">All sites</option>` +
            sites.map(s => `<option value="${s.id}">${s.name}</option>`).join("");
    } catch {
        select.innerHTML = `<option value="">Failed to load sites</option>`;
    }
}

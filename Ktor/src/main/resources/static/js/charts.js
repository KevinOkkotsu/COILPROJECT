/**
 * charts.js — Chart rendering helpers using Chart.js.
 * All chart instances must be created through these helpers.
 */

/**
 * Renders a time-series line chart into a canvas element.
 * @param {HTMLCanvasElement} canvas
 * @param {string[]} labels
 * @param {number[]} values
 * @param {string} label
 * @param {string} color
 * @returns {Chart}
 */
export function renderTimeSeriesChart(canvas, labels, values, label, color = "#1a6fc4") {
    return new Chart(canvas, {
        type: "line",
        data: {
            labels,
            datasets: [{
                label,
                data: values,
                borderColor: color,
                backgroundColor: color + "22",
                borderWidth: 2,
                pointRadius: 3,
                tension: 0.3,
                fill: true,
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { position: "top" },
                tooltip: { mode: "index", intersect: false }
            },
            scales: {
                x: { ticks: { maxTicksLimit: 10, maxRotation: 30 } },
                y: { beginAtZero: false }
            }
        }
    });
}

/**
 * Maps an array of SensorReading objects to chart-compatible arrays.
 * Sorts ascending by timestamp before mapping.
 * @param {Object[]} readings
 * @returns {{ labels: string[], values: number[] }}
 */
export function mapReadingsToChartData(readings) {
    const sorted = [...readings].sort((a, b) => a.timestamp.localeCompare(b.timestamp));
    return {
        labels: sorted.map(r => r.timestamp),
        values: sorted.map(r => r.value)
    };
}

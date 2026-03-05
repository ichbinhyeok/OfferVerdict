#!/usr/bin/env node
const { createGscMcpClient } = require("./lib/gsc_mcp_client");

function formatDate(date) {
    const y = date.getUTCFullYear();
    const m = String(date.getUTCMonth() + 1).padStart(2, "0");
    const d = String(date.getUTCDate()).padStart(2, "0");
    return `${y}-${m}-${d}`;
}

function parseArgs(argv) {
    const args = {};
    for (const entry of argv) {
        if (!entry.startsWith("--")) {
            continue;
        }
        const [key, value] = entry.slice(2).split("=");
        args[key] = value ?? "true";
    }
    return args;
}

function sumMetrics(rows) {
    return rows.reduce(
        (acc, row) => {
            acc.clicks += row.clicks || 0;
            acc.impressions += row.impressions || 0;
            return acc;
        },
        { clicks: 0, impressions: 0 }
    );
}

function splitTopPagesByHost(items, canonicalHost) {
    const canonicalPrefix = `https://${canonicalHost}`;
    const wwwPrefix = `https://www.${canonicalHost}`;
    const hostSplit = {
        canonical: { rows: 0, impressions: 0, clicks: 0 },
        www: { rows: 0, impressions: 0, clicks: 0 },
        other: { rows: 0, impressions: 0, clicks: 0 }
    };

    for (const row of items) {
        const key = row.key || "";
        const bucket = key.startsWith(canonicalPrefix)
            ? "canonical"
            : key.startsWith(wwwPrefix)
                ? "www"
                : "other";
        hostSplit[bucket].rows += 1;
        hostSplit[bucket].impressions += row.impressions || 0;
        hostSplit[bucket].clicks += row.clicks || 0;
    }
    return hostSplit;
}

async function run() {
    const args = parseArgs(process.argv.slice(2));
    const siteUrl = args.siteUrl || process.env.GSC_SITE_URL || "sc-domain:livingcostcheck.com";
    const targetCountry = (args.country || process.env.GSC_TARGET_COUNTRY || "usa").toLowerCase();
    const canonicalHost = args.canonicalHost || process.env.GSC_CANONICAL_HOST || "livingcostcheck.com";
    const days = Number(args.days || 28);

    const today = new Date();
    const end = args.end
        ? new Date(`${args.end}T00:00:00Z`)
        : new Date(Date.UTC(today.getUTCFullYear(), today.getUTCMonth(), today.getUTCDate() - 1));
    const start = args.start
        ? new Date(`${args.start}T00:00:00Z`)
        : new Date(end.getTime() - (days - 1) * 24 * 60 * 60 * 1000);
    const previousEnd = new Date(start.getTime() - 24 * 60 * 60 * 1000);
    const previousStart = new Date(previousEnd.getTime() - (days - 1) * 24 * 60 * 60 * 1000);

    const client = createGscMcpClient();
    try {
        await client.initialize();

        const periodCurrent = {
            start: formatDate(start),
            end: formatDate(end)
        };
        const periodPrevious = {
            start: formatDate(previousStart),
            end: formatDate(previousEnd)
        };

        const [countryRows, deviceRows, topPages, compare, dateCountryRows] = await Promise.all([
            client.callTool("analytics_query", {
                siteUrl,
                startDate: periodCurrent.start,
                endDate: periodCurrent.end,
                dimensions: ["country"],
                type: "web",
                aggregationType: "byProperty",
                dataState: "all",
                limit: 250,
                format: "json"
            }),
            client.callTool("analytics_query", {
                siteUrl,
                startDate: periodCurrent.start,
                endDate: periodCurrent.end,
                dimensions: ["device"],
                type: "web",
                aggregationType: "byProperty",
                dataState: "all",
                filters: [{ dimension: "country", operator: "equals", expression: targetCountry }],
                limit: 10,
                format: "json"
            }),
            client.callTool("analytics_top_pages", {
                siteUrl,
                days,
                limit: 250,
                sortBy: "impressions"
            }),
            client.callTool("analytics_compare_periods", {
                siteUrl,
                period1Start: periodCurrent.start,
                period1End: periodCurrent.end,
                period2Start: periodPrevious.start,
                period2End: periodPrevious.end
            }),
            client.callTool("analytics_query", {
                siteUrl,
                startDate: periodCurrent.start,
                endDate: periodCurrent.end,
                dimensions: ["date", "country"],
                type: "web",
                aggregationType: "auto",
                dataState: "all",
                filters: [{ dimension: "country", operator: "equals", expression: targetCountry }],
                limit: 2000,
                format: "json"
            })
        ]);

        const targetCountryRow = countryRows.find((row) => (row.keys?.[0] || "").toLowerCase() === targetCountry);
        const totals = sumMetrics(countryRows);
        const topPageItems = topPages.items || [];
        const parameterRows = topPageItems.filter((row) => (row.key || "").includes("?"));
        const parameterSummary = sumMetrics(parameterRows);
        parameterSummary.rows = parameterRows.length;
        const hostSplit = splitTopPagesByHost(topPageItems, canonicalHost);
        const topCountryRows = [...countryRows].sort((a, b) => (b.impressions || 0) - (a.impressions || 0)).slice(0, 10);
        const targetDaily = [...dateCountryRows]
            .sort((a, b) => (a.keys?.[0] || "").localeCompare(b.keys?.[0] || ""))
            .map((row) => ({
                date: row.keys?.[0] || "",
                clicks: row.clicks || 0,
                impressions: row.impressions || 0,
                ctr: row.ctr || 0,
                position: row.position || 0
            }));

        const output = {
            generatedAtUtc: new Date().toISOString(),
            siteUrl,
            periodCurrent,
            periodPrevious,
            targetCountry,
            performance: compare,
            totals,
            targetCountrySummary: targetCountryRow || null,
            topCountries: topCountryRows,
            targetCountryDevices: deviceRows,
            hostSplit,
            parameterUrlSummary: parameterSummary,
            targetCountryDaily: targetDaily,
            topPages: topPageItems.slice(0, 20)
        };

        console.log(JSON.stringify(output, null, 2));
    } finally {
        client.close();
    }
}

run().catch((error) => {
    console.error(error.message);
    process.exit(1);
});

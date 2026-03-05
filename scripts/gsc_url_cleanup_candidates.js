#!/usr/bin/env node
const { createGscMcpClient } = require("./lib/gsc_mcp_client");

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

function toNumber(value, fallback) {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : fallback;
}

function parseSalaryFromPath(url) {
    const match = url.match(/\/salary-check\/(?:[^/]+\/)?[^/]+\/(\d+)(?:$|\?)/);
    if (!match) {
        return null;
    }
    return Number(match[1]);
}

function scoreRow(row) {
    return (row.impressions || 0) * 10 + (row.position && row.position > 0 ? Math.max(0, 20 - row.position) : 0);
}

async function run() {
    const args = parseArgs(process.argv.slice(2));
    const siteUrl = args.siteUrl || process.env.GSC_SITE_URL || "sc-domain:livingcostcheck.com";
    const days = toNumber(args.days, 28);
    const minSalary = toNumber(args.minSalary || process.env.APP_SEO_SALARY_BUCKET_MIN, 30000);
    const maxSalary = toNumber(args.maxSalary || process.env.APP_SEO_SALARY_BUCKET_MAX, 500000);
    const canonicalHost = args.canonicalHost || process.env.GSC_CANONICAL_HOST || "livingcostcheck.com";
    const minImpressions = toNumber(args.minImpressions, 10);

    const client = createGscMcpClient();
    try {
        await client.initialize();
        const topPages = await client.callTool("analytics_top_pages", {
            siteUrl,
            days,
            limit: 250,
            sortBy: "impressions"
        });

        const rows = topPages.items || [];
        const hostMismatch = [];
        const parameterRows = [];
        const outlierSalaryRows = [];
        const lowCtrRows = [];
        const canonicalPrefix = `https://${canonicalHost}/`;
        const wwwPrefix = `https://www.${canonicalHost}/`;

        for (const row of rows) {
            const key = row.key || "";
            const impressions = row.impressions || 0;
            const clicks = row.clicks || 0;

            if (key.startsWith(wwwPrefix)) {
                hostMismatch.push(row);
            }
            if (key.includes("?")) {
                parameterRows.push(row);
            }

            const salary = parseSalaryFromPath(key);
            if (salary !== null && (salary < minSalary || salary > maxSalary)) {
                outlierSalaryRows.push({ ...row, salary });
            }

            if (impressions >= minImpressions && clicks === 0) {
                lowCtrRows.push(row);
            }
        }

        hostMismatch.sort((a, b) => scoreRow(b) - scoreRow(a));
        parameterRows.sort((a, b) => scoreRow(b) - scoreRow(a));
        outlierSalaryRows.sort((a, b) => scoreRow(b) - scoreRow(a));
        lowCtrRows.sort((a, b) => scoreRow(b) - scoreRow(a));

        const output = {
            generatedAtUtc: new Date().toISOString(),
            siteUrl,
            windowDays: days,
            seoSalaryBounds: { minSalary, maxSalary },
            rowCount: rows.length,
            recommendedRules: [
                {
                    name: "Canonical host redirect",
                    when: `URL starts with ${wwwPrefix}`,
                    action: `301 to ${canonicalPrefix}`
                },
                {
                    name: "Parameter URL de-indexing",
                    when: "Comparison/salary-check URL has query parameters",
                    action: "noindex and keep canonical to clean path"
                },
                {
                    name: "Out-of-range salary cleanup",
                    when: `Salary path segment < ${minSalary} or > ${maxSalary}`,
                    action: "301 to nearest in-range salary bucket"
                }
            ],
            hostMismatchTop: hostMismatch.slice(0, 20),
            parameterUrlTop: parameterRows.slice(0, 20),
            outlierSalaryTop: outlierSalaryRows.slice(0, 20),
            lowCtrCandidatesTop: lowCtrRows.slice(0, 30)
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

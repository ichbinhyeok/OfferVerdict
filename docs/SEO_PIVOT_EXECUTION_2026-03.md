# SEO Pivot Execution (March 2026)

## Goal
- Shift decision-making from global mixed traffic to US intent traffic.
- Reduce crawl/index noise from host split, parameter URLs, and out-of-range salary URLs.
- Improve CTR with clearer "take-home + cost-of-living" SERP messaging.

## Implemented In Code
1. Canonical host redirect (`www`/scheme normalization):
   - `src/main/java/com/offerverdict/config/CanonicalHostRedirectFilter.java`
2. Out-of-range salary URL cleanup (301 to bounded canonical salary):
   - `src/main/java/com/offerverdict/controller/SingleCityController.java`
   - `src/main/java/com/offerverdict/controller/ComparisonController.java`
3. Parameter comparison pages forced to `noindex`:
   - `src/main/java/com/offerverdict/controller/ComparisonController.java`
4. Single-city and comparison title/meta intent refresh:
   - `src/main/java/com/offerverdict/controller/SingleCityController.java`
   - `src/main/java/com/offerverdict/controller/ComparisonController.java`

## Weekly KPI Runbook (Search Console MCP)

### Prerequisites
- `GOOGLE_APPLICATION_CREDENTIALS` must point to a valid service-account key.
- `SEARCH_CONSOLE_MCP_ENTRY` optional; if omitted, script tries global npm path.

### 1) US KPI dashboard snapshot
```bash
node scripts/gsc_us_kpi_dashboard.js --siteUrl=sc-domain:livingcostcheck.com --country=usa --days=28 > docs/data/gsc_us_kpi_latest.json
```

### 2) URL cleanup candidate extraction
```bash
node scripts/gsc_url_cleanup_candidates.js --siteUrl=sc-domain:livingcostcheck.com --days=28 --minSalary=30000 --maxSalary=500000 > docs/data/gsc_url_cleanup_latest.json
```

## KPI Definitions (Primary)
1. US clicks (28d)
2. US impressions (28d)
3. US CTR (28d)
4. Host split (`www` vs canonical) impressions
5. Parameter URL impressions and clicks
6. Lead submit rate on top US landing pages (GA4/lead events)

## URL Cleanup Rules
1. Host canonicalization
- Condition: request host is `www.livingcostcheck.com`
- Action: `301` to `https://livingcostcheck.com`

2. Single-city outlier salary
- Condition: salary path segment outside `[30000, 500000]`
- Action: `301` to nearest bounded aligned salary path

3. Comparison page with explicit salary query parameters
- Condition: `currentSalary`/`offerSalary` query present
- Action: keep functional page, but force `noindex` to protect canonical clean path indexing

4. Comparison outlier salaries
- Condition: annualized explicit salary query outside `[30000, 500000]`
- Action: `301` to bounded salary query values

## Operation Loop
1. Run both scripts weekly.
2. Append summary numbers to `docs/TRACKING_LOG.md`.
3. If host mismatch or parameter impressions rise for 2 consecutive weeks, prioritize cleanup fixes before content expansion.
4. Expand content only for clusters that show US clicks + lead events, not raw impressions.

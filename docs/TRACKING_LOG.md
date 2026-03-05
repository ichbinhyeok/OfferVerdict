# Tracking Log

Purpose: persistent cross-session log for date-based analysis and implemented improvements.

## Entry Template
- Date (UTC):
- Analysis Window:
- Baseline Window:
- Analysis:
- Improvements Implemented:
- Verification:
- Next Actions:

---

## 2026-03-05 (UTC)

- Analysis Window:
  - 2026-02-02 to 2026-03-02
- Baseline Window:
  - 2026-01-05 to 2026-02-01

- Analysis:
  - Search Console property: `sc-domain:livingcostcheck.com`
  - Clicks: `1` (prev `10`, `-90%`)
  - Impressions: `513` (prev `2095`, `-75.51%`)
  - CTR: `0.195%` (prev `0.477%`)
  - Avg position: `4.48` (prev `15.14`, ranking improved while traffic dropped)
  - Interpretation:
    - Some ranking signal exists, but traffic and conversion signal are weak.
    - Likely intent mismatch between indexed pages and real user demand.

- Improvements Implemented:
  - Added canonical host redirect filter (`www/non-www` + scheme canonicalization for GET/HEAD):
    - `src/main/java/com/offerverdict/config/CanonicalHostRedirectFilter.java`
  - Added app toggle:
    - `app.enforceCanonicalHostRedirect` in:
      - `src/main/java/com/offerverdict/config/AppProperties.java`
      - `src/main/resources/application.yml`
  - Added explicit analysis date context:
    - `analysisDateUtc` in comparison and single-city flows
    - `dataModifiedDate` in comparison flow
  - Added analysis-date visibility in UI:
    - `src/main/resources/templates/result.html`
    - `src/main/resources/templates/single-verdict.html`
  - Added regression tests:
    - `src/test/java/com/offerverdict/config/CanonicalHostRedirectFilterTest.java`

- Verification:
  - Command:
    - `./gradlew test --tests com.offerverdict.config.CanonicalHostRedirectFilterTest --tests com.offerverdict.controller.SitemapControllerTest --no-daemon`
  - Result: PASS

- Next Actions:
  1. Commit and deploy current canonical/date improvements.
  2. Pivot content strategy from broad permutation pSEO to decision-intent pages.
  3. Track weekly: clicks, non-brand CTR, and lead conversion on top landing pages.

---

## 2026-03-05 (UTC) - Deep GSC Drill-Down

- Analysis Window:
  - 2026-02-02 to 2026-03-02
- Baseline Window:
  - 2026-01-05 to 2026-02-01

- Analysis:
  - Core compare (same as summary):
    - Clicks: `1` vs `10` (`-90%`)
    - Impressions: `513` vs `2095` (`-75.51%`)
    - CTR: `0.195%` vs `0.477%`
    - Avg position: `4.48` vs `15.14` (improved while traffic dropped)
  - Country split (window 2026-02-02 to 2026-03-02):
    - USA: `147` impressions, `1` click, avg position `9.27`
    - KOR: `309` impressions, `0` clicks, avg position `1.23`
  - Country split (baseline 2026-01-05 to 2026-02-01):
    - USA: `1427` impressions, `7` clicks
    - KOR: `13` impressions, `3` clicks
  - Date-country spike detail:
    - 2026-02-24 (KOR): `218` impressions, `0` clicks
    - 2026-02-28 (KOR): `86` impressions, `0` clicks
  - Device split (window 2026-02-02 to 2026-03-02):
    - MOBILE: `83` impressions, `1` click, avg position `8.98`
    - DESKTOP: `428` impressions, `0` clicks, avg position `3.57`
  - Page distribution (top pages report, limit 250):
    - Rows: `216`
    - `www` host rows: `80` (`603` impressions)
    - non-`www` rows: `136` (`1797` impressions)
    - URL parameter rows: `12` (`96` impressions, `0` clicks)

- Interpretation:
  - Traffic drop is not explained by sandbox effect alone.
  - US demand exposure dropped sharply; recent impressions are dominated by low-click KOR spikes.
  - Ranking improvement is concentrated on low-value or low-intent pages (high impressions, near-zero clicks).
  - Canonical host split (`www` + non-`www`) and long-tail URL surface still add noise.

- Improvements Recommended:
  1. Deploy canonical host redirect patch immediately.
  2. Add a US-only KPI view to avoid decision bias from non-target-country spikes.
  3. Keep seed sitemap strategy, but narrow to intent clusters with measurable lead potential.
  4. Prune/redirect outlier salary URLs outside SEO range instead of passive noindex-only handling.
  5. Rewrite top landing page title/meta for direct query intent match.

- Verification:
  - `sites_health_check` result: sitemap healthy, no current sitemap errors/warnings.
  - Latest downloaded sitemap: `2026-02-28T15:01:46.118Z`.

---

## 2026-03-05 (UTC) - Implementation Batch (Pivot Execution)

- Analysis Window:
  - 2026-02-02 to 2026-03-02
- Baseline Window:
  - 2026-01-05 to 2026-02-01

- Analysis:
  - Primary CTR and clicks remain weak despite improved average ranking.
  - High-noise index surface identified from:
    - explicit salary parameter URLs on comparison pages
    - out-of-range salary paths
    - host split (`www` vs non-`www`)

- Improvements Implemented:
  - Added SEO URL policy utility:
    - `src/main/java/com/offerverdict/seo/SeoUrlPolicy.java`
  - Added redirect cleanup for out-of-range salary pages:
    - `SingleCityController`: 301 to bounded aligned salary path
    - `ComparisonController`: 301 to bounded annualized salary query values when explicit salaries are out of SEO range
  - Strengthened index hygiene:
    - `ComparisonController`: explicit salary query pages now forced to `noindex`
  - Updated title/meta strategy for higher intent clarity:
    - `SingleCityController` title/description templates
    - `ComparisonController` title/description templates
  - Added Search Console operations scripts:
    - `scripts/gsc_us_kpi_dashboard.js`
    - `scripts/gsc_url_cleanup_candidates.js`
    - `scripts/lib/gsc_mcp_client.js`
  - Added operator playbook:
    - `docs/SEO_PIVOT_EXECUTION_2026-03.md`

- Verification:
  - Added unit tests for SEO URL policy:
    - `src/test/java/com/offerverdict/seo/SeoUrlPolicyTest.java`
  - Commands:
    - `./gradlew test --tests com.offerverdict.config.CanonicalHostRedirectFilterTest --tests com.offerverdict.controller.SitemapControllerTest --tests com.offerverdict.seo.SeoUrlPolicyTest --no-daemon`
    - `./gradlew test --tests com.offerverdict.service.ComparisonServiceTest --no-daemon`
  - Result: PASS

- Next Actions:
  1. Run targeted tests and full regression.
  2. Deploy and monitor US-only KPI dashboard weekly.
  3. If parameter URL impressions remain high for 2+ weeks, harden query-param normalization rules.

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

---

## 2026-03-22 (UTC)

- Analysis Window:
  - 2026-02-21 to 2026-03-20
- Baseline Window:
  - 2026-01-24 to 2026-02-20

- Analysis:
  - Search Console property: `sc-domain:livingcostcheck.com`
  - Clicks: `0` (prev `5`, `-100%`)
  - Impressions: `347` (prev `1437`, `-75.85%`)
  - CTR: `0%` (prev `0.3479%`)
  - Avg position: `1.68` (prev `11.11`, improved while traffic worsened)
  - Country split (last 28d):
    - KOR: `330` impressions, `0` clicks, avg position `1.22`
    - USA: `14` impressions, `0` clicks, avg position `9.71`
  - Operator note:
    - KOR impressions were later confirmed to be diagnostic/self-check noise and should not be treated as target-market demand.
    - The decision signal for this session is the US slice, which is still effectively zero-demand.
  - Technical findings:
    - Home-prefill query URLs such as `/?mode=compare&city1=...&job=...&salary=...` were indexable because they rendered the homepage without canonical/noindex controls.
    - This query surface was being generated internally from single-city CTA links.
    - Search Console inspection confirmed canonical comparison pages are indexable, while parameterized comparison pages remain mixed (`alternate canonical`, `blocked by robots`, or `unknown`) depending on the path.
    - Search Console reported an older `Unparsable structured data` error on at least one single-city page crawl; current source now renders JSON-LD via server-side serialization to avoid inline-template drift.
    - Sitemap remains healthy and downloadable, but Search Console sitemap coverage numbers still look inconsistent with inspected URL state.
    - Internal-link equity was being wasted in multiple places:
      - comparison pages linked to generic single-city URLs that are intentionally `noindex`
      - single-city pages linked back to hash-based compare URLs or `/cities` routes that are not suitable crawl targets
      - footer/home links still pointed at `noindex` directory surfaces
    - The site had no real crawlable hub layer between the homepage and deep salary/comparison URLs, so internal authority had few stable mid-level destinations.
  - Interpretation:
    - This is not a healthy recovery state. US-target organic demand is effectively absent in the current window.
    - Ranking improvements are still dominated by low-value or non-target traffic, not by meaningful US search demand.
    - The remaining technical leakage was materially contributing noise to indexing and should be closed before any further content expansion.
    - Strategy conclusion for this session:
      - keep the calculation engine
      - prune broad permutation SEO
      - concentrate crawl/index signals around decision-intent pages, high-value comparison URLs, and a small number of major-job hubs

- Implementation Batch A - Cleanup And Canonical Control:
  - Closed the root compare-prefill indexing leak:
    - `SingleCityController`: compare CTA no longer emits crawlable `/?mode=compare...` URLs.
    - `ComparisonController`: legacy `/?mode=compare&city1=...&job=...&salary=...` now `301` redirects to `/start#...`.
    - `index.html`: compare prefill reads `window.location.hash` first, with query fallback retained for UX.
  - Hardened homepage SEO controls:
    - `ComparisonController`: homepage now sets explicit `canonicalUrl` and `shouldIndex`.
    - `index.html`: canonical tag plus conditional `noindex` added for non-root query variants.
  - Hardened single-city structured data output:
    - `SingleCityController`: JSON-LD now assembled server-side and serialized with Jackson.
    - `single-verdict.html`: JSON-LD script now uses `structuredDataJson` directly.
  - Regression coverage added:
    - `src/test/java/com/offerverdict/controller/SeoRegressionIntegrationTest.java`

- Implementation Batch B - Decision-Intent Pivot Surface:
  - Reframed the root experience around offer evaluation rather than generic salary browsing:
    - homepage title/meta updated in `ComparisonController`
    - hero copy and supporting navigation updated in `index.html`
  - Added decision-intent landing layer:
    - `/should-i-take-this-offer`
    - `/job-offer-comparison-calculator`
    - `/relocation-salary-calculator`
    - `/is-this-salary-enough`
  - Template strategy:
    - route handling in `LandingController`
    - shared landing template in `offer-decision.html`
  - Sitemap strategy:
    - generic single-city salary URLs removed from sitemap
    - decision-intent landings added to sitemap

- Implementation Batch C - Pruning Rules And Link-Graph Repair:
  - Tightened index eligibility:
    - `ComparisonController`: comparison pages are now indexable only when:
      - job is `major`
      - both cities have `priority <= 2`
      - URL is canonical and not an explicit salary-param variant
    - `SingleCityController`: job-specific salary pages are now indexable only when:
      - job is `major`
      - city has `priority <= 2`
      - salary stays inside SEO salary boundaries
  - Tightened related-link generation:
    - `ComparisonService`: related comparison links now only surface `major` jobs
    - `SingleCityController`: related city/job blocks now skip low-priority cities and non-major jobs
  - Re-routed internal authority toward crawlable, indexable destinations:
    - `result.html` now links "Analyze city alone" to job-specific salary pages when indexable; otherwise it falls back to crawlable hubs/landings instead of generic city-only `noindex` URLs
    - `single-verdict.html` now links its main relocation CTA to a real canonical comparison URL when possible, instead of `#mode=compare` hash targets
    - `single-verdict.html` breadcrumb no longer points to `/cities`; it now points to a crawlable job guide or relocation guide destination
    - homepage/footer high-visibility links to `noindex` directory pages were replaced with crawlable decision or job-guide URLs
  - Introduced first crawlable hub layer:
    - `HubController` + `job-directory.html` now expose canonical major-job hubs
    - hubs use benchmark-driven salary entry points rather than hardcoded `$100,000` links
    - hubs include role context plus salary-check and comparison seed links
    - `SitemapController` now includes core job hubs:
      - `/job/software-engineer`
      - `/job/registered-nurse`
      - `/job/product-manager`

- Implementation Batch D - Role Positioning Expansion Beyond Tech:
  - Adjusted positioning to reduce over-reliance on developer/tech intent:
    - homepage and decision landings now surface role guides for non-tech users first, including:
      - `/job/registered-nurse`
      - `/job/accountant`
      - `/job/teacher`
      - `/job/project-manager`
      - `/job/marketing-manager`
      - `/job/pharmacist`
    - tech remains present, but no longer dominates the visible role-entry layer
  - Expanded crawlable hub coverage:
    - `SitemapController` now emits additional major-job hubs for non-tech roles so Google can discover and evaluate them directly
  - Rationale:
    - the product should not assume developers are the main audience for offer-decision tools
    - non-tech roles with relocation, credential, or salary-band tradeoffs may fit the decision workflow better than a developer audience that often defaults to general AI chat tools

- Implementation Batch E - Role Guide Refactor:
  - Refactored role-positioning data into a reusable service layer:
    - `RoleGuideService` now owns featured-role ordering, summaries, decision angles, and role-specific checklists
    - homepage, decision landings, and job hubs now consume the same role-guide source instead of duplicating role lists and copy
  - Increased job-hub usefulness beyond link aggregation:
    - job hubs now surface role-specific decision framing, a short rationale for why people compare that role across cities, and a checklist of what to validate before accepting an offer
    - this is intended to make role hubs less template-like and closer to actual offer-decision pages
  - Product interpretation:
    - the pivot should not stop at “show different roles”
    - the site now starts to express that different roles have different decision triggers:
      - RN => shift premiums, hospital relocation, sign-on distortion
      - Teacher => district salary schedules vs housing reality
      - Accountant => title/promotion gains vs city-cost drag
      - PM / Marketing / Pharmacist => city premium vs real monthly residual
  - Rationale:
    - positioning changed faster than the product in earlier batches
    - this refactor starts to close that gap by making role hubs carry actual role-specific decision value, not just SEO routing value

- Implementation Batch F - City Hub Layer:
  - Added a crawlable city-hub layer at `/city/{citySlug}`:
    - city hubs now act as relocation entry pages instead of routing users straight into generic city-only salary URLs
    - hubs are indexable only for priority cities, mirroring the broader pruning strategy
  - City hubs now combine:
    - city context (`job market`, `housing`, `industry focus`, `quality of life`)
    - role-aware benchmark cards for featured roles
    - crawlable entry points into role-specific salary checks and cross-city comparisons
  - Directory cleanup:
    - `/cities` remains a non-indexed directory, but its city links now point to `/city/{slug}` hubs instead of generic `/salary-check/{city}/100000`
  - Sitemap expansion:
    - added seed city hubs such as:
      - `/city/austin-tx`
      - `/city/dallas-tx`
      - `/city/seattle-wa`
      - `/city/new-york-ny`
      - `/city/san-francisco-ca`
      - `/city/miami-fl`
  - Product interpretation:
    - job hubs answer “Where should someone in this role compare offers?”
    - city hubs answer “If I am moving to this city, which role-specific salary checks should I run first?”

- Build Stability Note:
  - `bootJar` briefly failed because Spring Boot main-class auto-detection was not resolving consistently during local iterative work.
  - Fixed by explicitly setting:
    - `springBoot.mainClass = 'com.offerverdict.OfferVerdictApplication'`
    - in `build.gradle`

- Verification:
  - Test commands run:
    - `./gradlew test --tests com.offerverdict.controller.SeoRegressionIntegrationTest --tests com.offerverdict.controller.SitemapControllerTest --tests com.offerverdict.service.ComparisonServiceTest --no-daemon`
    - `./gradlew bootJar --no-daemon`
  - Result:
    - PASS
  - Regression assertions now cover:
    - legacy compare-prefill redirect behavior
    - valid JSON-LD render on single-city pages
    - generic single-city page => `noindex`
    - low-value single-city page => `noindex`
    - core single-city page => indexable
    - low-value comparison page => `noindex`
    - core comparison page => indexable
    - comparison page links do not leak to generic `/salary-check/{city}/{salary}` URLs
    - single-city page links to crawlable comparison/job-hub destinations
    - major job hub => indexable
    - non-major job hub => `noindex`
    - homepage and decision landing expose non-tech role-guide links
    - role-guide refactor compiles and keeps all SEO regression tests green
    - priority city hub => indexable
    - low-priority city hub => `noindex`
    - sitemap includes decision landings and selected job hubs while excluding generic single-city URLs

- Next Actions:
  1. Deploy this 2026-03-22 batch before evaluating strategy health again.
  2. After deploy, request inspection/re-crawl for:
    - `/job/software-engineer`
    - `/job/registered-nurse`
    - one core comparison URL
    - one core job-specific single-city URL
  3. Re-run a US-only Search Console snapshot 7-14 days after deploy and ignore non-target-country noise.
  4. Watch for the first positive signals:
    - impressions on job hubs
    - impressions on core comparison URLs
    - US impressions rising above the current near-zero baseline
  5. If those signals do not improve, next pivot should be content/positioning rather than more index-hygiene work:
    - strengthen job hubs further
    - consider a city relocation hub layer
    - consolidate thin keyword landings if they remain too template-like

## 2026-03-22 - UX Trust Pass (Result + Hub)

- Problem observed in local product review:
  - result page still showed stale artifact patterns from the old build layer:
    - `Decision noteAuthority Advice`
    - duplicated share labels like `X𝕏`, `inin`, `Link🔗`
    - hidden/pseudo icon text leaking into the accessible tree
  - job and city hubs looked cleaner after the pivot, but benchmark cards still repeated `$80,000` too often and felt template-generated

- Root cause:
  - the result page still had a mixed visual stack:
    - refreshed templates
    - older generated-resource state
    - legacy `seo-enhancements.css` overrides that no longer matched the new design system
  - hub benchmark cards were still using a single SEO-aligned bucket fallback, which collapsed too many role/city pairs into the same salary point

- Changes shipped locally:
  - Result page cleanup:
    - removed pseudo-label dependence for the verdict note and share buttons
    - restored explicit visible labels and `aria-label`s for share controls
    - switched malformed icon placeholders to real visible emoji in the verdict, simulator, reality, city-context, and job-context sections
    - replaced the old `seo-enhancements.css` with a simpler neutral version aligned with the current product design system
    - kept the page structure intact while improving readability and screen-reader output
  - Hub trust pass:
    - changed hub card wording from `Suggested benchmark` to `Starting salary check`
    - replaced the flat benchmark fallback with a localized heuristic:
      - role baseline
      - city cost index
      - city median income
    - goal: reduce repetitive `$80,000` cards and make hub entry points feel less synthetic

- Local validation:
  - `./gradlew clean test --tests com.offerverdict.controller.SeoRegressionIntegrationTest --tests com.offerverdict.controller.SitemapControllerTest --tests com.offerverdict.service.ComparisonServiceTest --no-daemon`
  - `./gradlew bootJar --no-daemon`
  - Playwright review after rebuild:
    - result page no longer shows duplicated verdict/share labels
    - share buttons now expose clean accessible names
    - job hub benchmarks now vary by city (`$90k`, `$100k`, `$110k`, etc.) instead of collapsing to one repeated value
    - city hub role cards now feel more believable as entry points

- Interpretation:
  - this did not change SEO strategy directly
  - it improved product trust and reduced the `template / generated site` feeling that had become visible after the pivot
  - this matters because the site now has to win on credibility, not just crawlability

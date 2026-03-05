# Session Handoff (2026-03-05)

## 1) Snapshot
- Project: OfferVerdict (`https://livingcostcheck.com`)
- Branch: `master`
- HEAD: `0974123`
- Updated at: 2026-03-05 (UTC)

## 2) What Was Done In This Session

### Git sync
- Ran `git pull` successfully (fast-forward to `0974123`).

### Search Console analysis (MCP)
- Property used: `sc-domain:livingcostcheck.com`
- Summary window: `2026-02-02` ~ `2026-03-02`
  - Clicks: `1`
  - Impressions: `513`
  - CTR: `0.195%`
  - Avg position: `4.48`
- Period compare: `2026-02-02`~`2026-03-02` vs `2026-01-05`~`2026-02-01`
  - Clicks: `-90%` (`10 -> 1`)
  - Impressions: `-75.51%` (`2095 -> 513`)
  - CTR: `-59.16%`
  - Avg position improved: `15.14 -> 4.48`

### Code changes implemented
- Added canonical host redirect filter (`www/non-www`, scheme canonicalization for GET/HEAD only):
  - `src/main/java/com/offerverdict/config/CanonicalHostRedirectFilter.java`
- Added app toggle:
  - `app.enforceCanonicalHostRedirect` in `AppProperties` + `application.yml`
- Added analysis date context to model and meta descriptions:
  - `ComparisonController` (`analysisDateUtc`, `dataModifiedDate`)
  - `SingleCityController` (`analysisDateUtc`)
- Added analysis-date UI exposure:
  - `result.html` (Analysis Date + Data Last Modified)
  - `single-verdict.html` (Analysis Date)
- Added tests:
  - `src/test/java/com/offerverdict/config/CanonicalHostRedirectFilterTest.java`

### Verification run
- Command:
  - `./gradlew test --tests com.offerverdict.config.CanonicalHostRedirectFilterTest --tests com.offerverdict.controller.SitemapControllerTest --no-daemon`
- Result: PASS

## 3) Current Working Tree (NOT committed yet)
Modified:
- `src/main/java/com/offerverdict/config/AppProperties.java`
- `src/main/java/com/offerverdict/controller/ComparisonController.java`
- `src/main/java/com/offerverdict/controller/SingleCityController.java`
- `src/main/resources/application.yml`
- `src/main/resources/templates/result.html`
- `src/main/resources/templates/single-verdict.html`

Untracked:
- `src/main/java/com/offerverdict/config/CanonicalHostRedirectFilter.java`
- `src/test/java/com/offerverdict/config/CanonicalHostRedirectFilterTest.java`

## 4) Project Health (as of 2026-03-05)
- Product state: functional salary/city comparison calculator with pSEO route generation.
- SEO state: rankings/position signal exists on subsets, but click volume is extremely low.
- Risk: too many thin/near-duplicate permutations vs actual demand; visibility does not convert to traffic.

## 5) Pivot Recommendation (High confidence)

### Recommended direction
Pivot from **"broad permutation pSEO"** to **"decision-intent workflow"**.

### Why
- Current data shows indexing/ranking signal but minimal clicks.
- Users with real intent are searching decision-style questions (offer acceptance, move feasibility, post-tax take-home confidence), not arbitrary salary permutations.

### 3-step pivot
1. **Narrow content surface**
   - Prioritize pages with clear intent: offer-acceptance checks, move affordability, salary-to-city feasibility.
   - De-emphasize low-value long-tail permutations.
2. **Strengthen decision UX + conversion**
   - Add explicit decision artifacts: threshold delta, confidence band, negotiation anchor.
   - Tighten lead CTA around user intent (negotiate, compare, relocate).
3. **Operate by weekly KPI loop**
   - KPI set: clicks, non-brand CTR, lead submit rate, top 20 landing pages.
   - Prune/expand based on measured lift, not route count.

## 6) Next Session Start Checklist
1. Review `git status -sb` and decide whether to commit current canonical/date patch.
2. Select pivot scope (which page families stay vs sunset).
3. Create `pivot backlog` issues:
   - Intent-based page templates
   - CTA and funnel instrumentation refinement
   - Pruning rules for low-value routes
4. Re-run Search Console compare after deploy and track delta weekly.

## 7) Commands To Resume Fast
```bash
git status -sb
git diff --name-only
./gradlew test --tests com.offerverdict.config.CanonicalHostRedirectFilterTest --tests com.offerverdict.controller.SitemapControllerTest --no-daemon
```

## 8) Deep-Dive Addendum (same day)
- Additional signal after deeper MCP checks:
  - In `2026-02-02`~`2026-03-02`, `KOR` produced `309` impressions with `0` clicks (avg position `1.23`).
  - In the same window, `USA` produced `147` impressions with `1` click (avg position `9.27`).
  - The KOR surge is concentrated on:
    - `2026-02-24`: `218` impressions, `0` clicks
    - `2026-02-28`: `86` impressions, `0` clicks
  - Device split in the same window:
    - Desktop `428` impressions, `0` clicks, avg position `3.57`
    - Mobile `83` impressions, `1` click, avg position `8.98`
  - Top-pages report (`limit=250`) still shows host split:
    - `www`: `80` rows / `603` impressions
    - non-`www`: `136` rows / `1797` impressions
- Interpretation:
  - Recent KPI is heavily skewed by non-target-country impression spikes.
  - Prioritize US-filtered KPI dashboarding and canonical host unification before strategy judgment.

## 9) Pivot Execution Addendum (same day)
- Added SEO cleanup utility:
  - `src/main/java/com/offerverdict/seo/SeoUrlPolicy.java`
- Added salary boundary redirects:
  - Single-city out-of-range salary path now `301` redirects to bounded aligned salary.
  - Comparison explicit out-of-range salary params now `301` redirect to bounded annualized values.
- Added stronger index hygiene:
  - Comparison pages with explicit salary query params are now forced to `noindex`.
- Updated CTR-focused metadata templates:
  - `ComparisonController`: intent-explicit title/meta.
  - `SingleCityController`: intent-explicit title/meta with gain/gap framing.
- Added operations scripts:
  - `scripts/gsc_us_kpi_dashboard.js`
  - `scripts/gsc_url_cleanup_candidates.js`
  - `scripts/lib/gsc_mcp_client.js`
- Added execution playbook:
  - `docs/SEO_PIVOT_EXECUTION_2026-03.md`
- Verification:
  - `./gradlew test --tests com.offerverdict.config.CanonicalHostRedirectFilterTest --tests com.offerverdict.controller.SitemapControllerTest --tests com.offerverdict.seo.SeoUrlPolicyTest --no-daemon` -> PASS
  - `./gradlew test --tests com.offerverdict.service.ComparisonServiceTest --no-daemon` -> PASS

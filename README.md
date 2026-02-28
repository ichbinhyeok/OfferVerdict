# OfferVerdict – Verdict-first relocation math

## Live Demo
This repository powers the OfferVerdict relocation and cost-of-living comparison tool. Live deployment: https://livingcostcheck.com

The service evaluates whether a job offer in a new city is financially sustainable based on salary, housing, and baseline living expenses.


## How to run locally
1. **Java 21 + Gradle wrapper:** ensure Java 21 is available.
2. **Install & run:**  
   ```bash
   ./gradlew bootRun
   ```  
   App boots at `http://localhost:8080`.
3. **Try a route:**  
   `http://localhost:8080/software-engineer-salary-new-york-ny-vs-austin-tx?currentSalary=150000&offerSalary=180000`

## Adding cities and jobs
- **Cities:** update `src/main/resources/data/CityCost.json` with `{city, state, slug, avgRent, colIndex}`. Slug must be `city-name-st`.  
- **Jobs:** update `src/main/resources/data/Jobs.json` with `{title, slug}` entries.  
- **Reload in dev:** if `app.devReloadEnabled=true`, hit `/admin/reload-data` to reload JSON without restarting.

## Lead Tracking and Persistence
- **Analytics:** GA4 is loaded from `templates/fragments/analytics.html`. Lead funnel events are emitted from `single-verdict.html` (`lead_form_open`, `lead_submit_click`, `lead_submit_attempt`, `lead_submit_success`, `lead_submit_error`, `generate_lead`).
- **Server event log:** `POST /api/leads/event` stores lead funnel events in CSV.
- **Lead capture log:** `POST /api/leads/capture` stores captured leads in CSV.
- **Storage path:** configure `APP_LEADS_STORAGE_DIR` (default `./data/leads`).
- **Backup path:** configure `APP_LEADS_BACKUP_DIR` (default `./data/leads-backup`).
- **De-duplication:** configure `APP_LEADS_DEDUPE_MINUTES` (default `15`) to suppress rapid duplicate submissions.
- **CSV outputs:** rolling + backup files are written together: `leads.csv`, `lead_events.csv`, `leads-YYYY-MM-DD.csv`, `lead_events-YYYY-MM-DD.csv`.
- **Docker persistence:** mount a host volume to `/app/data`, set `APP_LEADS_STORAGE_DIR=/app/data/leads`, and set `APP_LEADS_BACKUP_DIR=/app/data/leads-backup` so redeployments do not lose CSV files.

## Playwright Beta Smoke Suite
- **Scope:** end-to-end multi-persona smoke and beta flows (home, single analysis, comparison, SEO/noindex/canonical, robots/sitemap, lead funnel, simulation lab, mobile rendering).
- **Test class:** `src/test/java/com/offerverdict/e2e/PlaywrightBetaSmokeTest.java`
- **Run only beta suite:** `./gradlew test --tests com.offerverdict.e2e.PlaywrightBetaSmokeTest --no-daemon`
- **Run full regression:** `./gradlew test --no-daemon`
- **Artifacts:** screenshots are saved to `build/reports/playwright-beta/`

## Calculation logic
1. **Taxes:** progressive federal + state brackets from `StateTax.json`, plus FICA (SS up to the cap + Medicare).  
2. **Net monthly:** net annual / 12.  
3. **Costs:** rent from city data + `baselineLivingCost * (colIndex / 100)` (baseline defaults to $1,800).  
4. **Residual:** `netMonthly - (rent + livingCost)`.  
5. **Delta %:** `(residualB - residualA) / abs(residualA) * 100`.  
6. **Verdict bands:**  
   - `>= +10%` → **GO**  
   - `0 ~ +10%` → **CONDITIONAL**  
   - `-0 ~ -10%` → **WARNING**  
   - `<= -10%` → **NO_GO**

## SEO & pSEO strategy
- **Route shape:** `/{job}-salary-{cityA}-vs-{cityB}` with canonical slugs and 301s for non-canonical.  
- **Programmatic SEO:** sitemap index + chunked sitemaps generate all job/city permutations with deterministic canonical URLs.  
- **Metadata:** dynamic `<title>`, `<meta description>` (<=155 chars), OpenGraph, and JSON-LD (WebPage + FAQ).  
- **Engagement levers:** aggressive verdict banner, punchy copy, collapsible details, internal linking to related job/city comparisons, AdSense slots, and affiliate CTA variants per verdict.  
- **Robots & humans:** `/robots.txt` references sitemap, `/humans.txt` highlights the editorial nature.  
- **Deterministic rendering:** JSON is cached in-memory at startup; reload is opt-in for dev.

## Disclaimer
“Not financial or tax advice.”

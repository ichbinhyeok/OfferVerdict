# OfferVerdict – Verdict-first relocation math

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

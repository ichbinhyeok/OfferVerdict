# City Data Expansion Notes

This project now supports reproducible city expansion using public source data.

## Sources
- US Census ACS 1-Year 2023 (`B01003_001E` population, `B19013_001E` median household income)
  - `https://api.census.gov/data/2023/acs/acs1?get=NAME,B01003_001E,B19013_001E&for=place:*`
- Zillow ZORI (city monthly rent)
  - `https://files.zillowstatic.com/research/public_csvs/zori/City_zori_uc_sfrcondomfr_sm_month.csv`
- Zillow ZHVI (city home value index)
  - `https://files.zillowstatic.com/research/public_csvs/zhvi/City_zhvi_uc_sfrcondo_tier_0.33_0.67_sm_sa_month.csv`

## Script
- Path: `scripts/expand_city_data_from_public_sources.ps1`
- Purpose: append new cities into `src/main/resources/data/CityCost.json` from source-backed metrics.

## Run
```powershell
powershell -ExecutionPolicy Bypass -File scripts/expand_city_data_from_public_sources.ps1 -AddCount 40
```

Useful flags:
- `-MinPopulation 100000` to include smaller cities.
- `-ForceRefresh` to re-download Zillow CSVs.
- `-EvidenceCsvPath docs/city-data/city-expansion-sources.csv` to write source snapshot.

## Quality Guards Implemented
- Requires city-level matches across Census + ZORI + ZHVI.
- Filters out missing/invalid values and extreme outliers.
- Uses deterministic slug normalization and de-duplication against existing city slugs.
- Captures per-run evidence CSV with population/rent/home value/income snapshot.

## Notes
- Existing hand-curated cities remain untouched.
- Added cities currently include numeric data and estimated sub-cost breakdown (`details`).
- `city-context.json` is still only partially curated and should be expanded manually for highest-priority cities.

# Phase 5: SEO Infrastructure & Data Enrichment

## Status
- **Core Engine Ready**: Single City Analysis with Job Context is live.
- **Main Page Linked**: Users can access it from the home page.
- **Missing Piece**: Google doesn't know these thousands of pages exist yet.

## Objective
Make the pSEO pages discoverable by search engines (Google Indexing).

## Action Plan
1.  **Index/Directory Page (`/jobs`)**:
    -   Create/Update a `job-directory.html` page that lists all supported job titles.
    -   Link each job to a "Hub Page" or directly to top cities (e.g., "Software Engineer salaries in [Top Cities]").

2.  **Sitemap.xml (`/sitemap.xml`)**:
    -   Implement a dynamic Sitemap controller.
    -   Automatically generate URLs for:
        -   Static pages (About, Methodology)
        -   Job Directory
        -   Top 50 City x Top 20 Job combinations (to avoid creating a 10M line sitemap instantly, start with high-value targets).

## Immediate Next Step
I will inspect the existing `job-directory.html` and then create the `SitemapController` to start broadcasting our pages to Google.

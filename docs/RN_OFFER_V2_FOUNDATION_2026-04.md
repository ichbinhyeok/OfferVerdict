# RN Offer V2 Foundation

Date: 2026-04-24

## Current Product Truth

- The product has successfully pivoted from broad calculator/pSEO surface area toward `RN offer review`.
- The current codebase is still a `flat calculator schema`, not yet an `RN offer case schema`.
- The current OCR boundary is clear:
  - text PDFs: strong
  - scanned PDFs: usable
  - flat screenshots: usable
  - hard-perspective phone photos: still weak

The current implementation already has useful building blocks:

- financial/risk engine in [OfferRiskService.java](C:/Development/Owner/OfferVerdict/src/main/java/com/offerverdict/service/OfferRiskService.java:1)
- draft model in [OfferRiskDraft.java](C:/Development/Owner/OfferVerdict/src/main/java/com/offerverdict/model/OfferRiskDraft.java:1)
- parse metadata in [OfferTextParseResult.java](C:/Development/Owner/OfferVerdict/src/main/java/com/offerverdict/model/OfferTextParseResult.java:1)
- report model in [OfferRiskReport.java](C:/Development/Owner/OfferVerdict/src/main/java/com/offerverdict/model/OfferRiskReport.java:1)
- intake/orchestration in [OfferRiskController.java](C:/Development/Owner/OfferVerdict/src/main/java/com/offerverdict/controller/OfferRiskController.java:1)

The largest structural gap is that field values do not yet carry provenance. The system cannot cleanly distinguish:

- extracted from document
- corrected by user
- defaulted by app
- derived from other fields
- still unknown

That gap matters more than another round of prompt tuning.

## RN Offer Ontology V1

The product should be built around `RN offer survivability`, not just `RN pay math`.

### Top-level dimensions

1. `Compensation certainty`
- explicit rate vs posted range
- guaranteed FTE / guaranteed hours
- differentials that are real vs assumed

2. `Commitment and downside`
- sign-on
- relocation
- clawback / repayment trigger
- full vs prorated repayment
- what happens on unit change, schedule change, employer termination

3. `Schedule control`
- written shift vs verbal vs rotating
- weekends / holidays / call burden
- block scheduling / self-scheduling
- low-census flex / cancellation exposure

4. `Assignment safety`
- not just ratio, but acuity, turnover, admits/discharges/transfers
- float radius and float frequency
- boarded patients and overflow burden
- charge nurse assignment coverage

5. `Specialty readiness`
- orientation length
- preceptor quality
- residency / transition support
- whether the nurse can realistically survive the unit

6. `Leadership and work environment`
- manager support
- respect / escalation path for unsafe assignments
- incivility / violence handling

7. `Support infrastructure`
- CNA / tech
- unit clerk
- security
- interpreter
- RT / transport / lactation / ancillary support

8. `Location and life economics`
- local cost pressure
- commute
- housing friction
- state tax / compact license impact

9. `Career signal`
- specialty depth
- certification support
- internal mobility
- whether this job builds the nurse's career or just burns it

### Specialty branches that should split the engine

1. `ICU`
- orientation depth
- acuity mismatch
- ECMO / CRRT / vasoactive exposure
- rapid response / charge support
- float to stepdown or broader float expectation

2. `ED`
- boarding and crowding
- psych / behavioral health exposure
- security / violence response
- triage and hallway care burden
- onboarding structure

3. `Med-surg / Tele`
- total care burden
- admit/discharge/transfer churn
- telemetry load
- CNA support
- observation / stepdown mixing

4. `L&D`
- 1:1 induction / augmentation pressure
- OB triage
- C-section / PACU coverage
- hemorrhage / fetal monitoring readiness
- neonatal support

## Canonical Schema V1

The next schema should be `OfferCase`, not just a bigger `OfferRiskDraft`.

```yaml
OfferCase:
  mode: offer_review | job_post

  role:
    role_slug: string
    unit_type: med_surg | icu | ed | or | l_and_d | clinic | float_pool | other
    specialty_track: adult | peds | maternal | psych | periop | unknown

  source:
    source_kind: pasted_text | uploaded_pdf_text | uploaded_pdf_ocr | uploaded_image_ocr | manual
    source_label: string
    raw_text: string
    file_name: string
    warnings: [string]

  current_baseline:
    city_slug: string
    hourly_rate: number
    monthly_insurance: number

  offer_terms:
    city_slug: string
    hourly_rate: number
    pay_range_min: number
    pay_range_max: number
    pay_selection_policy: explicit | floor | midpoint | ceiling | unknown
    weekly_hours: number
    overtime_hours: number
    night_diff_percent: number
    night_hours: number
    weekend_diff_percent: number
    weekend_hours: number
    monthly_insurance: number

  contract_terms:
    sign_on_bonus: number
    relocation_stipend: number
    moving_cost_estimate: number
    contract_months: integer
    planned_stay_months: integer
    repayment_style: none | prorated | full | unknown
    repayment_trigger: voluntary_only | any_separation | unit_change | shift_change | unknown

  work_terms:
    shift_guarantee: written | verbal | rotating | unknown
    float_policy: home_unit_only | adjacent_units | hospital_wide | unknown
    cancel_policy: protected_hours | low_census_only | can_cancel_without_pay | unknown
    orientation_weeks: integer
    preceptor_model: dedicated | shared | unknown
    weekend_requirement: string
    holiday_requirement: string
    call_requirement: string

  specialty_context:
    charge_has_assignment: yes | no | unknown
    ancillary_support_level: strong | mixed | weak | unknown
    staffing_pressure_level: low | medium | high | unknown
    safety_pressure_level: low | medium | high | unknown

  provenance:
    fields:
      "<field_path>":
        status: extracted | user_confirmed | user_entered | defaulted | derived | unknown
        confidence_score: 0.0-1.0
        confidence_label: high | medium | low
        raw_value: string
        normalized_value: any
        evidence:
          - snippet: string
            page: integer
            source_kind: string
        warnings: [string]
```

## What Must Change In Code

### Immediate structural upgrades

1. Replace hidden fake defaults in `job_post`
- `repaymentStyle=none`
- `plannedStayMonths=0`
- `currentHourlyRate`
- `currentMonthlyInsurance`

These are not safe defaults. They are silent hallucinations.

2. Add pay range as first-class fields
- `pay_range_min`
- `pay_range_max`
- `pay_selection_policy`

3. Add field-level provenance
- every field needs:
  - status
  - confidence
  - raw value
  - normalized value
  - evidence snippets

4. Expose calculation assumptions
- household type
- housing type
- 401k default
- insurance fallback
- market anchor source

5. Split RN-only surface from non-RN roles
- the controller still accepts non-RN roles
- the actual taxonomy is RN-first
- the surface should reflect that instead of pretending to be broad

### The biggest schema mismatch today

The report already asks questions about:

- orientation length
- float radius
- staffing burden
- ratio / support reality

But the input model cannot represent those fields yet. The product is hinting at RN truth in copy, while the schema is still calculator-shaped.

## OCR / Document Intake Options

This comparison is for the real problem:

- offer letters
- recruiter PDFs
- screenshots
- scanned PDFs
- phone photos with skew / perspective / blur

### Option 1: Azure AI Document Intelligence

Why it is strong:

- document OCR runs at higher resolution than Azure Vision Read for document inputs
- supports PDF, scanned images, and Office/HTML formats
- outputs paragraphs, lines, words, polygons, and confidence
- supports searchable PDF output
- supports containers, which matters if privacy/governance tightens later

Why it fits:

- strong managed choice for small teams
- good bridge between OCR and later structured extraction
- best fit if we want cloud now and optional private deployment path later

Tradeoffs:

- still a managed vendor dependency
- best results still assume clear photo / high-quality scan
- pricing is usage-based and region-dependent

Sources:
- [Read model](https://learn.microsoft.com/en-us/azure/ai-services/document-intelligence/prebuilt/read?view=doc-intel-4.0.0)
- [Data privacy and security](https://learn.microsoft.com/en-us/azure/foundry/responsible-ai/document-intelligence/data-privacy-security)
- [Containers](https://learn.microsoft.com/en-us/azure/ai-services/document-intelligence/containers/install-run?view=doc-intel-4.0.0)
- [Pricing](https://azure.microsoft.com/en-us/pricing/details/ai-document-intelligence/)

### Option 2: Google Cloud Document AI

Why it is strong:

- mature document OCR platform
- supports OCR, layout parser, custom extraction, and downstream document workflows
- exposes image quality scores and detected defects
- strong security/compliance posture
- Google states customer content is not used to train Document AI models

Why it fits:

- good if we want to grow from OCR into layout-aware extraction and later schema learning
- OCR pricing is unusually clear

Tradeoffs:

- no container/on-prem path equivalent to Azure's document intelligence containers
- stronger if we are willing to live inside GCP

Sources:
- [Document AI docs](https://docs.cloud.google.com/document-ai/docs)
- [Enterprise Document OCR](https://docs.cloud.google.com/document-ai/docs/enterprise-document-ocr)
- [Security and compliance](https://docs.cloud.google.com/document-ai/docs/security)
- [Supported files](https://docs.cloud.google.com/document-ai/docs/file-types)
- [Limits](https://docs.cloud.google.com/document-ai/limits)
- [Pricing](https://cloud.google.com/document-ai/pricing)

### Option 3: Amazon Textract

Why it is strong:

- good for OCR + forms + tables + signatures + queries
- supports document rotation
- HIPAA eligible
- pricing for text detection is straightforward

Why it fits:

- good if the product later leans into structured fields and AWS-native ops

Tradeoffs:

- less attractive than Azure/Google for this product's likely next step, which is rich phone-photo offer intake plus evidence-linked field extraction
- sync limits are tighter for PDFs/TIFFs than async flows
- no on-prem path like Azure containers

Sources:
- [Analyzing documents](https://docs.aws.amazon.com/textract/latest/dg/how-it-works-analyzing.html)
- [Limits](https://docs.aws.amazon.com/textract/latest/dg/limits-document.html)
- [Best practices](https://docs.aws.amazon.com/en_us/textract/latest/dg/textract-best-practices.html)
- [HIPAA eligibility](https://aws.amazon.com/about-aws/whats-new/2019/10/amazon-textract-is-now-a-hipaa-eligible-service/)
- [Pricing](https://aws.amazon.com/textract/pricing/)

### Option 4: Mistral OCR

Why it is strong:

- OCR API is built for structured content and markdown-ready output
- supports images and PDFs in the OCR processor docs
- OCR is positioned for large-scale batch processing
- Mistral states OCR has zero data retention by default

Why it fits:

- attractive if we want AI-ready document markdown quickly
- strong candidate for experimentation when we care more about downstream LLM consumption than classic OCR object graphs

Tradeoffs:

- less enterprise implementation surface than Azure/Google/AWS document suites
- less obvious fit if we want deep document operations, compliance workflowing, or hybrid deployment

Sources:
- [OCR processor docs](https://docs.mistral.ai/studio-api/document-processing/basic_ocr)
- [OCR endpoint](https://docs.mistral.ai/api/endpoint/ocr)
- [OCR model docs](https://docs.mistral.ai/models/ocr-3-25-12)
- [Zero data retention](https://help.mistral.ai/en/articles/323752-can-i-activate-zero-data-retention-zdr)

### Option 5: PaddleOCR + document preprocessing

Why it is strong:

- open-source
- specifically offers document orientation classification and geometric distortion correction
- closest realistic self-hosted path for hard phone-photo recovery beyond Tesseract tuning

Why it fits:

- best next self-hosted candidate if we want stronger photographed-document preprocessing
- can be used as a front-end correction layer before OCR or downstream extraction

Tradeoffs:

- more engineering work
- less turnkey structure than managed document AI suites
- ongoing ops burden becomes ours

Sources:
- [Document image preprocessing pipeline](https://www.paddleocr.ai/main/en/version3.x/pipeline_usage/doc_preprocessor.html)

### Option 6: OCRmyPDF added to the current PDF path

Why it is useful:

- not a replacement for photo OCR
- useful immediate add-on for scanned PDFs
- gives deskew / clean / rotate-pages options around a PDF-first workflow

Tradeoffs:

- does not solve hard perspective phone photos
- still sits on traditional OCR foundations

Sources:
- [Advanced features](https://ocrmypdf.readthedocs.io/en/stable/advanced.html)

## Recommendation

### Product moat recommendation

The moat is not `better OCR` alone.

The moat is:

1. `RN offer ontology`
2. `field provenance`
3. `specialty-aware verdicting`
4. `evidence-linked decision support`

### Intake stack recommendation

1. `Primary managed path for the current product problem`
- Pilot `Google Document AI` first.
- Reason: best fit for the actual pain we have now, which is photographed documents, scan quality gating, and layout-aware OCR.
- Use its quality signals to decide when to re-shoot instead of pretending every upload is readable.

2. `Primary managed path if privacy/security becomes the immediate blocker`
- Pilot `Azure AI Document Intelligence` first instead.
- Reason: strongest enterprise/privacy story, container path, private networking story, and good document OCR surface.

3. `Meaning-extraction layer`
- Put `Structured Outputs` on top of OCR text, not instead of OCR.
- Reason: the RN moat is clause extraction and verdict traceability, not raw text recognition alone.

4. `Self-hosted hard-photo path`
- Add `PaddleOCR document preprocessing` only if we need to recover skewed mobile captures without sending everything to a vendor.

5. `Keep current stack as fallback`
- Keep Tesseract/PDFBox for:
  - offline fallback
  - low-cost fallback
  - dev/test
  - conservative fail-open path

6. `Optional PDF booster`
- Add `OCRmyPDF` only if scanned PDF quality remains a meaningful bottleneck after external OCR trials.

## Recommended Execution Order

### Phase 1

- introduce `OfferCase` schema in code
- add pay-range fields
- add field provenance
- remove fake `job_post` defaults

### Phase 2

- integrate a managed OCR adapter interface
- pilot Google Document AI first for photographed documents and scanned PDFs
- keep Azure Document Intelligence as the parallel enterprise/security benchmark
- keep current OCR pipeline as fallback
- run side-by-side evaluation on:
  - offer PDFs
  - recruiter screenshots
  - scanned PDFs
  - phone photos

### Phase 3

- add a structured extraction layer above OCR
- populate RN clause fields through schema-constrained extraction
- attach confidence and evidence to each field

### Phase 4

- branch the risk engine by specialty:
  - ICU
  - ED
  - Med-surg / tele
  - L&D

### Phase 5

- link verdict reasons directly to field evidence
- expose per-field confidence in the UI
- then layer LLM extraction on top of already-corrected OCR text

## Hard Conclusion

If we only swap OCR engines, the product gets easier to use but not defensible.

If we add `RN ontology + provenance + specialty-aware reasoning`, the product becomes meaningfully stronger even before perfect OCR arrives.

The correct path is:

`better document intake` + `better RN ontology` + `traceable verdicts`

not any one of those alone.

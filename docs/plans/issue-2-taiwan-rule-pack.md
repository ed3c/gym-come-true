# Issue plan — Taiwan supplement evidence and reviewed rule pack

## Outcome

Convert Taiwan product labels and primary-source documents into a versioned deterministic policy pack. The pack may support logging and review routing; it may not recommend an individualized dose.

## Acceptance

- Product-variant and serving schemas distinguish barcode, country, formulation, serving unit, and label revision.
- Representative Traditional Chinese label corpus has consent/provenance and an explicit retention policy.
- OCR field accuracy and correction completion are measured by field type.
- Every rule points to an archived primary source, hash, jurisdiction, effective date, and exact field mapping.
- Rule conflicts, ambiguity, expiry, rollout, and rollback are deterministic.
- A qualified reviewer signs the rule-pack version and user-facing wording.
- Tests cover IU, missing serving size, duplicated ingredients, proprietary blends, medication context, symptoms, and conflicting sources.
- Decision receipts preserve evidence and rule-pack versions.

## Hard limits

- No model-created rules.
- No universal medication interaction claim.
- No personal dose recommendation.
- No `CLINICALLY_REVIEWED` status without source, reviewer, tests, date, and rollback.

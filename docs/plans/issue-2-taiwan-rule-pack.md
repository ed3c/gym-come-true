# Issue plan — Taiwan supplement evidence and reviewed rule pack

## Outcome

Convert Taiwan product labels and primary-source documents into a versioned deterministic policy pack. The pack may support logging and review routing; it may not recommend an individualized dose.

## Current delivery stack

```text
PR #2   AUDITABLE_CROSS_PLATFORM_FOUNDATION
  └─ PR #15   TAIWAN_EVIDENCE_CONTRACT_DRAFT          # Phase 8A
       └─ agent/taiwan-source-lifecycle               # Phase 8B
            └─ Issue #8 REVIEWED_TAIWAN_RULE_PACK     # not complete
```

### Phase 8A — delivered in Draft PR #15

- product-variant and nullable serving schema;
- consent-aware corpus admission;
- OCR first-pass/correction metrics;
- Taiwan source candidate registry;
- deterministic rule-pack admission contract;
- reviewer coverage and wording-hash requirements;
- seven required safety cases;
- decision receipts with `modelUsedForDecision=false`.

### Phase 8B — source lifecycle stacked slice

- mutable official endpoints remain `CANDIDATE + DENY`;
- local-only source capture with SHA-256, byte length, atomic archive, and receipt;
- `HASH_VERIFIED`, `LEGAL_REVIEWED`, `TEST_ONLY`, and production-use boundaries;
- exact CSV/JSON/XML/PDF/HTML/text selectors;
- excerpt hashes and source/snapshot identity matching;
- reviewer requirement for high-impact regulatory mappings;
- deterministic `DRAFT -> REVIEWED -> STAGED -> ACTIVE` promotion;
- signed suspend, resume, revoke, expire, and rollback events;
- synthetic fixtures, JSON Schemas, Python validator, and Kotlin tests.

Phase 8B does not capture or redistribute real official source bytes and does not add a production rule.

## Acceptance

- Product-variant and serving schemas distinguish barcode, country, formulation, serving unit, and label revision.
- Representative Traditional Chinese label corpus has consent/provenance and an explicit retention policy.
- OCR field accuracy and correction completion are measured by field type.
- Every rule points to an archived primary source, hash, jurisdiction, effective date, and exact field mapping.
- Rule conflicts, ambiguity, expiry, rollout, and rollback are deterministic.
- A qualified reviewer signs the rule-pack version and user-facing wording.
- Tests cover IU, missing serving size, duplicated ingredients, proprietary blends, medication context, symptoms, and conflicting sources.
- Decision receipts preserve evidence and rule-pack versions.

## State gates

```text
UNKNOWN
  -> CANDIDATE             # mutable live endpoint only
  -> HASH_VERIFIED         # exact local bytes + content address
  -> LEGAL_REVIEWED        # admitted storage/use scope
  -> VERIFIED_MAPPING      # exact selector + excerpt hash
  -> CLINICALLY_REVIEWED   # qualified reviewer covers rules/wording
  -> STAGED
  -> ACTIVE                # exact version/date, no blocker
  -> SUSPENDED / REVOKED / EXPIRED / ROLLED_BACK
```

No transition may be inferred from a filename, URL, schema validity, LLM output, or manually edited status field.

## Hard limits

- No model-created rules or source mappings.
- No universal medication interaction claim.
- No personal dose recommendation.
- No `CLINICALLY_REVIEWED` status without source, reviewer, tests, date, wording review, and rollback.
- No official-source hash without capturing and hashing the exact bytes.
- No production `ALLOW` source before legal review.
- No production mapping without source/snapshot identity, exact selector, and excerpt hash.
- No self-declared `productionAdmitted=true`.
- No force rewrite of stacked evidence history merely to hide divergence.

## Remaining external work

- consented representative production corpus outside git;
- operational consent withdrawal and image deletion;
- Android ML Kit and Apple Vision field-level evaluation;
- approved capture of exact MOHW/TFDA bytes into immutable private storage;
- legal review and attribution/redistribution scope for each artifact;
- exact source-field mappings recalculated against captured bytes;
- deterministic production rules and full safety tests;
- qualified Taiwan reviewer qualification, conflict-of-interest record, signed rule and wording coverage;
- signed promotion, incident, revocation, and rollback receipts;
- exact-head hosted Android/Web/iOS evidence after the GitHub Actions budget gate is removed.

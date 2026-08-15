# Taiwan supplement evidence fixtures

This directory contains **synthetic or non-executable examples only**.

- `corpus-manifest.example.json` is a repository-authored Traditional Chinese label. It contains no real person, product image, barcode, or vendor data.
- `rule-pack.draft.example.json` is intentionally `DRAFT`, has no source snapshots or reviewer attestation, and must never execute a production decision.

A real corpus record requires explicit consent, provenance, a raw-text hash, product-variant identity, and a retention decision. Raw images default to no storage. Stored images require encryption, a deletion date, withdrawal support, and an out-of-repository consent receipt.

A real Taiwan rule pack requires archived primary-source snapshots and hashes, exact field mappings, deterministic conflict resolution, all required safety cases, qualified review, user-facing wording review, an effective window, and a rollback version.

## Machine-readable schemas

- `schemas/product-variant.schema.json`
- `schemas/corpus-record.schema.json`
- `schemas/rule-pack.schema.json`

The Kotlin validators remain authoritative for conflict detection, reviewer coverage, effective-window admission, and decision-receipt construction. JSON Schema provides transport validation only.

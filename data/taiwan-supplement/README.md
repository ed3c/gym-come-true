# Taiwan supplement evidence fixtures

This directory contains **synthetic or non-executable examples only**.

## Phase 8A — corpus and rule-pack admission

- `corpus-manifest.example.json` is a repository-authored Traditional Chinese label. It contains no real person, product image, barcode, or vendor data.
- `rule-pack.draft.example.json` is intentionally `DRAFT`, has no source snapshots or reviewer attestation, and must never execute a production decision.

A real corpus record requires explicit consent, provenance, a raw-text hash, product-variant identity, and a retention decision. Raw images default to no storage. Stored images require encryption, a deletion date, withdrawal support, and an out-of-repository consent receipt.

A real Taiwan rule pack requires archived primary-source snapshots and hashes, exact field mappings, deterministic conflict resolution, all required safety cases, qualified review, user-facing wording review, an effective window, and a rollback version.

## Phase 8B — immutable source and release lifecycle

- `source-snapshots/synthetic-labeling-guidance-v1.txt` is repository-authored synthetic text.
- `source-snapshot.synthetic.json` proves byte-length and SHA-256 binding for that text.
- `field-mapping.draft.example.json` contains one synthetic `VERIFIED + TEST_ONLY` mapping and only `DRAFT + DENY` mappings for official source candidates.
- `lifecycle.draft.example.json` is intentionally `DRAFT`; it contains no signatures, reviewer hashes, effective window, source-bundle hash, or rollback target.
- `../../legal/taiwan-official-resource-candidates.json` records mutable MOHW/TFDA live endpoints as `CANDIDATE + DENY`. It contains no fabricated snapshot hashes or archive URIs.

The local capture command has no HTTP client:

```bash
python3 scripts/capture_taiwan_source.py --help
```

It accepts approved local bytes, writes a content-addressed archive receipt, and always defaults to `HASH_VERIFIED + DENY`. Legal review, exact field verification, qualified review, and rule-pack promotion remain separate transitions.

Validate the complete Taiwan evidence contract:

```bash
python3 scripts/validate_taiwan_rule_pack.py
python3 scripts/validate_taiwan_source_lifecycle.py
```

## Phase 8C — consent corpus, OCR evaluation, and rule-pack gates (Issues #24, #25, #26)

- `synthetic-consent-wording-v1.txt` and `synthetic-consent-receipt-v1.txt` are repository-authored synthetic text. No human being was asked for consent and none gave it.
- `consent-grant.synthetic.json` carries the **real** SHA-256 digests and byte lengths of those two files. `scripts/validate_taiwan_corpus_contract.py` recomputes both, so a fabricated hash fails the build rather than passing as evidence.
- `deletion-request.manifest-only-counterexample.json` is intentionally the shape that must **fail**. It sets a manifest flag with zero erasure receipts, which resolves to `MANIFEST_ONLY`. A fixture with populated receipts would claim deletions that never ran.
- `ocr-evaluation-report.absent.json` records `measurement: ABSENT` with zero records, zero observations, and every engine/device/model version literally `ABSENT`. No ML Kit or Apple Vision run has ever executed for this repository; the zeros mean nothing was recognized, not that recognition was perfect.
- `rule-pack-external-gates.absent.json` lists every external gate for the reviewed Taiwan rule pack as `ABSENT`, which makes `EXTERNAL_GATES_ABSENT` the only decision the gate can return. Its gate names are cross-checked against the Kotlin `ExternalGate` enum.

Contract documentation is in [`../../docs/taiwan/README.md`](../../docs/taiwan/README.md).

```bash
python3 scripts/validate_taiwan_corpus_contract.py
```

## Machine-readable schemas

Phase 8A transport schemas:

- `schemas/product-variant.schema.json`
- `schemas/corpus-record.schema.json`
- `schemas/rule-pack.schema.json`

Phase 8B transport schemas:

- `schemas/source-snapshot.schema.json`
- `schemas/source-field-mapping.schema.json`
- `schemas/rule-pack-lifecycle.schema.json`

Phase 8C transport schemas:

- `schemas/ocr-evaluation-report.schema.json` — the aggregate permitted to leave an evaluation device. `additionalProperties` is `false` so raw label text, image references, file paths, and corpus record identifiers cannot ride along.

The Kotlin validators remain authoritative for source/snapshot identity, conflict detection, reviewer coverage, effective-window admission, lifecycle transitions, rollback targeting, and decision-receipt construction. JSON Schema provides transport validation only.

No fixture in this directory authorizes a medical claim, personalized dose, medication-interaction conclusion, official-source redistribution, or production rule.

# Phase 8B delivery — Taiwan source snapshot and lifecycle hardening

## State transition

```text
TAIWAN_EVIDENCE_CONTRACT_DRAFT
  -> TAIWAN_SOURCE_LIFECYCLE_DRAFT
```

This transition is an engineering contract only. It does not promote a source, mapping, rule pack, or clinical review state.

## Delivered

- Public MOHW/TFDA resources remain explicit `CANDIDATE` records with `productionUse=DENY` and no fabricated snapshot hash.
- Source artifacts have immutable byte identity fields, effective windows, license/attribution boundaries, and revocation states.
- Exact field mappings bind `sourceId`, `snapshotId`, selectors, excerpt hashes, deterministic transforms, and reviewer evidence.
- Rule-pack lifecycle is deterministic across `DRAFT`, `REVIEWED`, `STAGED`, `ACTIVE`, `SUSPENDED`, `REVOKED`, `EXPIRED`, and `ROLLED_BACK`.
- Production admission is derived by code; an input manifest cannot self-declare admission.
- Local capture has no network client and produces only `HASH_VERIFIED + DENY` receipts.
- Capture rejects symlinks, empty or oversized input, insecure live-source URLs, corrupted archive targets, and silent manifest replacement.
- Content-addressed local archive bytes are re-hashed, fsynced, and made read-only.

## Required evidence before promotion

```text
exact source bytes + SHA-256
  + approved archive location
  + license/legal review receipt
  + exact field mappings
  + deterministic tests
  + qualified reviewer attestation
  + reviewed user wording
  + effective window
  + incident/revocation path
  + rollback version
  + exact-head hosted checks
```

Missing any item keeps the pack out of production.

## Local verification contract

```bash
python3 scripts/validate_repository.py
python3 scripts/validate_taiwan_rule_pack.py
python3 scripts/validate_taiwan_source_lifecycle.py
python3 scripts/validate_taiwan_source_hardening.py
./gradlew :shared:jvmTest
```

The Python validators and focused Kotlin smoke cases can run locally. The repository's Kotlin 2.4.10 / Gradle 9.5.0 exact-head hosted build remains the authoritative integration gate.

## Rollback

- Reset to the pre-hardening Phase 8B head to remove only capture and failure-path hardening.
- Reset to the PR #15 head to remove Phase 8B entirely.
- No production data migration or active rule pack depends on this branch.

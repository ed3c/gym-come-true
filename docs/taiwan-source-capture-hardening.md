# Taiwan source capture hardening

## Scope

This Phase 8B slice hardens the boundary between a mutable live regulator URL and an immutable source-evidence receipt. It does not create a production Taiwan rule pack, clinical guidance, a personalized dose recommendation, or an approval claim.

## Capture contract

`scripts/capture_taiwan_source.py` has **no network client**. An operator must obtain the intended public document or dataset through an approved process, inspect the response, and supply the exact local bytes.

The command produces only:

```text
local bytes
  -> streamed SHA-256 + size limit
  -> atomic content-addressed archive
  -> re-hash after archive write
  -> read-only local archive file
  -> HASH_VERIFIED + DENY receipt
```

`HASH_VERIFIED + DENY` means that byte identity is known. It does not mean the source has been legally reviewed, mapped to a rule, clinically reviewed, or admitted to production.

## Replacement semantics

An evidence receipt is not silently overwritten. Re-running capture against an existing manifest fails closed. `--replace-manifest` is an explicit operator action and still cannot promote `productionUse`, create `legalReviewRef`, or set model authority.

The capture tool also rejects:

- symlink input files;
- a symlink archive root;
- empty artifacts;
- artifacts above the configured maximum size;
- non-HTTPS canonical URLs for non-synthetic sources;
- mutable network fetching inside the capture command.

## Storage boundary

A local read-only bit is tamper resistance for development evidence; it is **not WORM storage** and is not a legal-retention guarantee. Production evidence still requires an approved content-addressed store, retention policy, access control, backup/restore proof, incident response, and a signed legal-review receipt.

## Deterministic lifecycle tests

The shared lifecycle tests cover exact source identity, exact artifact hash binding, mapping status, future events, contiguous event sequence, self-declared admission, revocation, and rollback. A manifest cannot become production-active merely by setting a Boolean.

Production promotion remains blocked until all exact source snapshots, field mappings, safety tests, user-facing wording, rollback identity, and qualified review evidence are present. Exact-head hosted checks must execute successfully before a stacked PR can leave Draft.

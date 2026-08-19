# Build artifact identity

## Why this exists

GitHub Actions artifact archive digests and signed Android APK whole-file hashes are transport identities, not stable semantic product identities. A documentation-only commit can produce a different debug APK whole-file SHA-256 when the hosted runner uses a different ephemeral debug signing key, even when every APK ZIP entry is byte-identical.

The repository therefore records two different evidence lanes:

```text
transport identity
  = whole artifact bytes, including signing/container metadata

semantic payload identity
  = sorted payload path + uncompressed size + SHA-256(content)
```

Neither identity is a release-signing or provenance attestation. Production release signing remains a separate Human Admit boundary.

## Observed Shadow Architect evidence

Hosted runs #88 and #89 built two adjacent heads where #89 changed only `docs/implementation-status.md`.

- Web distribution: 27 files, 66,612,465 bytes. Canonical tree digest was identical across both runs: `a7d89b35399e73d209df28933434b2688fc9635ebbdc1d3eceaaef93acbbbac5`.
- Android APK: 561 ZIP entries and every extracted entry hash were identical, but the whole-file APK hashes differed. Binary differences were confined to the APK signing/container tail, consistent with runner-local debug signing identity rather than product payload drift.

This observation motivates the verifier; it does not claim production reproducible builds.

## Tool

```bash
python3 scripts/validate_artifact_identity.py self-test

python3 scripts/validate_artifact_identity.py apk \
  androidApp/build/outputs/apk/debug/androidApp-debug.apk \
  --output build/evidence/android-artifact-identity.json

python3 scripts/validate_artifact_identity.py tree \
  webApp/build/dist \
  --output build/evidence/web-artifact-identity.json
```

`self-test` proves that different ZIP metadata/compression can change the transport hash while preserving the semantic payload hash, and that an actual payload mutation changes the semantic identity.

## State machine

```text
BUILT
  -> TRANSPORT_HASHED
  -> PAYLOAD_ENUMERATED
  -> SEMANTIC_PAYLOAD_HASHED
  -> RECEIPT_EMITTED
  -> HOSTED_ARTIFACT_UPLOADED
```

The receipt does not advance any of these external states:

```text
DEBUG_SIGNED != RELEASE_SIGNED
SEMANTIC_PAYLOAD_HASHED != REPRODUCIBLE_BUILD_PROVEN
HOSTED_ARTIFACT_UPLOADED != SUPPLY_CHAIN_ATTESTED
```

## CI ownership

The Android/Web hosted job emits and uploads the two JSON receipts after the corresponding builds. The policy job runs the synthetic self-test so identity semantics fail closed before build publication.

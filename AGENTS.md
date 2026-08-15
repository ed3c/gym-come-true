# AGENTS.md — Gym Come True execution contract

This repository builds an evidence-first fitness protocol product for Android, iOS, and Web. Agents must preserve the safety, rights, privacy, and platform boundaries below even when a task asks for faster delivery.

## Current stack and state

```text
PR #2  AUDITABLE_CROSS_PLATFORM_FOUNDATION
  └─ PR #15  TAIWAN_EVIDENCE_CONTRACT_DRAFT
       └─ Issue #8  REVIEWED_TAIWAN_RULE_PACK          # not complete

Issues #9/#10  native health + alarm work
Issue  #11     licensed exercise catalog
Issue  #12     private LLM explanation gateway
Issues #13/#14 store/release + market validation
```

State machine:

```text
EMPTY_REPOSITORY
  -> AUDITABLE_CROSS_PLATFORM_FOUNDATION       # PR #2
  -> TAIWAN_EVIDENCE_CONTRACT_DRAFT            # PR #15
  -> REVIEWED_TAIWAN_RULE_PACK                  # Issue #8
  -> LICENSED_EXERCISE_CATALOG                  # Issue #11
  -> NATIVE_HEALTH_AND_ALARM_INTEGRATION        # Issues #9 / #10
  -> PRIVATE_LLM_EXPLANATION_GATEWAY            # Issue #12
  -> STORE_RELEASE_CANDIDATE                     # Issues #13 / #14
```

Do not claim a later state merely because an interface, stub, prompt, schema, synthetic fixture, or sample record exists.

### Stack lineage rule

PR #15 is a stacked PR whose base is PR #2's branch. If PR #2 advances, first compare the new foundation head with the current PR #15 head. Preserve evidence history. Prefer a non-force relock/merge when the slices are compatible; do not rewrite or discard admitted evidence commits merely to make the graph look linear. After relock, require `behind_by=0` relative to the foundation head and record the exact head in the PR body.

A GitHub Actions job with `steps=[]`, no runner allocation, or an explicit Actions-budget annotation is an infrastructure receipt. It is neither a PASS nor a product-code failure.

## Hard invariants

### OCR_IS_EVIDENCE_NOT_TRUTH

- OCR and barcode results begin as `UNVERIFIED`.
- Preserve the physical-label confirmation step.
- Never infer a missing serving size, ingredient, unit, product identity, or daily amount.
- Raw label images are temporary by default.
- Production corpus retention requires explicit consent, encrypted storage, expiry/deletion, withdrawal support, hashes, and provenance.

### TAIWAN_RULE_PACK_DEFAULT_DENY

- `DRAFT` is inspectable, never production executable.
- A schema-valid manifest is not a clinically reviewed pack.
- Every production rule requires immutable source evidence, exact rule/source mapping, an effective window, deterministic conflict handling, required safety cases, qualified reviewer coverage, reviewed wording identity, and rollback identity.
- Missing source snapshots, source hashes, reviewer qualification, conflict-of-interest record, rule coverage, wording review, or rollback evidence must fail closed.
- Do not fabricate MOHW/TFDA snapshot hashes or convert a live URL/dataset ID into an immutable evidence claim.

### MEDIA_DEFAULT_DENY

- Publicly reachable is not the same as redistributable.
- Do not add images, GIFs, video, SVG anatomy maps, 3D models, scraped IDs, or CDN links unless provenance and `legal/media-registry.json` establish the allowed scope and SHA-256.
- Do not hotlink ExerciseDB or another vendor CDN.
- Keep metadata, media, rendering code, and user-generated uploads as separate rights domains.

### LLM_EXPLANATION_ONLY

- Deterministic code owns unit conversion, arithmetic, warnings, blocking decisions, rule-pack admission, and protocol state.
- A model may explain a structured result; it may not calculate or recommend dosage, diagnose, suppress a warning, fill missing evidence, or become `modelUsedForDecision=true`.
- The client must not call a model provider directly. Future model access goes through a server-side policy gateway with minimized payloads and audit logs.

### NO_CLIENT_PROVIDER_SECRETS

- Never commit API keys, signing material, service-account credentials, store secrets, or private health-rule packs.
- Mobile and web artifacts must be safe to inspect and reverse engineer.
- Use protected CI/store secret systems only after the corresponding delivery issue explicitly permits them.

### REVIEWED_HEALTH_RULES_ONLY

- Generic mass conversion is limited to `mcg/µg/μg`, `mg`, and `g`.
- `IU`, volume, container count, proprietary blends, medication interactions, pregnancy, procedures, and symptoms fail closed.
- A daily total is an arithmetic observation, not a safe or recommended dose.
- A Taiwan or other regional rule pack is not production-ready until source evidence, reviewer evidence, version, effective date, tests, wording review, and rollback are recorded.

### HONEST_ALARM_SEMANTICS

- Android `set`/`setAndAllowWhileIdle` and iOS local notifications are reminders, not guaranteed alarms.
- Exact-alarm special access and AlarmKit require their own permission, review, fallback, and store-policy work.
- AlarmKit provides a system stop control; never claim a movement challenge can remove it.
- Never market background delivery as 100% reliable without measured evidence and platform qualification.

## Module ownership

```text
shared/
  domain models, deterministic safety, Taiwan evidence/rule-pack admission,
  daily ledger, protocol compiler, decision receipts, tests, shared Compose UI

androidApp/
  Android permissions, system camera hand-off, ML Kit, temporary files,
  notifications, future Health Connect and exact-alarm adapters

iosApp/
  SwiftUI host, PhotosPicker, Vision evidence adapter, UserNotifications,
  future HealthKit and AlarmKit adapters

webApp/
  JS/Wasm compatibility host and browser-safe features; no native-health parity claim

legal/ + data/ + assets/
  provenance and admission truth; production admission must fail closed

docs/
  product, legal, safety, Taiwan evidence, marketing, and delivery decisions

scripts/
  repository policy and Taiwan rule-pack contract validation
```

Shared code must not import Android, Apple, browser, store, or model-provider APIs.

## Canonical iOS source set

`iosApp/project.yml` is the only admitted iOS build specification. It explicitly lists the Swift files that CI compiles. Do not create a second “safe” project file to hide a broken default build. New Swift files enter the build only through an explicit `project.yml` change and hosted validation.

The current hosted iOS validation contract is the foundation's macOS 26 / Xcode 26 path. Do not downgrade CI to an older Xcode merely to hide missing current-SDK linkage without a documented compatibility reason.

## Required commands

For the current PR #15 stack:

```bash
python3 scripts/validate_repository.py
python3 scripts/validate_taiwan_rule_pack.py
sh ./gradlew :shared:jvmTest
sh ./gradlew :androidApp:assembleDebug :androidApp:lintDebug
sh ./gradlew :webApp:composeCompatibilityBrowserDistribution
```

On macOS with XcodeGen:

```bash
cd iosApp
xcodegen generate --spec project.yml
xcodebuild \
  -project GymComeTrue.xcodeproj \
  -scheme GymComeTrue \
  -sdk iphonesimulator \
  -configuration Debug \
  CODE_SIGNING_ALLOWED=NO \
  build
```

A documentation-only change may skip platform builds only when it cannot alter executable policy, source admission, store claims, generated build inputs, evidence-state claims, or the stack lineage contract.

## Change protocol

1. Identify one state transition and its rollback.
2. Resolve the current PR/base head before editing a stacked branch.
3. Define allowed paths before modifying files.
4. Add deterministic tests before adding an LLM prompt.
5. Add source and media records before importing data or assets.
6. Keep platform permissions behind explicit user actions.
7. Record what was actually validated; separate local checks from hosted CI evidence.
8. If the base branch advances, relock the stack without destroying evidence lineage and verify `behind_by=0`.
9. Open/keep a draft PR until all exact-head required checks execute and pass.

## Evidence states

Use these terms precisely:

```text
UNKNOWN        no trustworthy evidence yet
CANDIDATE      evidence exists but is unverified/review pending
DRAFT          machine-readable but not production executable
REVIEWED       qualified review exists but admission gates still apply
ADMITTED       all deterministic production gates passed for an exact version/date
EXPIRED        outside the admitted effective window
REVOKED        explicitly withdrawn from production use
BLOCKED        required external or infrastructure prerequisite is unavailable
```

Do not collapse `DRAFT`, `REVIEWED`, and `ADMITTED` into one "done" state.

## Prohibited shortcuts

- No WebView shell presented as native KMP completion.
- No scraped production catalog.
- No remote media hotlink.
- No direct client-to-LLM provider key.
- No unreviewed supplement threshold or interaction table.
- No automatic schedule change based on OCR alone.
- No fabricated clinical, copyright, reliability, revenue, download, conversion, source-snapshot, reviewer, or CI claim.
- No treating an Actions budget failure as passing or failing application code.
- No force-resetting stacked evidence branches merely to hide divergence when an evidence-preserving relock is possible.
- No switching repository visibility, transferring ownership, weakening branch protection, or changing licensing without explicit owner action.

## Review questions

Every PR must answer:

- What evidence changed from unknown to known?
- What exact base/head lineage does this result depend on?
- Which deterministic invariant protects the user if OCR or a model is wrong?
- Which source/reviewer record authorizes every production health rule?
- Which rights record permits every new asset and field?
- What data leaves the device, and why is it necessary?
- What happens when permission, network, model, store, platform API, reviewer, source snapshot, or CI runner access fails?
- Which capability is still not implemented?

# AGENTS.md — Gym Come True execution contract

This repository builds an evidence-first fitness protocol product for Android, iOS, and Web. Agents must preserve the safety, rights, privacy, evidence-lineage, and platform boundaries below even when a task asks for faster delivery.

## Current stack and state

```text
PR #2   AUDITABLE_CROSS_PLATFORM_FOUNDATION
  └─ PR #15   TAIWAN_EVIDENCE_CONTRACT_DRAFT
       └─ PR #16   TAIWAN_SOURCE_LIFECYCLE_DRAFT
            └─ Issue #8   REVIEWED_TAIWAN_RULE_PACK     # not complete

Issues #9/#10   native health + alarm work
Issue  #11      licensed exercise catalog
Issue  #12      private LLM explanation gateway
Issues #13/#14  store/release + market validation
```

State machine:

```text
EMPTY_REPOSITORY
  -> AUDITABLE_CROSS_PLATFORM_FOUNDATION       # PR #2
  -> TAIWAN_EVIDENCE_CONTRACT_DRAFT            # PR #15
  -> TAIWAN_SOURCE_LIFECYCLE_DRAFT             # PR #16
  -> REVIEWED_TAIWAN_RULE_PACK                  # Issue #8
  -> LICENSED_EXERCISE_CATALOG                  # Issue #11
  -> NATIVE_HEALTH_AND_ALARM_INTEGRATION        # Issues #9 / #10
  -> PRIVATE_LLM_EXPLANATION_GATEWAY            # Issue #12
  -> STORE_RELEASE_CANDIDATE                     # Issues #13 / #14
```

Do not claim a later state merely because an interface, stub, prompt, schema, synthetic fixture, filename, URL, status field, or sample record exists.

## Stack lineage rule

- PR #15 is stacked on PR #2; PR #16 is stacked on PR #15.
- Before editing a stacked branch, resolve the current base head and dependent head.
- If a base advances, compare the new base with the dependent branch before modifying files.
- Preserve evidence history. Prefer a non-force merge/relock when slices are compatible.
- Never rewrite or discard admitted evidence commits merely to make the graph look linear.
- After relock, require `behind_by=0` relative to the intended base and record exact base/head lineage in the PR body.
- A GitHub Actions job with `steps=[]`, no runner allocation, or an Actions-budget annotation is an infrastructure receipt. It is neither a PASS nor a product-code failure.

## Hard invariants

### OCR_IS_EVIDENCE_NOT_TRUTH

- OCR and barcode results begin as `UNVERIFIED`.
- Preserve the physical-label confirmation step.
- Never infer a missing serving size, ingredient, unit, product identity, daily amount, warning, or expiry.
- Raw label images are temporary by default.
- Production corpus retention requires explicit consent, encrypted storage, expiry/deletion, withdrawal support, hashes, and provenance.
- A corrected field does not retroactively improve first-pass OCR accuracy; report first-pass accuracy and correction completion separately.

### TAIWAN_RULE_PACK_DEFAULT_DENY

- `DRAFT` is inspectable and never production executable.
- A schema-valid manifest is not a clinically reviewed pack.
- Every production rule requires immutable source evidence, exact source-field mapping, jurisdiction/effective window, deterministic conflict handling, required safety cases, qualified reviewer coverage, reviewed wording identity, and rollback identity.
- Missing source snapshots, hashes, mapping selectors, reviewer qualification, conflict-of-interest record, rule coverage, wording review, signatures, tests, or rollback evidence must fail closed.
- Do not fabricate MOHW/TFDA snapshot hashes or convert a live URL/dataset ID into an immutable evidence claim.
- No model-created health rule, threshold, interaction table, mapping, or promotion event.

### IMMUTABLE_SOURCE_LIFECYCLE

- `LIVE_URL != IMMUTABLE_EVIDENCE`.
- A live URL, attachment ID, dataset ID, filename, or schema-valid JSON is only a mutable candidate.
- Official candidates stay `CANDIDATE + DENY` until approved local bytes are captured, hashed, content-addressed, and legally reviewed for the exact intended scope.
- The source-capture command accepts approved local regular files only. It must not gain an HTTP client or silently recapture mutable sources during CI/application startup.
- Capture receipts default to `HASH_VERIFIED + DENY`; capture does not create legal review or production admission.
- `HASH_VERIFIED != LEGAL_REVIEWED`.
- `LEGAL_REVIEWED != CLINICALLY_REVIEWED`.
- A verified mapping binds matching `sourceId`/`snapshotId`, an exact selector, deterministic transform, target field, and excerpt SHA-256.
- Regulatory text, reference values, and tolerance ranges require qualified reviewer evidence before production use.
- Rule-pack lifecycle must follow signed `DRAFT -> REVIEWED -> STAGED -> ACTIVE` transitions.
- Suspend, resume, revoke, and rollback require reason code and incident identity; rollback must target the exact declared rollback version.
- An input manifest may not self-declare `productionAdmitted=true`; deterministic resolution computes admission for an exact version/date.

### MEDIA_DEFAULT_DENY

- Publicly reachable is not the same as redistributable.
- Do not add images, GIFs, video, SVG anatomy maps, 3D models, scraped IDs, or CDN links unless provenance and `legal/media-registry.json` establish allowed scope and immutable SHA-256.
- Do not hotlink ExerciseDB or another vendor CDN.
- Keep metadata, media, rendering code, model files, and user-generated uploads as separate rights domains.
- No asset enters production without attribution, derivative/redistribution scope, territory/term, and takedown path.

### LLM_EXPLANATION_ONLY

- Deterministic code owns unit conversion, arithmetic, warnings, blocking decisions, source/rule-pack admission, release-state resolution, and protocol state.
- A model may explain a structured immutable receipt; it may not calculate/recommend dosage, diagnose, suppress a warning, fill missing evidence, create rules/mappings, sign review, or become `modelUsedForDecision=true`.
- The client must not call a model provider directly. Future model access goes through a server-side policy gateway with minimized payloads, schema validation, provider/version trace, audit, limits, fallback, and kill switch.

### NO_CLIENT_PROVIDER_SECRETS

- Never commit API keys, signing material, service-account credentials, store secrets, private source archives, reviewer identities/signatures, or privileged production health-rule packs.
- Mobile and web artifacts must be safe to inspect and reverse engineer.
- Use protected CI/store/evidence systems only after the corresponding delivery issue explicitly permits them.

### REVIEWED_HEALTH_RULES_ONLY

- Generic mass conversion is limited to `mcg/µg/μg`, `mg`, and `g`.
- `IU`, volume, container count, proprietary blends, medication interactions, pregnancy, procedures, and symptoms fail closed.
- A daily total is an arithmetic observation, not a safe or recommended dose.
- Registration, business identity, food-additive text, or government publication does not establish personalized safety, efficacy, or medication compatibility.
- A Taiwan or other regional rule pack is not production-ready until source evidence, legal scope, exact mappings, reviewer evidence, version, effective dates, tests, wording review, promotion signatures, and rollback are recorded.

### HONEST_ALARM_SEMANTICS

- Android `set`/`setAndAllowWhileIdle` and iOS local notifications are reminders, not guaranteed alarms.
- Exact-alarm special access and AlarmKit require their own permission, review, fallback, store-policy, reboot/timezone, and measured reliability work.
- AlarmKit provides a system stop control; never claim a movement challenge can remove it.
- Never market background delivery as 100% reliable without measured device/platform evidence.

## Module ownership

```text
shared/
  domain models, deterministic safety, Taiwan evidence/rule-pack admission,
  immutable source/mapping/release lifecycle, daily ledger, protocol compiler,
  decision receipts, tests, shared Compose UI

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
  product, legal, safety, Taiwan evidence/source lifecycle, marketing, delivery decisions

scripts/
  repository policy, Taiwan rule-pack/source-lifecycle validation,
  and local-only content-addressed source capture
```

Shared code must not import Android, Apple, browser, store, model-provider, network-fetch, or secret-management APIs.

## Canonical iOS source set

`iosApp/project.yml` is the only admitted iOS build specification. It explicitly lists Swift files compiled by CI. Do not create a second “safe” project file to hide a broken default build. New Swift files enter the build only through an explicit `project.yml` change and hosted validation.

The current hosted iOS contract is macOS 26 / Xcode 26. Do not downgrade CI to an older Xcode merely to hide missing current-SDK linkage without a documented compatibility reason.

## Required commands

For the current PR #16 stack:

```bash
python3 scripts/validate_repository.py
python3 scripts/validate_taiwan_rule_pack.py
python3 scripts/validate_taiwan_source_lifecycle.py
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

A documentation-only change may skip platform builds only when it cannot alter executable policy, source admission, store claims, generated build inputs, evidence-state claims, source identities, or stack lineage.

## Change protocol

1. Identify one state transition and explicit rollback.
2. Resolve current PR/base/head lineage before editing a stacked branch.
3. Define allowed paths and evidence authority before modifying files.
4. Add deterministic tests before adding an LLM prompt.
5. Add source/media records before importing data or assets.
6. Capture official bytes only through an approved local process; never fabricate hashes.
7. Keep platform permissions and external data transmission behind explicit user actions.
8. Record what was actually validated; separate local, hosted, legal, clinical, and infrastructure evidence.
9. If a base advances, relock without destroying evidence lineage and verify `behind_by=0`.
10. Keep the PR Draft until every exact-head required check executes and passes and external gates are honestly recorded.

## Evidence states

Use these terms precisely:

```text
UNKNOWN         no trustworthy evidence yet
CANDIDATE       mutable source/evidence exists but is not immutable or admitted
CAPTURED        bytes were copied but hash/content-address verification is incomplete
HASH_VERIFIED   exact bytes, byte length, hash, and content address match
LEGAL_REVIEWED  approved storage/use scope exists; not clinical approval
DRAFT           machine-readable but not production executable
VERIFIED        exact mapping/contract evidence passes its deterministic checks
REVIEWED        qualified review exists but admission gates still apply
STAGED          reviewed exact version prepared for activation
ACTIVE          exact version is within its window and all gates currently pass
ADMITTED        deterministic production admission receipt for exact version/date
SUSPENDED       temporarily removed from active decisions due to an incident
EXPIRED         outside the admitted effective window
REVOKED         explicitly withdrawn from production use
ROLLED_BACK     exact prior version restored through signed incident flow
BLOCKED         required external or infrastructure prerequisite is unavailable
```

Do not collapse `HASH_VERIFIED`, `LEGAL_REVIEWED`, `REVIEWED`, `ACTIVE`, and `ADMITTED` into one “done” state.

## Prohibited shortcuts

- No WebView shell presented as native KMP completion.
- No scraped production catalog or remote media hotlink.
- No direct client-to-LLM provider key.
- No unreviewed supplement threshold or interaction table.
- No automatic schedule change based on OCR alone.
- No fabricated clinical, copyright, reliability, revenue, download, conversion, source-snapshot, legal-review, reviewer, signature, or CI claim.
- No treating an Actions budget failure as passing or failing application code.
- No network recapture of mutable official-source bytes during CI or application startup.
- No hand-editing source hashes, legal-review state, reviewer hashes, lifecycle signatures, or `productionAdmitted` to simulate admission.
- No force-resetting stacked evidence branches merely to hide divergence when evidence-preserving relock is possible.
- No switching repository visibility, transferring ownership, weakening branch protection, or changing licensing without explicit owner action.

## Review questions

Every PR must answer:

- What evidence changed from unknown/candidate to a stronger state?
- What exact base/head lineage does this result depend on?
- Which immutable source snapshot, exact mapping, legal review, and qualified reviewer record authorize each production health rule?
- Which deterministic invariant protects the user if OCR, a source, or a model is wrong?
- Which rights record permits every new asset and field?
- What data leaves the device, where does it go, and why is it necessary?
- What happens when permission, network, model, store, platform API, source snapshot, legal review, reviewer, or CI runner access fails?
- Which capability is still not implemented, not reviewed, or not admitted?

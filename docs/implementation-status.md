# Implementation status

**Snapshot date:** 2026-08-18  
**Repository:** `ed3c/gym-come-true` (private, immutable repository ID `1334805292`)  
**Current leaf:** `main`, fast-forwarded to `a70a52cc6e3e2f4107edae2f7bb2034029161568` plus the delivery-contract commit  
**Merge state:** the whole stack is integrated into `main`; hosted evidence is still absent

## Integrated stack

| PR | Transition | Merged head | Current meaning |
|---:|---|---|---|
| [#2](https://github.com/ed3c/gym-come-true/pull/2) | `EMPTY_REPOSITORY -> AUDITABLE_CROSS_PLATFORM_FOUNDATION` | `58492815f22af65665172bcf98bfb661639ece92` | KMP Android/iOS/Web foundation and default-deny safety/rights boundary |
| [#15](https://github.com/ed3c/gym-come-true/pull/15) | `FOUNDATION -> TAIWAN_EVIDENCE_CONTRACT_DRAFT` | `79f8a65b370806925c32f0a15da88c7c0d7bda36` | Product/corpus identity, OCR metrics, rule-pack admission, decision receipts |
| [#16](https://github.com/ed3c/gym-come-true/pull/16) | `EVIDENCE_DRAFT -> TAIWAN_SOURCE_LIFECYCLE_DRAFT` | `f58a2feac580ca37bb4d7b3c30e122908bfd6b07` | Immutable source, exact mapping, release/revoke/rollback contracts |
| [#20](https://github.com/ed3c/gym-come-true/pull/20) | `SOURCE_LIFECYCLE_DRAFT -> DOCUMENTED_DELIVERY_GRAPH_DRAFT` | `ad065c8ac944f2fb4f9d60e65b008367b1291c43` | README/AGENTS/architecture/roadmap/Git Town governance convergence |
| [#22](https://github.com/ed3c/gym-come-true/pull/22) | `DELIVERY_GRAPH_DRAFT -> GIT_TOWN_CANDIDATE_EVIDENCE_RECORDED` | `a70a52cc6e3e2f4107edae2f7bb2034029161568` | Pinned Git Town candidate metadata and disposable canary harness |
| Issue [#23](https://github.com/ed3c/gym-come-true/issues/23) | `CANDIDATE_RECORDED -> MACHINE_VERIFIED_STACKED_DELIVERY` | delivered on `main` | Machine-verifiable delivery manifest, schema, validator, and templates |

Issues #24–#40 and #46–#48 advanced to local draft/contract states on 2026-08-18 (see the
domain-lane table below). Issues #41–#45 remain human/external-owned. No lane state is a
completed PR, a hosted check, or an external-gate admission.

## Domain lane drafts (2026-08-18, local only)

Seven sibling lanes were implemented in isolated worktrees from `main` and merged back without
conflicts. Every external gate (legal/clinical review, real consent, provider/store credentials,
devices, licensed media, signatures) remains `ABSENT` / `HUMAN_ADMIT_REQUIRED`.

| Lane branch | Issues | Delivered local state |
|---|---|---|
| `agent/lane-taiwan-corpus` | #24 #25 #26 | `CONSENT_CONTRACT_DRAFT`, `OCR_EVALUATION_DRAFT`; #26 deterministic admission gate only (`EXTERNAL_GATES_ABSENT`) |
| `agent/lane-ios-native` | #27 #28 #29 | `IOS_EVIDENCE_HANDOFF_DRAFT`, `IOS_MINIMAL_HEALTH_READS_DRAFT`, reminder contract draft (device evidence `ABSENT`) |
| `agent/lane-android-health` | #30 #31 | Health Connect adapter contract draft, reminder reliability harness contract (OEM matrix `ABSENT`) |
| `agent/lane-exercise-catalog` | #32 #33 #34 #48 | `TAXONOMY_CONTRACT`; top-50 bilingual draft; media-admission contract core (0 admitted assets); muscle slug mapping + intensity semantics |
| `agent/lane-explanation-gateway` | #35 #36 #37 | `EXPLANATION_GATEWAY_CONTRACT_DRAFT`, stub-only provider boundary, adversarial corpus draft |
| `agent/lane-entitlement-privacy` | #38 #39 #40 | `VERIFIED_ENTITLEMENT_DRAFT`, `ACCOUNT_DATA_LIFECYCLE_DRAFT`; release-manifest schema only for #40 |
| `agent/lane-nutrition` | #46 #47 | Food-catalog schema/validator draft, deterministic meal-plan compiler with tests |

## Implemented by directory

### `shared/`

- English and Traditional Chinese supplement-label parser.
- Ingredient names containing numbers, comma/thousands, and decimal variants.
- Generic `mcg/µg/μg`, `mg`, and `g` mass normalization.
- Explicit refusal to generically convert IU, volume, activity, or container-count units.
- Deterministic `LOG`, `REVIEW`, and `BLOCK` decisions.
- Verified-mass daily ledger and cross-product duplicate detection.
- A 16:00 / B 22:00 protocol compiler with cross-midnight `dayOffset`.
- Immutable explanation boundary with dose recommendation and warning override disabled.
- Product-variant identity and nullable serving definition.
- Consent-aware corpus admission and image-retention boundary.
- Field-level OCR metrics separating first-pass accuracy from correction completion.
- Taiwan rule-pack admission, required safety cases, reviewer/wording/rollback gates, and decision receipts.
- Immutable source artifact, exact mapping, rule-pack release, suspension, revocation, expiry, and rollback contracts.
- Common tests and negative controls.
- Shared Compose dashboard, timeline, and repository-authored schematic muscle view.

### `androidApp/`

- Compose host.
- Explicit system-camera capture via `TakePicture`.
- Private cache `FileProvider`.
- Bundled ML Kit Chinese text recognition and barcode scanning.
- SHA-256 identity for recognized text.
- Unverified candidate parsing.
- Temporary image deletion on success/failure paths.
- Notification permission flow.
- Inexact local reminder and receiver.

Not yet implemented: Health Connect, reboot/timezone rescheduling, recurrence proof, exact-alarm need assessment, OEM/device reliability matrix.

### `iosApp/`

- SwiftUI host for shared Compose UI.
- One canonical XcodeGen spec: `iosApp/project.yml`.
- Canonical native bridge: `iosApp/GymComeTrue/NativeCapabilityBridge.swift`.
- PhotosPicker and Apple Vision OCR/barcode candidate extraction.
- Recognized-text SHA-256 and visible unverified-evidence summary.
- UserNotifications reminder control.
- Camera/photo usage-purpose strings.

Not yet implemented: direct camera flow, structured Swift/Kotlin evidence handoff completion, HealthKit, recurrence/timezone harness, AlarmKit assessment, real-device evidence.

### `webApp/`

- Compose JS executable.
- Compose Wasm executable.
- Wasm/JS compatibility distribution.
- Browser host and responsive viewport.

Not yet implemented: native camera/health parity, browser notification reliability, production hosting/release evidence.

### `data/`, `legal/`, and `assets/`

- Repository-authored exercise seed and first-party demo metadata.
- First-party schematic muscle asset with provenance.
- Default-deny source and media registries.
- Taiwan source candidates with prohibited-inference boundaries.
- Synthetic Traditional Chinese corpus fixture.
- Draft Taiwan rule-pack fixture.
- Synthetic immutable source snapshot and exact excerpt-hash fixture.
- JSON Schema transport contracts.
- No third-party exercise image/video/GIF/3D model or vendor CDN hotlink is admitted.
- No official MOHW/TFDA source byte is production-admitted.

### `scripts/`

- Repository policy validator.
- Taiwan evidence/rule-pack validator.
- Taiwan source lifecycle validator.
- Taiwan source capture hardening validator.
- Approved local-byte content-addressed source capture.
- Source capture has no HTTP client and defaults to `HASH_VERIFIED + DENY`.

### `.github/workflows/`

The workflow defines separate lanes for:

- policy and provenance;
- shared JVM, Android, lint, and Web compatibility;
- iOS framework/XcodeGen/unsigned simulator host.

Runner allocation and command execution are separate states.

## Current state machines

```text
Label:
NOT_CAPTURED
  -> TEMPORARY_CAPTURE
  -> UNVERIFIED_CANDIDATE
  -> USER_CONFIRMED | REJECTED
  -> LOG | REVIEW | BLOCK
  -> DECISION_RECEIPT

Taiwan source:
CANDIDATE
  -> CAPTURED
  -> HASH_VERIFIED
  -> LEGAL_REVIEWED
  -> VERIFIED_MAPPING
  -> REVIEWED
  -> STAGED
  -> ACTIVE
  -> SUSPENDED | EXPIRED | REVOKED | ROLLED_BACK

Media:
QUARANTINED
  -> RIGHTS_REVIEWED
  -> HASHED
  -> ADMITTED
  -> PACKAGED
  -> REVOKED
  -> REMOVED

Delivery:
TASK_PACKET_DRAFT
  -> LEASED
  -> LOCALLY_VERIFIED
  -> PUBLICATION_ALLOW/BLOCK
  -> REMOTE_ANCESTRY_VERIFIED
  -> TRUSTED_CHECKS
  -> HUMAN_ADMIT
```

## Partial or intentionally constrained capabilities

| Capability | Current behavior | Missing production evidence |
|---|---|---|
| Supplement recognition | Local OCR/barcode candidates | representative consented corpus, field confidence UX, real device evaluation |
| Daily ledger | confirmed mass arithmetic and duplicates | chemical-form ontology, clinically reviewed Taiwan limits |
| Taiwan supplement policy | admission/lifecycle contracts | exact official bytes, legal scope, verified mappings, qualified review, production rules |
| LLM | typed explanation boundary only | authenticated backend, provider, output validator, adversarial evals, audit, kill switch |
| Exercise catalog | first-party demo/schema | canonical top-50, independent bilingual authoring, retention evidence |
| Exercise media | first-party schematic only | executed licenses/commissioned assets, derivative pipeline, revocation drill |
| Android reminders | inexact local reminder | reboot/timezone/OEM harness and exact-alarm assessment |
| iOS reminders | local notification control | recurrence/timezone/device tests and AlarmKit assessment |
| Health data | architecture boundary | least-privilege Health Connect/HealthKit adapters and privacy evidence |
| Entitlements | product requirement only | server validation, restore/refund/offline grace, provider credentials |
| Store release | docs/CI foundation | signing, forms, privacy manifests, listings, support and rollback operations |
| Market validation | strategy and material guidance | real interviews, rights-cleared creator contracts, retained-contribution evidence |

## Not implemented or not admitted

- diagnosis, medication advice, personal supplement dose recommendation, or automatic stack optimization;
- clinically reviewed Taiwan dose/interaction rules;
- real consented production label corpus in Git;
- production exercise importer or third-party media;
- Health Connect or HealthKit;
- Android exact alarms or AlarmKit;
- login, cloud sync, backend database, active LLM provider, or provider keys;
- StoreKit/Play/Web production entitlement authority;
- App Store/Google Play signing and submission;
- production analytics or creator CRM;
- Git Town executable/config/runtime canaries.

## Git Town / Worker adoption status

| Item | State |
|---|---|
| Shared canonical Skill | `PASS` — resolved and reviewed |
| Repo-owned Git profile | Documented |
| Stack graph / work-packet template | Documented |
| Exact Git Town version and executable | `ABSENT` |
| Executable checksum/provenance/SBOM/notices/legal review | `ABSENT` |
| `.git-town.toml` | `NOT_IMPLEMENTED` |
| Linked worktree/lease canary | `NOT_EXERCISED` |
| No-push sync canary | `NOT_EXERCISED` |
| Conflict canary | `NOT_EXERCISED` |
| Publication canary | `NOT_EXERCISED` |
| Background sync | disabled |
| Merge/ship/promotion | Human Admit |

Documentation proves the intended method and branch graph. It does not prove a Git Town runtime.

## Verification contract

```bash
python3 scripts/validate_repository.py
python3 scripts/validate_taiwan_rule_pack.py
python3 scripts/validate_taiwan_source_lifecycle.py
python3 scripts/validate_taiwan_source_hardening.py

sh ./gradlew :shared:jvmTest
sh ./gradlew :androidApp:assembleDebug :androidApp:lintDebug
sh ./gradlew :webApp:composeCompatibilityBrowserDistribution
```

macOS:

```bash
cd iosApp
xcodegen generate --spec project.yml
xcodebuild \
  -project GymComeTrue.xcodeproj \
  -scheme GymComeTrue \
  -sdk iphonesimulator \
  -configuration Debug \
  CODE_SIGNING_ALLOWED=NO \
  ONLY_ACTIVE_ARCH=YES \
  ARCHS=arm64 \
  build
```

Only exact commands that actually ran against the stated commit count as `PASS` or `FAIL`.

## Hosted evidence

No workflow run on this repository has ever allocated a runner. PR #16 exact head
`f58a2feac580ca37bb4d7b3c30e122908bfd6b07` has workflow run `31878284072` (run #79); its three jobs
contain no executed steps and ended before runner allocation. GitHub's annotation states that an
Actions budget prevented further use. Every other run to date matches.

Classification:

```text
PRE_RUN_BLOCKED_BY_ACTIONS_BUDGET
```

This is neither hosted PASS nor product-code failure. Restoring capacity is Issue #45 and stays
owner-controlled.

## Local evidence

Local execution is not hosted evidence and does not satisfy any exact-head acceptance box. It is
recorded because it is the only lane that has actually executed, and because it found three defects
the blocked hosted lane never reported. See
[2026-08-16 local verification receipt](delivery/2026-08-16-local-verification.md).

## Next admitted work

The dependency and molecular branch plan is maintained in:

- [Roadmap](roadmap.md)
- [GitHub Issue / PR index](github-issue-index.md)
- [Molecular Stacked PR graph](git/STACKED_PRS.md)

External gates remain source/legal review, consented corpus, qualified Taiwan review, exercise-media rights, provider/store credentials, real-device evidence, Actions capacity, and Human Admit.

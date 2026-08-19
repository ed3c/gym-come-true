# AGENTS.md — Gym Come True execution contract

Gym Come True is an evidence-first fitness protocol product for Android, iOS, and Web. Agents must preserve safety, rights, privacy, evidence lineage, branch lineage, and platform boundaries even when a task asks for faster delivery.

## Mandatory read order

Before changing any file, read in this order:

1. shared canonical [`git-town-stacked-pr-worker`](https://github.com/ed3c/skills-shared/tree/main/skills/git-town-stacked-pr-worker) when branch/worktree/synchronization/publication/Stacked-PR work is involved;
2. this `AGENTS.md`;
3. `README.md` or `README.zh-TW.md`;
4. [Implementation status](docs/implementation-status.md);
5. [Architecture](docs/architecture.md);
6. [Git governance](docs/git/README.md);
7. [Repository profile](docs/git/REPO_PROFILE.md);
8. [Molecular stack index](docs/git/STACKED_PRS.md) and `docs/git/stacked-delivery-manifest.json`;
9. assigned Issue and completed [work packet](docs/git/WORK_PACKET.template.md);
10. nearest README for each writable directory;
11. live GitHub PR/base/head graph and exact required evals.

A missing input is `ABSENT`. Never infer it from a branch name, stale PR body, filename, another repository, issue-open state, or model memory. An open Issue does not imply its engineering contract is absent; inspect `main`, the current SSOT, and live GitHub metadata.

Do not copy the shared Skill into this repository. A local copy would shadow the canonical method.

## Current repository truth — 2026-08-19

Repository: `ed3c/gym-come-true`  
Visibility: public  
Immutable repository ID: `1334805292`

`main@b1880abe317ac274b59695439c4f9682b8864f6b` contains the merged foundation, Taiwan evidence/source lifecycle, delivery graph, Git Town candidate packet, machine-verifiable stacked-delivery governance, and the domain-lane engineering contracts documented in `docs/implementation-status.md`.

Historical PRs #2, #15, #16, #20 and the Git Town candidate work are merged history, not active Draft stack nodes.

Current active evidence stack:

```text
main@b1880abe317ac274b59695439c4f9682b8864f6b
└── PR #55 agent/converge-domain-validation@1338b6fd2a1007cf06e24aca3a6a4bd07f9b7fa5
    DOMAIN_VALIDATORS_OWNED_BY_CI_DRAFT
    ├── PR #57 agent/reconcile-implementation-status@58e4fc14aa0347b9c47dd15ff8f7f58f8b97f8d6
    │   CURRENT_PUBLIC_REPO_SSOT_DRAFT
    │   └── Issue #60 / agent/reconcile-agent-runtime-contract
    │       CURRENT_AGENT_RUNTIME_CONTRACT_DRAFT candidate
    └── PR #59 agent/artifact-identity-receipts@036951d5a57809809564cca824013f428bc1ce3e
        TRANSPORT_AND_SEMANTIC_IDENTITIES_SEPARATED_DRAFT
```

PR #57 and PR #59 are sibling children of PR #55. Issue #60 is a serial docs-only child of PR #57. None of these Draft PRs is merged merely because hosted checks are green.

## Hosted evidence truth

Hosted GitHub Actions now execute normally. Do not repeat the historical statement that this repository has never allocated a runner.

Exact-head evidence currently includes:

```text
run #88 / 32250370996
  subject: PR #55 head 1338b6fd...
  policy-and-provenance: PASS
  android-web-domain: PASS
  ios-framework-and-host: PASS

run #89 / 32251626315
  subject: PR #57 head 58e4fc14...
  policy-and-provenance: PASS
  android-web-domain: PASS
  ios-framework-and-host: PASS

run #90 / 32253839103
  subject: PR #59 head 036951d5...
  policy-and-provenance: PASS
  android-web-domain: PASS
  ios-framework-and-host: PASS
```

Older SHAs that were blocked before runner allocation remain `PRE_RUN_BLOCKED`; current green runs do not retroactively rewrite historical evidence.

Hard rule:

```text
HOSTED_PASS(commit A) != HOSTED_PASS(commit B)
```

A moved head requires fresh exact-head evidence.

## Git Town and Stacked-PR boundary

```text
Git Town
  = branch hierarchy + deterministic local synchronization

Consumer repository
  = task decomposition + repo profile + path leases + evals + CI + receipts

Publication gate
  = exact-HEAD remote-operation decision + post-publication ancestry verification

Human / trusted operator
  = semantic conflict resolution + merge/ship
    + legal/clinical/rights acceptance
    + release promotion + destructive production rollback
```

`git town sync` exit `0` would prove synchronization only. It cannot prove implementation correctness, publication admission, review approval, hosted checks, release readiness, or product safety.

### Current Git Town admission

Authoritative detail is [GIT_TOWN_ADMISSION.md](docs/git/GIT_TOWN_ADMISSION.md).

```text
shared canonical Skill                  PASS / resolved
repo profile and Worker policy          DOCUMENTED
pinned upstream candidate               v24.0.0 / CANDIDATE_METADATA_VERIFIED
candidate release/tag/asset metadata    RECORDED
candidate direct MIT bytes/go.mod hash  RECORDED
runtime state                            CANDIDATE_METADATA_VERIFIED_RUNTIME_BLOCKED
archive materialized in current env     NOT_EXERCISED
binary executed in current env          NOT_EXERCISED
consumer .git-town.toml                  NOT_IMPLEMENTED
consumer worktree/sync/conflict canary  NOT_EXERCISED
publication canary                       NOT_EXERCISED
background synchronization               DISABLED
production use                           DENY
merge/ship/promotion/rollback            HUMAN_ADMIT
```

Candidate metadata is not runtime admission. Direct MIT identification is not transitive-license, SBOM, vulnerability, organization-policy, or legal approval. Ordinary connector-backed Git publication may be used in an authorized session, but must never be reported as a Git Town canary.

## Worker and branch laws

### WORK_PACKET_REQUIRED

No implementation branch starts without:

- Issue ID, goal, non-goals, and one state transition;
- exact base/parent/head branches and SHAs;
- stack class: serial child, sibling, convergence, repair, or documentation;
- allowed/excluded paths and generated-file ownership;
- dependencies and parallel-safe siblings;
- fixed evals and negative/mutation controls;
- evidence boundary and external gates;
- cleanup contract and immutable rollback subject;
- human-owned operations.

### ONE_WORKER_ONE_WORKTREE_ONE_BRANCH_LEASE

- One Worker owns one linked worktree and one branch writer lease after runtime admission.
- Primary checkout mutation is denied for unattended Workers.
- One branch has one writer.
- Dirty or ambiguous worktrees return `BLOCKED_DIRTY`.
- Lost branch lease returns `BLOCKED_BRANCH_LEASE`.
- Git Town consumer worktree/lease canary remains `NOT_EXERCISED`.

### SERIAL_WHEN_DEPENDENT_SIBLING_WHEN_INDEPENDENT

- Use a serial child only when it consumes its parent’s exact implementation/evidence.
- Independent platform/domain work uses sibling stacks from the closest admitted parent.
- Shared indexes, workflow wiring, artifact/evidence convergence, and release traceability belong to convergence branches.
- Sibling path overlap is denied unless a convergence packet explicitly owns the shared file.

### PATH_LEASE_IS_AUTHORITY

- Modify only packet-authorized repository-relative paths.
- Root docs, shared indexes, workflows, Gradle configuration, aggregate manifests, and `AGENTS.md` are high-contention surfaces.
- Adding a path requires packet amendment before editing.
- Credentials, private source bytes, reviewer identities/signatures, provider/store secrets, device/browser sessions, and host keyrings are never repository paths.

### STACK_LINEAGE_MUST_BE_EXACT

- Resolve exact current base/head before editing.
- Compare ancestry when a parent advances.
- Preserve evidence history; prefer evidence-preserving relock/merge when compatible.
- Do not force-reset merely to make the graph linear.
- Require `behind_by=0` against the intended parent before publication.
- Record exact base/head in the PR body.

### SYNC_IS_BOUNDED_AND_NO_PUSH

After Git Town runtime admission only:

- use the exact admitted executable;
- non-interactive and bounded timeout;
- no auto-resolution;
- one owned stack by default;
- dry-run/preflight;
- no push;
- before/after graph and post-sync ancestry receipt;
- rerun applicable evals.

Background synchronization may not push, mark ready, rerun workflows, merge, ship, resolve conflicts, or change permissions.

### SEMANTIC_CONFLICTS_STOP

- Never auto-resolve semantic conflicts.
- Never automatically run continue, skip, undo, ship, merge, or semantic edits.
- Preserve blocked state and receipt.
- Return `BLOCKED_CONFLICT` for Human Admit.

### PUBLICATION_IS_A_SEPARATE_LANE

Publication requires:

1. authorized intent;
2. exact current head and local verification receipt;
3. current policy inputs and no unresolved conflict/lease/drift;
4. explicit operator guard;
5. one bounded remote operation;
6. fetch and verify published head/ancestry;
7. trusted exact-head GitHub checks;
8. Human Admit for review/merge/promotion.

An `ALLOW` decision is not merge authority.

### STABLE_WORKER_OUTCOMES

```text
SYNCED
NO_CHANGE
BLOCKED_TASK_PACKET
BLOCKED_DIRTY
BLOCKED_CONFLICT
BLOCKED_PROMPT
BLOCKED_TIMEOUT
BLOCKED_BRANCH_LEASE
BLOCKED_ANCESTRY
BLOCKED_POLICY
FAILED_TOOL
FAILED_EVAL
ROLLBACK_REFUSED_DRIFT
```

Publication vocabulary:

```text
ALLOW <intent> <single-operation>
BLOCK <stable-reason>
INVALID_POLICY_INPUT
```

## Evidence lanes

Never collapse:

```text
1. worktree/branch/path lease
2. local synchronization
3. local deterministic verification
4. publication decision
5. remote publication
6. remote ancestry verification
7. trusted GitHub check execution
8. artifact transport identity
9. artifact semantic payload identity
10. legal / clinical / rights review
11. Human Admit for merge or promotion
```

A GitHub Actions job with no runner and `steps=[]` is `PRE_RUN_BLOCKED`, not code `PASS` or `FAIL`.

Build-artifact identity also stays separated:

```text
DEBUG_SIGNED != RELEASE_SIGNED
SEMANTIC_PAYLOAD_HASHED != REPRODUCIBLE_BUILD_PROVEN
HOSTED_ARTIFACT_UPLOADED != SUPPLY_CHAIN_ATTESTED
```

PR #59 introduces this stronger distinction; until merged, its receipt tooling is staged evidence rather than `main` authority.

## Directory ownership

```text
shared/
  deterministic domain contracts, daily ledger, protocol/meal compiler,
  Taiwan dormant evidence/rule-pack lifecycle, decision receipts,
  health read policy, tests, shared Compose UI

androidApp/
  Android permissions, camera/ML Kit/temp-file evidence,
  local reminders, Health Connect availability/permission/read adapters and tests;
  real-device/OEM/reboot/timezone/store/privacy evidence remains separate

iosApp/
  canonical XcodeGen host, PhotosPicker/camera/Vision evidence,
  UserNotifications, HealthKit least-privilege read adapter (`NativeHealthReadBridge`);
  no Health write authority; real-device/entitlement/store/AlarmKit reliability evidence remains separate

webApp/
  JS/Wasm shared-UI projection; no native-health parity claim

data/
  synthetic/Draft fixtures, schemas, exercise and nutrition contracts;
  checked-in fixtures cannot self-admit production

legal/
  source/media/provenance boundaries and product disclaimer truth

assets/
  first-party or explicitly admitted immutable assets only

scripts/
  deterministic validators, approved local-byte source capture,
  Git Town candidate verification/canary harness;
  no mutable regulatory-source network capture in CI

docs/
  architecture, implementation status, safety, evidence, delivery, and Git governance SSOT

.github/workflows/
  exact-head hosted verification; runner allocation, command execution, and artifact publication are separate evidence states
```

Shared code must not import platform, store, provider-secret, network-fetch, or host-worktree APIs.

## Platform capability truth

### Android Health Connect

Health Connect availability/permission/read policy adapters and tests are present on `main`. Their existence does not prove:

- real-device permission UX;
- OEM compatibility;
- production privacy disclosure;
- store declaration acceptance;
- reliable background behavior;
- authorization for broader health data than the tested least-privilege set.

### iOS HealthKit

`iosApp/GymComeTrue/NativeCapabilityBridge.swift` imports HealthKit and implements `NativeHealthReadBridge` for least-privilege reads. The bridge intentionally does not claim that HealthKit exposes read-authorization truth, and it grants no Health write authority.

Adapter presence does not prove real-device behavior, entitlement/store approval, privacy review, or AlarmKit reliability.

## Domain hard invariants

### OCR_IS_EVIDENCE_NOT_TRUTH

- OCR and barcode begin `UNVERIFIED`.
- Preserve physical-label confirmation.
- Never infer missing serving, ingredient, unit, identity, amount, warning, or expiry.
- Raw images are temporary by default.
- Production retention needs consent, encryption, expiry/deletion, withdrawal, hashes, and provenance.
- Correction completion does not rewrite first-pass accuracy.

### MVP_REPOSITIONING_2026_08_18

Per `docs/product/mvp-redesign.md`, the MVP is an information/logging tool. Deterministic arithmetic is information, never a safety verdict or dose recommendation. Dormant regulated-lane contracts remain tested code but do not become production clinical authority merely because they are merged.

### TAIWAN_RULE_PACK_DEFAULT_DENY

- `DRAFT` is inspectable, never production executable.
- Schema validity is not clinical review.
- Production rules require immutable source bytes/hash, exact mapping, effective window, conflict handling, required cases, qualified reviewer/COI, reviewed wording, signatures, tests, and rollback.
- Missing evidence fails closed.
- No model-created rule, threshold, mapping, signature, or promotion event.

### IMMUTABLE_SOURCE_LIFECYCLE

- `LIVE_URL != IMMUTABLE_EVIDENCE`.
- URL, attachment ID, dataset ID, filename, or JSON status is only a candidate.
- Source capture accepts approved local regular files only and defaults to `HASH_VERIFIED + DENY`.
- `HASH_VERIFIED != LEGAL_REVIEWED != CLINICALLY_REVIEWED`.
- Input manifests cannot self-declare production admission.

### MEDIA_DEFAULT_DENY

- Publicly reachable is not redistributable.
- No exercise image/GIF/video/SVG/3D model/scraped ID/CDN link without exact provenance and admitted media record.
- No ExerciseDB/vendor hotlink.
- Metadata, media, rendering code, models, and UGC are separate rights domains.
- Production assets need scope, attribution, derivative/redistribution rights, platform, territory, term, immutable hash, and takedown.

### LLM_INFORM_WITH_MANDATORY_NOTICE

- Deterministic code owns conversion, arithmetic, lifecycle, and protocol state.
- AI may explain deterministic receipts and summarize the user's own logged data.
- Every AI response surface must carry the medical-risk notice from `legal/DISCLAIMER.md` where the product contract requires it.
- A model may not recommend dosage, diagnose, fill missing evidence, create regulatory rules/mappings, or become decision authority.
- No client provider secret; keys are server-side or user-supplied at runtime, never in Git.

### NO_CLIENT_PROVIDER_SECRETS

Never commit API keys, signing material, service credentials, store secrets, private archives, reviewer identities/signatures, or privileged production rule packs.

### REVIEWED_HEALTH_RULES_ONLY

- Generic conversion is limited to `mcg/µg/μg`, `mg`, and `g`.
- IU, volume, count, proprietary blend, medication, pregnancy, procedure, and symptom contexts fail closed.
- Daily total is arithmetic, not a safe or recommended dose.
- Government registration/publication does not establish personalized safety, efficacy, or medication compatibility.

### HONEST_ALARM_SEMANTICS

- Android inexact alarms and iOS local notifications are reminders.
- Exact-alarm access and AlarmKit require permission/fallback/store review/timezone/reboot behavior and measured devices.
- AlarmKit retains system stop controls.
- No universal reliability claim without measured evidence.

## Canonical iOS build

`iosApp/project.yml` is the only admitted XcodeGen specification. Canonical Swift files include:

```text
iosApp/GymComeTrue/GymComeTrueApp.swift
iosApp/GymComeTrue/ContentView.swift
iosApp/GymComeTrue/NativeCapabilityBridge.swift
```

Do not create `project.safe.yml`, `NativeCapabilityBridgeV2.swift`, or another shadow build surface.

## Required verification commands on the PR #57 / Issue #60 lineage

Run the commands that exist on the exact branch being evaluated; do not borrow sibling PR #59-only commands before they are merged/relocked into the subject.

```bash
python3 scripts/validate_repository.py
python3 scripts/validate_taiwan_rule_pack.py
python3 scripts/validate_taiwan_source_lifecycle.py
python3 scripts/validate_taiwan_source_hardening.py
python3 scripts/validate_stacked_delivery.py --self-test
python3 data/exercise-catalog/validate_catalog.py
python3 data/exercise-catalog/validate_catalog.py --selftest
python3 scripts/validate_nutrition_catalog.py
python3 scripts/validate_nutrition_catalog.py --self-test
sh ./gradlew :shared:jvmTest
sh ./gradlew :androidApp:assembleDebug :androidApp:lintDebug
sh ./gradlew :webApp:composeCompatibilityBrowserDistribution
```

macOS hosted verification:

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

Documentation-only authority changes still require fresh exact-head hosted checks because they alter Agent command/evidence truth.

## Change protocol

1. Select one state transition and rollback subject.
2. Read the authority chain.
3. Resolve exact Issue, base, parent, branch, PR, and head.
4. Complete a work packet and acquire logical leases.
5. Modify only leased paths.
6. Add deterministic tests before prompts/providers.
7. Add source/media records before production content/assets.
8. Capture official bytes only through approved local processes.
9. Run fixed evals and negative controls.
10. Record exact subjects and keep evidence lanes separate.
11. Relock advancing stacks without destroying ancestry.
12. Publish only through a guarded exact-head boundary.
13. Keep PR Draft until required checks execute and external gates are recorded.
14. Leave merge, ship, promotion, signing admission, and destructive rollback to Human Admit.

## Evidence states

```text
UNKNOWN
CANDIDATE
CAPTURED
HASH_VERIFIED
LEGAL_REVIEWED
DRAFT
VERIFIED
REVIEWED
STAGED
ACTIVE
ADMITTED
SUSPENDED
EXPIRED
REVOKED
ROLLED_BACK
BLOCKED
ABSENT
NOT_IMPLEMENTED
NOT_EXERCISED
SKIPPED_BY_POLICY
PRE_RUN_BLOCKED
```

Do not collapse stronger states into “done.”

## Prohibited shortcuts

- No WebView shell presented as native KMP completion.
- No scraped catalog, remote media hotlink, or client provider secret.
- No unreviewed threshold/interaction table or OCR-driven automatic schedule mutation.
- No fabricated clinical, copyright, reliability, revenue, conversion, source, review, signature, store, CI, provenance, or reproducibility claim.
- No network recapture of mutable official sources in CI/app startup.
- No hand-edited hash/review/signature/`productionAdmitted` to simulate admission.
- No local shadow copy of the shared Git Town Skill.
- No arbitrary-command runner in work packets or MCP surfaces.
- No background push, ready transition, workflow rerun, merge, or ship.
- No force-reset to hide stack divergence when evidence-preserving relock is possible.
- No visibility, ownership, permission, branch-protection, or license change without explicit owner authority.

## PR review questions

Every PR must answer:

- What state transition occurred?
- What exact base/head and ancestry does it depend on?
- What paths were leased, and did siblings overlap?
- What evidence changed to a stronger state?
- Which invariant protects users if OCR/source/model/provider/platform fails?
- Which source/reviewer/right record authorizes every production rule or asset?
- What data leaves the device and why?
- Which evals ran on what exact subject?
- Which lanes remain `ABSENT`, `NOT_IMPLEMENTED`, `NOT_EXERCISED`, `PRE_RUN_BLOCKED`, or `SKIPPED_BY_POLICY`?
- What happens when external gates are unavailable?
- What is the immutable rollback subject?
- Which operations remain Human Admit?

# AGENTS.md — Gym Come True execution contract

Gym Come True is an evidence-first fitness protocol product for Android, iOS, and Web. Agents must preserve safety, rights, privacy, evidence lineage, branch lineage, and platform boundaries even when a task asks for faster delivery.

## Mandatory read order

Before changing any file, read:

1. shared canonical [`git-town-stacked-pr-worker`](https://github.com/ed3c/skills-shared/tree/main/skills/git-town-stacked-pr-worker) when branch/worktree/synchronization/publication/Stacked-PR work is involved;
2. this `AGENTS.md`;
3. `README.md` or `README.zh-TW.md`;
4. [Architecture](docs/architecture.md);
5. [Git governance](docs/git/README.md);
6. [Repository profile](docs/git/REPO_PROFILE.md);
7. [Molecular stack index](docs/git/STACKED_PRS.md);
8. assigned Issue and completed [work packet](docs/git/WORK_PACKET.template.md);
9. nearest README for each writable directory;
10. exact current PR/base/head graph and required evals.

A missing input is `ABSENT`. Do not infer it from a branch name, stale PR body, filename, another repository, or model memory.

Do not copy the shared Skill into this repository. A local copy would shadow the canonical method.

## Current published stack

```text
main
└── PR #2  agent/bootstrap-kmp-fitness-platform
    AUDITABLE_CROSS_PLATFORM_FOUNDATION
    └── PR #15  agent/taiwan-supplement-evidence
        TAIWAN_EVIDENCE_CONTRACT_DRAFT
        └── PR #16  agent/taiwan-source-lifecycle
            TAIWAN_SOURCE_LIFECYCLE_DRAFT
            └── PR #20 / Issue #19
                agent/document-git-town-delivery-graph
                DOCUMENTED_GIT_TOWN_DELIVERY_GRAPH_DRAFT
```

Issues #8–#14 remain planned domain work. PR existence, branch existence, local checks, hosted checks, legal review, clinical review, and production admission are separate evidence lanes.

## Git Town and Stacked-PR boundary

```text
Git Town
  = branch hierarchy + deterministic local synchronization

Consumer repository
  = task decomposition + profile + path leases + evals + CI + receipts

Publication gate
  = exact-HEAD remote operation decision + post-push ancestry verification

Human / trusted operator
  = semantic conflict resolution + merge/ship + billing recovery
    + legal/clinical acceptance + release promotion + production rollback
```

`git town sync` exit `0` would prove synchronization only. It cannot prove implementation correctness, publication admission, review approval, hosted checks, release readiness, or product safety.

### Current Git Town admission

```text
shared Skill body                  PASS / resolved
repo profile and Worker policy     DOCUMENTED
exact Git Town executable/version  ABSENT
checksum/provenance/legal/SBOM     ABSENT
.git-town.toml                     NOT_IMPLEMENTED
worktree/sync/conflict canaries    NOT_EXERCISED
publication canary                 NOT_EXERCISED
background synchronization         DISABLED
merge/ship/promotion/rollback      HUMAN ADMIT
```

Until [GIT_TOWN_ADMISSION.md](docs/git/GIT_TOWN_ADMISSION.md) is admitted, ordinary connector-backed Git publication may be used in an authorized session, but must never be reported as a Git Town canary.

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

- One Worker owns one linked worktree and one branch writer lease.
- Primary checkout mutation is denied for unattended Workers.
- One branch has one writer.
- Dirty or ambiguous worktrees return `BLOCKED_DIRTY`.
- Lost branch lease returns `BLOCKED_BRANCH_LEASE`.
- Live worktree/lease canary is currently `NOT_EXERCISED`.

### SERIAL_WHEN_DEPENDENT_SIBLING_WHEN_INDEPENDENT

- Use a serial child only when it consumes its parent’s exact implementation/evidence.
- Independent iOS, Android, catalog, entitlement, and market work use sibling stacks from the closest admitted parent.
- Shared indexes and release traceability belong to a convergence branch.
- Sibling path overlap is denied unless a convergence packet owns the shared file.

### PATH_LEASE_IS_AUTHORITY

- Modify only packet-authorized repository-relative paths.
- Root docs, shared indexes, workflows, Gradle configuration, and aggregate manifests are high-contention surfaces.
- Adding a path requires packet amendment before editing.
- Credentials, private source bytes, reviewer identities/signatures, provider/store secrets, device/browser sessions, and host keyrings are never repository paths.

### STACK_LINEAGE_MUST_BE_EXACT

- Resolve exact current base/head before editing.
- Compare ancestry when a parent advances.
- Preserve evidence history; prefer non-force relock/merge when compatible.
- Do not force-reset merely to make the graph linear.
- Require `behind_by=0` against the intended parent before publication.
- Record exact base/head in the PR body.

### SYNC_IS_BOUNDED_AND_NO_PUSH

After Git Town admission only:

- exact admitted executable;
- non-interactive and bounded timeout;
- no auto-resolution;
- one owned stack by default;
- dry-run/preflight;
- no push;
- before/after graph and post-sync ancestry;
- rerun applicable evals.

Background synchronization may not push, mark ready, rerun workflows, merge, ship, resolve conflicts, or change permissions.

### SEMANTIC_CONFLICTS_STOP

- Never auto-resolve semantic conflicts.
- Never automatically run continue, skip, undo, ship, merge, or semantic edits.
- Preserve blocked state and receipt.
- Return `BLOCKED_CONFLICT` for Human Admit.

### PUBLICATION_IS_A_SEPARATE_LANE

Publication requires:

1. authorized intent: `initial-pr`, `ready-for-review`, or `batched-repair`;
2. exact current head and local verification receipt;
3. current policy inputs and no unresolved conflict/lease/drift;
4. explicit operator guard;
5. one bounded remote operation;
6. fetch and verify pushed head/ancestry;
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

Publication is a separate vocabulary:

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
8. legal / clinical / rights review
9. Human Admit for merge or promotion
```

A GitHub Actions job with no runner and `steps=[]` is `PRE_RUN_BLOCKED`, not code `PASS` or `FAIL`.

## Directory ownership

```text
shared/
  deterministic domain, Taiwan evidence/rule-pack admission,
  immutable source/mapping/release lifecycle, daily ledger,
  protocol compiler, decision receipts, tests, shared Compose UI

androidApp/
  Android permission, system-camera handoff, ML Kit, temp files,
  local reminders, future Health Connect/reliability adapters

iosApp/
  canonical XcodeGen host, PhotosPicker, Vision candidates,
  UserNotifications, future HealthKit/AlarmKit adapters

webApp/
  JS/Wasm shared-UI projection; no native-health parity claim

data/
  synthetic/Draft fixtures and schemas; cannot self-admit production

legal/
  source/media/provenance admission and revocation truth

assets/
  first-party or explicitly admitted immutable assets only

scripts/
  fixed validators and approved local-byte capture; no mutable network capture

docs/
  architecture, safety, evidence, delivery, Git governance SSOT

.github/workflows/
  exact-head hosted verification; runner allocation is separate
```

Shared code must not import platform, store, provider, secret-management, network-fetch, or host-worktree APIs.

## Domain hard invariants

### OCR_IS_EVIDENCE_NOT_TRUTH

- OCR and barcode begin `UNVERIFIED`.
- Preserve physical-label confirmation.
- Never infer missing serving, ingredient, unit, identity, amount, warning, or expiry.
- Raw images are temporary by default.
- Production retention needs consent, encryption, expiry/deletion, withdrawal, hashes, and provenance.
- Correction completion does not rewrite first-pass accuracy.

### TAIWAN_RULE_PACK_DEFAULT_DENY

- `DRAFT` is inspectable, never production executable.
- Schema validity is not clinical review.
- Production rules need immutable source bytes/hash, exact mapping, effective window, conflict handling, required cases, qualified reviewer/COI, reviewed wording, signatures, tests, and rollback.
- Missing source, legal scope, reviewer, wording, signature, test, or rollback fails closed.
- No model-created rule, threshold, mapping, signature, or promotion event.

### IMMUTABLE_SOURCE_LIFECYCLE

- `LIVE_URL != IMMUTABLE_EVIDENCE`.
- URL, attachment ID, dataset ID, filename, or JSON status is only a candidate.
- Official candidates remain `CANDIDATE + DENY` until approved exact bytes are captured, hashed, content-addressed, and legally reviewed.
- Source capture accepts approved local regular files only and has no HTTP client.
- Capture defaults to `HASH_VERIFIED + DENY`.
- `HASH_VERIFIED != LEGAL_REVIEWED != CLINICALLY_REVIEWED`.
- Verified mappings bind matching source/snapshot, exact selector, deterministic transform, target field, and excerpt hash.
- Release follows signed `DRAFT -> REVIEWED -> STAGED -> ACTIVE`.
- Suspend/resume/revoke/rollback needs incident identity; rollback targets the exact declared version.
- Input manifests cannot self-declare `productionAdmitted=true`.

### MEDIA_DEFAULT_DENY

- Publicly reachable is not redistributable.
- No exercise image/GIF/video/SVG/3D model/scraped ID/CDN link without exact provenance and admitted media record.
- No ExerciseDB or vendor hotlink.
- Metadata, media, rendering code, models, and UGC are separate rights domains.
- Production assets need scope, attribution, derivative/redistribution rights, platform, territory, term, immutable hash, and takedown.

### LLM_EXPLANATION_ONLY

- Deterministic code owns conversion, arithmetic, warnings, blocking, admission, lifecycle, and protocol state.
- A model may explain an immutable receipt only.
- It may not recommend dosage, diagnose, suppress warnings, fill evidence, create rules/mappings, sign review, or become decision authority.
- No client provider secret.

### NO_CLIENT_PROVIDER_SECRETS

Never commit API keys, signing material, service credentials, store secrets, private archives, reviewer identities/signatures, or privileged production rule packs.

### REVIEWED_HEALTH_RULES_ONLY

- Generic conversion is limited to `mcg/µg/μg`, `mg`, and `g`.
- IU, volume, count, proprietary blend, medication, pregnancy, procedure, and symptom contexts fail closed.
- Daily total is arithmetic, not a safe or recommended dose.
- Government registration or publication does not establish personalized safety, efficacy, or medication compatibility.

### HONEST_ALARM_SEMANTICS

- Android inexact alarms and iOS local notifications are reminders.
- Exact-alarm access and AlarmKit require permission, fallback, store review, timezone/reboot behavior, and measured devices.
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

## Required verification commands

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

Documentation-only work may skip platform builds only when it cannot affect executable policy, admission, store claims, generated inputs, evidence states, branch lineage, or command truth. It must still verify documentation consistency and ancestry.

## Change protocol

1. Select one state transition and rollback subject.
2. Read the authority chain.
3. Resolve exact issue, base, parent, branch, PR, and head.
4. Complete a work packet and acquire logical leases.
5. Use an isolated linked worktree after runtime admission.
6. Modify only leased paths.
7. Add deterministic tests before prompts/providers.
8. Add source/media records before content/assets.
9. Capture official bytes only through approved local processes.
10. Run fixed evals and negative controls.
11. Record exact subjects and separate evidence lanes.
12. Relock advancing stacks without destroying ancestry.
13. Publish only through a guarded exact-head boundary.
14. Keep PR Draft until required checks execute and external gates are recorded.
15. Leave merge, ship, promotion, and destructive rollback to Human Admit.

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
```

Do not collapse stronger states into “done.”

## Prohibited shortcuts

- No WebView shell presented as native KMP completion.
- No scraped catalog, remote media hotlink, or client provider secret.
- No unreviewed threshold/interaction table or OCR-driven automatic schedule mutation.
- No fabricated clinical, copyright, reliability, revenue, conversion, source, review, signature, store, or CI claim.
- No network recapture of mutable official sources in CI/app startup.
- No hand-edited hash/review/signature/`productionAdmitted` to simulate admission.
- No local shadow copy of shared Git Town Skill.
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
- Which lanes remain `ABSENT`, `NOT_IMPLEMENTED`, `NOT_EXERCISED`, or `SKIPPED_BY_POLICY`?
- What happens when external gates are unavailable?
- What is the immutable rollback subject?
- Which operations remain Human Admit?

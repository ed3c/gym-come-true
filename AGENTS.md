# AGENTS.md — Gym Come True execution contract

Gym Come True is an evidence-first fitness protocol product for Android, iOS, and Web. Agents must preserve safety, rights, privacy, evidence lineage, branch lineage, and platform boundaries even when a task asks for faster delivery.

## Mandatory read order

Before changing any file, read in this order:

1. the shared canonical [`git-town-stacked-pr-worker`](https://github.com/ed3c/skills-shared/tree/main/skills/git-town-stacked-pr-worker) Skill when branch, worktree, synchronization, publication, or Stacked PR work is involved;
2. this root `AGENTS.md`;
3. root `README.md` or `README.zh-TW.md`;
4. [Architecture](docs/architecture.md);
5. [Git governance index](docs/git/README.md);
6. [Repository profile](docs/git/REPO_PROFILE.md);
7. [Stacked PR index](docs/git/STACKED_PRS.md);
8. the assigned GitHub Issue / work packet;
9. the nearest `README.md` for every writable directory;
10. the exact current PR/base/head graph and required eval commands.

A missing required input is `ABSENT`. Do not infer it from a branch name, an old PR body, a filename, another repository, or model memory.

Do not copy the shared Skill into `.agents/skills/` or another project-local location. A local copy would shadow the canonical shared method.

## Current published stack

```text
main
└── PR #2  agent/bootstrap-kmp-fitness-platform
    AUDITABLE_CROSS_PLATFORM_FOUNDATION
    └── PR #15  agent/taiwan-supplement-evidence
        TAIWAN_EVIDENCE_CONTRACT_DRAFT
        └── PR #16  agent/taiwan-source-lifecycle
            TAIWAN_SOURCE_LIFECYCLE_DRAFT
            └── Issue #19 / agent/document-git-town-delivery-graph
                DOCUMENTED_GIT_TOWN_DELIVERY_GRAPH_DRAFT
```

Issues #8–#14 are planned domain work, not completed PRs. The authoritative planned branch graph is [docs/git/STACKED_PRS.md](docs/git/STACKED_PRS.md).

## Git Town and Stacked PR boundary

```text
Git Town
  = branch hierarchy + deterministic local synchronization

Consumer repository
  = task decomposition + repo profile + path leases + evals + CI + receipts

GitHub publication gate
  = exact-HEAD publication decision + remote ancestry verification

Human / trusted operator
  = semantic conflict resolution + merge/ship + billing recovery
    + legal/clinical acceptance + release promotion + production rollback
```

`git town sync` exit `0` would prove only that synchronization completed. It would not prove implementation correctness, publication admission, review approval, hosted checks, release readiness, or product safety.

### Current Git Town admission state

- Shared Skill body: resolved.
- Repo profile and policy documents: present in `docs/git/`.
- Exact Git Town executable/version/checksum/provenance/SBOM/notices/legal approval: `ABSENT`.
- `.git-town.toml`: `NOT_IMPLEMENTED`.
- Worktree, no-push sync, conflict, and publication canaries: `NOT_EXERCISED`.
- Background synchronization: disabled.
- Merge/ship/promotion/rollback: Human Admit only.

Until [GIT_TOWN_ADMISSION.md](docs/git/GIT_TOWN_ADMISSION.md) is fully admitted, Agents may document the branch graph and use ordinary connector-backed Git publication, but must not report a real Git Town run.

## Worker and branch laws

### WORK_PACKET_REQUIRED

No implementation branch may start without a complete work packet containing:

- issue ID;
- goal and non-goals;
- base branch, parent branch, head branch, and stack class;
- allowed paths and excluded paths;
- dependencies and parallel-safe siblings;
- required evals and negative/mutation controls;
- evidence boundary;
- cleanup contract;
- rollback subject;
- human-owned operations.

Use [WORK_PACKET.template.md](docs/git/WORK_PACKET.template.md).

### ONE_WORKER_ONE_WORKTREE_ONE_BRANCH_LEASE

- One Worker owns one isolated linked worktree and one branch writer lease.
- Primary/shared checkout mutation is denied for unattended Workers.
- A Worker must not write a branch leased by another Worker.
- Dirty or ambiguous worktrees return `BLOCKED_DIRTY`.
- Lease loss returns `BLOCKED_BRANCH_LEASE`.
- The repository currently documents this contract; the live worktree/lease canary is `NOT_EXERCISED`.

### SERIAL_WHEN_DEPENDENT_SIBLING_WHEN_INDEPENDENT

- A child branch is serial only when it consumes its parent’s exact code/evidence transition.
- Independent domains use sibling branches from the closest common admitted parent.
- iOS, Android, catalog, entitlement, and market stacks must not be forced into one artificial serial chain.
- Shared indexes, aggregate manifests, and release traceability belong to a dedicated convergence branch.
- Sibling path overlap is denied unless one convergence packet explicitly owns the shared file.

### PATH_LEASE_IS_AUTHORITY

- Modify only paths named in the active work packet.
- Treat generated files, shared indexes, root docs, and aggregate manifests as separately leased surfaces.
- Adding a path after work begins requires an amended task packet before the edit.
- Secrets, private source archives, reviewer identities/signatures, provider/store credentials, device sessions, browser profiles, and host keyrings are never writable repository paths.

### STACK_LINEAGE_MUST_BE_EXACT

- Resolve current base and head before editing a stacked branch.
- If a base advances, compare ancestry before touching files.
- Preserve evidence history. Prefer a non-force merge/relock when compatible.
- Do not force-reset a stack merely to make the graph look linear.
- After relock require `behind_by=0` against the intended base and record the exact base/head in the PR.
- A stale PR body is documentation debt, not branch truth.

### SYNC_IS_BOUNDED_AND_NO_PUSH

When Git Town is eventually admitted:

- use the exact admitted version and executable;
- suppress prompts;
- use a bounded timeout;
- disable auto-resolution;
- default to one owned stack;
- default to no push;
- run dry-run/preflight and post-sync ancestry checks;
- rerun required evals after synchronization.

Background synchronization may never call `git town sync --push`, raw `git push`, mark ready for review, rerun a workflow, merge, ship, or resolve semantic conflicts.

### SEMANTIC_CONFLICTS_STOP

- Do not auto-resolve semantic conflicts.
- Do not run automatic `continue`, `skip`, `undo`, `ship`, merge, or semantic edits.
- Preserve the blocked worktree and receipt.
- Return `BLOCKED_CONFLICT` and hand the exact subjects to a human/trusted operator.

### PUBLICATION_IS_A_SEPARATE_LANE

Publication requires all of the following:

1. a task packet authorizing one portable intent: `initial-pr`, `ready-for-review`, or `batched-repair`;
2. current local verification bound to exact `HEAD`;
3. publication policy inputs that are not stale or ambiguous;
4. explicit operator guard;
5. one bounded remote operation;
6. post-push fetch and ancestry verification;
7. trusted GitHub checks;
8. Human Admit for review/merge/promotion.

A gate result `ALLOW` is not merge authority.

### STABLE_WORKER_OUTCOMES

Use only stable outcomes for Worker orchestration:

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

Publication is a different lane:

```text
ALLOW <intent> <single-operation>
BLOCK <stable-reason>
INVALID_POLICY_INPUT
```

## Completion evidence lanes

Keep these lanes separate:

```text
1. branch/worktree lease
2. local synchronization
3. local deterministic verification
4. publication decision
5. remote publication
6. remote ancestry verification
7. trusted GitHub check execution
8. legal / clinical / rights review
9. Human Admit for merge or promotion
```

One lane cannot proxy another. A documentation review cannot prove a live Git Town canary. A local PASS cannot replace exact-head hosted checks. A GitHub Actions job with no runner and `steps=[]` is `PRE_RUN_BLOCKED`, not code `PASS` or `FAIL`.

## Directory ownership

```text
shared/
  deterministic domain, Taiwan evidence/rule-pack admission,
  immutable source/mapping/release lifecycle, daily ledger,
  protocol compiler, decision receipts, tests, shared Compose UI

androidApp/
  Android permissions, system-camera handoff, ML Kit,
  temporary files, local reminders, future Health Connect/reliability adapters

iosApp/
  canonical XcodeGen host, PhotosPicker, Vision evidence adapter,
  UserNotifications, future HealthKit and AlarmKit adapters

webApp/
  JS/Wasm shared-UI projection and browser-safe capabilities;
  no native-health or guaranteed-notification parity claim

data/
  synthetic/Draft fixtures and schemas; fixtures cannot self-admit production

legal/
  source/media/provenance admission, prohibited-use boundaries, revocation truth

assets/
  first-party or explicitly admitted immutable assets only

scripts/
  fixed policy validators and approved local-byte capture;
  no mutable network capture during CI/application startup

docs/
  architecture, product, safety, evidence, Git governance, and delivery SSOT

.github/workflows/
  exact-head hosted verification and artifacts; runner allocation is separate
```

Shared code must not import Android, Apple, browser, store, provider, network-fetch, secret-management, or host-worktree APIs.

## Domain hard invariants

### OCR_IS_EVIDENCE_NOT_TRUTH

- OCR and barcode output begins `UNVERIFIED`.
- Keep the physical-label confirmation step.
- Never infer a missing serving size, ingredient, unit, product identity, daily amount, warning, or expiry.
- Raw label images are temporary by default.
- Production corpus retention requires explicit consent, encryption, expiry/deletion, withdrawal support, hashes, and provenance.
- Correction completion never rewrites first-pass OCR accuracy.

### TAIWAN_RULE_PACK_DEFAULT_DENY

- `DRAFT` is inspectable and never production executable.
- Schema validity is not clinical review.
- Every production rule requires immutable source evidence, exact mapping, jurisdiction/effective window, deterministic conflicts, required safety cases, qualified reviewer coverage, reviewed wording identity, signatures, and rollback identity.
- Missing source bytes, hash, selector, legal scope, reviewer qualification/COI, wording review, tests, or rollback fails closed.
- No model-created rule, threshold, interaction table, exact mapping, signature, or promotion event.

### IMMUTABLE_SOURCE_LIFECYCLE

- `LIVE_URL != IMMUTABLE_EVIDENCE`.
- URL, attachment ID, dataset ID, filename, or JSON status is only a candidate.
- Official candidates remain `CANDIDATE + DENY` until exact approved bytes are captured, hashed, content-addressed, and legally reviewed for intended scope.
- Source capture accepts approved local regular files only and must not acquire an HTTP client.
- Capture defaults to `HASH_VERIFIED + DENY`.
- `HASH_VERIFIED != LEGAL_REVIEWED`.
- `LEGAL_REVIEWED != CLINICALLY_REVIEWED`.
- Verified mappings bind matching source/snapshot IDs, exact selector, deterministic transform, target field, and excerpt SHA-256.
- Rule release follows signed `DRAFT -> REVIEWED -> STAGED -> ACTIVE`.
- Suspend/resume/revoke/rollback requires incident identity; rollback targets the exact declared prior version.
- Input manifests cannot self-declare `productionAdmitted=true`.

### MEDIA_DEFAULT_DENY

- Publicly reachable does not mean redistributable.
- Do not add exercise images, GIFs, video, anatomy SVGs, 3D models, scraped IDs, or CDN links without exact provenance and an admitted media record.
- Do not hotlink ExerciseDB or another vendor CDN.
- Separate metadata, media, rendering code, model files, and UGC rights.
- Production assets require attribution, derivative/redistribution scope, platform, territory, term, immutable hash, and takedown path.

### LLM_EXPLANATION_ONLY

- Deterministic code owns unit conversion, arithmetic, warnings, blocking decisions, admission, lifecycle resolution, and protocol state.
- A model may explain an immutable structured receipt.
- It may not recommend dosage, diagnose, suppress warnings, fill evidence, create rules/mappings, sign review, or become `modelUsedForDecision=true`.
- Clients never call a provider with a client-side secret.
- Future access uses an authenticated server policy gateway with minimized payloads, output schema, trace, limits, fallback, audit, and kill switch.

### NO_CLIENT_PROVIDER_SECRETS

- Never commit API keys, signing material, service-account credentials, store secrets, private archives, reviewer identities/signatures, or privileged production rule packs.
- Mobile and web artifacts must be safe to inspect and reverse engineer.
- Protected CI/store/evidence systems are configured only through separately authorized human-owned operations.

### REVIEWED_HEALTH_RULES_ONLY

- Generic conversion is limited to `mcg/µg/μg`, `mg`, and `g`.
- IU, volume, container count, proprietary blends, medication interaction, pregnancy, procedure, and symptom contexts fail closed.
- Daily totals are arithmetic observations, not safe or recommended doses.
- Registration, business identity, additive text, or a government publication does not establish personalized safety, efficacy, or medication compatibility.

### HONEST_ALARM_SEMANTICS

- Android inexact alarms and iOS local notifications are reminders, not guaranteed alarms.
- Exact-alarm access and AlarmKit require permission, fallback, store review, reboot/timezone behavior, and measured device evidence.
- AlarmKit retains system stop controls; do not claim a movement challenge removes them.
- Never claim universal reliability without measured platform/device evidence.

## Canonical iOS build

`iosApp/project.yml` is the only admitted XcodeGen specification.

Admitted Swift files include:

```text
iosApp/GymComeTrue/GymComeTrueApp.swift
iosApp/GymComeTrue/ContentView.swift
iosApp/GymComeTrue/NativeCapabilityBridge.swift
```

Do not create `project.safe.yml`, `NativeCapabilityBridgeV2.swift`, or another shadow build surface to hide a broken canonical build.

## Required verification commands

For the current PR #16 + Issue #19 documentation stack:

```bash
python3 scripts/validate_repository.py
python3 scripts/validate_taiwan_rule_pack.py
python3 scripts/validate_taiwan_source_lifecycle.py
python3 scripts/validate_taiwan_source_hardening.py

sh ./gradlew :shared:jvmTest
sh ./gradlew :androidApp:assembleDebug :androidApp:lintDebug
sh ./gradlew :webApp:composeCompatibilityBrowserDistribution
```

On macOS:

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

Documentation-only work may skip platform builds only when it cannot affect executable policy, source admission, store claims, generated inputs, evidence-state claims, branch lineage, or validation command truth. It must still run a documentation consistency review and compare ancestry.

## Change protocol

1. Select one state transition and rollback subject.
2. Read the mandatory authority chain.
3. Resolve exact issue, base, parent, branch, PR, and head.
4. Complete a work packet and acquire logical branch/path leases.
5. Use an isolated linked worktree when the admitted runtime supports it.
6. Modify only leased paths.
7. Add deterministic tests before LLM prompts or provider integration.
8. Add source/media records before importing data or assets.
9. Capture official bytes only through approved local processes.
10. Run fixed evals and negative controls.
11. Record exact subjects and separate local/hosted/legal/clinical evidence.
12. Relock advancing stacks without destroying ancestry.
13. Publish only through a guarded exact-head boundary.
14. Keep PR Draft until required checks execute and external gates are recorded.
15. Leave merge, ship, promotion, and destructive rollback to Human Admit.

## Evidence states

```text
UNKNOWN         no trustworthy evidence
CANDIDATE       mutable or unverified evidence
CAPTURED        bytes copied; content verification incomplete
HASH_VERIFIED   exact bytes/length/hash/content address match
LEGAL_REVIEWED  intended storage/use scope approved; not clinical
DRAFT           machine-readable, not production executable
VERIFIED        exact contract/mapping deterministic checks pass
REVIEWED        qualified review exists; admission gates still apply
STAGED          exact reviewed version prepared for activation
ACTIVE          exact version is effective and gates currently pass
ADMITTED        deterministic production receipt for exact version/date
SUSPENDED       temporarily removed after incident
EXPIRED         outside effective window
REVOKED         explicitly withdrawn
ROLLED_BACK     exact previous version restored through incident flow
BLOCKED         required external/infrastructure prerequisite unavailable
ABSENT          required artifact or evidence does not exist
NOT_IMPLEMENTED capability intentionally missing
NOT_EXERCISED   runtime canary has not run
SKIPPED_BY_POLICY operation deliberately not run
```

Do not collapse stronger states into “done.”

## Prohibited shortcuts

- No WebView shell presented as native KMP completion.
- No scraped production catalog or remote media hotlink.
- No direct client-to-provider secret.
- No unreviewed threshold or interaction table.
- No automatic schedule mutation from OCR alone.
- No fabricated clinical, copyright, reliability, revenue, conversion, source, review, signature, store, or CI claim.
- No treating Actions budget failure as code success or failure.
- No network recapture of official mutable sources in CI or app startup.
- No hand-editing hashes, review state, signatures, or `productionAdmitted` to simulate admission.
- No local shadow copy of the shared Git Town Skill.
- No arbitrary-command runner in work packets or MCP surfaces.
- No background push, ready transition, workflow rerun, merge, or ship.
- No force-resetting a stack to hide divergence when an evidence-preserving relock is possible.
- No repository visibility, ownership, branch-protection, permission, or license changes without explicit owner authority.

## PR review questions

Every PR must answer:

- What state transition occurred?
- What exact base/head and ancestry does the result depend on?
- What paths were leased, and did any sibling overlap?
- What evidence changed from `UNKNOWN`/`CANDIDATE` to a stronger state?
- Which deterministic invariant protects users when OCR, a source, a model, a provider, or a platform fails?
- Which source/reviewer/right record authorizes every new production rule or asset?
- What data leaves the device, where, and why?
- Which evals actually ran, and on what exact subject?
- Which lanes are `ABSENT`, `NOT_IMPLEMENTED`, `NOT_EXERCISED`, or `SKIPPED_BY_POLICY`?
- What happens when permission, network, model, store, source, legal review, reviewer, credential, or CI runner is unavailable?
- What is the immutable rollback subject?
- Which operations remain Human Admit?

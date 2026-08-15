# Git Town admission state

## Current decision

```yaml
state: BLOCKED_ABSENT_EXECUTABLE
runtime_admitted: false
config_admitted: false
live_canaries_exercised: false
background_sync_enabled: false
publication_enabled: false
```

The repository has adopted the shared method at the documentation/policy layer only. It has not admitted a Git Town executable.

## Canonical method evidence

```yaml
shared_repository: ed3c/skills-shared
skill_path: skills/git-town-stacked-pr-worker/SKILL.md
resolved_ref: main
observed_blob_sha: eb2d915bca3e8a3938625f7d33a10fae95a15769
consumer_shadow_copy: denied
```

`main` is sufficient to identify what was inspected during this documentation task, but it is not an executable identity. Runtime admission must pin an immutable Git Town release and its evidence independently.

## Admission matrix

| Evidence dimension | Current state | Required before admission |
|---|---|---|
| Git Town source repository | `ABSENT` | exact upstream/package source |
| Version/tag/commit | `ABSENT` | immutable version; never `latest` |
| Platform/architecture | `ABSENT` | execution-host subjects |
| Executable/package checksum | `ABSENT` | SHA-256 or host-owned immutable receipt |
| Provenance | `ABSENT` | release/package provenance |
| Direct license bytes/digest | `ABSENT` | exact license and SHA-256 |
| Transitive dependency/SBOM review | `ABSENT` | result and evidence reference |
| Required notices | `ABSENT` | reviewed notice set |
| Organization/legal approval | `ABSENT` | explicit decision for intended use |
| Binary version command | `NOT_EXERCISED` | exact output bound to executable hash |
| `.git-town.toml` | `NOT_IMPLEMENTED` | generated only after policy admission |
| Feature/perennial strategy canary | `NOT_EXERCISED` | observed behavior in disposable test repo |
| Linked-worktree canary | `NOT_EXERCISED` | primary checkout remains untouched |
| Branch/path lease canary | `NOT_EXERCISED` | duplicate/overlap controls fail closed |
| Dry-run no-push sync | `NOT_EXERCISED` | before/after graph and no remote movement |
| Planted conflict canary | `NOT_EXERCISED` | semantic conflict stops without auto-resolution |
| Prompt suppression canary | `NOT_EXERCISED` | no editor/credential prompt |
| Timeout canary | `NOT_EXERCISED` | bounded termination and receipt |
| Publication canary | `NOT_EXERCISED` | two guards + one operation + post-push verify |
| Cleanup/residue canary | `NOT_EXERCISED` | safe lease/worktree cleanup evidence |

No unchecked row may be inferred as complete.

## Why `.git-town.toml` is absent

A configuration file would imply branch-strategy and tool behavior before the repository has established:

- which exact Git Town version is executable;
- how that version interprets configuration;
- whether feature-branch rewrite is allowed;
- whether protected/perennial strategy is compatible with GitHub policy;
- whether creation/sync/push behavior matches the shared Worker contract;
- whether the executable and dependency/legal evidence is admitted.

Therefore:

```text
.git-town.toml = NOT_IMPLEMENTED
```

until the exact runtime and canaries are approved.

## Proposed policy after admission

This proposal is non-executable until approved:

```yaml
main-branch: main
feature-strategy: rebase
perennial-strategy: ff-only
new-branch-push: false
sync-default-scope: owned-stack
sync-auto-resolve: false
sync-push: false
background-sync: false
```

The actual syntax must be generated from the admitted Git Town version’s official schema, not from memory.

## Required admission procedure

### 1. Select exact executable

Record:

- source/package repository;
- immutable version/tag/commit;
- platform and architecture;
- acquisition method;
- checksum;
- provenance;
- version-command output.

### 2. Review policy and legal evidence

Record separately:

- direct license text and digest;
- transitive dependencies/SBOM;
- notices;
- host/package-manager/service terms;
- organization legal approval.

A permissive direct license does not automatically complete the other dimensions.

### 3. Create disposable canary repository

Do not make `gym-come-true` the first experimental subject.

Canary graph:

```text
main
└── feature-a
    └── feature-b
```

Plant:

- one clean synchronization;
- one stale parent;
- one deterministic semantic conflict;
- one dirty worktree;
- one duplicate lease;
- one sibling path overlap;
- one prompt attempt;
- one timeout;
- one unexpected remote movement.

### 4. Exercise worktree and leases

Prove:

- primary checkout mutation denied;
- linked worktree is isolated;
- branch writer lease is exclusive;
- path overlap fails closed;
- lease expiry/heartbeat/takeover is deterministic;
- blocked state is preserved.

### 5. Exercise no-push synchronization

Require:

- exact executable hash/version;
- non-interactive mode;
- no auto-resolve;
- bounded timeout;
- one owned stack;
- no remote movement;
- before/after graph;
- post-sync ancestry;
- eval rerun.

Expected safe outcomes:

```text
SYNCED
NO_CHANGE
BLOCKED_CONFLICT
BLOCKED_DIRTY
BLOCKED_TIMEOUT
BLOCKED_PROMPT
BLOCKED_ANCESTRY
```

### 6. Exercise conflict boundary

The planted semantic conflict must:

- stop synchronization;
- preserve the worktree;
- emit `BLOCKED_CONFLICT`;
- avoid automatic continue/skip/undo/ship;
- hand exact subjects to a human.

### 7. Exercise publication gate

In a disposable repository or authorized test branch:

- complete task packet;
- exact local head and verification;
- explicit `--publish`;
- environment guard;
- one allowed remote operation;
- fetch and verify pushed head/ancestry;
- do not merge or mark ready unless separately authorized.

### 8. Exercise cleanup and rollback

Prove:

- safe cleanup after success/no-change;
- preserve blocked worktree;
- release leases;
- refuse drifted rollback;
- no deletion of uncommitted human state.

### 9. Admit repository config

Only after canaries:

- create version-specific `.git-town.toml`;
- record file digest;
- run repository doctor;
- run one no-push stack sync;
- run repository evals;
- attach subject-bound receipts;
- obtain Human Admit.

## Required runtime report

```yaml
git_town_admission:
  executable:
    source: REQUIRED_ON_ADMISSION
    version: REQUIRED_ON_ADMISSION
    sha256: REQUIRED_ON_ADMISSION
    platform: REQUIRED_ON_ADMISSION
    architecture: REQUIRED_ON_ADMISSION
    provenance_ref: REQUIRED_ON_ADMISSION
  legal:
    direct_license: REQUIRED_ON_ADMISSION
    direct_license_sha256: REQUIRED_ON_ADMISSION
    sbom_or_transitive_review: REQUIRED_ON_ADMISSION
    notices_review: REQUIRED_ON_ADMISSION
    legal_approval: REQUIRED_ON_ADMISSION
  canaries:
    worktree_and_lease: REQUIRED_ON_ADMISSION
    no_push_sync: REQUIRED_ON_ADMISSION
    conflict_fail_closed: REQUIRED_ON_ADMISSION
    prompt_suppression: REQUIRED_ON_ADMISSION
    timeout: REQUIRED_ON_ADMISSION
    publication: REQUIRED_ON_ADMISSION_OR_SKIPPED_BY_POLICY_WITH_REASON
    cleanup_and_rollback: REQUIRED_ON_ADMISSION
  repository:
    config_sha256: REQUIRED_ON_ADMISSION
    doctor: REQUIRED_ON_ADMISSION
    no_push_sync: REQUIRED_ON_ADMISSION
    evals: REQUIRED_ON_ADMISSION_AND_EXACT_HEAD
  human_admit:
    reviewer_ref: REQUIRED_ON_ADMISSION
    decision: ADMIT_OR_REJECT
```

## Non-admission claims

The following do not prove runtime adoption:

- shared Skill URL resolves;
- this document exists;
- a branch graph is documented;
- a package is available in a package manager;
- a binary exists somewhere on a host;
- a sync command is described;
- a local Git rebase succeeded;
- a PR was created through the GitHub connector;
- an Actions workflow is queued;
- a model says the setup is correct.

Current truthful state remains `BLOCKED_ABSENT_EXECUTABLE`.

# Repository profile — Git Town Stacked-PR Worker

> Repository-owned profile for `ed3c/gym-come-true`. The shared canonical method remains in `ed3c/skills-shared`; this file does not copy it.

## Profile status

```yaml
schema: git-town-stacked-pr-worker/repo-profile/v1
profile_state: DOCUMENTED_NOT_RUNTIME_ADMITTED
updated_at: 2026-08-15
blocking_reason: BLOCKED_ABSENT_GIT_TOWN_EXECUTABLE
```

All required fields use explicit values or typed evidence states. `ABSENT` and `NOT_EXERCISED` block runtime claims.

## Identity

```yaml
repository:
  full_name: ed3c/gym-come-true
  immutable_identity: github-repository-id:1334805292
  visibility: private
  default_branch: main
  perennial_branches:
    - main
  allowed_remote_name: origin
  allowed_remote_url: https://github.com/ed3c/gym-come-true.git
  allowed_remote_url_pattern: '^https://github\.com/ed3c/gym-come-true\.git$'
  credential_bearing_remote_url: denied
```

## Authority documents

```yaml
authority:
  agents: AGENTS.md
  architecture: docs/architecture.md
  implementation_status: docs/implementation-status.md
  roadmap: docs/roadmap.md
  git_governance: docs/git/README.md
  stacked_pr_graph: docs/git/STACKED_PRS.md
  worker_protocol: docs/git/WORKER_PROTOCOL.md
  harness:
    - scripts/validate_repository.py
    - scripts/validate_taiwan_rule_pack.py
    - scripts/validate_taiwan_source_lifecycle.py
    - scripts/validate_taiwan_source_hardening.py
    - .github/workflows/verify.yml
  path_ownership: README.md#repository-map-directory-ownership-and-state-machines
  git_town_admission: docs/git/GIT_TOWN_ADMISSION.md
  issue_work_packet: docs/git/WORK_PACKET.template.md
  pull_request_template: ABSENT
```

The absence of a repository PR template does not authorize free-form publication. A PR body must still contain the work packet subjects and evidence lanes.

## Git Town admission

```yaml
git_town:
  admission_state: BLOCKED_ABSENT_EXECUTABLE
  version: ABSENT
  source_repository: ABSENT
  immutable_release: ABSENT
  platform: ABSENT
  architecture: ABSENT
  executable_sha256: ABSENT
  provenance_ref: ABSENT
  direct_license: ABSENT
  direct_license_sha256: ABSENT
  sbom_or_transitive_review: ABSENT
  notices_review: ABSENT
  legal_approval: ABSENT
  config_file: NOT_IMPLEMENTED
  live_version_check: NOT_EXERCISED
```

Do not use `latest`. Do not create `.git-town.toml` until exact executable and policy admission are complete.

## Synchronization policy

```yaml
sync:
  policy_state: PROPOSED_NOT_ADMITTED
  feature_strategy_after_admission: rebase
  perennial_strategy_after_admission: ff-only
  default_scope: owned-stack
  non_interactive: true
  auto_resolve: false
  default_push: false
  allow_all_stacks: false
  timeout_seconds: 120
  dry_run_required: true
  post_sync_ancestry_check: true
  rerun_evals_after_sync: true
  semantic_conflict_action: stop
```

Deviations from safe defaults: none.

Feature-branch rebase is only a proposed strategy. It is not authorized until branch policy, executable admission, and a planted conflict canary are reviewed.

## Worktree and lease policy

```yaml
workers:
  primary_checkout_mutation: denied
  linked_worktree_required: true
  worktree_root: HOST_OWNED:GYM_COME_TRUE_WORKTREES
  branch_lease_root: HOST_OWNED:GYM_COME_TRUE_BRANCH_LEASES
  repository_lease: required-for-shared-index-or-convergence-work
  lease_ttl_seconds: 1800
  heartbeat_seconds: 60
  sibling_path_overlap: denied
  preserve_blocked_worktree: true
  cleanup_requires_receipt: true
  live_canary: NOT_EXERCISED
```

Host-owned logical selectors must be resolved outside Git. Do not commit absolute user-home paths.

## Receipt policy

```yaml
receipts:
  root: HOST_OWNED:GYM_COME_TRUE_RECEIPTS
  schema: git-town-stacked-pr-worker/receipt/v1
  append_only: true
  max_stream_bytes: 1048576
  secret_values: denied
  absolute_secret_paths: denied
  task_packet_digest_required: true
  before_after_graph_required: true
  exact_head_required: true
  command_exit_required: true
  cleanup_lane_required: true
  legal_or_clinical_payloads: denied
```

Portable receipts may name a protected evidence reference but may not contain private source bytes, licenses, reviewer identity, signature material, or credential values.

## Background policy

```yaml
background:
  enabled: false
  max_iterations: 0
  interval_seconds: 0
  no_push: true
  stop_on_blocked_state: true
  stop_on_task_packet_change: true
  stop_on_lease_loss: true
  stop_on_conflict: true
  stop_on_failed_eval: true
```

Any future background loop must be separately implemented, evaluated, and admitted. Documentation does not enable it.

## Publication policy

```yaml
publication:
  enabled: false
  state: BLOCKED_UNTIL_EXACT_POLICY_AND_CANARY
  task_packet_authorization_required: true
  allowed_intents:
    - initial-pr
    - ready-for-review
    - batched-repair
  explicit_cli_flag: --publish
  environment_guard_name: GYM_COME_TRUE_PUBLISH
  environment_guard_expected_value: ALLOW_ONE_GUARDED_OPERATION
  allowed_remote: origin
  protected_branch_rewrite: denied
  post_push_fetch_and_verify: true
  merge_or_ship: denied
  workflow_rerun: denied-by-worker
  canary: NOT_EXERCISED
```

The guard value is not a secret. It is one of two explicit publication controls, not a credential.

## Prompt suppression

```yaml
unattended_environment:
  GIT_TERMINAL_PROMPT: "0"
  GIT_EDITOR: ":"
  GIT_SEQUENCE_EDITOR: ":"
  GCM_INTERACTIVE: "Never"
```

Secret host environment values must never enter receipts.

## Required task packet fields

```yaml
task_packet:
  required:
    - issue_id
    - goal
    - non_goals
    - base_branch
    - base_sha
    - parent_branch
    - parent_sha
    - head_branch
    - stack_class
    - allowed_paths
    - excluded_paths
    - dependencies
    - parallel_safe_siblings
    - required_evals
    - negative_or_mutation_controls
    - evidence_boundary
    - cleanup_contract
    - rollback_subject
    - human_owned_operations
```

Use `docs/git/WORK_PACKET.template.md`. No trailing arbitrary shell is allowed.

## Required eval commands

```yaml
evals:
  commands:
    - python3 scripts/validate_repository.py
    - python3 scripts/validate_taiwan_rule_pack.py
    - python3 scripts/validate_taiwan_source_lifecycle.py
    - python3 scripts/validate_taiwan_source_hardening.py
    - sh ./gradlew :shared:jvmTest
    - sh ./gradlew :androidApp:assembleDebug :androidApp:lintDebug
    - sh ./gradlew :webApp:composeCompatibilityBrowserDistribution
  ios_command:
    - cd iosApp
    - xcodegen generate --spec project.yml
    - xcodebuild -project GymComeTrue.xcodeproj -scheme GymComeTrue -sdk iphonesimulator -configuration Debug CODE_SIGNING_ALLOWED=NO ONLY_ACTIVE_ARCH=YES ARCHS=arm64 build
  documentation_consistency:
    - verify required headings and relative links
    - reject project.safe.yml and NativeCapabilityBridgeV2.swift
    - require actual Issue numbers 8 through 14
    - require explicit Git Town ABSENT/NOT_EXERCISED states
    - compare parent...head and require behind_by=0
  live_git_town_canary: NOT_EXERCISED
  conflict_canary: NOT_EXERCISED
  publication_canary: NOT_EXERCISED
```

A work packet may select the applicable subset, but it cannot omit a load-bearing domain validator for modified executable policy.

## Path classes

```yaml
paths:
  shared_high_contention:
    - README.md
    - README.zh-TW.md
    - AGENTS.md
    - docs/architecture.md
    - docs/implementation-status.md
    - docs/roadmap.md
    - docs/github-issue-index.md
    - .github/workflows/**
    - settings.gradle.kts
    - gradle/**
  domain:
    shared: shared/**
    android: androidApp/**
    ios: iosApp/**
    web: webApp/**
    taiwan_data: data/taiwan-supplement/**
    rights: legal/**, assets/**
    scripts: scripts/**
    git_governance: docs/git/**
```

Shared high-contention files require a convergence or explicitly leased documentation packet.

## Forbidden paths and data

```yaml
forbidden:
  path_patterns:
    - '**/.env'
    - '**/*secret*'
    - '**/*credential*'
    - '**/*private-key*'
    - data/private/**
    - evidence/private/**
    - receipts/live/**
    - browser-profiles/**
    - device-sessions/**
  data_classes:
    - credentials
    - tokens
    - private_keys
    - env_values
    - cookies
    - browser_profiles
    - device_sessions
    - host_keyrings
    - raw_private_source_bytes
    - reviewer_identity_or_signature
    - production_signing_material
    - unbounded_model_output
```

## Human-owned operations

```yaml
human_owned:
  - semantic_conflict_resolution
  - git_town_continue_skip_undo_ship
  - merge_or_merge_queue_admission
  - branch_protection_or_permission_change
  - billing_recovery
  - legal_or_license_acceptance
  - qualified_clinical_review
  - secret_or_credential_setup
  - store_console_operations
  - release_promotion
  - production_deployment
  - destructive_or_drifted_rollback
```

## Validation checklist

- [x] repository identity and credential-free remote are exact;
- [x] authority documents and current eval commands are listed;
- [x] unresolved runtime facts use `ABSENT` or `NOT_EXERCISED`;
- [x] default synchronization policy is bounded, non-interactive, no-auto-resolve, and no-push;
- [x] linked-worktree, branch lease, and path lease policy is documented;
- [x] background sync is disabled;
- [x] publication is disabled pending canary and policy admission;
- [x] Human Admit boundaries are preserved;
- [ ] exact Git Town executable/version/checksum/provenance admitted;
- [ ] direct/transitive license and notice review admitted;
- [ ] `.git-town.toml` created from admitted policy;
- [ ] linked worktree and branch/path lease canary passed;
- [ ] dry-run no-push sync canary passed;
- [ ] planted semantic conflict canary failed closed;
- [ ] guarded publication canary passed;
- [ ] cleanup and rollback canaries passed.

Runtime adoption remains incomplete while any unchecked item is required.

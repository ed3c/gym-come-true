# Repository profile — Git Town Stacked-PR Worker

> Repository-owned profile for `ed3c/gym-come-true`. The canonical Worker method remains in `ed3c/skills-shared`; this file binds that method to this repository without copying it.

## Profile status

```yaml
schema: git-town-stacked-pr-worker/repo-profile/v1
profile_state: CANDIDATE_METADATA_VERIFIED_NOT_RUNTIME_ADMITTED
updated_at: 2026-08-15
blocking_reasons:
  - BINARY_NOT_MATERIALIZED_IN_CURRENT_ENVIRONMENT
  - DISPOSABLE_CANARIES_NOT_EXERCISED
  - TRANSITIVE_AND_LEGAL_REVIEW_REQUIRED
  - CONSUMER_WORKTREE_AND_LEASE_CANARIES_NOT_EXERCISED
  - CONSUMER_CONFIG_NOT_IMPLEMENTED
  - CONSUMER_NO_PUSH_SYNC_NOT_EXERCISED
runtime_admitted: false
production_use: DENY
```

`CANDIDATE_METADATA_VERIFIED` is not a synonym for `BINARY_VERIFIED`, `CANARY_PASS`, `CONSUMER_SYNC_PASS`, `PUBLICATION_ALLOW`, `MERGED`, or `RELEASED`.

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
  shared_skill: https://github.com/ed3c/skills-shared/tree/main/skills/git-town-stacked-pr-worker
  agents: AGENTS.md
  repository_map: README.md
  architecture: docs/architecture.md
  implementation_status: docs/implementation-status.md
  roadmap: docs/roadmap.md
  git_governance: docs/git/README.md
  stacked_pr_graph: docs/git/STACKED_PRS.md
  worker_protocol: docs/git/WORKER_PROTOCOL.md
  git_town_admission: docs/git/GIT_TOWN_ADMISSION.md
  candidate_index: docs/git/admission/README.md
  candidate_manifest: docs/git/admission/git-town-v24.0.0-linux-x86_64.json
  task_packet_template: docs/git/WORK_PACKET.template.md
  pull_request_template: ABSENT
```

A missing PR template does not authorize free-form publication. Every PR body must preserve the work-packet subjects, exact ancestry, eval lanes, external gates, cleanup and rollback.

## Git Town candidate

```yaml
git_town:
  admission_state: CANDIDATE_METADATA_VERIFIED_RUNTIME_BLOCKED
  source_repository: git-town/git-town
  release_id: 358702660
  version: v24.0.0
  immutable_tag_commit: 0f3e55f5a6bae5b319dd713a0606263d0551af66
  release_immutable_metadata: true
  platform: linux
  architecture: x86_64
  archive_name: git-town_linux_intel_64.tar.gz
  archive_asset_id: 487215105
  archive_size_bytes: 7640994
  archive_sha256: 0ed4936f010b42db2ef573e4b2abd951289f4980d95b8236a619429e2501cbc7
  checksums_asset_id: 487215219
  checksums_sha256: 7532377166cb59dc01c74f86e3a71c54ba9567a461313a5d203a1ea99c571b24
  direct_license: MIT
  direct_license_sha256: eec8a092b92231375231488d27b959e2fa2be80559c97db60c1b0458d3298791
  dependency_manifest: go.mod
  dependency_manifest_sha256: 5a7627e581f45c29750ceef8116ee0bdf61f0c36ead5b31d8f1f3fe33753c721
  sbom: ABSENT
  transitive_license_review: REVIEW_REQUIRED
  notices_review: REVIEW_REQUIRED
  artifact_attestation: ABSENT
  artifact_signature: ABSENT
  organization_legal_approval: ABSENT
  archive_materialization: NOT_EXERCISED
  binary_sha256: NOT_EXERCISED
  live_version_check: NOT_EXERCISED
  disposable_canary: NOT_EXERCISED
  consumer_config_file: NOT_IMPLEMENTED
  consumer_repository_sync: NOT_EXERCISED
```

Never replace the exact candidate with `latest`. A direct MIT license is one input; it does not complete transitive dependencies, notices, patents, trademarks, service terms, export controls, vulnerabilities, organization policy or legal approval.

## Candidate harness

```yaml
candidate_harness:
  verifier: scripts/git-town/verify_admission.py
  disposable_canary: scripts/git-town/run_disposable_canary.sh
  manual_workflow: .github/workflows/git-town-admission.yml
  workflow_trigger: workflow_dispatch_only
  workflow_acknowledgement: EXECUTE_PINNED_CANDIDATE_IN_EPHEMERAL_RUNNER
  workflow_permissions: contents_read_only
  repository_secrets: denied
  workflow_action_pins:
    checkout: 3d3c42e5aac5ba805825da76410c181273ba90b1
    setup_python: 5fda3b95a4ea91299a34e894583c3862153e4b97
    upload_artifact: 043fb46d1a93c77aae656e7c1c64a875d1fc6a0a
  consumer_remote_target: denied
  publication: NOT_EXERCISED
```

The manual workflow must not be dispatched merely because it exists. Candidate execution, runner allocation, transitive review and legal approval are distinct gates.

## Synchronization policy

```yaml
sync:
  policy_state: PROPOSED_NOT_CONSUMER_ADMITTED
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
  background_sync: disabled
```

The disposable canary may create a version-specific config only inside a temporary repository. This policy does not authorize a consumer `.git-town.toml`.

## Worktree and lease policy

```yaml
workers:
  primary_checkout_mutation: denied
  linked_worktree_required: true
  worktree_root: HOST_OWNED:GYM_COME_TRUE_WORKTREES
  branch_lease_root: HOST_OWNED:GYM_COME_TRUE_BRANCH_LEASES
  path_lease_root: HOST_OWNED:GYM_COME_TRUE_PATH_LEASES
  repository_lease: required-for-shared-index-or-convergence-work
  lease_ttl_seconds: 1800
  heartbeat_seconds: 60
  sibling_path_overlap: denied
  preserve_blocked_worktree: true
  cleanup_requires_receipt: true
  live_worktree_canary: NOT_EXERCISED
  live_branch_lease_canary: NOT_EXERCISED
  live_path_lease_canary: NOT_EXERCISED
```

Host-owned selectors resolve outside Git. Never commit absolute home paths, lock files containing identities, credential paths or live receipt payloads.

## Receipt policy

```yaml
receipts:
  root: HOST_OWNED:GYM_COME_TRUE_RECEIPTS
  schema: git-town-stacked-pr-worker/receipt/v1
  append_only: true
  max_stream_bytes: 1048576
  secret_values: denied
  credential_urls: denied
  absolute_secret_paths: denied
  task_packet_digest_required: true
  exact_base_head_required: true
  before_after_graph_required: true
  command_and_exit_required: true
  remote_ref_digest_required_for_sync_or_publication: true
  cleanup_lane_required: true
  legal_clinical_private_payloads: denied
```

Portable receipts may name a protected evidence reference; they may not include private source bytes, contract terms, reviewer identity/signature, tokens, signing material or user data.

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

Any future background loop requires a separate implementation issue, fixed entrypoint, mutation controls, bounded canary and Human Admit.

## Publication policy

```yaml
publication:
  enabled: false
  state: BLOCKED_UNTIL_EXACT_POLICY_CANARY_AND_BILLING_OPEN
  task_packet_authorization_required: true
  allowed_intents:
    - initial-pr
    - ready-for-review
    - batched-repair
  explicit_cli_flag: --publish
  environment_guard_name: GYM_COME_TRUE_PUBLISH
  environment_guard_expected_value: ALLOW_ONE_GUARDED_OPERATION
  allowed_remote: origin
  one_remote_operation_only: true
  protected_branch_rewrite: denied
  post_push_fetch_and_verify: true
  workflow_rerun_by_worker: denied
  merge_or_ship: denied
  canary: NOT_EXERCISED
```

The environment guard is not a credential. Publication remains separate from synchronization and never authorizes merge, ship, promotion or rollback.

## Prompt suppression

```yaml
unattended_environment:
  GIT_TERMINAL_PROMPT: "0"
  GIT_EDITOR: ":"
  GIT_SEQUENCE_EDITOR: ":"
  GCM_INTERACTIVE: "Never"
```

No secret environment value may enter receipts or logs.

## Required task-packet fields

```yaml
task_packet:
  required:
    - issue_id
    - goal
    - non_goals
    - state_transition
    - base_branch
    - base_sha
    - parent_branch
    - parent_sha
    - head_branch
    - stack_class
    - allowed_paths
    - excluded_paths
    - generated_file_owner
    - dependencies
    - parallel_safe_siblings
    - required_evals
    - negative_or_mutation_controls
    - evidence_boundary
    - external_gates
    - cleanup_contract
    - rollback_subject
    - human_owned_operations
```

Use `docs/git/WORK_PACKET.template.md`. Arbitrary trailing shell is prohibited.

## Required eval routing

```yaml
evals:
  repository_policy:
    - python3 scripts/validate_repository.py
  taiwan_evidence:
    - python3 scripts/validate_taiwan_rule_pack.py
    - python3 scripts/validate_taiwan_source_lifecycle.py
    - python3 scripts/validate_taiwan_source_hardening.py
  shared_and_platform:
    - sh ./gradlew :shared:jvmTest
    - sh ./gradlew :androidApp:assembleDebug :androidApp:lintDebug
    - sh ./gradlew :webApp:composeCompatibilityBrowserDistribution
  ios:
    - xcodegen generate --spec iosApp/project.yml
    - unsigned simulator host build on the admitted Xcode image
  git_town_candidate_static:
    - python3 -m py_compile scripts/git-town/verify_admission.py
    - python3 scripts/git-town/verify_admission.py --candidate docs/git/admission/git-town-v24.0.0-linux-x86_64.json --self-test
    - bash -n scripts/git-town/run_disposable_canary.sh
  git_town_archive_binary: NOT_EXERCISED
  git_town_disposable_canary: NOT_EXERCISED
  git_town_consumer_worktree_and_lease: NOT_EXERCISED
  git_town_consumer_no_push_sync: NOT_EXERCISED
  git_town_publication_canary: NOT_EXERCISED
```

A packet may select the applicable subset but cannot omit a load-bearing validator for a modified executable policy or evidence record.

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

Shared high-contention paths require a convergence packet or an explicit exclusive path lease.

## Forbidden data and operations

```yaml
forbidden:
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
  worker_operations:
    - git_town_ship
    - git_town_continue
    - git_town_skip
    - git_town_undo
    - semantic_conflict_resolution
    - protected_branch_rewrite
    - permission_or_visibility_change
    - release_promotion
    - production_deployment
    - destructive_or_drifted_rollback
```

## Human-owned operations

```yaml
human_owned:
  - exact_candidate_execution_authorization
  - transitive_sbom_notices_vulnerability_review
  - legal_or_license_acceptance
  - consumer_config_admission
  - semantic_conflict_resolution
  - merge_or_merge_queue_admission
  - branch_protection_or_permission_change
  - billing_recovery
  - qualified_clinical_review
  - secret_or_credential_setup
  - store_console_operations
  - release_promotion
  - production_deployment
  - destructive_or_drifted_rollback
```

## Admission checklist

- [x] repository identity and credential-free remote are exact;
- [x] shared canonical method and repository authority documents are routed;
- [x] exact candidate release/tag/archive/checksums/license/go.mod subjects are recorded;
- [x] static validator and mutation controls exist;
- [x] disposable local-only canary exists;
- [x] manual hosted workflow is read-only, secret-free and action-SHA-pinned;
- [x] background sync and publication remain disabled;
- [x] Human Admit boundaries remain intact;
- [ ] exact archive/checksums bytes materialized and verified;
- [ ] extracted binary hash/version verified;
- [ ] SBOM/transitive license/notices/vulnerability review accepted;
- [ ] organization legal approval recorded;
- [ ] disposable branch/no-push/conflict/prompt/cleanup canaries passed;
- [ ] host-owned worktree/branch/path lease canaries passed;
- [ ] consumer `.git-town.toml` generated from the admitted version and hashed;
- [ ] consumer doctor and exact-head no-push sync passed;
- [ ] applicable repository evals passed on the exact consumer head;
- [ ] publication separately exercised or `SKIPPED_BY_POLICY` with reason;
- [ ] Human Admit decision recorded.

Runtime adoption remains incomplete while any required unchecked gate remains open.

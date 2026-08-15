# Work packet template

> Complete this packet before creating or modifying a delivery branch. Replace every instruction line. A missing required value is `ABSENT` and blocks Worker execution.

## Identity

```yaml
schema: gym-come-true/work-packet/v1
issue_id: <PUBLIC_ISSUE_ID>
title: <ONE_BOUNDED_OUTCOME>
owner: <PUBLIC_SAFE_OWNER_OR_WORKER_ID>
created_at: <YYYY-MM-DD>
packet_status: DRAFT|ADMITTED|SUPERSEDED
```

## Goal and state transition

```yaml
goal: <USER_OR_SYSTEM_OUTCOME>
from_state: <EXACT_STATE>
to_state: <EXACT_STATE>
one_sentence_transition: <FROM -> TO>
```

## Non-goals

```yaml
non_goals:
  - <EXPLICIT_NON_GOAL>
  - <NO_RELEASE_OR_PERMISSION_ESCALATION_IF_APPLICABLE>
  - <NO_EXTERNAL_EVIDENCE_CLAIM_IF_APPLICABLE>
```

## Branch subjects

```yaml
branch:
  base_branch: <BASE_BRANCH>
  base_sha: <EXACT_SHA>
  parent_branch: <PARENT_BRANCH>
  parent_sha: <EXACT_SHA>
  head_branch: <HEAD_BRANCH>
  existing_pull_request: <NUMBER_OR_ABSENT>
  stack_class: serial-child|sibling-stack|convergence|repair|documentation
```

## Dependencies and siblings

```yaml
dependencies:
  - issue_or_pr: <ID>
    exact_subject: <BRANCH_OR_SHA>
    required_state: <STATE>
parallel_safe_siblings:
  - <BRANCH_OR_NONE>
serial_reason: <WHY_PARENT_OUTPUT_IS_REQUIRED_OR_NOT_APPLICABLE>
```

Independent work must be a sibling from the closest common admitted parent. Do not invent a serial dependency.

## Path lease

```yaml
paths:
  allowed:
    - <REPOSITORY_RELATIVE_PATH_OR_GLOB>
  excluded:
    - <REPOSITORY_RELATIVE_PATH_OR_GLOB>
  shared_high_contention:
    - <PATH_REQUIRING_CONVERGENCE_OR_NONE>
  generated_owner:
    - <GENERATED_FILE_AND_OWNER_OR_NONE>
  sibling_overlap: denied
```

No path may be added after execution begins without amending and re-admitting this packet.

## Evidence authority

```yaml
evidence:
  authoritative_inputs:
    - <CODE_MANIFEST_RECEIPT_SOURCE>
  weaker_inputs:
    - <CANDIDATE_OR_DOCUMENTATION_ONLY_INPUT>
  prohibited_inferences:
    - <WHAT_MUST_NOT_BE_INFERRED>
  external_gates:
    - <LEGAL_CLINICAL_STORE_CREDENTIAL_DEVICE_OR_NONE>
```

A URL, branch name, status field, model output, or stale PR body is not authoritative evidence by itself.

## Required evals

Use fixed commands or typed entrypoints only.

```yaml
required_evals:
  - id: <STABLE_COMMAND_ID>
    command: <FIXED_COMMAND>
    applicable_subject: <EXACT_HEAD_OR_FILE_SET>
    pass_condition: <EXPLICIT>
  - id: <STABLE_COMMAND_ID>
    command: <FIXED_COMMAND>
    applicable_subject: <EXACT_HEAD_OR_FILE_SET>
    pass_condition: <EXPLICIT>
```

Common commands:

```bash
python3 scripts/validate_repository.py
python3 scripts/validate_taiwan_rule_pack.py
python3 scripts/validate_taiwan_source_lifecycle.py
python3 scripts/validate_taiwan_source_hardening.py
sh ./gradlew :shared:jvmTest
sh ./gradlew :androidApp:assembleDebug :androidApp:lintDebug
sh ./gradlew :webApp:composeCompatibilityBrowserDistribution
```

## Negative and mutation controls

```yaml
negative_controls:
  - mutation: <LOAD_BEARING_GUARD_REMOVED_OR_INVALID_INPUT>
    expected_outcome: <FAIL_OR_BLOCK>
  - mutation: <SECOND_CONTROL>
    expected_outcome: <FAIL_OR_BLOCK>
```

Positive-path tests without negative controls do not prove fail-closed behavior.

## Worker/runtime policy

```yaml
worker:
  linked_worktree_required: true
  primary_checkout_mutation: denied
  branch_writer_lease_required: true
  path_lease_required: true
  prompt_suppression_required: true
  timeout_seconds: <POSITIVE_INTEGER>
  auto_resolve: false
  default_push: false
  background_sync: false
```

If Git Town is not admitted, set:

```yaml
git_town:
  runtime_state: BLOCKED_ABSENT_EXECUTABLE
  sync: NOT_EXERCISED
  conflict_canary: NOT_EXERCISED
  publication_canary: NOT_EXERCISED
```

Do not substitute an ordinary Git operation and report it as a Git Town canary.

## Publication packet

```yaml
publication:
  authorized: true|false
  intent: initial-pr|ready-for-review|batched-repair|none
  expected_remote: origin
  exact_head_required: true
  explicit_cli_guard_required: true
  environment_guard_required: true
  post_push_fetch_and_verify: true
  mark_ready: human_owned
  merge_or_ship: human_owned
```

One `ALLOW` permits one remote operation only.

## Evidence boundary

```text
<WHAT THIS PACKET CAN PROVE>

Cannot prove:
- <RUNTIME_OR_EXTERNAL_LANE>
- <LEGAL_CLINICAL_RIGHTS_OR_STORE_LANE>
- <HOSTED_CHECK_IF_NOT_EXECUTED>
```

## Receipt requirements

```yaml
receipts:
  task_packet_sha256: required
  before_after_graph: required
  exact_head: required
  command_exit_and_log_digest: required
  publication_decision: required
  remote_ancestry: required_if_published
  cleanup: required
  secret_values: denied
  private_evidence_payloads: denied
```

## Cleanup contract

```yaml
cleanup:
  release_leases: required
  remove_clean_worktree: <true_or_false>
  preserve_on_conflict: true
  preserve_on_failed_eval: true
  preserve_on_ambiguous_remote: true
  residue_inventory: required
```

## Rollback subject

```yaml
rollback:
  target_sha_or_version: <IMMUTABLE_SUBJECT>
  expected_current_head: <SHA>
  expected_parent: <BRANCH_AND_SHA>
  drift_behavior: ROLLBACK_REFUSED_DRIFT
  destructive_rollback: human_owned
```

## Human-owned operations

```yaml
human_owned:
  - semantic_conflict_resolution
  - git_town_continue_skip_undo_ship
  - merge_or_merge_queue_admission
  - permission_or_branch_protection_change
  - billing_recovery
  - legal_or_license_acceptance
  - qualified_clinical_review
  - credential_or_secret_setup
  - store_console_or_production_deployment
  - release_promotion_or_destructive_rollback
```

## Admission checklist

- [ ] one bounded state transition;
- [ ] exact base, parent, and head subjects;
- [ ] path lease is disjoint or convergence-owned;
- [ ] dependencies and siblings are accurate;
- [ ] evidence authority and prohibited inferences are explicit;
- [ ] required evals are fixed commands;
- [ ] negative controls fail closed;
- [ ] external gates are not represented as repository-complete;
- [ ] Git Town runtime state is truthful;
- [ ] publication is separately guarded;
- [ ] rollback is immutable and drift-aware;
- [ ] cleanup and Human Admit boundaries are explicit;
- [ ] packet normalized and digest recorded.

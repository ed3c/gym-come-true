# Git Town admission state

## Current decision

```yaml
schema: gym-come-true/git-town-admission-state/v1
state: CANDIDATE_METADATA_VERIFIED_RUNTIME_BLOCKED
runtime_admitted: false
consumer_config_admitted: false
candidate_metadata_verified: true
archive_materialized_in_current_environment: false
binary_executed_in_current_environment: false
live_canaries_exercised: false
background_sync_enabled: false
publication_enabled: false
production_use: DENY
```

The repository has advanced from an unknown tool reference to one exact upstream **candidate**. This is not runtime admission. No Git Town command has executed against `gym-come-true`, no consumer `.git-town.toml` exists, and no Worker may report Git Town synchronization, publication, merge, ship, promotion, or rollback authority.

## Canonical method and repository evidence

| Subject | Authority |
|---|---|
| Portable Worker method | `ed3c/skills-shared/skills/git-town-stacked-pr-worker/SKILL.md` |
| Repository policy | `docs/git/REPO_PROFILE.md` |
| Molecular branch graph | `docs/git/STACKED_PRS.md` |
| Worker protocol | `docs/git/WORKER_PROTOCOL.md` |
| Exact candidate packet | `docs/git/admission/git-town-v24.0.0-linux-x86_64.json` |
| Tagged direct-license bytes | `docs/git/admission/upstream/git-town-v24.0.0-LICENSE.txt` |
| Tagged dependency manifest | `docs/git/admission/upstream/git-town-v24.0.0-go.mod` |
| Static/archive/binary verifier | `scripts/git-town/verify_admission.py` |
| Disposable local-only canary | `scripts/git-town/run_disposable_canary.sh` |
| Manual hosted canary | `.github/workflows/git-town-admission.yml` |

Do not copy the shared Skill into this repository. A consumer-local copy would shadow the canonical method.

## Exact upstream candidate

```yaml
upstream:
  repository: git-town/git-town
  release_id: 358702660
  tag: v24.0.0
  tag_commit: 0f3e55f5a6bae5b319dd713a0606263d0551af66
  release_immutable_metadata: true
  prerelease: false
  published_at: 2026-07-23T13:48:21Z
archive:
  asset_id: 487215105
  name: git-town_linux_intel_64.tar.gz
  platform: linux
  architecture: x86_64
  size_bytes: 7640994
  sha256: 0ed4936f010b42db2ef573e4b2abd951289f4980d95b8236a619429e2501cbc7
checksums_asset:
  asset_id: 487215219
  name: checksums.txt
  size_bytes: 1442
  sha256: 7532377166cb59dc01c74f86e3a71c54ba9567a461313a5d203a1ea99c571b24
direct_license:
  spdx: MIT
  tagged_bytes_length: 1093
  tagged_bytes_sha256: eec8a092b92231375231488d27b959e2fa2be80559c97db60c1b0458d3298791
dependency_manifest:
  path: go.mod
  source_ref: v24.0.0
  go_version: 1.26.1
  direct_modules: 24
  indirect_modules: 39
  byte_length: 3090
  sha256: 5a7627e581f45c29750ceef8116ee0bdf61f0c36ead5b31d8f1f3fe33753c721
```

These values identify a candidate for later verification. They do not prove archive download, executable identity, transitive-license acceptance, organization legal approval, or consumer-repository behavior.

## Evidence-state matrix

| Evidence lane | Current state | Required next evidence |
|---|---|---|
| Shared canonical Skill | `PASS` | Re-read when its exact content changes |
| Exact release/tag/asset metadata | `CANDIDATE_METADATA_VERIFIED` | Revalidate before materialization |
| Tagged MIT bytes | `DIRECT_LICENSE_IDENTIFIED` | Human/legal acceptance for intended use |
| Tagged dependency manifest | `MANIFEST_HASH_VERIFIED` | SBOM, transitive license, notices, vulnerability and policy review |
| Artifact attestation/signature | `ABSENT` | Explicit policy decision or trustworthy evidence |
| Organization legal approval | `ABSENT` | Human-owned approval |
| Archive bytes | `NOT_EXERCISED` | Verify exact size and SHA-256 before extraction |
| Binary SHA/version | `NOT_EXERCISED` | Extract only from verified archive and require exact `24.0.0` output |
| Disposable branch hierarchy | `NOT_EXERCISED` | Subject-bound local receipt |
| Dry-run no-push sync | `NOT_EXERCISED` | Before/after graph and unchanged local bare remote |
| Actual no-push sync | `NOT_EXERCISED` | Exact command, ancestry and remote immobility receipt |
| Semantic conflict fail-closed | `NOT_EXERCISED` | Non-zero exit and preserved inspectable blocked state |
| Prompt suppression | `NOT_EXERCISED` | No editor or credential prompt |
| Consumer worktree/lease | `NOT_EXERCISED` | Host-owned linked-worktree and lease canaries |
| Consumer `.git-town.toml` | `NOT_IMPLEMENTED` | Version-specific config after all prior gates |
| Consumer repository sync | `NOT_EXERCISED` | Human-approved exact-head no-push canary |
| Publication canary | `NOT_EXERCISED` | Separate two-guard publication packet or `SKIPPED_BY_POLICY` |
| Background synchronization | `DISABLED` | Separate implementation and admission |
| Merge/ship/promotion/rollback | `HUMAN_ADMIT` | Never Worker-owned |

No weaker row may stand in for a stronger row:

```text
CANDIDATE_METADATA_VERIFIED != BINARY_VERIFIED
BINARY_VERIFIED != DISPOSABLE_CANARY_PASS
DISPOSABLE_CANARY_PASS != CONSUMER_SYNC_PASS
CONSUMER_SYNC_PASS != PUBLICATION_ALLOW
PUBLICATION_ALLOW != MERGE_OR_SHIP
MIT != TRANSITIVE_OR_ORGANIZATION_LEGAL_APPROVAL
```

## Static verification

```bash
python3 -m py_compile scripts/git-town/verify_admission.py
python3 scripts/git-town/verify_admission.py \
  --candidate docs/git/admission/git-town-v24.0.0-linux-x86_64.json \
  --self-test \
  --receipt /tmp/git-town-candidate-static.json
bash -n scripts/git-town/run_disposable_canary.sh
```

Static PASS proves only that:

- repository-owned candidate fields match the pinned subjects;
- retained license and `go.mod` bytes match their hashes and lengths;
- unsafe policy flags remain false;
- runtime and canary states remain non-admitted;
- fourteen planted mutations fail closed;
- workflow/canary surfaces retain manual-only, read-only, no-push and no-consumer-target boundaries.

Static PASS cannot prove archive materialization, binary execution, Git Town behavior, hosted runner allocation, worktree isolation, publication or legal approval.

## Optional exact-byte verification

An authorized operator may provide exact local bytes without committing them:

```bash
python3 scripts/git-town/verify_admission.py \
  --candidate docs/git/admission/git-town-v24.0.0-linux-x86_64.json \
  --archive /approved/path/git-town_linux_intel_64.tar.gz \
  --self-test \
  --receipt /tmp/git-town-archive-verification.json

python3 scripts/git-town/verify_admission.py \
  --candidate docs/git/admission/git-town-v24.0.0-linux-x86_64.json \
  --binary /approved/path/git-town \
  --self-test \
  --receipt /tmp/git-town-binary-verification.json
```

The verifier rejects symlinks, non-regular files, archive traversal, links inside the archive, wrong size/hash, multiple executable members, non-executable binaries, command timeout and non-`24.0.0` output. Even a successful binary receipt keeps `runtimeAdmitted=false` and `productionUse=DENY`.

## Disposable canary

After exact binary verification, the canary may run only against temporary local repositories and local bare remotes:

```bash
bash scripts/git-town/run_disposable_canary.sh \
  /approved/path/git-town \
  docs/git/admission/git-town-v24.0.0-linux-x86_64.json \
  /tmp/git-town-candidate-receipts
```

The canary is designed to prove:

- local `main -> feature-a -> feature-b` hierarchy detection;
- dry-run and actual `--stack --non-interactive --no-auto-resolve --no-push` synchronization;
- unchanged local bare-remote refs;
- a planted semantic conflict exits non-zero and preserves inspectable blocked state;
- prompt suppression variables are active;
- no `ship`, `continue`, `skip`, `undo`, consumer publication, or consumer sync occurs;
- temporary repositories are removed on process exit.

Disposable canary PASS still does not authorize consumer configuration or synchronization.

## Manual hosted workflow

`.github/workflows/git-town-admission.yml` is `workflow_dispatch` only and requires the exact acknowledgement:

```text
EXECUTE_PINNED_CANDIDATE_IN_EPHEMERAL_RUNNER
```

The workflow has read-only repository permissions, consumes no repository secret, pins all GitHub Actions by commit SHA, downloads only URLs already present in the verified candidate, restricts redirects to HTTPS, verifies the checksums asset and archive before extraction, verifies the binary before execution, runs only the disposable canary, asserts no consumer Git Town configuration or tracked mutation, and uploads public-safe receipts.

Workflow existence or queueing is `NOT_EXERCISED`. A runner-budget block is infrastructure evidence, not tool PASS or code FAIL. Do not dispatch the workflow until the candidate execution and supply-chain review are explicitly authorized.

## Why `.git-town.toml` remains absent

```text
.git-town.toml = NOT_IMPLEMENTED
```

The consumer configuration would prematurely imply that the repository has admitted an exact executable, version-specific schema, rewrite strategy, no-push behavior, conflict boundary, worktree/lease controls, transitive/legal review and consumer synchronization. The disposable canary creates configuration only inside temporary repositories.

## Remaining admission procedure

1. Materialize and verify the exact checksums/archive bytes.
2. Verify the extracted executable hash and version output.
3. Complete SBOM/transitive-license/notices/vulnerability/policy review.
4. Obtain organization legal approval or an explicit rejection.
5. Run and review the disposable no-push/conflict canary receipts.
6. Implement and exercise host-owned linked-worktree, branch lease and path lease controls.
7. Generate a version-specific consumer `.git-town.toml`, record its digest and run repository doctor.
8. Run one Human-approved, exact-head, no-push consumer sync and all applicable repository evals.
9. Exercise publication separately or record `SKIPPED_BY_POLICY` with reason.
10. Obtain Human Admit for runtime use. Merge, ship, release promotion and production rollback remain separate Human Admit operations.

## Rollback

Removing the Issue #21 candidate files, verifier, canary and manual workflow returns the child branch to PR #20’s documentation-only state. No product data, user state, remote branch or production system depends on this candidate packet.

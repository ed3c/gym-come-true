# Git Town candidate admission evidence

This directory contains the public, repository-owned evidence for one exact Git Town **candidate**. It does not contain an executable and does not authorize use in the `gym-come-true` consumer repository.

## Read order

1. [`../GIT_TOWN_ADMISSION.md`](../GIT_TOWN_ADMISSION.md)
2. [`../REPO_PROFILE.md`](../REPO_PROFILE.md)
3. [`git-town-v24.0.0-linux-x86_64.json`](git-town-v24.0.0-linux-x86_64.json)
4. [`upstream/git-town-v24.0.0-LICENSE.txt`](upstream/git-town-v24.0.0-LICENSE.txt)
5. [`upstream/git-town-v24.0.0-go.mod`](upstream/git-town-v24.0.0-go.mod)
6. `scripts/git-town/verify_admission.py`
7. `scripts/git-town/run_disposable_canary.sh`
8. `.github/workflows/git-town-admission.yml`

## State machine

```text
UNKNOWN
  -> CANDIDATE_METADATA_VERIFIED
  -> ARCHIVE_HASH_VERIFIED
  -> BINARY_VERIFIED
  -> DISPOSABLE_CANARY_PASS
  -> TRANSITIVE_AND_LEGAL_REVIEWED
  -> CONSUMER_WORKTREE_AND_LEASE_PASS
  -> CONSUMER_CONFIG_REVIEWED
  -> CONSUMER_NO_PUSH_SYNC_PASS
  -> HUMAN_ADMIT
  -> RUNTIME_ADMITTED
```

Every transition requires a subject-bound receipt. A later state cannot be inferred from an earlier state.

The current state is:

```text
CANDIDATE_METADATA_VERIFIED
+ BINARY_NOT_MATERIALIZED
+ TRANSITIVE_REVIEW_REQUIRED
+ LEGAL_APPROVAL_ABSENT
+ CANARIES_NOT_EXERCISED
+ RUNTIME_ADMITTED=false
+ productionUse=DENY
```

## Exact retained subjects

| Subject | Identity |
|---|---|
| Release | `git-town/git-town` release `358702660`, tag `v24.0.0` |
| Tag commit | `0f3e55f5a6bae5b319dd713a0606263d0551af66` |
| Linux x86_64 archive | asset `487215105`, `git-town_linux_intel_64.tar.gz` |
| Archive SHA-256 | `0ed4936f010b42db2ef573e4b2abd951289f4980d95b8236a619429e2501cbc7` |
| Checksums asset | asset `487215219`, SHA-256 `7532377166cb59dc01c74f86e3a71c54ba9567a461313a5d203a1ea99c571b24` |
| Direct license | MIT, tagged bytes SHA-256 `eec8a092b92231375231488d27b959e2fa2be80559c97db60c1b0458d3298791` |
| Dependency manifest | tagged `go.mod`, SHA-256 `5a7627e581f45c29750ceef8116ee0bdf61f0c36ead5b31d8f1f3fe33753c721` |

The dependency manifest is an input to later SBOM/transitive review. It is not a conclusion that all dependencies, notices, patents, trademarks, services, export controls, or organization policy are acceptable.

The manual workflow also pins its three GitHub Actions dependencies to exact commit SHAs rather than mutable major-version tags. Those pins reduce workflow drift; they do not replace review of the actions themselves or GitHub-hosted runner trust.

## Static verification

```bash
python3 -m py_compile scripts/git-town/verify_admission.py
python3 scripts/git-town/verify_admission.py \
  --candidate docs/git/admission/git-town-v24.0.0-linux-x86_64.json \
  --self-test \
  --receipt /tmp/git-town-candidate-static.json
bash -n scripts/git-town/run_disposable_canary.sh
```

Static PASS proves:

- the candidate file matches the pinned exact subjects;
- retained license and `go.mod` bytes match their hashes and sizes;
- the candidate cannot self-declare runtime or production admission;
- fourteen planted mutations fail closed;
- the scripts parse.

It does not prove archive download, binary execution, Git Town behavior, consumer worktree isolation, publication, merge, or release.

## Optional runtime strengthening

An authorized operator can supply exact local bytes to the verifier. The verifier hashes before extraction or execution and still emits `runtimeAdmitted=false`.

```bash
python3 scripts/git-town/verify_admission.py \
  --archive /approved/path/git-town_linux_intel_64.tar.gz \
  --receipt /tmp/git-town-archive.json

python3 scripts/git-town/verify_admission.py \
  --binary /approved/path/git-town \
  --receipt /tmp/git-town-binary.json
```

After binary verification, run the disposable canary only against local temporary repositories:

```bash
bash scripts/git-town/run_disposable_canary.sh \
  /approved/path/git-town \
  docs/git/admission/git-town-v24.0.0-linux-x86_64.json \
  /tmp/git-town-canary-receipts
```

The canary never targets `ed3c/gym-come-true`, never publishes, and never invokes `git town ship`, `continue`, `skip`, or `undo`.

## Manual hosted workflow

`.github/workflows/git-town-admission.yml` is manual-only and requires an exact acknowledgement string. It downloads only the versioned URLs stored in the statically verified candidate, verifies checksums before extraction, executes the disposable canary in an ephemeral runner, and uploads public-safe receipts.

Workflow existence is `NOT_EXERCISED`. A queued job is not PASS. A job blocked before runner allocation is an infrastructure receipt, not a tool or code result.

## Forbidden repository changes at this state

- no consumer `.git-town.toml`;
- no automatic/background synchronization;
- no default push or auto-resolution;
- no remote publication from the canary;
- no merge, ship, promotion, or rollback authority;
- no binary, archive, token, private license payload, or host path committed to Git;
- no claim that MIT resolves transitive/legal approval;
- no promotion of synthetic or metadata-only evidence into runtime admission.

## Rollback

Delete this candidate packet, verifier, canary, and manual workflow to return to documentation-only state. Removing the packet does not alter any product branch, user data, remote branch, or production system.

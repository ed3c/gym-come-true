# Git and Stacked-PR governance

This directory is the repository-owned binding for the shared canonical [`git-town-stacked-pr-worker`](https://github.com/ed3c/skills-shared/tree/main/skills/git-town-stacked-pr-worker) method.

The shared Skill owns the portable method. `gym-come-true` owns repository identity, task packets, branch/path leases, evals, receipts, publication guards, and Human Admit boundaries.

## Current status

```text
shared canonical Skill      PASS / resolved
repository profile          DOCUMENTED
machine delivery graph      DOCUMENTED + VALIDATED
Worker protocol             DOCUMENTED
Git Town candidate          PINNED_CANDIDATE / v24.0.0
Git Town runtime admitted   false
.git-town.toml              NOT_IMPLEMENTED
consumer sync canary        NOT_EXERCISED
conflict canary             NOT_EXERCISED
publication canary          NOT_EXERCISED
background sync             DISABLED
merge / ship / promotion    HUMAN ADMIT
```

`PINNED_CANDIDATE` means exact candidate metadata/provenance surfaces exist. It does not mean the consumer repository has executed or admitted Git Town.

```text
GIT_TOWN_CANDIDATE != GIT_TOWN_RUNTIME_ADMITTED
```

## Mandatory read order for branch work

1. shared canonical Skill;
2. root `AGENTS.md`;
3. root `README.md`;
4. `docs/implementation-status.md`;
5. `docs/github-issue-index.md`;
6. this file;
7. `REPO_PROFILE.md`;
8. `STACKED_PRS.md` plus `stacked-delivery-manifest.json`;
9. `GIT_TOWN_ADMISSION.md`;
10. assigned Issue and completed work packet;
11. nearest README for every writable path;
12. live exact PR/base/head graph.

## Files and authority

| File | Purpose |
|---|---|
| [REPO_PROFILE.md](REPO_PROFILE.md) | Repository identity, safe defaults, leases, receipts, publication and eval policy |
| [STACKED_PRS.md](STACKED_PRS.md) | Current merged history, active Draft graph, planned convergence packet, path leases and Human Admit |
| [stacked-delivery-manifest.json](stacked-delivery-manifest.json) | SHA-bound machine projection of `STACKED_PRS.md` |
| [schemas/stacked-delivery-manifest.schema.json](schemas/stacked-delivery-manifest.schema.json) | Manifest transport schema |
| [WORKER_PROTOCOL.md](WORKER_PROTOCOL.md) | Worker lifecycle, sync/publication lanes, cleanup and recovery |
| [GIT_TOWN_ADMISSION.md](GIT_TOWN_ADMISSION.md) | Exact Git Town candidate/runtime admission evidence |
| [WORK_PACKET.template.md](WORK_PACKET.template.md) | Required packet before branch execution |

The machine projection is validated offline with:

```bash
python3 scripts/validate_stacked_delivery.py --self-test
```

Narrative and manifest are SHA-bound. Do not duplicate the full current branch graph in this entrypoint; `STACKED_PRS.md` owns it.

## Worker model

```text
Git Town candidate/runtime
  = branch hierarchy/synchronization mechanism only after admission

Worker
  = one isolated worktree + one branch lease + bounded path lease

Repository evals
  = implementation correctness + planted negative controls

Publication gate
  = exact-head remote-operation admission

Hosted checks
  = evidence for one exact commit only

Human Admit
  = semantic conflict resolution, merge, release, legal/clinical/rights acceptance, rollback
```

Branch classes:

- **Serial child:** consumes exact parent implementation/evidence.
- **Sibling:** independent work from the same parent with disjoint leases.
- **Convergence:** owns indexes, exact parent heads, merge/release traceability; it does not repair domain semantics.
- **Repair:** bounded response to one observed failure or review subject.
- **Documentation:** routing/handoff surfaces only; it cannot promote executable or external admission.

## Evidence laws

- `PASS`, `FAIL`, `PRE_RUN_BLOCKED`, `ABSENT`, `NOT_IMPLEMENTED`, `NOT_EXERCISED`, and `SKIPPED_BY_POLICY` are distinct.
- `HOSTED_PASS(commit A) != HOSTED_PASS(commit B)`.
- Historical budget-blocked runs remain historical; current hosted execution does not rewrite them.
- Open Issue does not mean implementation is absent.
- A Git Town sync result cannot proxy repository tests.
- A branch name or PR body cannot proxy ancestry.
- A documentation profile cannot proxy executable admission.
- A repository license cannot proxy third-party media/source rights.
- Merge, legal/clinical/editorial/rights/device/store/provider/signing/release and Git Town runtime admission remain Human or external gates.

## Current routing

Historical PRs #2/#15/#16/#20/#22 are merged. The active Draft evidence graph and the single machine-modeled future convergence packet are maintained in [`STACKED_PRS.md`](STACKED_PRS.md), not here.

At the current routing snapshot, PR #67 is the top delivery-SSOT Draft under PR #65, with PR #59 as the sibling artifact-identity lane under PR #55. Issue #68 is a documentation-only child of PR #67 that reconciles this entrypoint and `docs/roadmap.md`.

Before dispatch, always resolve the live GitHub state because checked-in routing is a snapshot, not network discovery.
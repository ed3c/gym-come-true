# Git and Stacked-PR governance

This directory is the repository-owned binding for the shared canonical [`git-town-stacked-pr-worker`](https://github.com/ed3c/skills-shared/tree/main/skills/git-town-stacked-pr-worker) method.

The shared Skill owns the portable method. `gym-come-true` owns its repository identity, branch graph, task packets, path leases, eval commands, CI, receipts, publication guards, and Human Admit boundaries.

## Status

```text
shared canonical Skill      PASS / resolved
repository profile          DOCUMENTED
molecular branch graph      DOCUMENTED
Worker protocol             DOCUMENTED
exact Git Town executable   ABSENT
.git-town.toml              NOT_IMPLEMENTED
live worktree/sync canary   NOT_EXERCISED
conflict canary             NOT_EXERCISED
publication canary          NOT_EXERCISED
background sync             DISABLED
merge / ship / promotion    HUMAN ADMIT
```

Documentation does not complete Git Town adoption.

## Mandatory read order for branch work

1. shared canonical Skill;
2. root `AGENTS.md`;
3. root `README.md`;
4. `docs/architecture.md`;
5. this file;
6. `REPO_PROFILE.md`;
7. `STACKED_PRS.md`;
8. `GIT_TOWN_ADMISSION.md`;
9. assigned Issue and completed work packet;
10. nearest README for each writable path;
11. current exact PR/base/head graph.

## Files

| File | Purpose |
|---|---|
| [REPO_PROFILE.md](REPO_PROFILE.md) | Immutable repo identity, authority paths, safe sync defaults, leases, receipts, publication and eval policy |
| [STACKED_PRS.md](STACKED_PRS.md) | Published and planned branch graph, parentage, path leases, state transitions, evals, rollback and Human Admit |
| [stacked-delivery-manifest.json](stacked-delivery-manifest.json) | Machine projection of the branch graph, gated by `scripts/validate_stacked_delivery.py --self-test` |
| [schemas/stacked-delivery-manifest.schema.json](schemas/stacked-delivery-manifest.schema.json) | JSON Schema for the delivery manifest |
| [WORKER_PROTOCOL.md](WORKER_PROTOCOL.md) | Worker lifecycle, stable outcomes, sync/publication lanes, cleanup and recovery |
| [GIT_TOWN_ADMISSION.md](GIT_TOWN_ADMISSION.md) | Exact executable/provenance admission state and canary checklist |
| [WORK_PACKET.template.md](WORK_PACKET.template.md) | Required issue/Worker packet before a branch can start |

## Ownership

Allowed contents:

- repository-specific branch and work policy;
- stable identifiers and exact evidence states;
- task packet templates;
- path-lease and eval routing;
- publication and Human Admit boundaries;
- documentation-only receipts that contain no secrets.

Forbidden contents:

- a copy of the shared canonical Skill;
- GitHub tokens, credentials, environment values, cookies, device sessions, key material, or secret paths;
- private source archives, reviewer identities/signatures, provider/store secrets;
- machine-specific absolute worktree paths when the host owns them;
- mutable `latest` executable identities;
- claims that a documented canary actually ran.

## Core boundary

```text
Git Town
  = branch hierarchy and synchronization

Worker
  = one isolated worktree + one branch lease + bounded path lease

Repository evals
  = implementation correctness and negative controls

Publication gate
  = exact-HEAD remote operation admission

Trusted GitHub checks
  = remote execution evidence

Human Admit
  = semantic conflict resolution, merge, release, legal/clinical acceptance, rollback
```

## Branch classes

- **Serial child:** consumes the exact implementation/evidence of its parent.
- **Sibling:** independent domain work from the same admitted parent with disjoint paths.
- **Convergence:** owns shared indexes, exact parent heads, merge order, and release traceability only.
- **Repair:** bounded repair of one existing branch after a specific failure/feedback subject.
- **Documentation:** decision and handoff surfaces only; cannot change executable admission.

## Evidence rules

- `PASS`, `FAIL`, `ABSENT`, `NOT_IMPLEMENTED`, `NOT_EXERCISED`, and `SKIPPED_BY_POLICY` are distinct.
- Local sync, local eval, publication, remote ancestry, hosted checks, legal/clinical review, and Human Admit are separate lanes.
- An Actions job that never allocates a runner is `PRE_RUN_BLOCKED`, not a product test result.
- A Git Town sync result cannot proxy repository tests.
- A branch name or PR body cannot proxy Git ancestry.
- A documentation-only profile cannot proxy exact executable admission.

## Current stack

```text
main
└── PR #2  agent/bootstrap-kmp-fitness-platform
    └── PR #15  agent/taiwan-supplement-evidence
        └── PR #16  agent/taiwan-source-lifecycle
            └── Issue #19 / agent/document-git-town-delivery-graph
```

Future domain branches are listed in [STACKED_PRS.md](STACKED_PRS.md). Independent iOS, Android, catalog, entitlement, and market work are sibling stacks, not one artificial chain.

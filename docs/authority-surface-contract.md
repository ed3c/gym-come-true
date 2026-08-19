# Authority surface drift contract

`AGENTS.md`, `docs/implementation-status.md`, `README.md`, `README.zh-TW.md`, `docs/roadmap.md`, and `docs/git/README.md` are repository routing/authority surfaces. A stale claim in any of them can send a later Agent or reviewer into an obsolete branch, evidence, licensing, platform, or admission state.

## State progression

```text
MANUALLY_RECONCILED_AUTHORITY
  -> MACHINE_GATED_AUTHORITY_DRAFT
  -> README_AUTHORITY_RECONCILED_AND_GATED_DRAFT
  -> LIVE_DELIVERY_GRAPH_RECONCILED_DRAFT
  -> SECONDARY_ROUTING_SURFACES_RECONCILED_DRAFT
```

The owning offline gate is:

```bash
python3 scripts/validate_authority_surfaces.py
python3 scripts/validate_authority_surfaces.py --self-test
```

## What the gate rejects

The validator fails closed on already-disproved states, including:

- repository visibility described as private;
- global claims that hosted Actions never executed or that the historical Actions-budget block is still current;
- merged historical PRs described as current Draft/unmerged work;
- Health Connect or HealthKit adapter work described as absent/future-only;
- the pinned Git Town v24.0.0 candidate described as absent or its runtime boundary promoted;
- an old branch graph duplicated as the current Git governance graph;
- open Issue treated as proof that engineering is absent;
- repository-authored code described as proprietary despite Apache-2.0;
- exact-head evidence collapsed across commits;
- Human Admit, legal/clinical/rights, real-device, store/provider/signing/release, or Git Town runtime boundaries removed.

The self-test plants representative stale claims across all six routing surfaces and requires every mutation to fail. The current suite contains 20 planted drifts.

## Ownership split

- `docs/implementation-status.md`: merged/staged engineering and remaining gates.
- `docs/github-issue-index.md`: GitHub Issue/PR semantics and historical/current evidence distinction.
- `docs/git/STACKED_PRS.md` + machine manifest: branch-level delivery graph.
- `docs/roadmap.md`: current dispatch/dependency routing; historical plans are not executable authority.
- `docs/git/README.md`: Git governance entrypoint; it routes to the graph instead of duplicating it.
- root README/AGENTS: public and Agent-facing hard laws.

## Evidence boundary

This is a repository-content consistency gate. It is network-free and therefore does not discover live GitHub state. Live PR heads, issue states, repository visibility, workflow conclusions, branch ancestry, external asset licenses, device behavior, credentials, and external approvals must still be resolved from their owning evidence source.

```text
CHECKED_IN_AUTHORITY_PASS != LIVE_GITHUB_STATE_PROVEN
HISTORICAL_RECORD != CURRENT_ROUTING_AUTHORITY
HOSTED_PASS(commit A) != HOSTED_PASS(commit B)
HOSTED_CHECK_PASS != MERGE_AUTHORITY
ADAPTER_PRESENT != REAL_DEVICE_VALIDATION
GIT_TOWN_CANDIDATE != GIT_TOWN_RUNTIME_ADMITTED
CODE_OR_TEST != LEGAL_CLINICAL_RIGHTS_APPROVAL
APACHE_2_0_REPO_LICENSE != THIRD_PARTY_MEDIA_RIGHTS
DEBUG_BUILD != RELEASE_SIGNING_ADMISSION
```

When an intentional architecture, licensing, platform, product, or evidence transition makes an assertion obsolete, update the owning implementation/evidence first, then reconcile every affected routing surface and its planted controls. Never weaken the gate merely to make CI green.

Merge, release promotion, signing admission, legal/clinical/editorial/rights acceptance, provider/store credentials, real-device evidence, Git Town runtime admission, and destructive rollback remain Human Admit or external gates.
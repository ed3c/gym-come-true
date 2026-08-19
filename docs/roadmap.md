# Delivery roadmap

**Current routing snapshot: 2026-08-19.** This document is a routing map, not a substitute for live GitHub state. Read `docs/implementation-status.md`, `docs/github-issue-index.md`, and `docs/git/STACKED_PRS.md` before dispatching work.

## Current product direction

The owner-directed MVP is the information/logging product described by [`docs/product/mvp-redesign.md`](product/mvp-redesign.md), with the medical-risk notice governed by [`legal/DISCLAIMER.md`](../legal/DISCLAIMER.md).

Engineering already present in `main` must not be redispatched merely because an umbrella or admission Issue remains open. Conversely, code or hosted checks do not close legal, clinical, editorial, rights, device, provider, store, signing, or release gates.

```text
OPEN_ISSUE != ABSENT_IMPLEMENTATION
MERGED_ENGINEERING != PRODUCTION_ADMISSION
HOSTED_PASS(commit A) != HOSTED_PASS(commit B)
GITHUB_CHECK_PASS != HUMAN_ADMIT
```

## Merged engineering baseline

Historical delivery PRs #2, #15, #16, #20, and #22 are merged history, not current Draft PRs. They established the KMP Android/iOS/Web foundation, Taiwan evidence/source lifecycle contracts, repository delivery governance, and the pinned Git Town v24.0.0 candidate packet.

Current `main` also contains engineering contracts for exercise taxonomy/catalog, nutrition/meal planning, explanation boundaries, Health Connect/HealthKit adapter surfaces, reminders, and other MVP slices. Their remaining admission gates are listed in `docs/implementation-status.md` and the live GitHub Issues.

Historical Actions runs that ended before runner allocation remain `PRE_RUN_BLOCKED` evidence for those exact old SHAs only. Current Draft heads have executed hosted workflows; never rewrite either history in the other direction.

## Active Draft evidence graph

The branch-level source of truth is [`docs/git/STACKED_PRS.md`](git/STACKED_PRS.md). At this routing snapshot the active evidence stack is:

```text
main@b1880abe...
└── PR #55  DOMAIN_VALIDATORS_OWNED_BY_CI_DRAFT
    ├── PR #57  CURRENT_PUBLIC_REPO_SSOT_DRAFT
    │   └── PR #61  CURRENT_AGENT_RUNTIME_CONTRACT_DRAFT
    │       └── PR #63  MACHINE_GATED_AUTHORITY_DRAFT
    │           └── PR #65  README_AUTHORITY_RECONCILED_AND_GATED_DRAFT
    │               └── PR #67  LIVE_DELIVERY_GRAPH_RECONCILED_DRAFT
    │                   └── Issue #68 / X9 (this routing reconciliation)
    └── PR #59  TRANSPORT_AND_SEMANTIC_IDENTITIES_SEPARATED_DRAFT
```

Every PR remains an exact-head evidence subject. Parent green does not prove a child head. Merge remains Human Admit.

## Current dispatch rule

Before starting a packet:

1. resolve the live Issue and PR state;
2. determine whether the requested capability is absent, merged engineering with an external gate, or active Draft work;
3. freeze the exact parent SHA and path lease;
4. use the shared `git-town-stacked-pr-worker` method plus repository-owned Git governance;
5. require deterministic repository checks and fresh exact-head hosted evidence;
6. stop at Human/external admission boundaries instead of manufacturing evidence.

Independent product/domain work should be sibling stacks from the nearest admitted parent when path leases are disjoint. Documentation and convergence work must not repair domain semantics.

## Current remaining classes of work

### Repository-internal convergence

Only dispatch when Shadow Architect finds a concrete evidence/authority drift. Current convergence work is the Draft chain above. Issue #44 remains the future release-convergence packet and must consume only Human-selected admitted heads; it cannot repair domain code or hide blockers.

### Engineering present, admission still open

Examples include exercise catalog/editorial-rights review, nutrition real-source/reuse-rights admission, explanation security/privacy review, Health Connect/HealthKit real-device/privacy/store evidence, reminder reliability, and Git Town runtime admission. Consult the owning Issue before deciding whether additional code is actually missing.

### External / Human gates

These cannot be completed by repository prose or CI alone:

- real Taiwan source bytes, reuse-rights/legal review, and qualified review where required;
- consented production corpora and authorized device evaluation;
- third-party media or creator rights;
- real-device/OEM/App Store/Play evidence;
- provider/store credentials and production deployment;
- signing, release promotion, rollback authority;
- Git Town consumer runtime/config/canary admission;
- merge and semantic-conflict decisions.

## Historical roadmap

The detailed pre-repositioning phase plan previously stored in this file is historical design context only. It must not be interpreted as current branch state, current Actions state, or authorization to reopen retired/superseded work packets. Git history retains the full earlier roadmap.

For current work use, in order:

1. `AGENTS.md`;
2. `docs/implementation-status.md`;
3. `docs/github-issue-index.md`;
4. `docs/git/README.md`;
5. `docs/git/STACKED_PRS.md` and its SHA-bound machine manifest;
6. the live assigned Issue/PR and exact head.
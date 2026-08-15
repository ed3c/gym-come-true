# GitHub Delivery Issue and Pull Request Index

This file is the operational mapping between actual GitHub Issues, published PRs, proposed molecular branches, and duplicate/superseded records. Branch names and documents do not create implementation status; GitHub PR state and exact commit ancestry remain authoritative.

## Published PR stack

| PR | Issue | Branch → Base | State transition | Status at 2026-08-15 |
|---:|---:|---|---|---|
| [#2](https://github.com/ed3c/gym-come-true/pull/2) | [#1](https://github.com/ed3c/gym-come-true/issues/1) | `agent/bootstrap-kmp-fitness-platform` → `main` | `EMPTY_REPOSITORY -> AUDITABLE_CROSS_PLATFORM_FOUNDATION` | Open Draft, unmerged |
| [#15](https://github.com/ed3c/gym-come-true/pull/15) | [#8](https://github.com/ed3c/gym-come-true/issues/8) | `agent/taiwan-supplement-evidence` → `agent/bootstrap-kmp-fitness-platform` | `FOUNDATION -> TAIWAN_EVIDENCE_CONTRACT_DRAFT` | Open Draft, unmerged |
| [#16](https://github.com/ed3c/gym-come-true/pull/16) | [#8](https://github.com/ed3c/gym-come-true/issues/8) | `agent/taiwan-source-lifecycle` → `agent/taiwan-supplement-evidence` | `EVIDENCE_DRAFT -> TAIWAN_SOURCE_LIFECYCLE_DRAFT` | Open Draft, unmerged |
| Draft PR pending in this slice | [#19](https://github.com/ed3c/gym-come-true/issues/19) | `agent/document-git-town-delivery-graph` → `agent/taiwan-source-lifecycle` | `SOURCE_LIFECYCLE_DRAFT -> DOCUMENTED_GIT_TOWN_DELIVERY_GRAPH_DRAFT` | Branch created; documentation publication packet |

At the start of Issue #19, PRs #2, #15, and #16 are the only published implementation PRs.

## Active product and delivery issues

| Issue | Outcome | Published implementation | Primary unresolved gate |
|---:|---|---|---|
| [#1](https://github.com/ed3c/gym-come-true/issues/1) | KMP Android/iOS/Web foundation | PR #2 | exact-head hosted execution |
| [#8](https://github.com/ed3c/gym-come-true/issues/8) | Taiwan evidence and reviewed rule pack | PR #15 and PR #16 are partial Draft slices | consented corpus, official bytes/terms, qualified reviewer, production rules |
| [#9](https://github.com/ed3c/gym-come-true/issues/9) | iOS native evidence, HealthKit, reminders, AlarmKit assessment | No PR | device/privacy/store evidence |
| [#10](https://github.com/ed3c/gym-come-true/issues/10) | Android Health Connect and reminder reliability | No PR | least privilege and device/OEM reliability harness |
| [#11](https://github.com/ed3c/gym-come-true/issues/11) | Copyright-clean top-50 catalog and media | No PR | per-record provenance and executed media rights |
| [#12](https://github.com/ed3c/gym-come-true/issues/12) | Private explanation gateway | No PR | admitted Taiwan receipts, server/provider/secret boundary, adversarial evals |
| [#13](https://github.com/ed3c/gym-come-true/issues/13) | Entitlements, privacy, stores, release | No PR | provider/store accounts, signing, privacy and release operations |
| [#14](https://github.com/ed3c/gym-come-true/issues/14) | Creator-market validation | No PR | real interviews, rights-cleared creative, retained-contribution evidence |
| [#19](https://github.com/ed3c/gym-come-true/issues/19) | Directory state machines and Git Town Stacked-PR index | Documentation branch in progress | docs review and exact branch ancestry |

## Overlapping Taiwan source issues

Issues [#17](https://github.com/ed3c/gym-come-true/issues/17) and [#18](https://github.com/ed3c/gym-come-true/issues/18) describe substantially overlapping immutable-source/promotion work that is already represented by PR #16.

They remain open records at this snapshot. Agents must not start two competing implementations from them. A human/trusted operator should either:

- close them as duplicate/superseded by Issue #8 + PR #16; or
- rewrite one as the remaining real-source acquisition/legal-review packet.

Until that decision, Issue #8 and PR #16 are the implementation authority.

## Earlier superseded issue numbers

The repository previously recorded the following logical duplicates as closed/superseded:

- #3 → active Issue #11;
- #4 → active Issues #9 and #10;
- #5 → active Issues #9 and #10;
- #6 → active Issues #8 and #12;
- #7 → active Issue #13.

Do not reuse these numbers in new roadmap headings.

## Molecular branch index

The complete proposed branch graph, path leases, evals, rollback subjects, and Human Admit boundary are maintained in [docs/git/STACKED_PRS.md](git/STACKED_PRS.md).

Status rules:

```text
OPEN DRAFT PR       GitHub PR exists and remains unmerged
BRANCH_CREATED      branch exists but does not imply review admission
PLANNED_WORK_PACKET no branch or PR is implied
EXTERNAL_GATE       repository cannot manufacture the required evidence
```

## Exact-head evidence note

PR #16 current head is `f58a2feac580ca37bb4d7b3c30e122908bfd6b07`. Workflow run `31878284072` (run #79) did not allocate a runner because Actions budget prevented use. That receipt is `PRE_RUN_BLOCKED_BY_ACTIONS_BUDGET`, not test `PASS` or product-code `FAIL`.

# GitHub Delivery Issue and Pull Request Index

This file maps actual GitHub Issues, published PRs, proposed molecular branches, and duplicate/superseded records. Branch names and documents do not create implementation status; GitHub PR state and exact commit ancestry are authoritative.

## Published Draft stack

| PR | Issue | Branch → Base | State transition | Status at 2026-08-15 |
|---:|---:|---|---|---|
| [#2](https://github.com/ed3c/gym-come-true/pull/2) | [#1](https://github.com/ed3c/gym-come-true/issues/1) | `agent/bootstrap-kmp-fitness-platform` → `main` | `EMPTY_REPOSITORY -> AUDITABLE_CROSS_PLATFORM_FOUNDATION` | Open Draft, unmerged |
| [#15](https://github.com/ed3c/gym-come-true/pull/15) | [#8](https://github.com/ed3c/gym-come-true/issues/8) | `agent/taiwan-supplement-evidence` → foundation | `FOUNDATION -> TAIWAN_EVIDENCE_CONTRACT_DRAFT` | Open Draft, unmerged |
| [#16](https://github.com/ed3c/gym-come-true/pull/16) | [#8](https://github.com/ed3c/gym-come-true/issues/8) | `agent/taiwan-source-lifecycle` → Taiwan evidence | `EVIDENCE_DRAFT -> TAIWAN_SOURCE_LIFECYCLE_DRAFT` | Open Draft, unmerged |
| [#20](https://github.com/ed3c/gym-come-true/pull/20) | [#19](https://github.com/ed3c/gym-come-true/issues/19) | `agent/document-git-town-delivery-graph` → source lifecycle | `SOURCE_LIFECYCLE_DRAFT -> DOCUMENTED_GIT_TOWN_DELIVERY_GRAPH_DRAFT` | Open Draft, unmerged |

PR #20 initial publication head was `5995ac50058f6a4c0a9fd72c96d211046631fd35`. Its current exact head is authoritative in PR metadata after documentation-index follow-up commits.

## Active product and delivery issues

| Issue | Outcome | Published implementation | Primary unresolved gate |
|---:|---|---|---|
| [#1](https://github.com/ed3c/gym-come-true/issues/1) | KMP Android/iOS/Web foundation | PR #2 | exact-head hosted execution |
| [#8](https://github.com/ed3c/gym-come-true/issues/8) | Taiwan evidence and reviewed rule pack | PR #15 and PR #16 are partial Draft slices | consented corpus, official bytes/terms, qualified reviewer, production rules |
| [#9](https://github.com/ed3c/gym-come-true/issues/9) | iOS native evidence, HealthKit, reminders, AlarmKit assessment | No implementation PR | device/privacy/store evidence |
| [#10](https://github.com/ed3c/gym-come-true/issues/10) | Android Health Connect and reminder reliability | No implementation PR | least privilege and device/OEM reliability harness |
| [#11](https://github.com/ed3c/gym-come-true/issues/11) | Copyright-clean top-50 catalog and media | No implementation PR | per-record provenance and executed media rights |
| [#12](https://github.com/ed3c/gym-come-true/issues/12) | Private explanation gateway | No implementation PR | admitted Taiwan receipts, server/provider/secret boundary, adversarial evals |
| [#13](https://github.com/ed3c/gym-come-true/issues/13) | Entitlements, privacy, stores, release | No implementation PR | provider/store accounts, signing, privacy and release operations |
| [#14](https://github.com/ed3c/gym-come-true/issues/14) | Creator-market validation | No implementation PR | real interviews, rights-cleared creative, retained-contribution evidence |
| [#19](https://github.com/ed3c/gym-come-true/issues/19) | Directory state machines and Git Town Stacked-PR index | PR #20 | docs review, exact-head hosted status, Human Admit |

## Overlapping Taiwan source issues

Issues [#17](https://github.com/ed3c/gym-come-true/issues/17) and [#18](https://github.com/ed3c/gym-come-true/issues/18) substantially overlap immutable-source/promotion work already represented by Issue #8 and PR #16.

Agents must not start competing implementations from them. A human/trusted operator should either:

- close them as duplicate/superseded by Issue #8 + PR #16; or
- rewrite one as the remaining real-source acquisition/legal-review packet.

Until then, Issue #8 and PR #16 are implementation authority.

## Earlier superseded issue numbers

- #3 → active Issue #11;
- #4 → active Issues #9 and #10;
- #5 → active Issues #9 and #10;
- #6 → active Issues #8 and #12;
- #7 → active Issue #13.

Do not reuse those numbers in new roadmap headings.

## Molecular branch index

The full proposed graph, path leases, evals, rollback subjects, and Human Admit boundaries are in [docs/git/STACKED_PRS.md](git/STACKED_PRS.md).

```text
OPEN DRAFT PR       GitHub PR exists and remains unmerged
BRANCH_CREATED      branch exists; no review admission implied
PLANNED_WORK_PACKET no branch or PR implied
EXTERNAL_GATE       repository cannot manufacture required evidence
```

## Hosted evidence notes

PR #16 exact head `f58a2feac580ca37bb4d7b3c30e122908bfd6b07` had workflow run `31878284072` (run #79). No runner was allocated because Actions budget prevented use. Classification: `PRE_RUN_BLOCKED_BY_ACTIONS_BUDGET`, not test `PASS` and not product-code `FAIL`.

PR #20 must be classified from its own exact current head. A no-runner/budget receipt remains infrastructure-blocked rather than a code result.

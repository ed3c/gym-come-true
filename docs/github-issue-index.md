# GitHub Delivery Issue Index

This file is the canonical mapping between the dependency-oriented roadmap and the issue numbers that GitHub assigned after PR #2 and an earlier duplicate issue set were created.

## Active delivery stack

```text
#1  Foundation issue
 └─ PR #2  AUDITABLE_CROSS_PLATFORM_FOUNDATION

#8  Taiwan supplement evidence and reviewed rule pack
 ├─> #12 Private LLM explanation gateway and adversarial evals
 └─> #13 Entitlements, privacy, stores, and release operations

#9  iOS native evidence, Apple Health, reminders, AlarmKit assessment
 └─> #13

#10 Android Health Connect and reminder reliability harness
 └─> #13

#11 Copyright-clean exercise catalog and licensed media pipeline
 └─> #13

#14 Creator-market validation and launch evidence
 └─> release evidence for #13
```

## Active issues

| Issue | Outcome | Primary gate |
|---:|---|---|
| #8 | Taiwan supplement evidence and deterministic reviewed rule pack | No `CLINICALLY_REVIEWED` state without source, reviewer, tests, dates, and rollback |
| #9 | iOS native evidence, Apple Health, reminders, AlarmKit assessment | Honest capability fallback and device/store evidence |
| #10 | Android Health Connect and reminder reliability | Least privilege, reboot/time-zone harness, measured OEM behavior |
| #11 | Rights-clean top-50 exercise catalog and media pipeline | Per-record provenance, hashes, signed scope, takedown/kill switch |
| #12 | Private LLM explanation gateway | Deterministic policy remains authoritative; no client secret or dose advice |
| #13 | Entitlements, privacy, stores, release operations | Exact build, store declarations, signing, rollback, support, account-data lifecycle |
| #14 | Creator-market validation | Rights-cleared disclosed content and retained-contribution evidence |

## Superseded duplicates

- #3 → #11
- #4 → #9 and #10
- #5 → #9 and #10
- #6 → #8 and #12
- #7 → #13

The superseded issues are closed with `duplicate` state reason. Their useful requirements were retained in the active issues; closing them removes competing sources of truth rather than deleting scope.

## Numbering note

`docs/roadmap.md` uses logical phase numbers in its section headings. Use this file and the active GitHub issue URLs as the operational source of truth until the roadmap headings are renumbered in a documentation-only cleanup.

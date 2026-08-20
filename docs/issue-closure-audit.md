# Issue and real-problem closure audit

**Snapshot:** 2026-08-20  
**Rule:** repository engineering, hosted evidence, and external admission are separate.

## Shadow Architect result

The repo-internal convergence chain #55/#57/#61/#63/#65/#67/#69/#71/#73/#75/#77/#79 and #81 is integrated into `main`. Historical sibling #59 was not force-merged after a semantic workflow conflict; its artifact-identity semantics were replayed by #81 on current main and re-proven by run #128.

```text
MERGED_ENGINEERING != PRODUCTION_ADMISSION
OPEN_ISSUE != ABSENT_IMPLEMENTATION
GITHUB_CHECK_PASS != HUMAN_ADMIT
```

## Close / keep-open decision

| Issue / PR | Decision | Evidence / reason |
|---|---|---|
| #54/#55 | closed / merged | domain validators CI-owned |
| #56/#57 | closed / merged | implementation SSOT reconciled |
| #60/#61 | closed / merged | AGENTS runtime contract reconciled |
| #62/#63 | closed / merged | authority drift machine-gated |
| #64/#65 | closed / merged | bilingual README authority reconciled |
| #66/#67 | closed / merged | delivery machine SSOT reconciled |
| #68/#69 | closed / merged | roadmap/Git routing reconciled |
| #70/#71 | closed / merged | implementation status current-chain reconciled |
| #72/#73 | closed / merged | architecture/platform authority reconciled |
| #74/#75 | closed / merged | product/safety authority reconciled |
| #76/#77 | closed / merged | current product implementation SSOT reconciled |
| #78/#79 | closed / merged | machine delivery graph reconciled |
| #58/#59 | closed / **not merged** | historical exact-head evidence valid; workflow conflict stopped merge; superseded by #80/#81 |
| #80/#81 | closed / merged | current-main artifact identity convergence, run #128 3/3 PASS |
| #32 | **keep open** | engineering exists; taxonomy/rights Human Admit still missing |
| #33 | **keep open** | first-party top-50 Draft exists; editorial/rights/media admission missing |
| #35 | **keep open** | provider-boundary contract exists; live adapter/credentials/security/privacy missing |
| #46 | **keep open** | synthetic/default-deny nutrition layer exists; exact real source/version/license mapping missing |
| #47 | **keep open** | deterministic meal compiler exists; production input depends on admitted records from #46 |

## Article / design-request closure mapping

| Original requested problem | Repo closure | Residual gate |
|---|---|---|
| KMP Android+iOS+Web | repository builds and shared architecture repeatedly hosted-proven | signing/store/device production admission |
| copyright-compliant exercise DB | first-party taxonomy + bilingual 50-record Draft + provenance validator | editorial/rights approval and licensed/commissioned media |
| muscle visualization | first-party schematic/local mapping exists | no medical/anatomical validation claim; third-party model rights separate |
| ML Kit / Apple Vision supplement recognition | candidate extraction boundaries implemented | representative consented real-device accuracy corpus |
| supplement daily totals | deterministic compatible-mass arithmetic/logging | intentionally no personalized dose/safety verdict |
| workout/diet timetable | A/B timetable + deterministic meal compiler + reminders | real admitted nutrition records/device reliability |
| LLM Body Hacker analysis | constrained logged/general-information provider contract | live provider deployment/security/privacy |
| Health Connect / HealthKit | least-privilege read adapters implemented | real-device/OEM/entitlement/privacy/store |
| AlarmKit/system exact alarm | not claimed as implemented product authority | platform permission/reliability/store/device evidence required |
| media licensing/vendor CDN | hotlinking/default-unknown rights denied | purchase/commission/exact license receipts required |
| Git Town stacked worker | method + v24 candidate/verifier documented | consumer runtime/config/live canaries remain blocked |

## Closure law

An Issue is closed only when its repository-owned acceptance is complete or when an exact superseding convergence delivers the same semantics. External admission tasks remain open until the required external evidence exists; documentation cannot convert `ABSENT` evidence into `PASS`.

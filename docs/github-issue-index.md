# GitHub Delivery Issue and Pull Request Index

**Authoritative snapshot:** 2026-08-20  
**Repository:** `ed3c/gym-come-true` — public  
**Live-state rule:** GitHub PR/Issue state and exact commit ancestry outrank historical prose.

This index separates merged history, staged engineering, exact-head Draft evidence, and Human/external admission.

```text
OPEN_ISSUE != ABSENT_IMPLEMENTATION
OPEN_DRAFT_PR != MERGED_TO_MAIN
HOSTED_PASS(commit A) != HOSTED_PASS(commit B)
GITHUB_CHECK_PASS != HUMAN_ADMIT
```

## Historical merged baseline

Historical PRs #2/#15/#16/#20/#22 are merged to `main`; their older `PRE_RUN_BLOCKED_BY_ACTIONS_BUDGET` receipts remain historical evidence only. `main@b1880abe317ac274b59695439c4f9682b8864f6b` also contains domain engineering for exercise, nutrition, supplement logging/evidence contracts, explanation/provider boundaries, Health Connect/HealthKit adapters, reminders, and product hardening.

`MERGED_ENGINEERING != PRODUCTION_ADMISSION`.

## Active Draft evidence graph

```text
main@b1880abe...
└── PR #55  DOMAIN_VALIDATORS_OWNED_BY_CI_DRAFT
    ├── PR #57  CURRENT_PUBLIC_REPO_SSOT_DRAFT
    │   └── PR #61  CURRENT_AGENT_RUNTIME_CONTRACT_DRAFT
    │       └── PR #63  MACHINE_GATED_AUTHORITY_DRAFT
    │           └── PR #65  README_AUTHORITY_RECONCILED_AND_GATED_DRAFT
    │               └── PR #67  LIVE_DELIVERY_GRAPH_RECONCILED_DRAFT
    │                   └── PR #69  SECONDARY_ROUTING_SURFACES_RECONCILED_DRAFT
    │                       └── PR #71  CURRENT_IMPLEMENTATION_SSOT_DRAFT
    │                           └── PR #73  CURRENT_ARCHITECTURE_PLATFORM_AUTHORITY_DRAFT
    │                               └── PR #75  CURRENT_PRODUCT_SAFETY_AUTHORITY_DRAFT
    │                                   └── PR #77  CURRENT_PRODUCT_IMPLEMENTATION_SSOT_DRAFT
    │                                       └── PR #79 / Issue #78  CURRENT_MACHINE_DELIVERY_GRAPH_DRAFT
    └── PR #59  TRANSPORT_AND_SEMANTIC_IDENTITIES_SEPARATED_DRAFT
```

Every Draft head owns separate evidence. A child does not inherit a parent's green run.

## Exact-head hosted receipts

| PR | Issue | Exact head | Hosted receipt | Meaning |
|---:|---:|---|---|---|
| #55 | #54 | `1338b6fd2a1007cf06e24aca3a6a4bd07f9b7fa5` | run #88, 3/3 PASS | domain validators CI-owned |
| #57 | #56 | `58e4fc14aa0347b9c47dd15ff8f7f58f8b97f8d6` | run #89, 3/3 PASS | public implementation SSOT |
| #59 | #58 | `036951d5a57809809564cca824013f428bc1ce3e` | run #90, 3/3 PASS | artifact identity lane |
| #61 | #60 | `7a59f6b80f806476fdcea90f4b7722dc0ecc8ef3` | run #91, 3/3 PASS | Agent runtime authority |
| #63 | #62 | `0c76c71413a73194986418e5f24571840623197f` | run #92, 3/3 PASS | authority machine gate |
| #65 | #64 | `3046807758d025e0a9ad903f1109d1c6942e312f` | run #96, 3/3 PASS | bilingual README authority |
| #67 | #66 | `9b04470098c438f1fb9bf9cb80e87752754e10de` | run #100, 3/3 PASS | delivery graph X8 |
| #69 | #68 | `c9837970086ea0a522bda35a435ee1acc89e4ff2` | run #102, 3/3 PASS | roadmap/Git routing |
| #71 | #70 | `59cd8d3bea3a13b1c3acf36530c0436196d34a01` | run #103, 3/3 PASS | implementation SSOT X10 |
| #73 | #72 | `c633066df3361b3c57ebbfafd994755a8b6c999c` | run #105, 3/3 PASS | architecture/platform authority |
| #75 | #74 | `144035f07694fc0439a094ba5326e0e93f9ee3a4` | run #108, 3/3 PASS | product/safety authority |
| #77 | #76 | `56844bba2f46b0488ee18669f7bdde28f6603690` | run #110, 3/3 PASS | current product implementation SSOT |
| #79 | #78 | current branch head | fresh exact-head run required | current machine-delivery reconciliation |

## Current domain/product truth

| Domain | Engineering present | Still not admitted |
|---|---|---|
| Supplement/product | OCR/barcode candidates, confirmed arithmetic, logging/timetable contracts; current MVP is information/logging and renders no safety verdict | diagnosis, personalized dose, medication compatibility, clinical authority |
| Taiwan source lifecycle | immutable-source/mapping/release/revoke/rollback contracts | real official bytes, reuse/legal/qualified review, production activation |
| Exercise | canonical taxonomy + first-party bilingual 50-record `DRAFT` catalog + validator | editorial/rights/media admission |
| Nutrition | synthetic/default-deny catalog + admission schema + deterministic meal-plan compiler | real source/version/license/mappings |
| AI/explanation | OpenAI/Anthropic provider descriptors, mandatory medical-risk notice, logged-totals/general-information surface, deterministic fallback | real provider adapters, credentials/deployment, security/privacy review |
| Android/iOS health | Health Connect / HealthKit read adapter surfaces | real-device/OEM/entitlement/privacy/store evidence |
| Reminders | local notification/recurrence contracts | exact/system alarm admission and device reliability |
| Git Town | v24.0.0 pinned candidate metadata/verifier/canary harness | consumer config, live canaries, runtime/supply-chain/legal admission |

Dormant rule-pack/decision-receipt engineering is provenance/tested code, not current MVP product authority.

## Open Issue semantics

Issues #32/#33/#35/#46/#47 remain open because external or Human acceptance remains, not because their repository engineering is absent. Issues #54/#56/#58/#60/#62/#64/#66/#68/#70/#72/#74/#76 have exact Draft PR engineering; Issue #78 is implemented by Draft PR #79 and awaits fresh exact-head evidence plus Human merge.

Issue #45's Actions-capacity problem is historical. Current hosted execution exists; old blocked SHAs must not be rewritten as PASS.

## Machine delivery authority

The delivery graph is owned as a bound pair:

- `docs/git/STACKED_PRS.md`
- `docs/git/stacked-delivery-manifest.json`
- `python3 scripts/validate_stacked_delivery.py --self-test`

The manifest SHA-256 binds the exact narrative bytes. Any graph transition must update the pair atomically.

## Human / external gates

Documentation and CI cannot close legal/clinical/editorial/rights acceptance, real source licensing, real-device/OEM/entitlement/store evidence, provider credentials/deployment/security review, release signing/promotion, destructive rollback, merge authority, or Git Town runtime admission.

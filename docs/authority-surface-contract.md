# Authority surface drift contract

Repository authority is distributed across nine checked-in surfaces:

- `AGENTS.md`;
- `docs/implementation-status.md`;
- `README.md`;
- `README.zh-TW.md`;
- `docs/roadmap.md`;
- `docs/git/README.md`;
- `docs/architecture.md`;
- `docs/platform-capability-matrix.md`;
- `docs/store-compliance.md`.

A stale claim in any of them can send an Agent/reviewer into an obsolete branch, capability, licensing, product, platform, or admission state.

## State progression

```text
MANUALLY_RECONCILED_AUTHORITY
  -> MACHINE_GATED_AUTHORITY_DRAFT
  -> README_AUTHORITY_RECONCILED_AND_GATED_DRAFT
  -> LIVE_DELIVERY_GRAPH_RECONCILED_DRAFT
  -> SECONDARY_ROUTING_SURFACES_RECONCILED_DRAFT
  -> CURRENT_IMPLEMENTATION_SSOT_DRAFT
  -> CURRENT_ARCHITECTURE_PLATFORM_AUTHORITY_DRAFT
  -> CURRENT_PRODUCT_SAFETY_AUTHORITY_DRAFT
  -> CURRENT_PRODUCT_IMPLEMENTATION_SSOT_DRAFT
```

The owning offline gate is:

```bash
python3 scripts/validate_authority_surfaces.py
python3 scripts/validate_authority_surfaces.py --self-test
```

`docs/product/mvp-redesign.md`, `legal/DISCLAIMER.md`, `docs/health-safety.md`, and `scripts/validate_product_safety_authority.py` separately own the product/safety authority binding. This nine-surface gate ensures the general Agent-facing implementation status does not route back into a product state that the product/safety gate has retired.

## What the gate rejects

The validator fails closed on already-disproved states, including:

- private-repository or global no-hosted-run claims;
- merged history described as current Draft work;
- implementation-status routed to obsolete PR #55/Issue #56, PR #69/Issue #70 current-state authority, or any older head after PR #75 became the current top Draft evidence;
- implementation-status reintroducing `LOG/REVIEW/BLOCK` as the current MVP supplement authority;
- implementation-status collapsing the current AI surface back to receipt-only safety-decision explanation rather than logged/general-information provider boundaries;
- removal of `INFORMATION_OR_LOGGING != SAFETY_VERDICT` or promotion of dormant reviewed-rule-pack contracts into a current MVP prerequisite;
- OpenAI/Anthropic provider-boundary engineering described as absent while real provider credentials/deployment are falsely described as admitted;
- open Issue collapsed into absent implementation;
- Health Connect/HealthKit adapters described as absent, future-only, or boundary-only;
- architecture that routes current health/reminder work to obsolete future ownership;
- current iOS `NSHealthShareUsageDescription` or Android least-privilege Health Connect permissions described as not implemented;
- adapter presence promoted into real-device, entitlement, OEM, privacy, or store admission;
- Git Town v24.0.0 candidate described as absent or runtime-admitted;
- exact/system alarm capability inflated beyond current reminder semantics;
- repository-authored code described as proprietary despite Apache-2.0;
- exact-head evidence collapsed across commits;
- Human Admit, legal/clinical/rights, device/store/provider/signing/release boundaries removed.

The self-test plants representative stale claims across all nine surfaces. The current suite contains **41 planted drifts**, including product-routing, architecture/platform/store and historical-evidence regressions.

## Ownership split

- `docs/implementation-status.md`: merged/staged engineering, current owner-product routing, exact-head evidence, open-Issue semantics and remaining gates.
- `docs/architecture.md`: directory/state-machine/data-flow design and implementation-vs-admission boundaries.
- `docs/platform-capability-matrix.md`: per-platform checked-in capability truth versus missing runtime evidence.
- `docs/store-compliance.md`: checked-in permission/data-flow state versus external store/privacy/release gates.
- `docs/github-issue-index.md`: GitHub Issue/PR semantics and historical/current evidence distinction.
- `docs/git/STACKED_PRS.md` + manifest: branch-level delivery graph.
- `docs/roadmap.md`: current dispatch/dependency routing.
- `docs/git/README.md`: Git governance entrypoint.
- root README/AGENTS: public and Agent-facing hard laws.
- product/safety quartet: owner product decision, disclaimer SSOT, health-safety contract and dedicated product-safety validator.

## Evidence boundary

This is a network-free repository-content consistency gate. It does not discover live GitHub state or prove device/store/external facts.

```text
CHECKED_IN_AUTHORITY_PASS != LIVE_GITHUB_STATE_PROVEN
HISTORICAL_RECORD != CURRENT_ROUTING_AUTHORITY
OPEN_ISSUE != ABSENT_IMPLEMENTATION
INFORMATION_OR_LOGGING != SAFETY_VERDICT
REVIEWED_RULE_PACK_CONTRACT_PRESENT != MVP_RULE_PACK_REQUIRED
CONTRACT_CODE != LIVE_PROVIDER_EVIDENCE
HOSTED_PASS(commit A) != HOSTED_PASS(commit B)
HOSTED_CHECK_PASS != MERGE_AUTHORITY
ADAPTER_PRESENT != REAL_DEVICE_VALIDATION
DECLARED_PERMISSION != STORE_APPROVAL
GIT_TOWN_CANDIDATE != GIT_TOWN_RUNTIME_ADMITTED
CODE_OR_TEST != LEGAL_CLINICAL_RIGHTS_APPROVAL
APACHE_2_0_REPO_LICENSE != THIRD_PARTY_MEDIA_RIGHTS
DEBUG_BUILD != RELEASE_SIGNING_ADMISSION
```

When an intentional architecture, licensing, platform, product, branch, permission, or evidence transition makes an assertion obsolete, update the owning implementation/evidence first, then reconcile every affected authority surface and planted control. Never weaken the gate merely to make CI green.

Merge, release promotion, signing admission, legal/clinical/editorial/rights acceptance, provider/store credentials, real-device/OEM/entitlement evidence, Git Town runtime admission, and destructive rollback remain Human Admit or external gates.

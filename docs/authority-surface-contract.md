# Authority surface drift contract

`AGENTS.md`, `docs/implementation-status.md`, `README.md`, and `README.zh-TW.md` are repository authority surfaces. A stale claim in any of them can route a later Agent, reviewer, or contributor into the wrong implementation, evidence, licensing, or admission decision.

## State transition

```text
MANUALLY_RECONCILED_AUTHORITY
  -> MACHINE_GATED_AUTHORITY_DRAFT
  -> README_AUTHORITY_RECONCILED_AND_GATED_DRAFT
```

The owning offline gate is:

```bash
python3 scripts/validate_authority_surfaces.py
python3 scripts/validate_authority_surfaces.py --self-test
```

## What the gate rejects

The validator fails closed when checked-in authority regresses to already-disproved states, including:

- repository visibility described as private;
- a global claim that hosted Actions never executed;
- Health Connect or HealthKit adapter work described as absent/future-only;
- the pinned Git Town v24.0.0 candidate described as absent or its blocked runtime boundary removed;
- English/Traditional Chinese README describing already-merged/staged domain work as globally future-only;
- repository-authored code described as proprietary despite the Apache-2.0 `LICENSE`;
- exact-head hosted evidence collapsed across commits;
- Human Admit, legal/clinical/rights, real-device, or runtime-admission boundaries removed.

The self-test plants representative stale claims across Agent, implementation-status, English README, and Traditional Chinese README surfaces and requires every mutation to fail.

## Evidence boundary

This is a repository-content consistency gate. It is intentionally network-free and therefore does **not** discover live GitHub state. Live PR heads, repository visibility, workflow conclusions, issue state, branch ancestry, licenses of external assets, device behavior, and external approvals must still be resolved from their owning evidence source.

A PASS proves only that checked-in authority surfaces preserve the distinctions encoded by this validator. It does not prove:

```text
CHECKED_IN_AUTHORITY_PASS != LIVE_GITHUB_STATE_PROVEN
HOSTED_CHECK_PASS != MERGE_AUTHORITY
ADAPTER_PRESENT != REAL_DEVICE_VALIDATION
GIT_TOWN_CANDIDATE != GIT_TOWN_RUNTIME_ADMITTED
CODE_OR_TEST != LEGAL_CLINICAL_RIGHTS_APPROVAL
APACHE_2_0_REPO_LICENSE != THIRD_PARTY_MEDIA_RIGHTS
DEBUG_BUILD != RELEASE_SIGNING_ADMISSION
```

## Change law

When an intentional architecture, licensing, platform, or evidence transition makes one of these assertions obsolete, update the implementation/evidence first, then update every affected authority surface and this validator in the same convergence packet. Never weaken the gate merely to make CI green.

Merge, release promotion, signing admission, legal/clinical/rights acceptance, provider/store credentials, real-device evidence, Git Town runtime admission, and destructive rollback remain Human Admit or external gates.

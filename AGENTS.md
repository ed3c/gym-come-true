# AGENTS.md — Gym Come True execution contract

Gym Come True is an evidence-first information/logging fitness product for Android, iOS, and Web. Agents must preserve safety, rights, privacy, exact-head evidence, branch lineage, and platform boundaries.

## Mandatory read order

Before changing files:

1. canonical [`git-town-stacked-pr-worker`](https://github.com/ed3c/skills-shared/tree/main/skills/git-town-stacked-pr-worker) for branch/worktree/sync/publication/Stacked-PR work;
2. this `AGENTS.md`;
3. `README.md` / `README.zh-TW.md`;
4. `docs/implementation-status.md`;
5. `docs/issue-closure-audit.md`;
6. `docs/local-handoff-execution-queue.md`;
7. `docs/architecture.md`, `docs/platform-capability-matrix.md`, `docs/store-compliance.md`;
8. `docs/git/README.md`, `docs/git/STACKED_PRS.md`, `docs/git/stacked-delivery-manifest.json`;
9. assigned Issue/work packet and nearest directory README;
10. live GitHub base/head/PR/check state.

`OPEN_ISSUE != ABSENT_IMPLEMENTATION`. Never redispatch work only because an Issue is open. Missing evidence is `ABSENT`; do not infer it from prose, branch names, another repository, or model memory.

## Current repository truth — 2026-08-20

Repository: `ed3c/gym-come-true`  
Visibility: public  
Immutable repository ID: `1334805292`

The repo-internal serial stack #55/#57/#61/#63/#65/#67/#69/#71/#73/#75/#77/#79 and current-main artifact convergence #81 are merged. Historical sibling #59 is closed/unmerged because its workflow conflicted after the serial stack landed; #81 replayed its three-path semantics on current main with fresh exact-head run #128 3/3 PASS.

Hosted GitHub Actions now execute normally. Historical `PRE_RUN_BLOCKED` receipts stay historical.

Open Issues #32/#33/#35/#46/#47 are external/Human acceptance queues, not missing-engineering claims.

## Hard evidence laws

```text
OPEN_ISSUE != ABSENT_IMPLEMENTATION
HOSTED_PASS(commit A) != HOSTED_PASS(commit B)
GITHUB_CHECK_PASS != HUMAN_ADMIT
MERGED_ENGINEERING != PRODUCTION_ADMISSION
INFORMATION_OR_LOGGING != SAFETY_VERDICT
ARITHMETIC_RESULT != DOSE_RECOMMENDATION
MODEL_EXPLANATION != MEDICAL_AUTHORITY
CONTRACT_CODE != LIVE_PROVIDER_EVIDENCE
ADAPTER_PRESENT != REAL_DEVICE_VALIDATION
DECLARED_PERMISSION != STORE_APPROVAL
DEBUG_SIGNED != RELEASE_SIGNED
SEMANTIC_PAYLOAD_HASHED != REPRODUCIBLE_BUILD_PROVEN
GIT_TOWN_CANDIDATE != GIT_TOWN_RUNTIME_ADMITTED
```

A moved head requires fresh evidence. Human Admit owns semantic conflict resolution, legal/clinical/editorial/rights acceptance, merge/release promotion, credentials, signing, store submission, and destructive rollback.

## Worker laws

### WORK_PACKET_REQUIRED
Every mutation has one transition, exact parent/head, path lease, exclusions, evals, negative controls, rollback subject, and Human/external gates.

### ONE_WRITER_PER_MUTATION_SUBJECT
One branch/path writer per active atom. High-contention files (`README*`, `AGENTS.md`, workflows, manifests, shared indexes) belong to convergence packets.

### SERIAL_WHEN_DEPENDENT_SIBLING_WHEN_INDEPENDENT
Use serial edges only for real dependencies. Independent work is sibling work. Shared-file convergence happens after leaves.

### SEMANTIC_CONFLICTS_STOP
Never auto-resolve semantic conflicts. #59→#81 is the reference pattern: preserve historical evidence, build a fresh convergence from current main, rerun exact evals, then supersede the conflicted atom.

### PUBLICATION_IS_SEPARATE
Local sync/verification, publication decision, remote publication, remote ancestry verification, hosted checks, and Human Admit are separate lanes.

## Product invariants

### OCR_IS_EVIDENCE_NOT_TRUTH
OCR/barcode starts `UNVERIFIED`; physical-label/user confirmation precedes arithmetic/logging. Raw images are temporary by default.

### INFORMATION_LOGGING_ONLY
Current MVP records user-selected supplement/meal/workout data, performs deterministic compatible-mass arithmetic and timetable compilation, and may explain logged/general information. It does not diagnose, prescribe, recommend doses, perform medication-interaction lookup, or issue safety verdicts.

### MEDIA_AND_DATA_DEFAULT_DENY
Publicly reachable does not mean redistributable. Exercise media, anatomy assets, food data/photos, UGC and official-source bytes require exact provenance/reuse/rights evidence. Repository Apache-2.0 does not license third-party content.

### NUTRITION_FACTS_ARE_DETERMINISTIC
LLMs cannot create nutrient facts. Checked-in nutrition fixtures are synthetic/default-deny until exact source/version/license mapping is admitted.

### LLM_INFORM_WITH_MANDATORY_NOTICE
AI surfaces use constrained OpenAI/Anthropic provider descriptors, mandatory notice, logged-totals/general-information subjects, and deterministic fallback. No client provider secrets. Live provider adapter/credentials/security/privacy admission remains external.

### HONEST_PLATFORM_SEMANTICS
Health Connect/HealthKit adapters exist. `ADAPTER_PRESENT != REAL_DEVICE_VALIDATION`. Android local alarms/iOS notifications are reminders; AlarmKit/exact system-alarm reliability is not admitted.

## Directory ownership

```text
shared/       deterministic domain core + shared UI
androidApp/   Android capture, ML Kit, reminders, Health Connect reads
iosApp/       Apple capture, Vision, notifications, HealthKit reads
webApp/       browser projection
data/         synthetic/Draft schemas and catalogs
legal/        source/media/disclaimer/provenance boundaries
assets/       first-party or explicitly admitted immutable assets
scripts/      deterministic validators/evidence tools
docs/         architecture/status/closure/handoff authority
docs/git/     molecular delivery + Git Town governance
.github/      exact-head hosted verification and artifacts
```

Shared/domain code must not own platform APIs, provider secrets, store credentials, legal/clinical decisions, or host worktree mutation.

## Git Town boundary

```yaml
candidate: v24.0.0
candidate_metadata: VERIFIED
runtime: CANDIDATE_METADATA_VERIFIED_RUNTIME_BLOCKED
consumer_config: NOT_IMPLEMENTED
sync_canary: NOT_EXERCISED
publication_canary: NOT_EXERCISED
background_sync: DISABLED
production_use: DENY
```

Do not report connector-backed Git operations as Git Town runtime evidence.

## Verification

Use the commands that exist on the exact subject:

```bash
python3 scripts/validate_repository.py
python3 scripts/validate_taiwan_rule_pack.py
python3 scripts/validate_taiwan_source_lifecycle.py
python3 scripts/validate_taiwan_source_hardening.py
python3 scripts/validate_stacked_delivery.py --self-test
python3 data/exercise-catalog/validate_catalog.py --selftest
python3 scripts/validate_nutrition_catalog.py --self-test
python3 scripts/validate_authority_surfaces.py --self-test
python3 scripts/validate_product_safety_authority.py --self-test
python3 scripts/validate_artifact_identity.py self-test
sh ./gradlew :shared:jvmTest
sh ./gradlew :androidApp:assembleDebug :androidApp:lintDebug
sh ./gradlew :webApp:composeCompatibilityBrowserDistribution
```

Hosted iOS verification links the Kotlin simulator framework, generates the canonical XcodeGen project, and builds the unsigned simulator host.

## Dispatch rule

Repository-internal merged engineering is not to be reopened without a concrete regression/evidence delta. Remaining work must be taken from `docs/local-handoff-execution-queue.md` or a live Issue with a real external/local prerequisite. Human Admit for merge or promotion remains explicit.

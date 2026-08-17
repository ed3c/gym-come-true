# Exercise content, taxonomy, media, and visualization

This directory documents the exercise-catalog lane: Issues #32 (taxonomy contract), #33 (rights-clean
bilingual top-50), #34 (media admission pipeline), and #48 (muscle visualization).

| Document | Issue | Transition delivered |
|---|---|---|
| [Taxonomy and catalog](exercise-taxonomy-and-catalog.md) | #32, #33 | `DEMO_CATALOG -> TAXONOMY_CONTRACT`, then `TAXONOMY_CONTRACT -> RIGHTS_CLEAN_TOP50_DRAFT` |
| [Media admission](exercise-media-admission.md) | #34 | contract core only; `LICENSED_MEDIA_PIPELINE` is **not** reached |
| [Muscle visualization](muscle-visualization.md) | #48 | canonical muscle mapping and intensity semantics; Compose rendering **not** delivered |

## What this lane owns

```text
shared/src/commonMain/kotlin/dev/ed3c/gymcometrue/catalog/
  ExerciseTaxonomy.kt      closed vocabulary + bilingual muscle labels
  ExerciseProvenance.kt    per-field provenance and licence-grant rules
  ExerciseCatalog.kt       raw-record validation, claim screen, accessibility text
  MuscleVisualization.kt   muscle -> schematic region binding, draw plan
  MediaAdmission.kt        media intake ladder, derivatives, takedown, ledger

shared/src/commonTest/kotlin/dev/ed3c/gymcometrue/catalog/
  ExerciseCatalogContractTest.kt
  MediaAdmissionTest.kt

data/exercise-catalog/
  taxonomy.v1.json           machine mirror of the Kotlin vocabulary
  catalog.v1.json            50 authored bilingual records
  media-intake.synthetic.json  media fixture, nothing past HASH_VERIFIED
  validate_catalog.py        cross-artifact gate, with --selftest

legal/provenance/exercise-catalog-v1.json   authorship record for the 50 records
```

## Verification actually executed

```bash
python3 data/exercise-catalog/validate_catalog.py            # 5 checks, exit 0
python3 data/exercise-catalog/validate_catalog.py --selftest # 17 planted defects, all detected
python3 scripts/validate_repository.py                       # unchanged, exit 0
```

`sh ./gradlew :shared:jvmTest` was **NOT_EXERCISED** in this lane. The Kotlin sources and tests here
have never been compiled or run; they are `STATIC`-grade only until a serial integrator executes the
JVM test task. Do not read a green Python run as evidence about Kotlin.

## External gate ledger

Nothing in this lane may be read as stronger than the row that authorizes it.

| Gate | State | What would change it |
|---|---|---|
| Editorial review of the 50 records | `HUMAN_ADMIT_REQUIRED` | A named human reads the wording and accepts it |
| Rights review of the 50 records | `HUMAN_ADMIT_REQUIRED` | A qualified reviewer confirms the authorship claim and scope |
| Clinical / physiotherapy review | `ABSENT` | Out of this lane entirely; no record claims it |
| Executed commercial media rights | `ABSENT` | A signed scope with platform, territory, term, derivative, and redistribution terms |
| Commissioned first-party artwork | `ABSENT` | A commission with an assignment or licence and an invoice |
| Reviewer attestation hashes | `ABSENT` | Produced by the reviewer, never by code and never by hand |
| Real device / cross-platform snapshot runs | `NOT_EXERCISED` | Compose rendering exists and runs on a device |
| Hosted CI check on this branch | `NOT_EXERCISED` | Repository-wide; see Issue #45 |

## Invariants this lane is built to hold

- **A repository-level licence never authorizes a record or an asset.** `LICENSE` covers code. Every
  exercise record and every media row must name its own grant, and `REPOSITORY_ROOT_LICENSE` is a
  hard blocker in both `FieldProvenanceValidator` and `MediaAdmissionValidator`.
- **Unknown taxonomy fails closed.** Vocabularies are closed enums; an unrecognised token is
  rejected, never coerced into a nearest match and never bucketed into `OTHER`.
- **Media defaults to DENY.** No image, animation, video, 3D model, vendor id, or CDN link is
  admitted. The one hashed asset in this lane is the first-party schematic that already existed in
  the repository, and it stops at `HASH_VERIFIED` because no rights review has happened.
- **Muscle intensity is editorial, not physiological.** `PRIMARY`/`SECONDARY`/`STABILIZER` are a
  movement classification used for deterministic rendering weight. They are not EMG, not a
  percentage of maximal voluntary contraction, and support no clinical claim.
- **Agent-drafted text is labelled as such.** `AuthorshipMethod.FIRST_PARTY_AGENT_DRAFTED` is a
  distinct member from `FIRST_PARTY_HUMAN_ORIGINAL` precisely so the two cannot be collapsed, and
  production admission blocks on it until a human accepts the wording.

## Known follow-ups outside this lane's path lease

- `data/seed/first-party-demo-exercises.json` uses an older private vocabulary (`WALL`,
  `EXERCISE_MAT_OPTIONAL`) that the canonical taxonomy does not contain. Reconciling the demo seed
  is a convergence change, not a sibling one.
- `data/exercise-catalog/validate_catalog.py` is not wired into `scripts/` or
  `.github/workflows/verify.yml`. Both surfaces are outside this lane's lease and need a convergence
  packet before the gate runs automatically.
- `legal/provenance/muscle-map-schematic.json` still carries
  `sha256: "TO_BE_FILLED_BY_ASSET_VALIDATION_SCRIPT"`. The real digest is now recorded and checked in
  `data/exercise-catalog/media-intake.synthetic.json`; filling the legal record itself belongs to
  whoever owns that provenance file.

# Exercise taxonomy and the rights-clean top-50 catalog

Issues #32 (`DEMO_CATALOG -> TAXONOMY_CONTRACT`) and #33 (`TAXONOMY_CONTRACT -> RIGHTS_CLEAN_TOP50`).

Delivered state: **`TAXONOMY_CONTRACT`** for #32, and **`RIGHTS_CLEAN_TOP50_DRAFT`** for #33 — the 50
records exist, validate, and carry per-field provenance, but no human has accepted the wording or the
rights claim, so the `RIGHTS_CLEAN_TOP50` transition is not complete.

## 1. Identity

```text
slug   lowercase-hyphen, unique          back-squat
id     CatalogSchema.ID_PREFIX + slug    gct-back-squat
```

The id is derived, not independent. A record whose id is not exactly `gct-<slug>` is rejected, which
removes the whole class of bugs where an id and a slug drift apart and two surfaces disagree about
which exercise they are talking about.

`schemaVersion` is a single executable value (`CatalogSchema.CURRENT_VERSION = 1`). An older or newer
input fails closed rather than being migrated in place: there is no second version to migrate from,
and writing a speculative migration path now would be a mechanism with no case to serve. When a
version 2 exists, the migration belongs next to the reason it exists.

## 2. Closed vocabularies

| Dimension | Members | Notes |
|---|---|---|
| `MuscleGroup` | 20 | 15 renderable by the first-party schematic, 5 not — see [muscle visualization](muscle-visualization.md) |
| `MovementPattern` | 18 | all 18 are exercised by the 50 records |
| `Mechanics` | 3 | `COMPOUND`, `ISOLATION`, `ISOMETRIC` |
| `ForceVector` | 3 | `PUSH`, `PULL`, `STATIC` |
| `Laterality` | 3 | `BILATERAL`, `UNILATERAL`, `ALTERNATING` |
| `SkillLevel` | 3 | `BEGINNER`, `INTERMEDIATE`, `ADVANCED` |
| `EquipmentClass` | 13 | trimmed to what the 50 records actually need |
| `ActivationIntensity` | 3 | `PRIMARY(3)`, `SECONDARY(2)`, `STABILIZER(1)` |

Resolution goes through `Taxonomy.muscle(raw)` and friends, which return `null` for an unknown token.
There is no fallback member, no `OTHER` bucket, and no fuzzy match. `ExerciseCatalogValidator` turns
that `null` into `Unknown taxonomy token '<raw>' for <field>` and rejects the record.

The vocabulary exists twice on purpose — once as Kotlin enums (executable) and once in
`data/exercise-catalog/taxonomy.v1.json` (readable by the Python gate and by tooling that is not
Kotlin). Two copies is a drift risk, so `validate_catalog.py` re-parses the Kotlin enums by brace
matching and requires token-for-token equality, including order. The same check covers the bilingual
muscle label table.

## 3. Per-field provenance

```text
FieldProvenance(field, authorship, licenseGrant, provenanceRecordId, reviewState)
```

Provenance is attached per field, not per record, because the rights domains genuinely differ: the
English summary, the Traditional Chinese steps, the muscle mapping, and any future image can each
have a different origin and a different grant. A record-level claim would let one field inherit
authorization from its neighbours.

### Negative controls

| Input | Result |
|---|---|
| `licenseGrant = REPOSITORY_ROOT_LICENSE` | rejected — the root `LICENSE` covers code, never a record |
| `licenseGrant = NONE` | rejected — no grant is not an implicit grant |
| `authorship = SCRAPED_OR_MIRRORED` | rejected, at any review state, permanently |
| `authorship = UNKNOWN` | rejected — unknown rights fail closed |
| `authorship = LICENSED_THIRD_PARTY` without `EXECUTED_ASSET_SCOPE` | rejected |
| provenance missing for a required field | rejected — a gap is a blocker, not a default |
| provenance present for `MEDIA` on a record with no media | rejected — inventing a grant for an absent asset is the same fabrication as omitting one for a present asset |

`CatalogField.MEDIA` is deliberately outside `ALWAYS_REQUIRED` and becomes required the moment a
record cites a media id.

### Why `FIRST_PARTY_AGENT_DRAFTED` is its own member

Every string in `catalog.v1.json` was drafted by an automated agent working for the repository owner.
That is first-party in the copyright sense, and it is *not* the same evidence as a human writing and
signing off on the wording. Collapsing the two into one "first party" member would make the catalog
look reviewed. Instead:

- the member exists separately from `FIRST_PARTY_HUMAN_ORIGINAL`;
- every field's `reviewState` is `DRAFT`, and `validate_catalog.py` fails if any row claims more;
- `FieldProvenanceValidator` blocks production for agent-drafted fields with an explicit
  "requires human editorial acceptance" message;
- `legal/provenance/exercise-catalog-v1.json` records `decision: PENDING_REVIEW` and `reviewer: null`.

## 4. The 50 records

`data/exercise-catalog/catalog.v1.json`. Each record carries:

- bilingual `name` and `summary` (`en`, `zh-Hant-TW`, both mandatory, neither may be blank);
- 3 bilingual `steps` and 1–2 bilingual `commonErrors`, with **equal entry counts across locales** so
  a translation cannot quietly lose a step;
- `muscleEngagement` with at least one `PRIMARY` muscle and no repeated muscle;
- `safetyNoteRef` into one of five shared safety notes;
- `mediaRefs: []`, because nothing has been admitted;
- seven `fieldProvenance` entries.

The Traditional Chinese is written in Taiwanese usage and word order, not converted from Simplified
and not machine-translated from the English line. The two locales are parallel first drafts of the
same movement, which is why neither is marked as a translation of the other.

Safety notes live once at catalog level and are referenced by key. Fifty copies of the same stop-rule
paragraph would be fifty places for it to drift; one copy with a referential integrity check is the
same guarantee with less surface. An unresolvable `safetyNoteRef` is a blocker.

### Medical-claim screen

`MedicalClaimScreen` is a deterministic substring screen over the instructional fields only — name,
summary, steps, common errors. It runs in both languages (`治療`, `療效`, `診斷`, `預防受傷`,
`保證`, `燃脂`, … alongside `cures`, `treats `, `diagnosis`, `prevents injury`, `clinically proven`, …).

It deliberately does **not** run over the safety notes: those legitimately say "not medical advice"
and "不是復健或醫療建議", and a screen that flags its own disclaimer trains people to disable it.

This is a screen, not a review. Passing means no known claim phrase was found. It says nothing about
whether the wording is accurate, appropriate, or acceptable — that is the editorial gate, and it is
`HUMAN_ADMIT_REQUIRED`.

A URL-shaped string anywhere in catalog text is also a blocker. Catalog text carries no links, which
is what a hotlink would look like if one were ever pasted in.

## 5. Validated records are a different type

`ExerciseCatalogValidator` is the only thing that can build an `ExerciseRecord`: its constructor is
`internal`, and the serialized shape is `RawExerciseRecord`. Downstream code cannot hand-assemble a
record out of unchecked strings, so "was this validated?" is answered by the compiler instead of by
remembering to call a validator. `ExerciseRecord` is intentionally not `@Serializable`: anything
arriving from disk or the network is raw and must be re-validated.

## 6. Accessibility text is derived, not authored

`ExerciseAccessibility.label(record, locale)` builds the spoken description from the record's name
and its muscle engagement:

```text
en  Back Squat. primary muscles: Glutes, Quadriceps. supporting muscles: ... .
zh  槓鈴背蹲舉。主要肌群：股四頭肌、臀大肌。協同肌群：…。
```

Deriving it means the screen-reader description cannot drift away from the muscle map that the
visualization draws, and it removes 100 hand-written strings from the catalog. Ordering is fixed
(intensity descending, then muscle label sorted) so the output is stable enough to assert on.

## 7. What is not delivered

- No human editorial or rights review. Both are `HUMAN_ADMIT_REQUIRED`.
- No JSON Schema file. The executable contract is the Kotlin validator plus `validate_catalog.py`; a
  third hand-maintained description of the same shape would be a third thing to drift.
- No migration path from `data/seed/first-party-demo-exercises.json` *into* `catalog.v1.json`. The
  seed's own vocabulary was reconciled with the canonical one under Issue #48 and is now gated by
  `check_seed` in `validate_catalog.py` — see [Muscle visualization](muscle-visualization.md) — but
  the three demo records are still a separate dataset with a separate rights record, not catalog
  rows.
- No retention or breadth evidence. The "top 50" here is a scope decision, not a claim that these are
  the 50 most-used exercises by any measured population.

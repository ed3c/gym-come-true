# Nutrition catalog fixtures

This directory contains **synthetic or non-executable examples only**, matching the convention
already established by `data/taiwan-supplement/`. Nothing here is a real, legally reviewed, or
production-admitted nutrition dataset.

## Issue #46 (N1) — copyright-clean Taiwan food/nutrition data layer

- `food-catalog.example.json` is a repository-authored, bilingual (Traditional Chinese / English)
  sample catalog. Every entry is `synthetic: true`, `sourceId: "synthetic-fixture"`, and carries a
  note stating the values are not a verified nutrition claim. One entry (`unresolved-serving-example`)
  deliberately has no gram-resolvable serving, to prove the compiler leaves it unresolved instead of
  guessing.
- `source-candidates.example.json` records real Taiwan institutions/portals that were *evaluated* for
  future admission (TFDA's food composition database, the data.gov.tw open-data portal, MOHW/HPA
  dietary reference intakes). It contains **no scraped data, no pinned dataset ID, and no snapshot
  hash** — every source stays `CANDIDATE + DENY`. See
  [`../../docs/nutrition/taiwan-source-evaluation.md`](../../docs/nutrition/taiwan-source-evaluation.md)
  for the prose evaluation, including what remains `ABSENT`/`HUMAN_ADMIT_REQUIRED`.

A real catalog entry requires an admitted `SourceFieldMapping` (from
`dev.ed3c.gymcometrue.domain`, reused as-is — this lane does not fork a second source-lifecycle
implementation) bound to a `LEGAL_REVIEWED`, hash-verified `ImmutableSourceArtifact`. The Kotlin
`FoodCatalogAdmissionValidator` (`shared/src/commonMain/.../nutrition/FoodCatalog.kt`) enforces this
default-deny chain; see `FoodCatalogTest.kt` for the admitted/rejected/test-only cases.

`legal/` remains the authoritative production candidate-source registry location (out of this lane's
path lease). Promoting anything from `source-candidates.example.json` into a real registry entry is a
separate packet and Human Admit, not something this lane can do.

## Issue #47 (N2) — deterministic daily meal-plan/timetable compiler

No fixture file backs the meal-plan compiler: it is pure deterministic domain logic
(`shared/src/commonMain/.../mealplan/MealPlanCompiler.kt`) over the existing A/B protocol timetable
in `dev.ed3c.gymcometrue.domain.DailyProtocolCompiler`, plus `FoodPortion`/`MealSlotOverride` values
supplied by the caller. See `MealPlanCompilerTest.kt` for the exhaustive deterministic cases
(missing serving, unit mismatch, duplicate foods, cross-midnight ordering, unconfirmed/unknown
overrides, impossible constraints) and
[`../../docs/nutrition/meal-plan-compiler.md`](../../docs/nutrition/meal-plan-compiler.md) for the
narrative.

## Machine-readable schema

- `schemas/food-catalog-entry.schema.json` — transport validation only for `food-catalog.example.json`.
  As with the Taiwan supplement schemas, the Kotlin validator remains authoritative for admission;
  this JSON Schema only checks shape and is not wired into `scripts/validate_*.py` by this lane (out
  of this lane's path lease — recorded as a follow-up dependency, not silently done).

No fixture in this directory authorizes a medical claim, personalized diet/therapeutic target,
scraped dataset, or production nutrient fact.

# Nutrition domain — index

> **Status:** Draft engineering contract for Issues #46 (N1) and #47 (N2).
> This document does not admit a real food-composition source, a clinical/dietary rule, a
> personalized target, or a production nutrient fact.

This directory documents the `nutrition`/`mealplan` shared-domain slice. It follows the same
evidence discipline as `docs/taiwan-source-lifecycle.md` and `docs/taiwan-supplement-evidence.md`:
default-deny, immutable source lineage, deterministic code owns arithmetic, and every external gate
is recorded honestly instead of assumed.

## What this slice is

```text
shared/src/commonMain/kotlin/dev/ed3c/gymcometrue/nutrition/FoodCatalog.kt
  Canonical food identity (bilingual zh-TW/en), per-100g macro/micronutrient profile,
  serving -> gram normalization, and a default-deny FoodCatalogAdmissionValidator that
  reuses the existing dev.ed3c.gymcometrue.domain immutable-source-lifecycle types
  (ImmutableSourceArtifact / SourceFieldMapping / SourceFieldMappingValidator) instead
  of forking a second source-admission pipeline.

shared/src/commonMain/kotlin/dev/ed3c/gymcometrue/mealplan/MealPlanCompiler.kt
  Deterministic daily meal-plan/timetable compiler built on top of the existing
  dev.ed3c.gymcometrue.domain.DailyProtocolCompiler A/B (16:00 / 22:00) timetable.
  Adds food portions, nutrition arithmetic, confirmed user overrides, unresolved/
  duplicate reporting, a constraint evaluator, and an LLM_EXPLANATION_ONLY boundary.

data/nutrition-catalog/
  Synthetic bilingual sample catalog + an illustrative (non-executable) record of
  real Taiwan sources evaluated for future admission. No real dataset is vendored.

docs/nutrition/
  This index, the Taiwan source license evaluation (#46), and the meal-plan compiler
  design note (#47).
```

## Evidence lanes this slice touches

Per `AGENTS.md`'s evidence-lane discipline, this lane only advances:

1. worktree/branch/path lease — this branch, this lease.
2. local deterministic verification — `FoodCatalogTest.kt`, `MealPlanCompilerTest.kt`.

It explicitly does **not** advance, and this repository records each as `ABSENT` or
`HUMAN_ADMIT_REQUIRED`:

```text
real Taiwan food-composition source capture       ABSENT (see taiwan-source-evaluation.md)
exact dataset ID / license clause confirmation     HUMAN_ADMIT_REQUIRED
legal / rights review of any real source           HUMAN_ADMIT_REQUIRED
qualified/clinical review of any nutrition target  HUMAN_ADMIT_REQUIRED
food photo/media rights                            ABSENT (out of scope; see below)
scripts/validate_nutrition_catalog.py wiring       ABSENT (scripts/ is out of this lane's lease)
device reminder/notification delivery              ABSENT (separate system; see meal-plan-compiler.md)
```

## Hard limits this slice enforces in code, not just prose

- **No invented nutrient values.** `FoodCatalogAdmissionValidator` rejects any non-synthetic entry
  that lacks an admitted `SourceFieldMapping`; a synthetic entry can never reach `ADMITTED` in
  production (`FoodCatalogTest.syntheticEntryWithoutMappingIsTestOnlyNeverAdmitted`).
- **Generic unit conversion stays mcg/mg/g only.** `FoodPortionScaler` reuses
  `dev.ed3c.gymcometrue.domain.MassUnitConverter`; IU and unrecognized micronutrient units are
  dropped from arithmetic rather than guessed
  (`FoodCatalogTest.portionScalerScalesMacrosLinearlyAndDropsNonMassMicronutrients`).
- **No schedule mutation from unconfirmed output.** `MealSlotOverride.confirmedByUser == false` is
  always rejected, never partially merged
  (`MealPlanCompilerTest.unconfirmedOverrideIsRejectedAndNeverMutatesTheSchedule`).
- **No disease-treatment diet / no target presented as medically appropriate.**
  `MealPlanConstraintEvaluator` only reports arithmetic satisfiability against a caller-supplied
  target and refuses to claim satisfiability while any food portion remains unresolved
  (`MealPlanCompilerTest.unresolvedPortionsBlockConstraintSatisfiabilityClaim`).
- **LLM is explanation-only.** `MealPlanExplanationBoundary` mirrors
  `dev.ed3c.gymcometrue.domain.LlmExplanationBoundary`: `mayInventNutrientFacts`,
  `mayInventMedicalRestrictions`, and `mayInventTherapeuticTargets` are always `false`.

## Separation of rights domains

Per `docs/copyright-and-data-governance.md`'s rights-domain table, **nutrition metadata rights are
separate from food-photo/media rights**. This slice defines no photo/media field on
`FoodCatalogEntry`, and food imagery is out of scope for #46/#47. Any future food-photo feature must
go through the same `MEDIA_DEFAULT_DENY` registry pattern as exercise media
(`legal/media-registry.json`), not be smuggled in through the nutrition catalog.

## Reading order

1. This file.
2. [`taiwan-source-evaluation.md`](taiwan-source-evaluation.md) — the #46 license/rights evaluation.
3. [`meal-plan-compiler.md`](meal-plan-compiler.md) — the #47 compiler design and test map.

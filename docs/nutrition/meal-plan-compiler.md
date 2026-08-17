# Deterministic daily meal-plan / timetable compiler (Issue #47)

> **Status:** Draft engineering contract. Pure deterministic domain logic; no reminder wiring, no
> catalog admission, no clinical review performed here.

## Goal

Turn user-selected foods and explicit targets into an editable daily meal timetable shared across
Android/iOS/Web, sharing the existing A/B (16:00 / 22:00) workout schedule instead of re-deriving it.

## What was reused instead of rebuilt

Per the issue's own framing ("Parent: #46 plus existing A/B protocol compiler"), the meal *timetable*
already existed: `dev.ed3c.gymcometrue.domain.DailyProtocolCompiler` already compiles an A/B
(`TrainingVariant.AFTERNOON_1600` / `NIGHT_2200`) daily protocol with `MEAL`-category
`ProtocolEvent`s, using `ProtocolTime`'s `dayOffset` field for cross-midnight ordering (see its
`b-sleep` event at `00:15 dayOffset=1`). `MealPlanCompiler` filters that existing timetable to `MEAL`
events and layers food portions and arithmetic on top — it does not define a second time table, a
second `TrainingVariant` enum, or a second cross-midnight mechanism.

```text
DailyProtocolCompiler.compile(variant)
  -> filter { category == MEAL }              (dev.ed3c.gymcometrue.domain)
  -> attach FoodPortion(s) per slot            (this lane)
  -> apply confirmed MealSlotOverride(s)       (this lane)
  -> FoodPortionScaler.scale(...) per portion  (dev.ed3c.gymcometrue.nutrition, this lane)
  -> NutrientAmounts.sumOf(...) per slot/day   (this lane)
  -> sortedBy { it.time.sortKey }              (dev.ed3c.gymcometrue.domain.ProtocolTime)
```

## Deterministic arithmetic and missing-data review states

`MealPlanCompiler.buildSlot` classifies every `FoodPortion` into exactly one of:

- **resolved** — `grams != null && profile != null && provenanceAdmitted == true`; scaled via
  `FoodPortionScaler.scale` and folded into the slot's `resolvedTotals`.
- **unresolved** — missing serving size, missing nutrient profile, or an un-admitted food record.
  Recorded as an `UnresolvedFoodPortion` with an explicit reason string; never silently dropped and
  never silently counted as zero-and-forgotten (the slot's `unresolved` list is part of its output).

Within a resolved portion, micronutrients that cannot use the generic mcg/mg/g mass conversion (IU,
unrecognized units) are excluded from that portion's `micronutrientsMg` map by
`MassUnitConverter.toMilligrams` returning `null` — the same `REVIEWED_HEALTH_RULES_ONLY` boundary
already enforced for supplements, reused rather than re-implemented.

Duplicate foods within a slot (the same `foodId` in more than one portion) are reported in
`duplicateFoodIds` and still summed individually — they are not silently merged into one line, so a
caller can decide whether that duplication was intentional (e.g. two half-portions) or a data-entry
mistake.

## User overrides and the unconfirmed-mutation hard limit

`MealSlotOverride` can retime a slot, add portions, or remove portions by ID. Every override is
resolved against one of three outcomes, recorded so nothing is silently ignored:

1. **Unknown slot id** → `RejectedOverride("Unknown meal slot id.")`. Overrides never create a new,
   unplanned slot.
2. **`confirmedByUser == false`** → `RejectedOverride("Override was not confirmed by the user.")`,
   applied **before** any mutation — this directly operationalizes the hard limit "no schedule
   mutation from unconfirmed OCR/LLM output": an unconfirmed change is rejected wholesale, never
   partially merged.
3. **Confirmed, known slot** → applied in the given order (`removePortionIds` then `addPortions`,
   then an optional `time` replacement), and the slot is marked `userOverridden = true`.

## Constraint evaluation is arithmetic, not medical advice

`MealPlanConstraintEvaluator.evaluate(plan, DailyNutritionTarget)` only compares the plan's resolved
`dailyTotals` against caller-supplied numeric bounds (`maxEnergyKcal`, `minProteinG`). It:

- reports every violated bound as a distinct string, rather than collapsing to a single boolean, so
  an "impossible constraint" (e.g. a calorie ceiling below what fixed meals already deliver) is
  visible and explained, not silently accepted or silently truncated;
- refuses to claim `satisfiable = true` while any slot still has unresolved food portions, because a
  total computed over incomplete data cannot honestly claim to satisfy or violate a target;
- never claims medical appropriateness — its output is arithmetic satisfiability against numbers the
  caller chose, matching the hard limit "no automatic calorie/macro target presented as medically
  appropriate."

## Reminder generation is a separate system

Nothing in `dev.ed3c.gymcometrue.mealplan` schedules a device notification. `androidApp`'s
`ProtocolReminder` (existing, out of this lane's lease) already consumes `ProtocolEvent`/`ProtocolTime`
from the shared domain layer independently; a future reminder feature for meal slots should read a
`DailyMealPlan`'s `slots` the same way, not be folded into the compiler itself. This keeps "what time
should this slot be" and "should the device buzz" as separate, independently testable concerns.

## LLM_EXPLANATION_ONLY boundary

`MealPlanExplanationBoundary.createPayload(plan)` mirrors
`dev.ed3c.gymcometrue.domain.LlmExplanationBoundary`: it wraps the compiled, immutable plan with
`mayInventNutrientFacts = false`, `mayInventMedicalRestrictions = false`,
`mayInventTherapeuticTargets = false`, and explicit instructions to repeat (not fill in) unresolved
and rejected-override reasons. A model may narrate the payload; the payload itself never grants
narration authority over facts it did not compute.

## Test map (`MealPlanCompilerTest.kt`)

| Acceptance requirement | Test |
|---|---|
| A/B slots follow the existing protocol compiler timetable | `aVariantSlotsFollowTheExistingProtocolCompilerTimetable` |
| Missing serving | `missingServingSizeLeavesPortionUnresolvedInsteadOfSilentlyDropped` |
| Unit mismatch | `unitMismatchExcludesOnlyThatMicronutrientButMacrosStillResolve` |
| Duplicate foods | `duplicateFoodsAreReportedAndStillSummedNotMerged` |
| Timezone/day rollover (cross-midnight) | `confirmedOverrideCanCrossMidnightAndSortsAfterTheLateSession` |
| No mutation from unconfirmed input | `unconfirmedOverrideIsRejectedAndNeverMutatesTheSchedule` |
| Unknown override target | `unknownSlotOverrideIsRejectedNotSilentlyIgnored` |
| Ordered multi-override state (add then remove) | `laterConfirmedOverrideCanRemoveAnEarlierAddedPortion` |
| Impossible constraints | `impossibleConstraintsAreReportedNotSilentlyAccepted` |
| Constraint claims require complete data | `unresolvedPortionsBlockConstraintSatisfiabilityClaim` |
| LLM explanation boundary never grants invention rights | `explanationBoundaryNeverClaimsInventionRights` |

## What remains `ABSENT`

- Wiring `DailyMealPlan` into `androidApp`'s reminder system, iOS `UserNotifications`, or a Compose
  UI screen — none of that is part of this shared-domain lane.
- Any real `FoodPortion.profile` sourced from an admitted catalog entry — see
  `taiwan-source-evaluation.md`; today every test/example profile is synthetic.
- Clinical or dietary-professional review of `DailyNutritionTarget` semantics — the evaluator is
  arithmetic only, and no reviewer attestation exists for it.

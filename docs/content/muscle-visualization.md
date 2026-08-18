# Muscle visualization (V1)

Issue #48. Parent: the #32 taxonomy.

Delivered state: **canonical muscle mapping and intensity semantics, bound to the #32 taxonomy and to
the first-party asset already in this repository; a renderer-independent draw plan; the Compose
Multiplatform renderer that consumes it; and deterministic tests over all of it.** Device snapshots
and accessibility runs on real hardware remain **NOT_EXERCISED**, and no Kotlin in this lane has been
compiled or executed here — see [README](README.md).

## Why no paid Muscle Visualizer API

The goal is a rights-clean highlight, not an anatomical illustration. Everything drawn comes from
`assets/first-party/muscle-map-schematic.svg`, which already exists in this repository with a
provenance record at `legal/provenance/muscle-map-schematic.json` recording
`anatomically_validated: false` and `diagnostic: false`. No anatomy artwork, 3D model, or vendor
asset is fetched, hotlinked, or referenced.

## The binding

`MuscleRegionMap` maps each `MuscleGroup` to a `BodyView` and the exact `id` attributes inside that
SVG:

```text
FRONT  ANTERIOR_DELTOID   muscle-front-left-shoulder, muscle-front-right-shoulder
       PECTORALIS_MAJOR   muscle-front-left-chest, muscle-front-right-chest
       BICEPS_BRACHII     muscle-front-left-arm, muscle-front-right-arm
       ABDOMINALS         muscle-front-abdominals
       OBLIQUES           muscle-front-left-oblique, muscle-front-right-oblique
       QUADRICEPS         muscle-front-left-quadriceps, muscle-front-right-quadriceps
       TIBIALIS_ANTERIOR  muscle-front-left-lower-leg, muscle-front-right-lower-leg

BACK   POSTERIOR_DELTOID  muscle-back-left-shoulder, muscle-back-right-shoulder
       TRAPEZIUS          muscle-back-trapezius
       LATISSIMUS_DORSI   muscle-back-left-lat, muscle-back-right-lat
       TRICEPS_BRACHII    muscle-back-left-triceps, muscle-back-right-triceps
       ERECTOR_SPINAE     muscle-back-erectors
       GLUTEUS_MAXIMUS    muscle-back-left-glute, muscle-back-right-glute
       HAMSTRINGS         muscle-back-left-hamstring, muscle-back-right-hamstring
       GASTROCNEMIUS      muscle-back-left-calf, muscle-back-right-calf
```

`data/exercise-catalog/validate_catalog.py` re-reads the SVG on every run and fails if any mapped id
is missing from the asset, if an id's `data-muscle` attribute disagrees with the map, or if the asset
carries a region no map claims. Both directions matter: a mapping that points at nothing draws
nothing, and an asset region nobody maps is a region that silently never lights up.

## Absent regions are reported, not dropped

Five muscles in the taxonomy have no region in the v1 asset:

```text
ADDUCTORS  FOREARM_FLEXORS  HIP_FLEXORS  ROTATOR_CUFF  SERRATUS_ANTERIOR
```

They stay in `MuscleGroup` because exercises genuinely load them — removing them would understate the
engagement and quietly make the data wrong to fit the picture. Instead:

- `MuscleVisualizationPlan.unrenderedMuscles` lists them explicitly, so a caller can decide;
- they remain in `accessibilitySummary`, so the spoken fallback is complete even where the drawing is
  not;
- `ExerciseCatalogValidator` emits a review note naming each one.

This is the difference between "the picture is incomplete and says so" and "the picture is complete
because we deleted what it could not draw".

## Intensity semantics

```text
PRIMARY      level 3    opacity 0.90
SECONDARY    level 2    opacity 0.60
STABILIZER   level 1    opacity 0.30
```

This is an **editorial movement classification**. It is not an EMG measurement, not a percentage of
maximal voluntary contraction, and it supports no physiological, diagnostic, or clinical claim. The
same disclaimer is carried in `taxonomy.v1.json` under `intensitySemantics.claim`, and
`validate_catalog.py` fails if it is removed.

The mapping from intensity to opacity lives in `MuscleVisualizationPlanner.opacityFor`, not in a
renderer, so Android, iOS, and the web projection cannot disagree about how strongly a region is lit.

The scale is closed at both ends, and `RegionHighlight` rejects any opacity outside
`0.30..0.90` in its `init` — including on deserialization. This is the clamp that makes a *day*
safe to draw: a muscle logged in six exercises is still one `PRIMARY` region, because "more
exercises" is not "more activation" for an editorial class. Accumulating alpha would have invented a
physiological reading the data does not contain.

## The draw plan

```kotlin
MuscleVisualizationPlanner.plan(engagements, locale): MuscleVisualizationPlan
```

Renderer-independent by design. It returns the asset path, the ordered highlights, the unrendered
muscles, and the bilingual spoken summary; drawing it is the platform's job. Two behaviours are
fixed:

- **strongest wins.** A muscle listed twice resolves to its highest intensity, so duplicate input
  cannot produce two conflicting highlights for one region.
- **ordering is total.** Highlights sort by view, then muscle name, then region id, which is what
  makes a snapshot test stable rather than incidentally passing.

## From logged data to a plan

```kotlin
MuscleLogResolver.resolve(index, loggedSlugs, locale): MuscleLogResolution
```

One entry point for both surfaces: one slug is the per-exercise view, a day's slugs are the per-day
view, and they cannot disagree about aggregation because there is only one aggregation. `index` is
built from validated `ExerciseRecord`s, so raw catalog JSON has no path in.

The result is three states rather than a nullable plan, and only one of them carries a plan:

```text
NoLoggedExercises   nothing logged; the screen says so and draws nothing
UnknownExercises    a logged slug resolves to no record; NOTHING is drawn
Resolved            the plan, plus the distinct slugs it was built from
```

`UnknownExercises` fails closed on purpose. Drawing the entries that *did* resolve would show a
partial day as if it were the whole day, and the missing part is invisible precisely because it is
missing.

## The Compose renderer

`shared/src/commonMain/kotlin/dev/ed3c/gymcometrue/ui/MuscleMap.kt` draws the plan, in shared code,
for all three platforms. It decides colours and layout and nothing else: region choice comes from
`MuscleRegionMap`, alpha comes from the plan's own `opacity`, and every sentence comes from
`ProductCopy` so the banned-vocabulary scan in `UserFacingLanguageTest` can see it.

Compose has no SVG parser in `commonMain`, and this lane will not add one or bundle a second
drawing. So `MuscleSchematic` carries each region's bounding box **normalized out of the asset
itself** — the same `id`s, expressed as a fraction of the silhouette outline — plus the silhouette
aspect ratio and head. `validate_catalog.py` recomputes every one of those boxes from
`assets/first-party/muscle-map-schematic.svg` on each run and fails past `1e-3`. Without that check
the renderer would be a second, unadmitted drawing that happens to share region names: it would pass
every other gate while lighting the wrong part of the body.

The screen renders it as information, never as advice. The panel states that the shading is an
editorial movement classification rather than a measurement or a recommendation, lists the muscles
the schematic cannot draw instead of hiding them, and carries the plan's spoken summary as both
visible text and the canvas `contentDescription`.

### The demo day it renders

This repository has no exercise-log store yet. `SampleTrainingLog` therefore supplies the demo day —
and it is not invented UI content: its three exercises and their engagement are
`data/seed/first-party-demo-exercises.json`, the only exercise data here whose rights record
(`legal/provenance/first-party-demo-exercises.json`) covers demo display.
`validate_catalog.py` fails if the Kotlin table and that seed disagree, or if a demo day logs a slug
that resolves to nothing. Replacing the demo with a real store replaces those two members and
nothing else.

## The demo seed speaks the canonical vocabulary now

`data/seed/first-party-demo-exercises.json` predated the #32 taxonomy and used private tokens.
Reconciled in this lane:

| Was | Now | Why |
|---|---|---|
| `WALL` (wall push-up) | `BODYWEIGHT` | `EquipmentClass` has no `WALL` member. Adding one is a taxonomy change, not a seed change; the wall is described in the record's own steps |
| `EXERCISE_MAT_OPTIONAL` | `FLOOR_MAT` | The canonical member for the same thing |
| `"intensity": 3 / 2 / 1` | `PRIMARY` / `SECONDARY` / `STABILIZER` | The numeric scale was a private mirror of `ActivationIntensity.level` |

`check_seed` then holds the seed to the same rules as the catalog: closed vocabulary, `gct-<slug>`
ids, no media, a provenance record that exists, and no muscle shaded that the record itself does not
list or shaded `PRIMARY` without being a primary muscle.

## What is not delivered

| Item | State | Note |
|---|---|---|
| Compose Multiplatform rendering | `DRAFT` | Written in `commonMain` and never compiled or run here; `STATIC`-grade until a serial integrator executes the build |
| Rendering on a real device or browser | `NOT_EXERCISED` | Needs a platform build this lane may not run |
| Cross-platform visual regression / snapshot tests | `NOT_EXERCISED` | Needs a Compose test host; the geometry is checked against the asset instead |
| Accessibility checks on a device | `NOT_EXERCISED` | `contentDescription` is set from the plan and asserted in a unit test only; no screen reader has read it |
| Front/back view toggle UI | `NOT_IMPLEMENTED` | Both views render side by side; `BodyView` is on every highlight, so a toggle stays a filter |
| Real exercise-log store | `ABSENT` | No logging store exists in this repository; the screen projects `SampleTrainingLog` through the same resolver |
| Asset hash enforced at build time | `PARTIAL` | The digest is recorded and checked by `validate_catalog.py`; `legal/provenance/muscle-map-schematic.json` still holds a placeholder, and that file is outside this lane's lease |
| Takedown path for the asset | `PARTIAL` | The mechanism exists in `MediaAdmission.kt` and the schematic has a revocation key in the fixture; no drill has been run |

The Kotlin here has not been compiled or executed in this lane — see the verification note in
[README](README.md). A green `validate_catalog.py` says the artifacts agree with each other; it says
nothing about whether the Kotlin builds.

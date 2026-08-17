# Muscle visualization (V1)

Issue #48. Parent: the #32 taxonomy.

Delivered state: **canonical muscle mapping and intensity semantics, bound to the #32 taxonomy and to
the first-party asset already in this repository, with a renderer-independent draw plan and
deterministic tests.** Compose Multiplatform rendering, device snapshots, and accessibility runs on
real hardware are **NOT_EXERCISED**.

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

## What is not delivered

| Item | State | Note |
|---|---|---|
| Compose Multiplatform rendering | `NOT_IMPLEMENTED` | The plan is the seam a renderer consumes; no `@Composable` was added |
| Web fallback rendering | `NOT_IMPLEMENTED` | Would consume the same plan, which is the point of the seam |
| Cross-platform visual regression / snapshot tests | `NOT_EXERCISED` | Needs a renderer first |
| Accessibility checks on a device | `NOT_EXERCISED` | The generated label is asserted in a unit test only |
| Front/back view toggle UI | `NOT_IMPLEMENTED` | `BodyView` is on every highlight, so the toggle is a filter |
| Asset hash enforced at build time | `PARTIAL` | The digest is recorded and checked by `validate_catalog.py`; `legal/provenance/muscle-map-schematic.json` still holds a placeholder, and that file is outside this lane's lease |
| Takedown path for the asset | `PARTIAL` | The mechanism exists in `MediaAdmission.kt` and the schematic has a revocation key in the fixture; no drill has been run |

The Kotlin here has not been compiled or executed in this lane — see the verification note in
[README](README.md).

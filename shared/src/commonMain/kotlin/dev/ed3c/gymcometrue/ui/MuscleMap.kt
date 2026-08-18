package dev.ed3c.gymcometrue.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.ed3c.gymcometrue.catalog.ActivationIntensity
import dev.ed3c.gymcometrue.catalog.BodyView
import dev.ed3c.gymcometrue.catalog.CatalogLocale
import dev.ed3c.gymcometrue.catalog.MuscleEngagement
import dev.ed3c.gymcometrue.catalog.MuscleGroup
import dev.ed3c.gymcometrue.catalog.MuscleLabels
import dev.ed3c.gymcometrue.catalog.MuscleLogResolution
import dev.ed3c.gymcometrue.catalog.MuscleLogResolver
import dev.ed3c.gymcometrue.catalog.MuscleSchematic
import dev.ed3c.gymcometrue.catalog.MuscleVisualizationPlan
import dev.ed3c.gymcometrue.catalog.MuscleVisualizationPlanner
import dev.ed3c.gymcometrue.catalog.RegionHighlight
import dev.ed3c.gymcometrue.catalog.SchematicBox
import dev.ed3c.gymcometrue.domain.TrainingVariant

/** Unlit muscle and silhouette fill. A region at rest is visible but plainly not highlighted. */
internal val Silhouette = Color(0xFF303B4C)

private val FigureWidth = 132.dp

/** 132 / 0.5077 rounded to whole dp: the frame keeps [MuscleSchematic.ASPECT_RATIO]. */
private val FigureHeight = 260.dp

/**
 * The muscle map (Issue #48 rendering).
 *
 * Everything drawn comes from [MuscleVisualizationPlanner]'s plan and [MuscleSchematic]'s
 * normalized geometry, so Android, iOS, and the web projection light the same regions at the same
 * strength; this file decides colours and layout and nothing else. It renders information about
 * what the user logged. It renders no verdict, no score, and no recommendation, and every string
 * comes from [ProductCopy] so the banned-vocabulary test sees all of them.
 */
@Composable
fun MuscleActivationPanel(resolution: MuscleLogResolution, localeTag: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(ProductCopy.muscleHeading.forLocale(localeTag), fontWeight = FontWeight.Bold)
            Text(
                ProductCopy.muscleInformationNote.forLocale(localeTag),
                color = Muted,
                style = MaterialTheme.typography.bodySmall,
            )
            when (resolution) {
                MuscleLogResolution.NoLoggedExercises -> Text(
                    ProductCopy.muscleLogEmpty.forLocale(localeTag),
                    color = Muted,
                    style = MaterialTheme.typography.bodyMedium,
                )

                is MuscleLogResolution.UnknownExercises -> {
                    Text(
                        ProductCopy.muscleLogUnresolved.forLocale(localeTag),
                        color = Warning,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        resolution.slugs.joinToString(", "),
                        color = Warning,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }

                is MuscleLogResolution.Resolved -> LoggedMuscleBody(
                    plan = resolution.plan,
                    localeTag = localeTag,
                )
            }
            Text(
                ProductCopy.muscleNote.forLocale(localeTag),
                color = Muted,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun LoggedMuscleBody(plan: MuscleVisualizationPlan, localeTag: String) {
    val locale = catalogLocale(localeTag)
    val byView = plan.highlights.groupBy { it.view }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Top,
    ) {
        BodyView.entries.forEach { view ->
            SchematicFigure(
                view = view,
                highlights = byView[view].orEmpty(),
                label = ProductCopy.muscleViewLabel(view).forLocale(localeTag),
                description = plan.accessibilitySummary,
            )
        }
    }
    IntensityLegend(localeTag = localeTag)
    Text(
        plan.accessibilitySummary,
        color = TextPrimary,
        style = MaterialTheme.typography.bodySmall,
    )
    if (plan.unrenderedMuscles.isNotEmpty()) {
        // The picture is incomplete and says so, rather than being complete because the data that
        // did not fit was dropped.
        Text(
            ProductCopy.muscleUnrenderedNote.forLocale(localeTag) +
                plan.unrenderedMuscles.joinToString(muscleSeparator(locale)) {
                    MuscleLabels.label(it, locale)
                },
            color = Muted,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

/**
 * One body view. The alpha of every region is the plan's own `opacity`; there is no local scale,
 * no per-platform curve, and no accumulation across exercises.
 */
@Composable
private fun SchematicFigure(
    view: BodyView,
    highlights: List<RegionHighlight>,
    label: String,
    description: String,
) {
    val opacityByRegion: Map<String, Float> =
        highlights.associate { it.regionId to it.opacity.toFloat() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Canvas(
            modifier = Modifier
                .size(width = FigureWidth, height = FigureHeight)
                .semantics { contentDescription = description },
        ) {
            fill(MuscleSchematic.headBox, Silhouette, oval = true)
            MuscleSchematic.regionIdsFor(view).forEach { regionId ->
                val box = MuscleSchematic.boxFor(regionId) ?: return@forEach
                val opacity = opacityByRegion[regionId]
                fill(box, if (opacity == null) Silhouette else Lime.copy(alpha = opacity))
            }
        }
        Text(label, color = Muted, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun IntensityLegend(localeTag: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ActivationIntensity.entries.sortedByDescending { it.level }.forEach { intensity ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            color = Lime.copy(
                                alpha = MuscleVisualizationPlanner.opacityFor(intensity).toFloat(),
                            ),
                            shape = RoundedCornerShape(3.dp),
                        ),
                )
                Text(
                    ProductCopy.muscleIntensityLabels.getValue(intensity).forLocale(localeTag),
                    color = Muted,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

/** Scales one normalized box onto the canvas. The only place schematic units become pixels. */
private fun DrawScope.fill(box: SchematicBox, color: Color, oval: Boolean = false) {
    val topLeft = Offset(
        x = (box.left * size.width).toFloat(),
        y = (box.top * size.height).toFloat(),
    )
    val extent = Size(
        width = (box.width * size.width).toFloat(),
        height = (box.height * size.height).toFloat(),
    )
    if (oval) {
        drawOval(color = color, topLeft = topLeft, size = extent)
    } else {
        drawRoundRect(
            color = color,
            topLeft = topLeft,
            size = extent,
            cornerRadius = CornerRadius(12f, 12f),
        )
    }
}

private fun muscleSeparator(locale: CatalogLocale): String =
    if (locale == CatalogLocale.EN) ", " else "、"

/** The app's `zh-TW` / `en` UI tag mapped onto the catalog's BCP 47 vocabulary. */
fun catalogLocale(localeTag: String): CatalogLocale =
    if (localeTag.startsWith("zh")) CatalogLocale.ZH_HANT_TW else CatalogLocale.EN

/**
 * The demo day the shipped screen shows.
 *
 * This repository has no exercise-log store yet, so the screen needs some logged data to project.
 * These three exercises and their engagement are the first-party demo records in
 * `data/seed/first-party-demo-exercises.json` — the only exercise data in this repository with a
 * rights record (`legal/provenance/first-party-demo-exercises.json`) that covers demo display.
 * `data/exercise-catalog/validate_catalog.py` fails if this table and that seed disagree, so the
 * screen cannot quietly drift onto data nobody authorized.
 *
 * It goes through [MuscleLogResolver] exactly like a real log would: swapping the store for a real
 * one replaces [engagementBySlug] and [loggedSlugs] and nothing else.
 */
object SampleTrainingLog {
    val engagementBySlug: Map<String, List<MuscleEngagement>> = mapOf(
        "bodyweight-squat" to listOf(
            MuscleEngagement(MuscleGroup.QUADRICEPS, ActivationIntensity.PRIMARY),
            MuscleEngagement(MuscleGroup.GLUTEUS_MAXIMUS, ActivationIntensity.PRIMARY),
            MuscleEngagement(MuscleGroup.HAMSTRINGS, ActivationIntensity.STABILIZER),
            MuscleEngagement(MuscleGroup.ABDOMINALS, ActivationIntensity.STABILIZER),
        ),
        "wall-push-up" to listOf(
            MuscleEngagement(MuscleGroup.PECTORALIS_MAJOR, ActivationIntensity.PRIMARY),
            MuscleEngagement(MuscleGroup.TRICEPS_BRACHII, ActivationIntensity.SECONDARY),
            MuscleEngagement(MuscleGroup.ANTERIOR_DELTOID, ActivationIntensity.SECONDARY),
            MuscleEngagement(MuscleGroup.SERRATUS_ANTERIOR, ActivationIntensity.STABILIZER),
        ),
        "glute-bridge" to listOf(
            MuscleEngagement(MuscleGroup.GLUTEUS_MAXIMUS, ActivationIntensity.PRIMARY),
            MuscleEngagement(MuscleGroup.HAMSTRINGS, ActivationIntensity.SECONDARY),
            MuscleEngagement(MuscleGroup.ABDOMINALS, ActivationIntensity.STABILIZER),
        ),
    )

    /** What the demo day contains per training slot. A slug typo here fails closed on screen. */
    fun loggedSlugs(variant: TrainingVariant): List<String> = when (variant) {
        TrainingVariant.AFTERNOON_1600 -> listOf("wall-push-up", "bodyweight-squat")
        TrainingVariant.NIGHT_2200 -> listOf("glute-bridge")
    }

    fun resolve(variant: TrainingVariant, localeTag: String): MuscleLogResolution =
        MuscleLogResolver.resolve(
            index = engagementBySlug,
            loggedSlugs = loggedSlugs(variant),
            locale = catalogLocale(localeTag),
        )
}

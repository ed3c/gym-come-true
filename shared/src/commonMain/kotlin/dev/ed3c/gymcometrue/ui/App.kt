package dev.ed3c.gymcometrue.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.ed3c.gymcometrue.domain.DailyProtocolCompiler
import dev.ed3c.gymcometrue.domain.MuscleActivation
import dev.ed3c.gymcometrue.domain.ProtocolCategory
import dev.ed3c.gymcometrue.domain.ProtocolEvent
import dev.ed3c.gymcometrue.domain.SafetyContext
import dev.ed3c.gymcometrue.domain.SafetyDecision
import dev.ed3c.gymcometrue.domain.ScanEvidence
import dev.ed3c.gymcometrue.domain.SupplementSafetyEngine
import dev.ed3c.gymcometrue.domain.TrainingVariant
import dev.ed3c.gymcometrue.explanation.ExplanationPlan

internal val Ink = Color(0xFF0A0D12)
internal val Panel = Color(0xFF131923)
internal val PanelRaised = Color(0xFF1A2230)
internal val Lime = Color(0xFFA8FF60)
internal val Cyan = Color(0xFF75E6FF)
internal val Warning = Color(0xFFFFC45D)
internal val Muted = Color(0xFF98A2B3)
internal val TextPrimary = Color(0xFFF5F7FA)

/**
 * [aiAccess] is resolved by the host from the persisted acknowledgement log
 * ([FirstRunAcknowledgement.resolve]); the session flag below only carries the tap that happened
 * on this screen, so an unwritten log cannot look acknowledged on the next launch.
 */
@Composable
fun GymComeTrueApp(
    platformName: String,
    scanSummary: String? = null,
    onScanLabel: (() -> Unit)? = null,
    onScheduleNextReminder: (() -> Unit)? = null,
    localeTag: String = "zh-TW",
    aiAccess: AiAccess = AiAccess.UNACKNOWLEDGED,
    explanationPlan: ExplanationPlan? = null,
    onAcknowledgeNotice: () -> Unit = {},
) {
    var variant by remember { mutableStateOf(TrainingVariant.AFTERNOON_1600) }
    var acknowledgedInSession by remember { mutableStateOf(false) }
    val events = remember(variant) { DailyProtocolCompiler.compile(variant) }
    val safety = remember {
        SupplementSafetyEngine.evaluate(
            evidence = ScanEvidence(
                rawTextSha256 = "not-scanned",
                candidates = emptyList(),
            ),
            context = SafetyContext(),
        )
    }
    val effectiveAccess = if (acknowledgedInSession) AiAccess.ACKNOWLEDGED else aiAccess

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Ink,
            contentColor = TextPrimary,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Header(platformName = platformName, localeTag = localeTag)
                LoggedDataPanel(decision = safety.decision, localeTag = localeTag)
                AiFeatureSection(
                    access = effectiveAccess,
                    plan = explanationPlan,
                    localeTag = localeTag,
                    onAcknowledge = {
                        acknowledgedInSession = true
                        onAcknowledgeNotice()
                    },
                )
                ActionPanel(
                    scanSummary = scanSummary,
                    localeTag = localeTag,
                    onScanLabel = onScanLabel,
                    onScheduleNextReminder = onScheduleNextReminder,
                )
                VariantSelector(
                    selected = variant,
                    localeTag = localeTag,
                    onSelect = { variant = it },
                )
                MusclePanel(
                    localeTag = localeTag,
                    activations = if (variant == TrainingVariant.AFTERNOON_1600) {
                        listOf(
                            MuscleActivation("chest", 8),
                            MuscleActivation("triceps", 6),
                            MuscleActivation("core", 5),
                        )
                    } else {
                        listOf(
                            MuscleActivation("back", 7),
                            MuscleActivation("glutes", 6),
                            MuscleActivation("legs", 5),
                        )
                    },
                )
                ProtocolTimeline(events = events, localeTag = localeTag)
                PositioningNotice(localeTag = localeTag)
                Spacer(Modifier.height(18.dp))
            }
        }
    }
}

@Composable
private fun Header(platformName: String, localeTag: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "GYM COME TRUE",
            style = MaterialTheme.typography.labelLarge,
            color = Lime,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = ProductCopy.positioning.forLocale(localeTag),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "$platformName · local-first",
            color = Muted,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/**
 * The deterministic engine still returns a [SafetyDecision]; this panel is the only place it turns
 * into something a person reads, and it reads as information about their own log rather than as a
 * verdict about their body.
 */
@Composable
private fun LoggedDataPanel(decision: SafetyDecision, localeTag: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PanelRaised),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(ProductCopy.loggedDataHeading.forLocale(localeTag), fontWeight = FontWeight.Bold)
            Text(
                ProductCopy.informationFor(decision).forLocale(localeTag),
                color = Muted,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                ProductCopy.aiBoundary.forLocale(localeTag),
                color = Warning,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun ActionPanel(
    scanSummary: String?,
    localeTag: String,
    onScanLabel: (() -> Unit)?,
    onScheduleNextReminder: (() -> Unit)?,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(ProductCopy.ocrAssistHeading.forLocale(localeTag), fontWeight = FontWeight.Bold)
            Text(
                scanSummary ?: ProductCopy.ocrAssistIdle.forLocale(localeTag),
                color = Muted,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                ProductCopy.ocrAssist.forLocale(localeTag),
                color = Warning,
                style = MaterialTheme.typography.bodySmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { onScanLabel?.invoke() },
                    enabled = onScanLabel != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Lime,
                        contentColor = Ink,
                    ),
                ) {
                    Text(ProductCopy.scanAction.forLocale(localeTag), fontWeight = FontWeight.Bold)
                }
                TextButton(
                    onClick = { onScheduleNextReminder?.invoke() },
                    enabled = onScheduleNextReminder != null,
                ) {
                    Text(ProductCopy.reminderAction.forLocale(localeTag), color = Cyan)
                }
            }
        }
    }
}

@Composable
private fun VariantSelector(
    selected: TrainingVariant,
    localeTag: String,
    onSelect: (TrainingVariant) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                ProductCopy.timelineHeadingVariants.forLocale(localeTag),
                fontWeight = FontWeight.Bold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VariantButton(
                    label = "A · 16:00",
                    selected = selected == TrainingVariant.AFTERNOON_1600,
                    onClick = { onSelect(TrainingVariant.AFTERNOON_1600) },
                )
                VariantButton(
                    label = "B · 22:00",
                    selected = selected == TrainingVariant.NIGHT_2200,
                    onClick = { onSelect(TrainingVariant.NIGHT_2200) },
                )
            }
            Text(
                ProductCopy.timelineVariantNote.forLocale(localeTag),
                color = Muted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun VariantButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Cyan else PanelRaised,
            contentColor = if (selected) Ink else TextPrimary,
        ),
    ) {
        Text(label, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MusclePanel(localeTag: String, activations: List<MuscleActivation>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(ProductCopy.muscleHeading.forLocale(localeTag), fontWeight = FontWeight.Bold)
            Text(
                ProductCopy.muscleNote.forLocale(localeTag),
                color = Muted,
                style = MaterialTheme.typography.bodySmall,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MuscleFigure(activations)
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    activations.forEach { activation ->
                        Text(
                            "${activation.muscle.uppercase()}  ${activation.intensity}/10",
                            color = activationColor(activation.intensity),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MuscleFigure(activations: List<MuscleActivation>) {
    val chest = activations.intensityOf("chest")
    val back = activations.intensityOf("back")
    val arms = maxOf(activations.intensityOf("triceps"), activations.intensityOf("arms"))
    val core = activations.intensityOf("core")
    val lower = maxOf(activations.intensityOf("glutes"), activations.intensityOf("legs"))

    Canvas(modifier = Modifier.size(width = 130.dp, height = 240.dp)) {
        val center = size.width / 2f
        drawCircle(
            color = Color(0xFF303B4C),
            radius = size.width * 0.13f,
            center = Offset(center, size.height * 0.10f),
        )
        bodyPart(
            color = activationColor(maxOf(chest, back)),
            topLeft = Offset(center - size.width * 0.20f, size.height * 0.22f),
            partSize = Size(size.width * 0.40f, size.height * 0.25f),
        )
        bodyPart(
            color = activationColor(core),
            topLeft = Offset(center - size.width * 0.14f, size.height * 0.43f),
            partSize = Size(size.width * 0.28f, size.height * 0.18f),
        )
        bodyPart(
            color = activationColor(arms),
            topLeft = Offset(center - size.width * 0.38f, size.height * 0.25f),
            partSize = Size(size.width * 0.13f, size.height * 0.35f),
        )
        bodyPart(
            color = activationColor(arms),
            topLeft = Offset(center + size.width * 0.25f, size.height * 0.25f),
            partSize = Size(size.width * 0.13f, size.height * 0.35f),
        )
        bodyPart(
            color = activationColor(lower),
            topLeft = Offset(center - size.width * 0.17f, size.height * 0.60f),
            partSize = Size(size.width * 0.14f, size.height * 0.34f),
        )
        bodyPart(
            color = activationColor(lower),
            topLeft = Offset(center + size.width * 0.03f, size.height * 0.60f),
            partSize = Size(size.width * 0.14f, size.height * 0.34f),
        )
    }
}

private fun DrawScope.bodyPart(color: Color, topLeft: Offset, partSize: Size) {
    drawRoundRect(
        color = color,
        topLeft = topLeft,
        size = partSize,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f, 24f),
    )
}

private fun List<MuscleActivation>.intensityOf(name: String): Int =
    firstOrNull { it.muscle.equals(name, ignoreCase = true) }?.intensity ?: 0

private fun activationColor(intensity: Int): Color = when {
    intensity >= 8 -> Lime
    intensity >= 5 -> Cyan
    intensity > 0 -> Warning
    else -> Color(0xFF303B4C)
}

@Composable
private fun ProtocolTimeline(events: List<ProtocolEvent>, localeTag: String) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            ProductCopy.timelineHeading.forLocale(localeTag),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        events.forEach { event -> ProtocolCard(event = event, localeTag = localeTag) }
    }
}

@Composable
private fun ProtocolCard(event: ProtocolEvent, localeTag: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .background(categoryColor(event.category).copy(alpha = 0.16f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 9.dp, vertical = 7.dp),
            ) {
                Text(
                    event.time.display(),
                    color = categoryColor(event.category),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(event.title, fontWeight = FontWeight.Bold)
                Text(event.note, color = Muted, style = MaterialTheme.typography.bodySmall)
                if (event.requiresConfirmation) {
                    Text(
                        ProductCopy.confirmationRequired.forLocale(localeTag),
                        color = Warning,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

private fun categoryColor(category: ProtocolCategory): Color = when (category) {
    ProtocolCategory.MEAL -> Lime
    ProtocolCategory.SUPPLEMENT_CHECKPOINT -> Warning
    ProtocolCategory.TRAINING -> Cyan
    ProtocolCategory.HYDRATION -> Color(0xFF8EBBFF)
    ProtocolCategory.RECOVERY -> Color(0xFFC2A8FF)
    ProtocolCategory.SLEEP -> Color(0xFFFFA8D3)
}

@Composable
private fun PositioningNotice(localeTag: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF201A12)),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                ProductCopy.positioning.forLocale(localeTag),
                color = Warning,
                fontWeight = FontWeight.Bold,
            )
            Text(
                ProductCopy.aiResponseNotice.forLocale(localeTag),
                color = TextPrimary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

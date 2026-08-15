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
import dev.ed3c.gymcometrue.domain.ScanEvidence
import dev.ed3c.gymcometrue.domain.SupplementSafetyEngine
import dev.ed3c.gymcometrue.domain.TrainingVariant

private val Ink = Color(0xFF0A0D12)
private val Panel = Color(0xFF131923)
private val PanelRaised = Color(0xFF1A2230)
private val Lime = Color(0xFFA8FF60)
private val Cyan = Color(0xFF75E6FF)
private val Warning = Color(0xFFFFC45D)
private val Muted = Color(0xFF98A2B3)
private val TextPrimary = Color(0xFFF5F7FA)

@Composable
fun GymComeTrueApp(
    platformName: String,
    scanSummary: String? = null,
    onScanLabel: (() -> Unit)? = null,
    onScheduleNextReminder: (() -> Unit)? = null,
) {
    var variant by remember { mutableStateOf(TrainingVariant.AFTERNOON_1600) }
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
                Header(platformName = platformName)
                SafetyGate(
                    decision = safety.decision.name,
                    reason = safety.reasons.firstOrNull()
                        ?: "Only reviewed evidence can enter an executable protocol.",
                )
                ActionPanel(
                    scanSummary = scanSummary,
                    onScanLabel = onScanLabel,
                    onScheduleNextReminder = onScheduleNextReminder,
                )
                VariantSelector(
                    selected = variant,
                    onSelect = { variant = it },
                )
                MusclePanel(
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
                ProtocolTimeline(events)
                EvidenceNotice()
                Spacer(Modifier.height(18.dp))
            }
        }
    }
}

@Composable
private fun Header(platformName: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "GYM COME TRUE",
            style = MaterialTheme.typography.labelLarge,
            color = Lime,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Your protocol, with evidence attached.",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "$platformName foundation · local-first · proof before advice",
            color = Muted,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun SafetyGate(decision: String, reason: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PanelRaised),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Safety gate", fontWeight = FontWeight.Bold)
                StatusPill(decision)
            }
            Text(reason, color = Muted, style = MaterialTheme.typography.bodyMedium)
            Text(
                "The LLM cannot clear this gate or calculate a supplement dose.",
                color = Warning,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun StatusPill(text: String) {
    Box(
        modifier = Modifier
            .background(Warning.copy(alpha = 0.16f), RoundedCornerShape(100.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(text, color = Warning, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ActionPanel(
    scanSummary: String?,
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
            Text("Evidence capture", fontWeight = FontWeight.Bold)
            Text(
                scanSummary ?: "No label has been scanned. Device OCR creates a candidate that must be confirmed.",
                color = Muted,
                style = MaterialTheme.typography.bodyMedium,
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
                    Text("Scan label", fontWeight = FontWeight.Bold)
                }
                TextButton(
                    onClick = { onScheduleNextReminder?.invoke() },
                    enabled = onScheduleNextReminder != null,
                ) {
                    Text("Test reminder", color = Cyan)
                }
            }
        }
    }
}

@Composable
private fun VariantSelector(
    selected: TrainingVariant,
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
            Text("Training-day compiler", fontWeight = FontWeight.Bold)
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
                "Late-plan events preserve next-day ordering instead of placing 00:15 before 22:00.",
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
private fun MusclePanel(activations: List<MuscleActivation>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Local muscle view", fontWeight = FontWeight.Bold)
            Text(
                "Generated from simple Compose geometry. No third-party anatomy illustration is bundled.",
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
private fun ProtocolTimeline(events: List<ProtocolEvent>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "Today's protocol",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        events.forEach { event -> ProtocolCard(event) }
    }
}

@Composable
private fun ProtocolCard(event: ProtocolEvent) {
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
                    Text("CONFIRMATION REQUIRED", color = Warning, style = MaterialTheme.typography.labelSmall)
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
private fun EvidenceNotice() {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF201A12)),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("Not medical advice", color = Warning, fontWeight = FontWeight.Bold)
            Text(
                "Use this foundation to capture and organize evidence. A qualified professional must assess medication interactions, symptoms, pregnancy, procedures, and supplement dosing.",
                color = TextPrimary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

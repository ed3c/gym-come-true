package dev.ed3c.gymcometrue.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.ed3c.gymcometrue.explanation.AdmittedExplanationTemplates
import dev.ed3c.gymcometrue.explanation.ExplanationPlan
import dev.ed3c.gymcometrue.privacy.ConsentEvent
import dev.ed3c.gymcometrue.privacy.ConsentHistoryResolver
import dev.ed3c.gymcometrue.privacy.ConsentState
import dev.ed3c.gymcometrue.privacy.ProcessingPurpose

/**
 * Whether an AI surface may be rendered at all. Every state is produced by
 * [FirstRunAcknowledgement.resolve]; none exists only for tests.
 */
enum class AiAccess {
    /** No current acknowledgement of the risk notice. AI surfaces are not reachable. */
    UNACKNOWLEDGED,

    /** The acknowledgement log itself is broken, so acknowledgement cannot be proven. */
    HISTORY_UNRESOLVED,

    ACKNOWLEDGED,
}

/**
 * First-run acknowledgement of the `legal/DISCLAIMER.md` risk notice (Issue #50).
 *
 * There is no new store: the acknowledgement is written into the append-only consent log that
 * `dev.ed3c.gymcometrue.privacy` already owns, so withdrawal, ordering and a lost record behave the
 * way every other consent record behaves. A gap in the log is not "still acknowledged"; it is
 * [AiAccess.HISTORY_UNRESOLVED].
 *
 * Writing that log to disk belongs to the host application; shared code holds the event shape and
 * the resolution, and has no filesystem.
 */
object FirstRunAcknowledgement {
    /** Bumping this after the notice wording changes forces a fresh acknowledgement. */
    const val POLICY_VERSION: String = "disclaimer-2026-08-18"

    const val EVIDENCE_REF: String = "legal/DISCLAIMER.md#first-run-acknowledgement"

    fun event(
        sequence: Int,
        occurredAtIsoDate: String,
        granted: Boolean = true,
    ): ConsentEvent = ConsentEvent(
        sequence = sequence,
        purpose = ProcessingPurpose.SERVICE_DELIVERY,
        granted = granted,
        occurredAtIsoDate = occurredAtIsoDate,
        policyVersion = POLICY_VERSION,
        evidenceRef = EVIDENCE_REF,
    )

    fun resolve(history: List<ConsentEvent>, asOfIsoDate: String): AiAccess {
        val resolved = ConsentHistoryResolver.resolve(history, asOfIsoDate)
        if (resolved.blockers.isNotEmpty()) return AiAccess.HISTORY_UNRESOLVED
        if (resolved.states[ProcessingPurpose.SERVICE_DELIVERY] != ConsentState.GRANTED) {
            return AiAccess.UNACKNOWLEDGED
        }
        // Blocker-free history is ordered and fully in the past, so the highest sequence is the
        // event the resolver actually applied.
        val latest = history
            .filter { it.purpose == ProcessingPurpose.SERVICE_DELIVERY }
            .maxByOrNull { it.sequence }
        return if (latest?.policyVersion == POLICY_VERSION) {
            AiAccess.ACKNOWLEDGED
        } else {
            AiAccess.UNACKNOWLEDGED
        }
    }
}

/**
 * A renderable AI response. The mandatory notice is not a constructor parameter but a property
 * derived from the disclaimer SSOT, so an AI response without the notice has no representation —
 * the test for "the notice is on every AI surface" cannot be defeated by a new call site.
 */
class AiResponseView internal constructor(
    val localeTag: String,
    val bodyLines: List<String>,
) {
    val notice: String get() = ProductCopy.aiResponseNotice.forLocale(localeTag)
}

object AiResponsePresenter {
    /**
     * The only way to build an [AiResponseView].
     *
     * Returns `null` — nothing to render — when the user has not acknowledged the notice, when
     * there is no plan, or when the plan names a template this repository has no reviewed wording
     * for. An unknown template id fails closed rather than reaching a screen as a raw identifier.
     */
    fun present(access: AiAccess, plan: ExplanationPlan?): AiResponseView? {
        if (access != AiAccess.ACKNOWLEDGED || plan == null) return null
        val body = plan.templateIds
            .filter { it != AdmittedExplanationTemplates.DISCLAIMER_TEMPLATE }
            .map { ProductCopy.templateCopy[it] ?: return null }
            .map { it.forLocale(plan.localeTag) }
        return AiResponseView(localeTag = plan.localeTag, bodyLines = body)
    }
}

/**
 * The AI entry point. Until the notice is acknowledged this renders the acknowledgement and
 * nothing else, which is what "acknowledgement gates AI features" means on screen.
 */
@Composable
fun AiFeatureSection(
    access: AiAccess,
    plan: ExplanationPlan?,
    localeTag: String,
    onAcknowledge: () -> Unit,
) {
    val view = AiResponsePresenter.present(access, plan)
    Card(
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(ProductCopy.aiSectionHeading.forLocale(localeTag), fontWeight = FontWeight.Bold)
            if (access != AiAccess.ACKNOWLEDGED) {
                FirstRunAcknowledgementBody(localeTag = localeTag, onAcknowledge = onAcknowledge)
            } else {
                Text(
                    ProductCopy.aiBoundary.forLocale(localeTag),
                    color = Muted,
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (view == null) {
                    Text(
                        ProductCopy.aiResponseAbsent.forLocale(localeTag),
                        color = Muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    view.bodyLines.forEach { line ->
                        Text(line, style = MaterialTheme.typography.bodyMedium)
                    }
                    AiResponseNotice(view)
                }
            }
        }
    }
}

/**
 * The reusable notice component. It takes an [AiResponseView] rather than a string so that a
 * surface cannot embed a notice it authored itself.
 */
@Composable
fun AiResponseNotice(view: AiResponseView) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF201A12)),
        shape = RoundedCornerShape(14.dp),
    ) {
        Text(
            view.notice,
            modifier = Modifier.padding(14.dp),
            color = Warning,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

/** zh-Hant primary and en secondary are both shown, the order legal/DISCLAIMER.md records. */
@Composable
private fun FirstRunAcknowledgementBody(localeTag: String, onAcknowledge: () -> Unit) {
    Text(ProductCopy.firstRunHeading.forLocale(localeTag), color = Warning)
    Text(
        ProductCopy.firstRunAcknowledgement.zhHant,
        style = MaterialTheme.typography.bodyMedium,
    )
    Text(
        ProductCopy.firstRunAcknowledgement.en,
        color = Muted,
        style = MaterialTheme.typography.bodySmall,
    )
    Button(
        onClick = onAcknowledge,
        colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Ink),
    ) {
        Text(ProductCopy.firstRunAcknowledgeAction.forLocale(localeTag), fontWeight = FontWeight.Bold)
    }
}

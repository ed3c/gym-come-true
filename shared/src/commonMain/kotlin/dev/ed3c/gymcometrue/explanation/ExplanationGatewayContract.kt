package dev.ed3c.gymcometrue.explanation

import dev.ed3c.gymcometrue.domain.SafetyDecision
import dev.ed3c.gymcometrue.domain.SupplementDecisionReceipt
import kotlinx.serialization.Serializable

/**
 * Receipt-only explanation gateway contract (Issue #35).
 *
 * The gateway accepts one payload shape: a minimized projection of an immutable deterministic
 * decision receipt. Raw images, OCR text, free-form medication or symptom context, dose questions
 * and diagnosis questions have no admitted representation and are rejected at the gate.
 *
 * A model never returns prose here. It returns an [ExplanationPlan] that selects template IDs from
 * an admitted catalogue, so every user-visible sentence stays repository-authored and reviewable.
 */

private val sha256Pattern = Regex("^[0-9a-f]{64}$")

/** Identifiers that cross the boundary are opaque tokens; prose cannot ride inside one. */
private val tokenPattern = Regex("^[A-Za-z0-9][A-Za-z0-9._:-]{0,119}$")

internal fun String.isSha256(): Boolean = sha256Pattern.matches(this)

internal fun String.isToken(): Boolean = tokenPattern.matches(this)

@Serializable
enum class ExplanationReasonKey {
    NO_CONFIRMED_INGREDIENT,
    ADVERSE_SYMPTOM,
    PREGNANCY_OR_BREASTFEEDING,
    PLANNED_PROCEDURE,
    MEDICATION_CONTEXT,
    UNVERIFIED_EVIDENCE,
    UNRESOLVED_UNIT,
    NO_REVIEWED_RULE_PACK,
}

/**
 * Maps the exact deterministic reason strings emitted by
 * `dev.ed3c.gymcometrue.domain.SupplementSafetyEngine` onto enumerated keys.
 *
 * The table is intentionally exact-match: if the deterministic engine ever emits a reason this
 * table does not know, minimization fails closed instead of forwarding unreviewed free text.
 */
object DeterministicReasonKeys {
    private val byExactReason: Map<String, ExplanationReasonKey> = mapOf(
        "No ingredient-and-amount pair was confirmed from the label." to
            ExplanationReasonKey.NO_CONFIRMED_INGREDIENT,
        "Adverse symptoms require immediate human assessment; automation is blocked." to
            ExplanationReasonKey.ADVERSE_SYMPTOM,
        "Pregnancy or breastfeeding requires qualified professional review." to
            ExplanationReasonKey.PREGNANCY_OR_BREASTFEEDING,
        "A planned procedure can change supplement risk and requires professional review." to
            ExplanationReasonKey.PLANNED_PROCEDURE,
        "Recent medication use can create interactions that this generic engine cannot assess." to
            ExplanationReasonKey.MEDICATION_CONTEXT,
        "OCR and user-entered label facts remain unverified evidence." to
            ExplanationReasonKey.UNVERIFIED_EVIDENCE,
        "IU, volume, count, or unknown units cannot use a generic mass conversion." to
            ExplanationReasonKey.UNRESOLVED_UNIT,
        "No clinically reviewed regional rule pack is active." to
            ExplanationReasonKey.NO_REVIEWED_RULE_PACK,
    )

    fun keyFor(reason: String): ExplanationReasonKey? = byExactReason[reason.trim()]
}

/**
 * The only sentences the product may show. Template bodies live in reviewed localization
 * resources; this catalogue only admits their identifiers. There is deliberately no dose,
 * diagnosis, interaction or "safe amount" template, so a provider cannot request one.
 */
object AdmittedExplanationTemplates {
    const val DISCLAIMER_TEMPLATE: String = "tpl.footer.not-medical-advice"

    val decisionTemplates: Map<SafetyDecision, String> = mapOf(
        SafetyDecision.LOG_ONLY to "tpl.decision.log-only",
        SafetyDecision.REVIEW_REQUIRED to "tpl.decision.review-required",
        SafetyDecision.BLOCK_AUTOMATION to "tpl.decision.block-automation",
    )

    val reasonTemplates: Map<ExplanationReasonKey, String> = mapOf(
        ExplanationReasonKey.NO_CONFIRMED_INGREDIENT to "tpl.reason.no-confirmed-ingredient",
        ExplanationReasonKey.ADVERSE_SYMPTOM to "tpl.reason.adverse-symptom",
        ExplanationReasonKey.PREGNANCY_OR_BREASTFEEDING to "tpl.reason.pregnancy-or-breastfeeding",
        ExplanationReasonKey.PLANNED_PROCEDURE to "tpl.reason.planned-procedure",
        ExplanationReasonKey.MEDICATION_CONTEXT to "tpl.reason.medication-context",
        ExplanationReasonKey.UNVERIFIED_EVIDENCE to "tpl.reason.unverified-evidence",
        ExplanationReasonKey.UNRESOLVED_UNIT to "tpl.reason.unresolved-unit",
        ExplanationReasonKey.NO_REVIEWED_RULE_PACK to "tpl.reason.no-reviewed-rule-pack",
    )

    /** The provider's entire degree of freedom: which admitted next step to surface. */
    val optionalTemplates: Set<String> = setOf(
        "tpl.next-step.confirm-physical-label",
        "tpl.next-step.seek-qualified-review",
        "tpl.next-step.record-symptom",
    )

    val admittedLocales: Set<String> = setOf("zh-TW", "en")

    val all: Set<String> =
        decisionTemplates.values.toSet() + reasonTemplates.values.toSet() + optionalTemplates + DISCLAIMER_TEMPLATE
}

/**
 * The only payload the gateway forwards. Every field is a hash, an enum, or an opaque token:
 * no product name, no label text, no OCR candidate, no user-authored sentence.
 *
 * `receiptSha256` and `productVariantKeySha256` are supplied by the caller because shared code has
 * no hasher; shared code only verifies their shape. Content-addressing of the receipt itself is
 * therefore `ABSENT` in this module and remains a server-side responsibility.
 */
@Serializable
data class MinimizedDecisionReceipt(
    val receiptId: String,
    val receiptSha256: String,
    val productVariantKeySha256: String,
    val evidenceSha256: String,
    val rulePackId: String,
    val rulePackVersion: String,
    val rulePackContentSha256: String,
    val decision: SafetyDecision,
    val triggeredRuleIds: List<String> = emptyList(),
    val reasonKeys: List<ExplanationReasonKey> = emptyList(),
    val modelUsedForDecision: Boolean = false,
)

@Serializable
enum class ExplanationIntent {
    EXPLAIN_RECEIPT,
    DOSE_RECOMMENDATION,
    DIAGNOSIS,
    RULE_AUTHORING,
    FREE_FORM_ADVICE,
}

/** Untrusted wire shape. Fields that must never be accepted exist here only to be rejected. */
@Serializable
data class ExplanationRequestEnvelope(
    val intent: ExplanationIntent,
    val localeTag: String = "zh-TW",
    val receipt: MinimizedDecisionReceipt? = null,
    val rawImageBase64: String? = null,
    val rawOcrText: String? = null,
    val freeTextContext: String? = null,
)

@Serializable
data class GatewayCaller(
    val sessionPseudonymId: String,
    val authenticated: Boolean = false,
    val serverSide: Boolean = false,
)

@Serializable
enum class GatewayRejection {
    UNAUTHENTICATED,
    CLIENT_SIDE_EXECUTION_DENIED,
    UNSUPPORTED_INTENT,
    UNSUPPORTED_LOCALE,
    RAW_IMAGE_PRESENT,
    RAW_OCR_TEXT_PRESENT,
    FREE_TEXT_CONTEXT_PRESENT,
    MISSING_RECEIPT,
    MALFORMED_HASH,
    NON_TOKEN_FIELD,
    UNMAPPED_REASON_TEXT,
    MODEL_CLAIMED_DECISION,
    KILL_SWITCH_ENGAGED,
    PROVIDER_ABSENT,
    PROVIDER_NOT_ADMITTED,
    PROVIDER_TIMEOUT,
    PROVIDER_FAILURE,
    COST_LIMIT_EXCEEDED,
    PLAN_RECEIPT_MISMATCH,
    PLAN_DECISION_MUTATED,
    PLAN_TEMPLATE_NOT_ADMITTED,
    PLAN_INVENTED_REASON,
    PLAN_SUPPRESSED_WARNING,
    PLAN_MISSING_DISCLAIMER,
}

/**
 * A request that passed the gate. Only [ExplanationRequestGate] can build one, so a provider call
 * site cannot assemble an unchecked request by hand.
 */
class AdmittedExplanationRequest internal constructor(
    val receipt: MinimizedDecisionReceipt,
    val localeTag: String,
    val callerSessionPseudonymId: String,
)

data class RequestAdmissionResult(
    val request: AdmittedExplanationRequest?,
    val rejections: List<GatewayRejection>,
)

@Serializable
data class MinimizationResult(
    val receipt: MinimizedDecisionReceipt?,
    val rejections: List<GatewayRejection>,
)

/** Projects an immutable deterministic receipt down to the only shape the gateway forwards. */
object ReceiptMinimizer {
    fun minimize(
        receipt: SupplementDecisionReceipt,
        receiptSha256: String,
        productVariantKeySha256: String,
    ): MinimizationResult {
        val rejections = mutableListOf<GatewayRejection>()
        val reasonKeys = mutableListOf<ExplanationReasonKey>()
        receipt.reasons.forEach { reason ->
            val key = DeterministicReasonKeys.keyFor(reason)
            if (key == null) {
                rejections += GatewayRejection.UNMAPPED_REASON_TEXT
            } else {
                reasonKeys += key
            }
        }
        if (!receiptSha256.isSha256() || !productVariantKeySha256.isSha256()) {
            rejections += GatewayRejection.MALFORMED_HASH
        }
        if (receipt.modelUsedForDecision) {
            rejections += GatewayRejection.MODEL_CLAIMED_DECISION
        }
        if (rejections.isNotEmpty()) {
            return MinimizationResult(null, rejections.distinct())
        }
        return MinimizationResult(
            receipt = MinimizedDecisionReceipt(
                receiptId = receipt.receiptId,
                receiptSha256 = receiptSha256,
                productVariantKeySha256 = productVariantKeySha256,
                evidenceSha256 = receipt.evidenceSha256,
                rulePackId = receipt.rulePackId,
                rulePackVersion = receipt.rulePackVersion,
                rulePackContentSha256 = receipt.rulePackContentSha256,
                decision = receipt.deterministicDecision,
                triggeredRuleIds = receipt.triggeredRuleIds,
                reasonKeys = reasonKeys.distinct(),
            ),
            rejections = emptyList(),
        )
    }
}

object ExplanationRequestGate {
    fun admit(
        caller: GatewayCaller,
        envelope: ExplanationRequestEnvelope,
    ): RequestAdmissionResult {
        val rejections = mutableListOf<GatewayRejection>()
        if (!caller.authenticated) rejections += GatewayRejection.UNAUTHENTICATED
        if (!caller.serverSide) rejections += GatewayRejection.CLIENT_SIDE_EXECUTION_DENIED
        if (!caller.sessionPseudonymId.isToken()) rejections += GatewayRejection.NON_TOKEN_FIELD
        if (envelope.intent != ExplanationIntent.EXPLAIN_RECEIPT) {
            rejections += GatewayRejection.UNSUPPORTED_INTENT
        }
        if (envelope.rawImageBase64 != null) rejections += GatewayRejection.RAW_IMAGE_PRESENT
        if (envelope.rawOcrText != null) rejections += GatewayRejection.RAW_OCR_TEXT_PRESENT
        if (envelope.freeTextContext != null) rejections += GatewayRejection.FREE_TEXT_CONTEXT_PRESENT
        if (envelope.localeTag !in AdmittedExplanationTemplates.admittedLocales) {
            rejections += GatewayRejection.UNSUPPORTED_LOCALE
        }

        val receipt = envelope.receipt
        if (receipt == null) {
            rejections += GatewayRejection.MISSING_RECEIPT
        } else {
            rejections += receiptRejections(receipt)
        }

        if (rejections.isNotEmpty() || receipt == null) {
            return RequestAdmissionResult(null, rejections.distinct())
        }
        return RequestAdmissionResult(
            request = AdmittedExplanationRequest(
                receipt = receipt,
                localeTag = envelope.localeTag,
                callerSessionPseudonymId = caller.sessionPseudonymId,
            ),
            rejections = emptyList(),
        )
    }

    private fun receiptRejections(receipt: MinimizedDecisionReceipt): List<GatewayRejection> {
        val rejections = mutableListOf<GatewayRejection>()
        if (receipt.modelUsedForDecision) rejections += GatewayRejection.MODEL_CLAIMED_DECISION
        val hashes = listOf(
            receipt.receiptSha256,
            receipt.productVariantKeySha256,
            receipt.evidenceSha256,
            receipt.rulePackContentSha256,
        )
        if (hashes.any { !it.isSha256() }) rejections += GatewayRejection.MALFORMED_HASH
        val tokens = listOf(receipt.receiptId, receipt.rulePackId, receipt.rulePackVersion) +
            receipt.triggeredRuleIds
        if (tokens.any { !it.isToken() }) rejections += GatewayRejection.NON_TOKEN_FIELD
        return rejections
    }
}

/** Schema-bound provider output: template selection, never generated sentences. */
@Serializable
data class ExplanationPlan(
    val receiptId: String,
    val receiptSha256: String,
    val decisionRestated: SafetyDecision,
    val templateIds: List<String>,
    val reasonKeysCovered: List<ExplanationReasonKey>,
    val localeTag: String,
)

object DeterministicExplanationPlanner {
    /** Every template the user must see for this receipt, independent of any model. */
    fun requiredTemplates(receipt: MinimizedDecisionReceipt): Set<String> {
        val templates = mutableSetOf<String>()
        val decisionTemplate = AdmittedExplanationTemplates.decisionTemplates[receipt.decision]
        if (decisionTemplate != null) templates += decisionTemplate
        receipt.reasonKeys.forEach { key ->
            val reasonTemplate = AdmittedExplanationTemplates.reasonTemplates[key]
            if (reasonTemplate != null) templates += reasonTemplate
        }
        templates += AdmittedExplanationTemplates.DISCLAIMER_TEMPLATE
        return templates
    }

    /** The plan served when no provider is admitted, times out, overruns cost, or misbehaves. */
    fun fallbackPlan(request: AdmittedExplanationRequest): ExplanationPlan = ExplanationPlan(
        receiptId = request.receipt.receiptId,
        receiptSha256 = request.receipt.receiptSha256,
        decisionRestated = request.receipt.decision,
        templateIds = requiredTemplates(request.receipt).sorted(),
        reasonKeysCovered = request.receipt.reasonKeys.distinct(),
        localeTag = request.localeTag,
    )
}

object ExplanationPlanVerifier {
    fun verify(
        request: AdmittedExplanationRequest,
        plan: ExplanationPlan,
    ): List<GatewayRejection> {
        val receipt = request.receipt
        val rejections = mutableListOf<GatewayRejection>()

        if (plan.receiptId != receipt.receiptId || plan.receiptSha256 != receipt.receiptSha256) {
            rejections += GatewayRejection.PLAN_RECEIPT_MISMATCH
        }
        if (plan.decisionRestated != receipt.decision) {
            rejections += GatewayRejection.PLAN_DECISION_MUTATED
        }
        if (plan.localeTag != request.localeTag) {
            rejections += GatewayRejection.UNSUPPORTED_LOCALE
        }

        val templates = plan.templateIds.toSet()
        if (templates.any { it !in AdmittedExplanationTemplates.all }) {
            rejections += GatewayRejection.PLAN_TEMPLATE_NOT_ADMITTED
        }

        val receiptKeys = receipt.reasonKeys.toSet()
        val coveredKeys = plan.reasonKeysCovered.toSet()
        val templateKeys = AdmittedExplanationTemplates.reasonTemplates
            .filterValues { it in templates }
            .keys
        if (coveredKeys.any { it !in receiptKeys } || templateKeys.any { it !in receiptKeys }) {
            rejections += GatewayRejection.PLAN_INVENTED_REASON
        }

        val missing = DeterministicExplanationPlanner.requiredTemplates(receipt) - templates
        if (AdmittedExplanationTemplates.DISCLAIMER_TEMPLATE in missing) {
            rejections += GatewayRejection.PLAN_MISSING_DISCLAIMER
        }
        val missingSafety = missing - AdmittedExplanationTemplates.DISCLAIMER_TEMPLATE
        if (missingSafety.isNotEmpty() || receiptKeys.any { it !in coveredKeys }) {
            rejections += GatewayRejection.PLAN_SUPPRESSED_WARNING
        }
        return rejections.distinct()
    }
}

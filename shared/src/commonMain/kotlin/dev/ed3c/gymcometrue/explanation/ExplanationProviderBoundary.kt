package dev.ed3c.gymcometrue.explanation

import dev.ed3c.gymcometrue.domain.SafetyDecision
import kotlinx.serialization.Serializable

/**
 * Provider adapter boundary and audit surface (Issue #36).
 *
 * No provider implementation ships here: `PROVIDER_IMPLEMENTATION = ABSENT`. Shared code holds the
 * interface, the deterministic policy that decides whether a provider may be consulted at all, the
 * verification of whatever it returns, and the fallback that makes the provider optional by
 * construction. Credentials are never a repository path and are never a parameter of this API — a
 * credential-bearing provider stays `PROVIDER_NOT_ADMITTED` until a human admits the deployment.
 */

@Serializable
enum class ProviderCredentialSource {
    /** Test doubles and the deterministic fallback: nothing to inject, nothing to leak. */
    NONE_LOCAL_DETERMINISTIC,

    /** A real provider whose credential is injected server-side, outside this repository. */
    SERVER_INJECTED,
}

@Serializable
data class ProviderDescriptor(
    val providerId: String,
    val modelId: String,
    val modelVersion: String,
    val credentialSource: ProviderCredentialSource,
) {
    init {
        require(providerId.isToken()) { "providerId must be an opaque token." }
        require(modelId.isToken()) { "modelId must be an opaque token." }
        require(modelVersion.isToken()) { "modelVersion must be an opaque token." }
    }
}

sealed interface ProviderOutcome {
    data class Proposed(
        val plan: ExplanationPlan,
        val costUnits: Int,
        val latencyMs: Long,
    ) : ProviderOutcome

    data object TimedOut : ProviderOutcome

    data class Failed(val code: String) : ProviderOutcome
}

/**
 * The adapter a server-side deployment would implement. It receives an [AdmittedExplanationRequest]
 * that only the gate can construct, and its output is verified before any user sees it.
 */
interface ExplanationProvider {
    val descriptor: ProviderDescriptor

    fun propose(request: AdmittedExplanationRequest): ProviderOutcome
}

@Serializable
data class GatewayPolicy(
    /** Deterministic kill switch: when engaged, no request reaches any provider. */
    val killSwitchEngaged: Boolean = false,
    /** Stays false until a human admits credentials and a deployment environment. */
    val serverCredentialAdmitted: Boolean = false,
    val maxCostUnitsPerRequest: Int = 4,
    val timeoutMs: Long = 4_000L,
)

@Serializable
enum class GatewayOutcomeKind {
    REQUEST_REJECTED,
    DETERMINISTIC_FALLBACK,
    MODEL_PLAN_ACCEPTED,
}

/**
 * The one admission gate in front of every provider call, whatever the subject: the deterministic
 * kill switch, provider presence, and the human-admitted credential — in that order. Both the
 * receipt path and the logged-totals path below call this exact function, so widening the subject
 * cannot widen the gate.
 */
internal fun providerBlocker(
    policy: GatewayPolicy,
    descriptor: ProviderDescriptor?,
): GatewayRejection? = when {
    policy.killSwitchEngaged -> GatewayRejection.KILL_SWITCH_ENGAGED
    descriptor == null -> GatewayRejection.PROVIDER_ABSENT
    descriptor.credentialSource == ProviderCredentialSource.SERVER_INJECTED &&
        !policy.serverCredentialAdmitted -> GatewayRejection.PROVIDER_NOT_ADMITTED
    else -> null
}

/**
 * Hash-only audit record. Every field is a hash, an enum, a token, or a number; there is no field
 * that could carry label text, a product name, a symptom description, or model prose.
 */
@Serializable
data class GatewayAuditRecord(
    val callerSessionPseudonymId: String,
    val receiptSha256: String?,
    val rulePackContentSha256: String?,
    val decision: SafetyDecision?,
    val outcome: GatewayOutcomeKind,
    val rejections: List<GatewayRejection>,
    val providerId: String? = null,
    val modelId: String? = null,
    val modelVersion: String? = null,
    val costUnits: Int = 0,
    val latencyMs: Long = 0L,
)

@Serializable
data class GatewayResponse(
    val plan: ExplanationPlan?,
    val outcome: GatewayOutcomeKind,
    val rejections: List<GatewayRejection>,
    val audit: GatewayAuditRecord,
)

object ExplanationGatewayService {
    private const val UNMINIMIZED_CALLER = "unminimized-caller-id"

    fun explain(
        caller: GatewayCaller,
        envelope: ExplanationRequestEnvelope,
        policy: GatewayPolicy = GatewayPolicy(),
        provider: ExplanationProvider? = null,
    ): GatewayResponse {
        val admission = ExplanationRequestGate.admit(caller, envelope)
        val sessionId = if (caller.sessionPseudonymId.isToken()) {
            caller.sessionPseudonymId
        } else {
            UNMINIMIZED_CALLER
        }
        val request = admission.request
        if (request == null) {
            return GatewayResponse(
                plan = null,
                outcome = GatewayOutcomeKind.REQUEST_REJECTED,
                rejections = admission.rejections,
                audit = GatewayAuditRecord(
                    callerSessionPseudonymId = sessionId,
                    receiptSha256 = envelope.receipt?.receiptSha256?.takeIf { it.isSha256() },
                    rulePackContentSha256 = envelope.receipt?.rulePackContentSha256?.takeIf { it.isSha256() },
                    decision = envelope.receipt?.decision,
                    outcome = GatewayOutcomeKind.REQUEST_REJECTED,
                    rejections = admission.rejections,
                ),
            )
        }

        val fallback = DeterministicExplanationPlanner.fallbackPlan(request)
        val blocker = providerBlocker(policy, provider?.descriptor)
        if (provider == null || blocker != null) {
            return fallbackResponse(
                sessionId = sessionId,
                request = request,
                fallback = fallback,
                rejections = listOfNotNull(blocker),
                descriptor = provider?.descriptor,
                costUnits = 0,
                latencyMs = 0L,
            )
        }

        val outcome = provider.propose(request)
        val rejections = mutableListOf<GatewayRejection>()
        var costUnits = 0
        var latencyMs = 0L
        var acceptedPlan: ExplanationPlan? = null
        when (outcome) {
            is ProviderOutcome.TimedOut -> rejections += GatewayRejection.PROVIDER_TIMEOUT
            is ProviderOutcome.Failed -> rejections += GatewayRejection.PROVIDER_FAILURE
            is ProviderOutcome.Proposed -> {
                costUnits = outcome.costUnits
                latencyMs = outcome.latencyMs
                if (costUnits > policy.maxCostUnitsPerRequest) {
                    rejections += GatewayRejection.COST_LIMIT_EXCEEDED
                }
                if (latencyMs > policy.timeoutMs) {
                    rejections += GatewayRejection.PROVIDER_TIMEOUT
                }
                rejections += ExplanationPlanVerifier.verify(request, outcome.plan)
                if (rejections.isEmpty()) {
                    acceptedPlan = outcome.plan
                }
            }
        }

        if (acceptedPlan == null) {
            return fallbackResponse(
                sessionId = sessionId,
                request = request,
                fallback = fallback,
                rejections = rejections,
                descriptor = provider.descriptor,
                costUnits = costUnits,
                latencyMs = latencyMs,
            )
        }
        return GatewayResponse(
            plan = acceptedPlan,
            outcome = GatewayOutcomeKind.MODEL_PLAN_ACCEPTED,
            rejections = emptyList(),
            audit = auditRecord(
                sessionId = sessionId,
                request = request,
                outcome = GatewayOutcomeKind.MODEL_PLAN_ACCEPTED,
                rejections = emptyList(),
                descriptor = provider.descriptor,
                costUnits = costUnits,
                latencyMs = latencyMs,
            ),
        )
    }

    private fun fallbackResponse(
        sessionId: String,
        request: AdmittedExplanationRequest,
        fallback: ExplanationPlan,
        rejections: List<GatewayRejection>,
        descriptor: ProviderDescriptor?,
        costUnits: Int,
        latencyMs: Long,
    ): GatewayResponse = GatewayResponse(
        plan = fallback,
        outcome = GatewayOutcomeKind.DETERMINISTIC_FALLBACK,
        rejections = rejections.distinct(),
        audit = auditRecord(
            sessionId = sessionId,
            request = request,
            outcome = GatewayOutcomeKind.DETERMINISTIC_FALLBACK,
            rejections = rejections.distinct(),
            descriptor = descriptor,
            costUnits = costUnits,
            latencyMs = latencyMs,
        ),
    )

    private fun auditRecord(
        sessionId: String,
        request: AdmittedExplanationRequest,
        outcome: GatewayOutcomeKind,
        rejections: List<GatewayRejection>,
        descriptor: ProviderDescriptor?,
        costUnits: Int,
        latencyMs: Long,
    ): GatewayAuditRecord = GatewayAuditRecord(
        callerSessionPseudonymId = sessionId,
        receiptSha256 = request.receipt.receiptSha256,
        rulePackContentSha256 = request.receipt.rulePackContentSha256,
        decision = request.receipt.decision,
        outcome = outcome,
        rejections = rejections,
        providerId = descriptor?.providerId,
        modelId = descriptor?.modelId,
        modelVersion = descriptor?.modelVersion,
        costUnits = costUnits,
        latencyMs = latencyMs,
    )
}

/**
 * Logged-totals widening (Issue #51).
 *
 * The receipt path above is untouched: same kill switch, same credential gate, same
 * [GatewayRejection] ladder, same verifier. What is new is that a second admitted subject — the
 * user's own logged totals — may also reach a provider instead of being deterministic-only.
 *
 * Two properties keep the widening from weakening anything:
 *
 * - the provider's input is an [ExplainSubject.LoggedTotals], whose constructor is internal to this
 *   module, so only `AiExplanationService.explainLoggedTotals` — after its subject gate has passed —
 *   can hand a provider anything to work on;
 * - the provider's output is a template selection verified against the logging catalogue, which
 *   contains no decision, dose, diagnosis or clearance template, so an accepted plan can only ever
 *   say something the repository already authored.
 *
 * [LoggedTotalsPlan] is that schema-bound output: template selection, never generated sentences.
 */
@Serializable
data class LoggedTotalsPlan(
    val summarySha256: String,
    val templateIds: List<String>,
    val localeTag: String,
)

sealed interface LoggedTotalsOutcome {
    data class Proposed(
        val plan: LoggedTotalsPlan,
        val costUnits: Int,
        val latencyMs: Long,
    ) : LoggedTotalsOutcome

    data object TimedOut : LoggedTotalsOutcome

    data class Failed(val code: String) : LoggedTotalsOutcome
}

/**
 * The adapter a server-side deployment would implement for the logging surface.
 * `PROVIDER_IMPLEMENTATION = ABSENT` here too: shared code holds the interface, the gate, and the
 * verification of whatever comes back.
 */
interface LoggedTotalsProvider {
    val descriptor: ProviderDescriptor

    fun propose(subject: ExplainSubject.LoggedTotals, localeTag: String): LoggedTotalsOutcome
}

object DeterministicLoggedTotalsPlanner {
    /** Every template the user must see for these totals, independent of any model. */
    fun requiredTemplates(totals: MinimizedLoggedTotals): Set<String> {
        val templates = mutableSetOf(AdmittedLoggedTotalsTemplates.DAILY_TOTALS)
        if (totals.duplicateIngredientKeyCount > 0) {
            templates += AdmittedLoggedTotalsTemplates.DUPLICATE_INGREDIENT
        }
        if (totals.unresolvedEntryCount > 0) {
            templates += AdmittedLoggedTotalsTemplates.UNRESOLVED_ENTRY
        }
        templates += AdmittedExplanationTemplates.DISCLAIMER_TEMPLATE
        return templates
    }

    /** The plan served when no provider is admitted, times out, overruns cost, or misbehaves. */
    fun fallbackPlan(
        subject: ExplainSubject.LoggedTotals,
        localeTag: String,
    ): LoggedTotalsPlan = LoggedTotalsPlan(
        summarySha256 = subject.subjectSha256,
        templateIds = requiredTemplates(subject.totals).toList(),
        localeTag = localeTag,
    )
}

object LoggedTotalsPlanVerifier {
    /**
     * The whole surface an accepted logging plan may draw from: the `tpl.logged.*` catalogue plus
     * the disclaimer. No decision template is reachable, so a provider cannot restate a safety
     * verdict on a surface that renders none.
     */
    private val admittedSurface: Set<String> =
        AdmittedLoggedTotalsTemplates.all + AdmittedExplanationTemplates.DISCLAIMER_TEMPLATE

    fun verify(
        subject: ExplainSubject.LoggedTotals,
        localeTag: String,
        plan: LoggedTotalsPlan,
    ): List<GatewayRejection> {
        val totals = subject.totals
        val rejections = mutableListOf<GatewayRejection>()

        if (plan.summarySha256 != subject.subjectSha256) {
            rejections += GatewayRejection.PLAN_RECEIPT_MISMATCH
        }
        if (plan.localeTag != localeTag) {
            rejections += GatewayRejection.UNSUPPORTED_LOCALE
        }

        val templates = plan.templateIds.toSet()
        if (templates.any { it !in admittedSurface }) {
            rejections += GatewayRejection.PLAN_TEMPLATE_NOT_ADMITTED
        }
        // An optional next step is an observation about the user's own data: it may only appear
        // when the deterministic counts support it, exactly as a reason key may only appear when
        // the receipt carries it.
        val inventedDuplicate = AdmittedLoggedTotalsTemplates.REVIEW_DUPLICATE in templates &&
            totals.duplicateIngredientKeyCount == 0
        val inventedUnresolved = AdmittedLoggedTotalsTemplates.CONFIRM_UNRESOLVED_ENTRY in templates &&
            totals.unresolvedEntryCount == 0
        if (inventedDuplicate || inventedUnresolved) {
            rejections += GatewayRejection.PLAN_INVENTED_REASON
        }

        val missing = DeterministicLoggedTotalsPlanner.requiredTemplates(totals) - templates
        if (AdmittedExplanationTemplates.DISCLAIMER_TEMPLATE in missing) {
            rejections += GatewayRejection.PLAN_MISSING_DISCLAIMER
        }
        if ((missing - AdmittedExplanationTemplates.DISCLAIMER_TEMPLATE).isNotEmpty()) {
            rejections += GatewayRejection.PLAN_SUPPRESSED_WARNING
        }
        return rejections.distinct()
    }
}

/** What the gateway decided to serve for a logged-totals subject. Only the gateway builds one. */
class ServedLoggedTotals internal constructor(
    val plan: LoggedTotalsPlan,
    val outcome: GatewayOutcomeKind,
    val rejections: List<GatewayRejection>,
)

object LoggedTotalsGatewayService {
    /**
     * Consults [provider] for an already-admitted logged-totals subject and verifies what comes
     * back. Every failure mode — blocked, absent, timed out, over cost, or an unverifiable plan —
     * degrades to the deterministic plan rather than to nothing, so the surface always has
     * something notice-bearing to render.
     */
    fun serve(
        subject: ExplainSubject.LoggedTotals,
        localeTag: String,
        policy: GatewayPolicy = GatewayPolicy(),
        provider: LoggedTotalsProvider? = null,
    ): ServedLoggedTotals {
        val fallback = DeterministicLoggedTotalsPlanner.fallbackPlan(subject, localeTag)
        val blocker = providerBlocker(policy, provider?.descriptor)
        if (provider == null || blocker != null) {
            return ServedLoggedTotals(
                plan = fallback,
                outcome = GatewayOutcomeKind.DETERMINISTIC_FALLBACK,
                rejections = listOfNotNull(blocker),
            )
        }

        val rejections = mutableListOf<GatewayRejection>()
        var acceptedPlan: LoggedTotalsPlan? = null
        when (val outcome = provider.propose(subject, localeTag)) {
            is LoggedTotalsOutcome.TimedOut -> rejections += GatewayRejection.PROVIDER_TIMEOUT
            is LoggedTotalsOutcome.Failed -> rejections += GatewayRejection.PROVIDER_FAILURE
            is LoggedTotalsOutcome.Proposed -> {
                if (outcome.costUnits > policy.maxCostUnitsPerRequest) {
                    rejections += GatewayRejection.COST_LIMIT_EXCEEDED
                }
                if (outcome.latencyMs > policy.timeoutMs) {
                    rejections += GatewayRejection.PROVIDER_TIMEOUT
                }
                rejections += LoggedTotalsPlanVerifier.verify(subject, localeTag, outcome.plan)
                if (rejections.isEmpty()) {
                    acceptedPlan = outcome.plan
                }
            }
        }

        val accepted = acceptedPlan
        return if (accepted == null) {
            ServedLoggedTotals(
                plan = fallback,
                outcome = GatewayOutcomeKind.DETERMINISTIC_FALLBACK,
                rejections = rejections.distinct(),
            )
        } else {
            ServedLoggedTotals(
                plan = accepted,
                outcome = GatewayOutcomeKind.MODEL_PLAN_ACCEPTED,
                rejections = emptyList(),
            )
        }
    }
}

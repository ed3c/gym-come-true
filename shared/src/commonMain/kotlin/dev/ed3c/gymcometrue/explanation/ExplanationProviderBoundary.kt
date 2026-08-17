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
        val providerBlocker = providerBlocker(policy, provider)
        if (provider == null || providerBlocker != null) {
            return fallbackResponse(
                sessionId = sessionId,
                request = request,
                fallback = fallback,
                rejections = listOfNotNull(providerBlocker),
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

    private fun providerBlocker(
        policy: GatewayPolicy,
        provider: ExplanationProvider?,
    ): GatewayRejection? = when {
        policy.killSwitchEngaged -> GatewayRejection.KILL_SWITCH_ENGAGED
        provider == null -> GatewayRejection.PROVIDER_ABSENT
        provider.descriptor.credentialSource == ProviderCredentialSource.SERVER_INJECTED &&
            !policy.serverCredentialAdmitted -> GatewayRejection.PROVIDER_NOT_ADMITTED
        else -> null
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

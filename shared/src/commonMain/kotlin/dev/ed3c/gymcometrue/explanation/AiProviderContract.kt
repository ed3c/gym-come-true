package dev.ed3c.gymcometrue.explanation

import kotlinx.serialization.Serializable

/**
 * Provider-agnostic AI explain layer (Issue #49, owner decision `docs/product/mvp-redesign.md`).
 *
 * This file composes [ExplanationRequestGate], [ExplanationGatewayService] and the admitted template
 * catalogue; it does not fork them. What it adds is the part the MVP repositioning needs:
 *
 * - two named providers ([ProviderId]) with a per-provider descriptor and kill switch,
 * - a provider-agnostic request whose only admitted subjects are a gateway-admitted receipt or a
 *   hash-only projection of the user's own logged totals,
 * - a response type that cannot exist without the medical-risk notice from `legal/DISCLAIMER.md`,
 * - a hash-only audit record carrying the per-provider identity.
 *
 * Credentials are never a parameter, a constant, or a repository path here. See
 * `docs/llm-gateway/ai-provider-key-boundary.md`.
 */

@Serializable
enum class ProviderId {
    OPENAI_CHATGPT,
    ANTHROPIC_CLAUDE,
}

/** Where a provider credential lives. Neither class puts a key in Git or in a client build. */
@Serializable
enum class ProviderEndpointClass {
    /** The key is held by a server-side relay this repository does not describe. */
    SERVER_SIDE_RELAY,

    /** The key is typed by the user at runtime and held for that session only. */
    USER_SUPPLIED_RUNTIME_KEY,
}

/**
 * Per-provider configuration. [killSwitchEngaged] defaults to `true`: a provider is off until a
 * deployment turns it on, so a missing configuration blocks rather than dials out.
 */
@Serializable
data class AiProviderDescriptor(
    val providerId: ProviderId,
    /** Opaque family token. Real vendor model identifiers are deployment data, never repository data. */
    val modelFamily: String,
    val endpointClass: ProviderEndpointClass,
    val killSwitchEngaged: Boolean = true,
    /** Stays false until a human admits a credential for this provider in a deployment. */
    val credentialAdmitted: Boolean = false,
) {
    init {
        require(modelFamily.isToken()) { "modelFamily must be an opaque token." }
    }
}

/**
 * Hash-only projection of the user's own logged totals. Ingredient identities arrive as opaque keys
 * because shared code has no hasher; free-text ingredient names are rejected, not silently
 * forwarded. The totals are arithmetic and carry no comparison against any reviewed limit.
 */
@Serializable
data class MinimizedLoggedTotals(
    val summarySha256: String,
    val ingredientKeyTotalsMg: Map<String, Double> = emptyMap(),
    val duplicateIngredientKeyCount: Int = 0,
    val unresolvedEntryCount: Int = 0,
)

/** Templates for the logging surface. There is deliberately no verdict, dose, or limit template. */
object AdmittedLoggedTotalsTemplates {
    const val DAILY_TOTALS: String = "tpl.logged.daily-totals"
    const val DUPLICATE_INGREDIENT: String = "tpl.logged.duplicate-ingredient"
    const val UNRESOLVED_ENTRY: String = "tpl.logged.unresolved-entry"

    /**
     * A provider's entire degree of freedom on this surface: which admitted next step about the
     * user's own log to surface. Each one is verified against the deterministic counts, so it
     * cannot become an invented observation.
     */
    const val CONFIRM_UNRESOLVED_ENTRY: String = "tpl.logged.next-step.confirm-entry"
    const val REVIEW_DUPLICATE: String = "tpl.logged.next-step.review-duplicate"

    val optionalTemplates: Set<String> = setOf(CONFIRM_UNRESOLVED_ENTRY, REVIEW_DUPLICATE)

    val all: Set<String> =
        setOf(DAILY_TOTALS, DUPLICATE_INGREDIENT, UNRESOLVED_ENTRY) + optionalTemplates
}

/**
 * The only two subjects an explain request may carry. Both constructors are internal, so a call site
 * cannot assemble a subject out of raw text, an image, or a free-form question.
 */
sealed interface ExplainSubject {
    val subjectSha256: String

    /** Wraps proof of admission: only [ExplanationRequestGate] can produce the wrapped request. */
    class GatewayReceipt internal constructor(
        val admitted: AdmittedExplanationRequest,
    ) : ExplainSubject {
        override val subjectSha256: String get() = admitted.receipt.receiptSha256
    }

    class LoggedTotals internal constructor(
        val totals: MinimizedLoggedTotals,
    ) : ExplainSubject {
        override val subjectSha256: String get() = totals.summarySha256
    }
}

/** Formed only after the per-provider kill switch and the subject gate have both passed. */
class AiExplainRequest internal constructor(
    val providerId: ProviderId,
    val subject: ExplainSubject,
    val localeTag: String,
    val callerSessionPseudonymId: String,
)

/**
 * The mandatory medical-risk notice. The constructor is private and the single authorized instance
 * is [MANDATORY], whose bytes are the `legal/DISCLAIMER.md` SSOT wording, so no call site can build
 * a weakened, translated, or empty notice.
 */
class MedicalRiskNotice private constructor(
    val zhHant: String,
    val en: String,
) {
    companion object {
        val MANDATORY: MedicalRiskNotice = MedicalRiskNotice(
            zhHant = "AI 回應僅為一般資訊，並非醫療建議。本 App 不提供診斷、治療或劑量建議；" +
                "補充品、飲食與運動安排請諮詢合格醫療專業人員。相關決定與後果由使用者自行負責。",
            en = "AI responses are general information only and are not medical advice. " +
                "This app does not diagnose, treat, or recommend doses. Consult a qualified " +
                "healthcare professional about supplements, diet, and exercise. Decisions and " +
                "their outcomes remain the user's own.",
        )
    }
}

/**
 * A response the user may be shown. [notice] is not a constructor parameter: no call site can omit
 * it, null it, or substitute different wording, so a notice-less response has no representation.
 */
class AiExplanationResponse internal constructor(
    val providerId: ProviderId,
    val subjectSha256: String,
    val templateIds: List<String>,
    val localeTag: String,
) {
    val notice: MedicalRiskNotice = MedicalRiskNotice.MANDATORY
}

/**
 * Hash-only audit hook. Every field is a hash, an enum, or an opaque token; nothing here can carry a
 * product name, a label string, a logged ingredient name, or model prose. A deployment forwards this
 * record to its own sink.
 */
@Serializable
data class AiExplainAudit(
    val callerSessionPseudonymId: String,
    val providerId: ProviderId,
    val modelFamily: String,
    val endpointClass: ProviderEndpointClass,
    val subjectSha256: String?,
    val outcome: GatewayOutcomeKind,
    val rejections: List<GatewayRejection>,
)

class AiExplainOutcome internal constructor(
    /** Null whenever the kill switch or a gate blocked: the request never formed. */
    val request: AiExplainRequest?,
    val response: AiExplanationResponse?,
    val rejections: List<GatewayRejection>,
    val audit: AiExplainAudit,
)

object AiExplanationService {
    private const val UNMINIMIZED_CALLER = "unminimized-caller-id"

    /**
     * Explains a deterministic decision receipt. The receipt gate, the provider call, the plan
     * verification and the deterministic fallback all stay in [ExplanationGatewayService]; this
     * function adds the per-provider kill switch in front of it and the notice behind it.
     */
    fun explainReceipt(
        descriptor: AiProviderDescriptor,
        caller: GatewayCaller,
        envelope: ExplanationRequestEnvelope,
        policy: GatewayPolicy = GatewayPolicy(),
        provider: ExplanationProvider? = null,
    ): AiExplainOutcome {
        val subjectSha256 = envelope.receipt?.receiptSha256?.takeIf { it.isSha256() }
        val blockers = providerBlockers(descriptor)
        if (blockers.isNotEmpty()) {
            return blocked(descriptor, caller, subjectSha256, blockers)
        }

        val admission = ExplanationRequestGate.admit(caller, envelope)
        val admitted = admission.request
            ?: return blocked(descriptor, caller, subjectSha256, admission.rejections)

        val request = AiExplainRequest(
            providerId = descriptor.providerId,
            subject = ExplainSubject.GatewayReceipt(admitted),
            localeTag = admitted.localeTag,
            callerSessionPseudonymId = admitted.callerSessionPseudonymId,
        )
        // The gate is deterministic, so the admission repeated inside the service below is the same
        // decision on the same bytes; the request above is what proves it was admitted at all.
        val gateway = ExplanationGatewayService.explain(caller, envelope, policy, provider)
        val plan = gateway.plan
            ?: return blocked(descriptor, caller, subjectSha256, gateway.rejections)

        return AiExplainOutcome(
            request = request,
            response = AiExplanationResponse(
                providerId = descriptor.providerId,
                subjectSha256 = plan.receiptSha256,
                templateIds = plan.templateIds,
                localeTag = plan.localeTag,
            ),
            rejections = gateway.rejections,
            audit = audit(
                descriptor = descriptor,
                caller = caller,
                subjectSha256 = plan.receiptSha256,
                outcome = gateway.outcome,
                rejections = gateway.rejections,
            ),
        )
    }

    /**
     * Explains the user's own logged totals. An admitted [provider] may propose which templates to
     * surface (Issue #51); it is consulted only after the subject gate below has passed, and only
     * through [LoggedTotalsGatewayService], which verifies the proposal and otherwise serves the
     * deterministic plan. With no provider the surface behaves exactly as it did before.
     */
    fun explainLoggedTotals(
        descriptor: AiProviderDescriptor,
        caller: GatewayCaller,
        totals: MinimizedLoggedTotals,
        localeTag: String = "zh-TW",
        policy: GatewayPolicy = GatewayPolicy(),
        provider: LoggedTotalsProvider? = null,
    ): AiExplainOutcome {
        val subjectSha256 = totals.summarySha256.takeIf { it.isSha256() }
        val blockers = providerBlockers(descriptor)
        if (blockers.isNotEmpty()) {
            return blocked(descriptor, caller, subjectSha256, blockers)
        }

        val rejections = mutableListOf<GatewayRejection>()
        if (!caller.authenticated) rejections += GatewayRejection.UNAUTHENTICATED
        // A user-supplied runtime key is by definition used from the user's own device; every other
        // endpoint class stays server-side.
        val userKeyed = descriptor.endpointClass == ProviderEndpointClass.USER_SUPPLIED_RUNTIME_KEY
        if (!caller.serverSide && !userKeyed) {
            rejections += GatewayRejection.CLIENT_SIDE_EXECUTION_DENIED
        }
        if (!caller.sessionPseudonymId.isToken()) rejections += GatewayRejection.NON_TOKEN_FIELD
        if (localeTag !in AdmittedExplanationTemplates.admittedLocales) {
            rejections += GatewayRejection.UNSUPPORTED_LOCALE
        }
        if (subjectSha256 == null) rejections += GatewayRejection.MALFORMED_HASH
        if (totals.ingredientKeyTotalsMg.keys.any { !it.isToken() }) {
            rejections += GatewayRejection.NON_TOKEN_FIELD
        }
        if (rejections.isNotEmpty() || subjectSha256 == null) {
            return blocked(descriptor, caller, subjectSha256, rejections)
        }

        val subject = ExplainSubject.LoggedTotals(totals)
        val served = LoggedTotalsGatewayService.serve(subject, localeTag, policy, provider)
        return AiExplainOutcome(
            request = AiExplainRequest(
                providerId = descriptor.providerId,
                subject = subject,
                localeTag = localeTag,
                callerSessionPseudonymId = caller.sessionPseudonymId,
            ),
            response = AiExplanationResponse(
                providerId = descriptor.providerId,
                subjectSha256 = served.plan.summarySha256,
                templateIds = served.plan.templateIds,
                localeTag = served.plan.localeTag,
            ),
            rejections = served.rejections,
            audit = audit(
                descriptor = descriptor,
                caller = caller,
                subjectSha256 = subjectSha256,
                outcome = served.outcome,
                rejections = served.rejections,
            ),
        )
    }

    /** Checked before any request object forms, so an engaged kill switch cannot leak a payload. */
    private fun providerBlockers(descriptor: AiProviderDescriptor): List<GatewayRejection> =
        buildList {
            if (descriptor.killSwitchEngaged) add(GatewayRejection.KILL_SWITCH_ENGAGED)
            if (!descriptor.credentialAdmitted) add(GatewayRejection.PROVIDER_NOT_ADMITTED)
        }

    private fun blocked(
        descriptor: AiProviderDescriptor,
        caller: GatewayCaller,
        subjectSha256: String?,
        rejections: List<GatewayRejection>,
    ): AiExplainOutcome = AiExplainOutcome(
        request = null,
        response = null,
        rejections = rejections.distinct(),
        audit = audit(
            descriptor = descriptor,
            caller = caller,
            subjectSha256 = subjectSha256,
            outcome = GatewayOutcomeKind.REQUEST_REJECTED,
            rejections = rejections.distinct(),
        ),
    )

    private fun audit(
        descriptor: AiProviderDescriptor,
        caller: GatewayCaller,
        subjectSha256: String?,
        outcome: GatewayOutcomeKind,
        rejections: List<GatewayRejection>,
    ): AiExplainAudit = AiExplainAudit(
        callerSessionPseudonymId = caller.sessionPseudonymId.takeIf { it.isToken() }
            ?: UNMINIMIZED_CALLER,
        providerId = descriptor.providerId,
        modelFamily = descriptor.modelFamily,
        endpointClass = descriptor.endpointClass,
        subjectSha256 = subjectSha256,
        outcome = outcome,
        rejections = rejections,
    )
}

package dev.ed3c.gymcometrue.domain

import kotlinx.serialization.Serializable

/**
 * Issue #26 (TW3) — reviewed Taiwan supplement rule-pack admission.
 *
 * Target transition: `OCR_EVALUATED -> REVIEWED_TAIWAN_RULE_PACK`. That transition is **not**
 * reached here and cannot be reached from this repository.
 *
 * Everything #26 actually requires — approved immutable MOHW/TFDA bytes, legal reuse review, a
 * qualified clinical reviewer with a conflict-of-interest declaration, reviewed user-facing
 * wording, cryptographic signatures, and activation/revocation/rollback receipts — is produced
 * outside this repository by people and systems that do not exist in it yet. Fabricating any of
 * them here would be indistinguishable, in the repository, from actually having them.
 *
 * So this file delivers exactly one thing: the deterministic logic that decides admission *given*
 * that evidence, plus a ledger in which every external gate defaults to
 * [ExternalGateState.ABSENT]. The gate can only ever answer
 * [ReviewedRulePackDecision.EXTERNAL_GATES_ABSENT] from repository state, and that is the correct
 * answer.
 */

/** One external input that this repository cannot produce for itself. */
@Serializable
enum class ExternalGate {
    IMMUTABLE_OFFICIAL_SOURCE_BYTES,
    LEGAL_REUSE_REVIEW,
    QUALIFIED_CLINICAL_REVIEWER,
    REVIEWER_CONFLICT_OF_INTEREST_DECLARATION,
    REVIEWED_USER_FACING_WORDING,
    CRYPTOGRAPHIC_SIGNATURES,
    ACTIVATION_RECEIPT,
    REVOCATION_AND_ROLLBACK_RECEIPT,
    CONSENTED_CORPUS,
    AUTHORIZED_DEVICE_OCR_EVALUATION,
}

@Serializable
enum class ExternalGateState {
    /** Nothing exists. The default, and the only state repository evidence can justify. */
    ABSENT,

    /** Someone asserted it exists; no verified artifact backs the assertion. Still denies. */
    DECLARED_NOT_VERIFIED,

    /**
     * A human with authority accepted it out of band.
     *
     * No code path in this repository writes this state, and none may. It exists so the admission
     * logic is a real function of the gate ledger rather than a hard-coded rejection — the
     * difference between "we check and it fails" and "we never check".
     */
    HUMAN_ADMITTED,
}

@Serializable
data class ExternalGateStatus(
    val gate: ExternalGate,
    val state: ExternalGateState = ExternalGateState.ABSENT,
    val evidenceRef: String? = null,
    val note: String = "",
)

@Serializable
data class ExternalGateLedger(
    val statuses: List<ExternalGateStatus> = emptyList(),
) {
    /** An unlisted gate is [ExternalGateState.ABSENT]. Silence is never admission. */
    fun stateOf(gate: ExternalGate): ExternalGateState =
        statuses.firstOrNull { it.gate == gate }?.state ?: ExternalGateState.ABSENT

    fun gatesNotAdmitted(): List<ExternalGate> =
        ExternalGate.entries.filter { stateOf(it) != ExternalGateState.HUMAN_ADMITTED }
}

/**
 * A reviewer's conflict-of-interest declaration.
 *
 * A declared interest does not disqualify a reviewer; an undeclared or unmitigated one does.
 */
@Serializable
data class ConflictOfInterestDeclaration(
    val reviewerPseudonymousId: String,
    val declaredAtIsoDate: String,
    val hasFinancialInterest: Boolean = false,
    val hasEmploymentRelationship: Boolean = false,
    val hasResearchFunding: Boolean = false,
    val mitigationNote: String? = null,
    val declarationSignatureSha256: String? = null,
) {
    init {
        require(reviewerPseudonymousId.isNotBlank()) { "A COI declaration requires a pseudonymous reviewer id." }
    }
}

@Serializable
enum class ReviewedRulePackDecision {
    /** The pack's own deterministic evidence is wrong. Fix the pack. */
    REJECTED,

    /** The pack is internally consistent; one or more external gates are not human-admitted. */
    EXTERNAL_GATES_ABSENT,

    /** Deterministic evidence is consistent and every external gate is human-admitted. */
    ADMITTED,
}

@Serializable
data class ReviewedRulePackGateResult(
    val decision: ReviewedRulePackDecision,
    val blockers: List<String>,
    val absentGates: List<ExternalGate>,
    val reviewNotes: List<String>,
)

object TaiwanReviewedRulePackGate {
    /**
     * Composes the pack's own admission check with the external-gate ledger.
     *
     * Order matters: deterministic defects are reported as [ReviewedRulePackDecision.REJECTED]
     * before gates are considered, so "the pack is broken" is never reported as "we are waiting on
     * a reviewer".
     */
    fun evaluate(
        pack: TaiwanRulePackManifest,
        ledger: ExternalGateLedger,
        conflictOfInterest: ConflictOfInterestDeclaration?,
        asOfIsoDate: String,
    ): ReviewedRulePackGateResult {
        val packResult = TaiwanRulePackAdmissionValidator.validate(pack, asOfIsoDate)
        val blockers = packResult.blockers.toMutableList()
        val reviewNotes = packResult.reviewNotes.toMutableList()

        if (pack.status != RulePackStatus.CLINICALLY_REVIEWED) {
            blockers += "Only a clinically reviewed pack can enter the TW3 admission gate."
        }
        // #26 negative control: no unbounded effective window.
        if (pack.effectiveFrom == null || pack.effectiveUntil == null) {
            blockers += "A reviewed pack requires a bounded effective window."
        }

        blockers += conflictOfInterestBlockers(pack, conflictOfInterest, asOfIsoDate)

        val absentGates = ledger.gatesNotAdmitted()
        absentGates.forEach { gate ->
            reviewNotes += "External gate ${gate.name} is ${ledger.stateOf(gate).name}."
        }

        val decision = when {
            blockers.isNotEmpty() -> ReviewedRulePackDecision.REJECTED
            absentGates.isNotEmpty() -> ReviewedRulePackDecision.EXTERNAL_GATES_ABSENT
            else -> ReviewedRulePackDecision.ADMITTED
        }
        return ReviewedRulePackGateResult(
            decision = decision,
            blockers = blockers.distinct(),
            absentGates = absentGates,
            reviewNotes = reviewNotes.distinct(),
        )
    }

    private fun conflictOfInterestBlockers(
        pack: TaiwanRulePackManifest,
        declaration: ConflictOfInterestDeclaration?,
        asOfIsoDate: String,
    ): List<String> {
        val blockers = mutableListOf<String>()
        if (declaration == null) {
            blockers += "A reviewed pack requires a reviewer conflict-of-interest declaration."
            return blockers
        }
        val declaredAt = declaration.declaredAtIsoDate.taiwanIsoDateKey()
        val asOf = asOfIsoDate.taiwanIsoDateKey()
        if (declaredAt == null) blockers += "The COI declaration date must be a valid YYYY-MM-DD date."
        if (declaredAt != null && asOf != null && declaredAt > asOf) {
            blockers += "The COI declaration is dated after the admission date."
        }
        if (!declaration.declarationSignatureSha256.isTaiwanSha256()) {
            blockers += "The COI declaration requires a signature SHA-256."
        }

        val attestation = pack.reviewerAttestation
        if (attestation != null && attestation.reviewerPseudonymousId != declaration.reviewerPseudonymousId) {
            blockers += "The COI declaration names a different reviewer than the attestation."
        }

        val hasInterest = declaration.hasFinancialInterest ||
            declaration.hasEmploymentRelationship ||
            declaration.hasResearchFunding
        if (hasInterest && declaration.mitigationNote.isNullOrBlank()) {
            blockers += "A declared conflict of interest requires a recorded mitigation."
        }
        return blockers
    }
}

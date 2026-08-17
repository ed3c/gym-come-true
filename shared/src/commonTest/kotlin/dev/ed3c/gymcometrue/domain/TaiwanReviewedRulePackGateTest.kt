package dev.ed3c.gymcometrue.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Issue #26 (TW3) — reviewed rule-pack admission gate.
 *
 * Every external input this gate consumes is `ABSENT` in this repository. These tests exercise the
 * decision logic; they do not and cannot demonstrate that a Taiwan rule pack was actually reviewed.
 */
class TaiwanReviewedRulePackGateTest {
    @Test
    fun anUnlistedGateIsAbsentSoTheDefaultLedgerAdmitsNothing() {
        val ledger = ExternalGateLedger()

        assertEquals(ExternalGateState.ABSENT, ledger.stateOf(ExternalGate.LEGAL_REUSE_REVIEW))
        assertEquals(ExternalGate.entries.size, ledger.gatesNotAdmitted().size)
    }

    @Test
    fun anInternallyConsistentPackStillStopsAtTheExternalGates() {
        val result = TaiwanReviewedRulePackGate.evaluate(
            pack = reviewedPack(),
            ledger = ExternalGateLedger(),
            conflictOfInterest = coi(),
            asOfIsoDate = TODAY,
        )

        assertEquals(ReviewedRulePackDecision.EXTERNAL_GATES_ABSENT, result.decision)
        assertTrue(result.blockers.isEmpty())
        assertEquals(ExternalGate.entries.size, result.absentGates.size)
        assertTrue(result.reviewNotes.any { it.contains("IMMUTABLE_OFFICIAL_SOURCE_BYTES") })
        assertTrue(result.reviewNotes.any { it.contains("QUALIFIED_CLINICAL_REVIEWER") })
    }

    @Test
    fun aGateThatIsMerelyDeclaredIsNotAdmitted() {
        val ledger = ExternalGateLedger(
            ExternalGate.entries.map { gate ->
                ExternalGateStatus(gate, ExternalGateState.DECLARED_NOT_VERIFIED)
            },
        )

        val result = TaiwanReviewedRulePackGate.evaluate(reviewedPack(), ledger, coi(), TODAY)

        assertEquals(ReviewedRulePackDecision.EXTERNAL_GATES_ABSENT, result.decision)
        assertTrue(result.reviewNotes.all { it.contains("DECLARED_NOT_VERIFIED") })
    }

    @Test
    fun theGateIsARealFunctionOfTheLedgerRatherThanAHardCodedRejection() {
        val result = TaiwanReviewedRulePackGate.evaluate(
            reviewedPack(),
            humanAdmittedLedger(),
            coi(),
            TODAY,
        )

        assertEquals(ReviewedRulePackDecision.ADMITTED, result.decision)
        assertTrue(result.absentGates.isEmpty())
    }

    @Test
    fun aDeterministicDefectIsRejectedBeforeExternalGatesAreConsidered() {
        val draft = reviewedPack().copy(status = RulePackStatus.DRAFT)

        val result = TaiwanReviewedRulePackGate.evaluate(draft, humanAdmittedLedger(), coi(), TODAY)

        assertEquals(ReviewedRulePackDecision.REJECTED, result.decision)
        assertTrue(result.blockers.any { it.contains("clinically reviewed pack") })
    }

    @Test
    fun anUnboundedEffectiveWindowIsRejected() {
        val unbounded = reviewedPack().copy(effectiveUntil = null)

        val result = TaiwanReviewedRulePackGate.evaluate(unbounded, humanAdmittedLedger(), coi(), TODAY)

        assertEquals(ReviewedRulePackDecision.REJECTED, result.decision)
        assertTrue(result.blockers.any { it.contains("bounded effective window") })
    }

    @Test
    fun aMissingConflictOfInterestDeclarationIsRejected() {
        val result = TaiwanReviewedRulePackGate.evaluate(
            reviewedPack(),
            humanAdmittedLedger(),
            conflictOfInterest = null,
            asOfIsoDate = TODAY,
        )

        assertEquals(ReviewedRulePackDecision.REJECTED, result.decision)
        assertTrue(result.blockers.any { it.contains("conflict-of-interest declaration") })
    }

    @Test
    fun aDeclaredInterestWithoutMitigationIsRejectedButAMitigatedOneIsNot() {
        val conflicted = coi().copy(hasFinancialInterest = true)

        val rejected = TaiwanReviewedRulePackGate.evaluate(
            reviewedPack(),
            humanAdmittedLedger(),
            conflicted,
            TODAY,
        )
        assertEquals(ReviewedRulePackDecision.REJECTED, rejected.decision)
        assertTrue(rejected.blockers.any { it.contains("mitigation") })

        val mitigated = TaiwanReviewedRulePackGate.evaluate(
            reviewedPack(),
            humanAdmittedLedger(),
            conflicted.copy(mitigationNote = "Recused from the affected ingredient rules."),
            TODAY,
        )
        assertEquals(ReviewedRulePackDecision.ADMITTED, mitigated.decision)
    }

    @Test
    fun aConflictOfInterestDeclarationFromAnotherReviewerIsRejected() {
        val result = TaiwanReviewedRulePackGate.evaluate(
            reviewedPack(),
            humanAdmittedLedger(),
            coi().copy(reviewerPseudonymousId = "reviewer-other"),
            TODAY,
        )

        assertEquals(ReviewedRulePackDecision.REJECTED, result.decision)
        assertTrue(result.blockers.any { it.contains("different reviewer") })
    }

    @Test
    fun anUnsignedConflictOfInterestDeclarationIsRejected() {
        val result = TaiwanReviewedRulePackGate.evaluate(
            reviewedPack(),
            humanAdmittedLedger(),
            coi().copy(declarationSignatureSha256 = null),
            TODAY,
        )

        assertEquals(ReviewedRulePackDecision.REJECTED, result.decision)
        assertTrue(result.blockers.any { it.contains("signature SHA-256") })
    }

    private fun humanAdmittedLedger() = ExternalGateLedger(
        ExternalGate.entries.map { gate -> ExternalGateStatus(gate, ExternalGateState.HUMAN_ADMITTED) },
    )

    private fun coi() = ConflictOfInterestDeclaration(
        reviewerPseudonymousId = REVIEWER_ID,
        declaredAtIsoDate = "2026-08-15",
        declarationSignatureSha256 = hash("coi"),
    )

    private fun source() = PrimarySourceSnapshot(
        sourceId = "source-a",
        sourceType = PrimarySourceType.REGULATOR_GUIDANCE,
        publisher = "Synthetic regulator",
        title = "Synthetic test guidance",
        canonicalUrl = "https://example.invalid/source-a",
        snapshotSha256 = hash("snapshot"),
        publishedDate = "2026-08-01",
        effectiveFrom = "2026-08-15",
        retrievedAt = "2026-08-15",
        licenseOrTerms = "Test fixture only",
    )

    private fun rule() = DeterministicSupplementRule(
        ruleId = "rule-a",
        condition = RuleConditionKind.MISSING_SERVING_SIZE,
        effect = DeterministicRuleEffect.REVIEW_REQUIRED,
        priority = 100,
        sourceIds = listOf("source-a"),
        userMessageKey = "missing-serving-review",
    )

    private fun reviewedPack(): TaiwanRulePackManifest {
        val rules = listOf(rule())
        return TaiwanRulePackManifest(
            packId = "tw-supplement",
            version = "1.0.0",
            status = RulePackStatus.CLINICALLY_REVIEWED,
            generatedAtIsoDate = "2026-08-15",
            effectiveFrom = "2026-08-15",
            effectiveUntil = "2027-08-15",
            contentSha256 = hash("content"),
            sources = listOf(source()),
            rules = rules,
            testCaseIds = TaiwanRulePackAdmissionValidator.requiredSafetyCaseIds,
            reviewerAttestation = ReviewerAttestation(
                reviewerPseudonymousId = REVIEWER_ID,
                qualification = "Synthetic qualified reviewer fixture",
                scope = "Test admission contract only",
                signedAtIsoDate = "2026-08-15",
                signatureSha256 = hash("signature"),
                userFacingWordingSha256 = hash("wording"),
                reviewedRuleIds = rules.map { it.ruleId }.toSet(),
            ),
            rollbackToVersion = "0.9.0",
        )
    }

    private fun hash(seed: String): String =
        seed.map { it.code }.sum().toString(16).padStart(64, '0').takeLast(64)

    private companion object {
        const val TODAY = "2026-08-18"
        const val REVIEWER_ID = "reviewer-test"
    }
}

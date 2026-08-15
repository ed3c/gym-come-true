package dev.ed3c.gymcometrue.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TaiwanSupplementEvidenceTest {
    @Test
    fun corpusFailsClosedForUnknownConsentAndUnencryptedImages() {
        val record = LabelCorpusRecord(
            recordId = "label-1",
            product = product(),
            rawTextSha256 = hash("a"),
            imageSha256 = hash("b"),
            consent = LabelCorpusConsent.UNKNOWN,
            retentionPolicy = LabelRetentionPolicy.DO_NOT_STORE_IMAGE,
            storesRawImage = true,
            encryptedAtRest = false,
            provenanceNote = "Test record",
        )

        val result = LabelCorpusAdmissionValidator.validate(record)

        assertEquals(EvidenceAdmission.DENY, result.admission)
        assertTrue(result.reasons.any { it.contains("Consent") })
        assertTrue(result.reasons.any { it.contains("encrypted") })
    }

    @Test
    fun syntheticNoImageCorpusRecordCanBeAdmitted() {
        val record = LabelCorpusRecord(
            recordId = "synthetic-1",
            product = product(),
            rawTextSha256 = hash("a"),
            consent = LabelCorpusConsent.SYNTHETIC,
            retentionPolicy = LabelRetentionPolicy.DO_NOT_STORE_IMAGE,
            provenanceNote = "Repository-authored synthetic label",
        )

        assertEquals(EvidenceAdmission.ALLOW, LabelCorpusAdmissionValidator.validate(record).admission)
    }

    @Test
    fun ocrMetricsSeparateFirstPassAccuracyFromCorrectionCompletion() {
        val metrics = OcrMetricCompiler.summarize(
            listOf(
                OcrFieldObservation(OcrFieldType.UNIT, expected = "毫克", observed = "毫克"),
                OcrFieldObservation(OcrFieldType.UNIT, expected = "微克", observed = "毫克", corrected = "微克"),
                OcrFieldObservation(OcrFieldType.UNIT, expected = "國際單位", observed = "國際單位l"),
            ),
        ).single()

        assertEquals(1, metrics.exactMatches)
        assertEquals(2, metrics.correctionsRequired)
        assertEquals(1, metrics.correctionsCompleted)
        assertEquals(1, metrics.unresolved)
        assertEquals(1.0 / 3.0, metrics.exactAccuracy)
        assertEquals(0.5, metrics.correctionCompletion)
    }

    @Test
    fun draftPackCanNeverBeAdmitted() {
        val draft = TaiwanRulePackManifest(
            packId = "tw-supplement",
            version = "0.1.0-draft",
            generatedAtIsoDate = "2026-08-15",
            sources = listOf(source(snapshotSha256 = null)),
        )

        val result = TaiwanRulePackAdmissionValidator.validate(draft, "2026-08-15")

        assertEquals(RulePackAdmission.REVIEW_REQUIRED, result.admission)
        assertTrue(result.reviewNotes.any { it.contains("never execute") })
    }

    @Test
    fun impossibleCalendarDatesAreRejected() {
        val draft = TaiwanRulePackManifest(
            packId = "tw-supplement",
            version = "0.1.0-draft",
            generatedAtIsoDate = "2026-02-30",
        )

        val result = TaiwanRulePackAdmissionValidator.validate(draft, "2026-13-01")

        assertEquals(RulePackAdmission.REJECTED, result.admission)
        assertTrue(result.blockers.any { it.contains("generatedAtIsoDate") })
        assertTrue(result.blockers.any { it.contains("validation date") })
    }

    @Test
    fun reviewedPackRequiresSourceHashesReviewerTestsAndRollback() {
        val incomplete = TaiwanRulePackManifest(
            packId = "tw-supplement",
            version = "1.0.0",
            status = RulePackStatus.CLINICALLY_REVIEWED,
            generatedAtIsoDate = "2026-08-15",
            effectiveFrom = "2026-08-15",
            sources = listOf(source(snapshotSha256 = null)),
            rules = listOf(rule()),
        )

        val result = TaiwanRulePackAdmissionValidator.validate(incomplete, "2026-08-15")

        assertEquals(RulePackAdmission.REJECTED, result.admission)
        assertTrue(result.blockers.any { it.contains("snapshot") })
        assertTrue(result.blockers.any { it.contains("attestation") })
        assertTrue(result.blockers.any { it.contains("safety cases") })
        assertTrue(result.blockers.any { it.contains("rollback") })
    }

    @Test
    fun conflictingRulesAreRejectedEvenWhenAttested() {
        val rules = listOf(
            rule(effect = DeterministicRuleEffect.REVIEW_REQUIRED, id = "rule-a"),
            rule(effect = DeterministicRuleEffect.BLOCK_AUTOMATION, id = "rule-b"),
        )
        val pack = reviewedPack(rules)

        val result = TaiwanRulePackAdmissionValidator.validate(pack, "2026-08-15")

        assertEquals(RulePackAdmission.REJECTED, result.admission)
        assertTrue(result.blockers.any { it.contains("Conflicting") })
    }

    @Test
    fun fullyAnchoredSyntheticManifestCanPassTheAdmissionContract() {
        val rules = listOf(rule())
        val pack = reviewedPack(rules)

        val result = TaiwanRulePackAdmissionValidator.validate(pack, "2026-08-15")

        assertEquals(RulePackAdmission.ADMITTED, result.admission)
        assertTrue(result.blockers.isEmpty())
    }

    @Test
    fun receiptPreservesEvidenceAndCannotAssignDecisionAuthorityToAModel() {
        val rules = listOf(rule())
        val pack = reviewedPack(rules)
        val evidence = ScanEvidence(
            rawTextSha256 = hash("e"),
            candidates = emptyList(),
            evidenceStatus = EvidenceStatus.VERIFIED_BY_REVIEWED_SOURCE,
        )
        val evaluation = SafetyEvaluation(
            decision = SafetyDecision.REVIEW_REQUIRED,
            reasons = listOf("Serving evidence is incomplete."),
        )

        val receipt = SupplementDecisionReceiptFactory.create(
            receiptId = "receipt-1",
            product = product(),
            evidence = evidence,
            evaluation = evaluation,
            rulePack = pack,
            triggeredRuleIds = listOf("rule-a"),
            confirmedAtIsoDate = "2026-08-15",
        )

        assertEquals(hash("e"), receipt.evidenceSha256)
        assertEquals("1.0.0", receipt.rulePackVersion)
        assertEquals(false, receipt.modelUsedForDecision)

        assertFailsWith<IllegalArgumentException> {
            receipt.copy(modelUsedForDecision = true)
        }
    }

    private fun product() = ProductVariantIdentity(
        productId = "synthetic-product",
        productName = "Synthetic Zinc",
        formulation = "capsule",
        labelRevision = "rev-1",
        labelEffectiveDate = "2026-08-15",
        serving = ServingDefinition(amount = 1.0, unit = "capsule"),
    )

    private fun source(snapshotSha256: String?) = PrimarySourceSnapshot(
        sourceId = "source-a",
        sourceType = PrimarySourceType.REGULATOR_GUIDANCE,
        publisher = "Synthetic regulator",
        title = "Synthetic test guidance",
        canonicalUrl = "https://example.invalid/source-a",
        snapshotSha256 = snapshotSha256,
        publishedDate = "2026-08-01",
        effectiveFrom = "2026-08-15",
        retrievedAt = "2026-08-15",
        licenseOrTerms = "Test fixture only",
    )

    private fun rule(
        effect: DeterministicRuleEffect = DeterministicRuleEffect.REVIEW_REQUIRED,
        id: String = "rule-a",
    ) = DeterministicSupplementRule(
        ruleId = id,
        condition = RuleConditionKind.MISSING_SERVING_SIZE,
        effect = effect,
        priority = 100,
        sourceIds = listOf("source-a"),
        userMessageKey = "missing-serving-review",
    )

    private fun reviewedPack(rules: List<DeterministicSupplementRule>): TaiwanRulePackManifest =
        TaiwanRulePackManifest(
            packId = "tw-supplement",
            version = "1.0.0",
            status = RulePackStatus.CLINICALLY_REVIEWED,
            generatedAtIsoDate = "2026-08-15",
            effectiveFrom = "2026-08-15",
            effectiveUntil = "2027-08-15",
            contentSha256 = hash("c"),
            sources = listOf(source(snapshotSha256 = hash("s"))),
            rules = rules,
            testCaseIds = TaiwanRulePackAdmissionValidator.requiredSafetyCaseIds,
            reviewerAttestation = ReviewerAttestation(
                reviewerPseudonymousId = "reviewer-test",
                qualification = "Synthetic qualified reviewer fixture",
                scope = "Test admission contract only",
                signedAtIsoDate = "2026-08-15",
                signatureSha256 = hash("r"),
                userFacingWordingSha256 = hash("w"),
                reviewedRuleIds = rules.map { it.ruleId }.toSet(),
            ),
            rollbackToVersion = "0.9.0",
        )

    private fun hash(value: String): String = value.firstOrNull()?.code.orZero().toString(16).padStart(64, '0').takeLast(64)

    private fun Int?.orZero(): Int = this ?: 0
}

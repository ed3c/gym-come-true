package dev.ed3c.gymcometrue.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DomainTest {
    @Test
    fun parserExtractsMassAndActivityUnitsAsUnverifiedCandidates() {
        val candidates = SupplementLabelParser.parse(
            """
            Creatine Monohydrate 5 g
            Vitamin D3 25 mcg
            Zinc: 15 mg
            Vitamin E 400 IU
            """.trimIndent(),
        )

        assertEquals(4, candidates.size)
        assertEquals(MassUnit.G, candidates[0].unit)
        assertEquals(MassUnit.MCG, candidates[1].unit)
        assertEquals(MassUnit.MG, candidates[2].unit)
        assertEquals(MassUnit.IU, candidates[3].unit)
        assertTrue(candidates.all { it.evidenceStatus == EvidenceStatus.UNVERIFIED })
    }

    @Test
    fun genericConversionSupportsOnlyCompatibleMassUnits() {
        assertEquals(0.025, MassUnitConverter.toMilligrams(25.0, MassUnit.MCG))
        assertEquals(15.0, MassUnitConverter.toMilligrams(15.0, MassUnit.MG))
        assertEquals(5_000.0, MassUnitConverter.toMilligrams(5.0, MassUnit.G))
        assertNull(MassUnitConverter.toMilligrams(400.0, MassUnit.IU))
    }

    @Test
    fun iuAndMedicationContextBlockAutomation() {
        val candidate = SupplementFactCandidate(
            ingredient = "Vitamin D3",
            amount = 1_000.0,
            unit = MassUnit.IU,
            rawUnit = "IU",
        )
        val result = SupplementSafetyEngine.evaluate(
            evidence = ScanEvidence(
                rawTextSha256 = "abc",
                candidates = listOf(candidate),
            ),
            context = SafetyContext(
                medicationUsedWithin72Hours = true,
                rulePackStatus = RulePackStatus.CLINICALLY_REVIEWED,
            ),
        )

        assertEquals(SafetyDecision.BLOCK_AUTOMATION, result.decision)
        assertTrue(result.reasons.any { it.contains("IU") })
        assertTrue(result.reasons.any { it.contains("medication", ignoreCase = true) })
    }

    @Test
    fun reviewedCompatibleEvidenceCanBeLoggedWithoutDoseAdvice() {
        val candidate = SupplementFactCandidate(
            ingredient = "Magnesium",
            amount = 100.0,
            unit = MassUnit.MG,
            rawUnit = "mg",
            evidenceStatus = EvidenceStatus.VERIFIED_BY_REVIEWED_SOURCE,
        )
        val evidence = ScanEvidence(
            rawTextSha256 = "abc",
            candidates = listOf(candidate),
            evidenceStatus = EvidenceStatus.VERIFIED_BY_REVIEWED_SOURCE,
        )
        val result = SupplementSafetyEngine.evaluate(
            evidence = evidence,
            context = SafetyContext(rulePackStatus = RulePackStatus.CLINICALLY_REVIEWED),
        )
        val payload = LlmExplanationBoundary.createPayload(evidence, result)

        assertEquals(SafetyDecision.LOG_ONLY, result.decision)
        assertEquals(100.0, result.normalizedMassMg["Magnesium"])
        assertFalse(payload.mayRecommendDose)
        assertFalse(payload.mayOverrideWarnings)
    }

    @Test
    fun lateWorkoutPlanPreservesCrossMidnightOrder() {
        val events = DailyProtocolCompiler.compile(TrainingVariant.NIGHT_2200)
        val training = events.first { it.id == "b-training" }
        val sleep = events.first { it.id == "b-sleep" }

        assertTrue(training.time.sortKey < sleep.time.sortKey)
        assertEquals(1, sleep.time.dayOffset)
        assertEquals(events.map { it.time.sortKey }.sorted(), events.map { it.time.sortKey })
    }
}

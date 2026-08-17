package dev.ed3c.gymcometrue.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Issue #25 (TW2) — field-level OCR evaluation contract. */
class TaiwanOcrEvaluationTest {
    @Test
    fun runLevelAccuracySeparatesFirstPassFromCorrectionCompletion() {
        val report = OcrEvaluationCompiler.compile(evaluationRun())

        assertEquals(2, report.recordCount)
        assertEquals(3, report.observationCount)
        assertEquals(2, report.fieldMetrics.size)
        assertEquals(1.0 / 3.0, report.firstPassExactAccuracy)
        assertEquals(0.5, report.correctionCompletion)
    }

    @Test
    fun aRunWithNothingToCorrectReportsNullCompletionRatherThanPerfectCompletion() {
        val perfect = evaluationRun().copy(
            records = listOf(
                OcrEvaluatedRecord(
                    corpusRecordId = "record-1",
                    consentState = ConsentState.ACTIVE,
                    observations = listOf(
                        OcrFieldObservation(OcrFieldType.UNIT, expected = "毫克", observed = "毫克"),
                    ),
                ),
            ),
        )

        val report = OcrEvaluationCompiler.compile(perfect)

        assertEquals(1.0, report.firstPassExactAccuracy)
        assertNull(report.correctionCompletion)
    }

    @Test
    fun aRunWithoutExactEngineDeviceAndModelVersionsIsRejected() {
        val unbound = evaluationRun().copy(
            binding = binding().copy(engineVersion = "", modelVersion = "", deviceModel = "", osVersion = ""),
        )

        val result = OcrEvaluationRunValidator.validate(unbound, TODAY)

        assertEquals(OcrEvaluationAdmission.REJECTED, result.admission)
        assertNull(result.report)
        assertTrue(result.blockers.any { it.contains("engine version") })
        assertTrue(result.blockers.any { it.contains("recognition model version") })
        assertTrue(result.blockers.any { it.contains("device model") })
        assertTrue(result.blockers.any { it.contains("OS version") })
    }

    @Test
    fun aRunThatDoesNotDeclareTraditionalChineseIsRejected() {
        val simplifiedOnly = evaluationRun().copy(
            binding = binding().copy(recognitionLanguages = listOf("zh-Hans")),
        )

        val result = OcrEvaluationRunValidator.validate(simplifiedOnly, TODAY)

        assertEquals(OcrEvaluationAdmission.REJECTED, result.admission)
        assertTrue(result.blockers.any { it.contains("Traditional Chinese") })
    }

    @Test
    fun everyNonActiveConsentStateBlocksEvaluationOfThatRecord() {
        listOf(
            ConsentState.UNKNOWN,
            ConsentState.UNVERIFIABLE,
            ConsentState.WITHDRAWN,
            ConsentState.EXPIRED,
        ).forEach { state ->
            val candidate = evaluationRun().copy(
                records = listOf(
                    OcrEvaluatedRecord(
                        corpusRecordId = "record-1",
                        consentState = state,
                        observations = listOf(
                            OcrFieldObservation(OcrFieldType.UNIT, expected = "毫克", observed = "毫克"),
                        ),
                    ),
                ),
            )

            val result = OcrEvaluationRunValidator.validate(candidate, TODAY)

            assertEquals(OcrEvaluationAdmission.REJECTED, result.admission, "consent=$state")
            assertTrue(result.blockers.any { it.contains("may not be evaluated") }, "consent=$state")
        }
    }

    @Test
    fun theAggregateReportCarriesNoLabelTextAndNoRecordIdentifiers() {
        val report = OcrEvaluationCompiler.compile(evaluationRun())

        assertTrue(OcrAggregateLeakScanner.scan(report).isEmpty())
        val strings = listOf(
            report.runId,
            report.engineVersion,
            report.modelVersion,
            report.deviceModel,
            report.osVersion,
        )
        assertTrue(strings.none { it.contains("record-") })
        assertTrue(strings.none { it.contains("毫克") })
    }

    @Test
    fun theLeakScannerCatchesLabelTextPathsAndUrisSmuggledIntoMetadata() {
        val leaky = OcrEvaluationCompiler.compile(
            evaluationRun().copy(
                binding = binding().copy(
                    deviceModel = "維生素D3 800 國際單位",
                    modelVersion = "/var/mobile/labels/img-001.jpg",
                    osVersion = "https://example.invalid/label.png",
                ),
            ),
        )

        val findings = OcrAggregateLeakScanner.scan(leaky)

        assertTrue(findings.any { it.startsWith("deviceModel") && it.contains("Han characters") })
        assertTrue(findings.any { it.startsWith("modelVersion") && it.contains("path") })
        assertTrue(findings.any { it.startsWith("osVersion") && it.contains("URI") })
    }

    @Test
    fun aLeakingRunIsRejectedAndPublishesNoReport() {
        val leaky = evaluationRun().copy(binding = binding().copy(deviceModel = "維生素D3"))

        val result = OcrEvaluationRunValidator.validate(leaky, TODAY)

        assertEquals(OcrEvaluationAdmission.REJECTED, result.admission)
        assertNull(result.report)
    }

    @Test
    fun aRunThatNeverTouchedARealDeviceCannotBeAdmitted() {
        val result = OcrEvaluationRunValidator.validate(evaluationRun(), TODAY)

        assertEquals(OcrEvaluationAdmission.REVIEW_REQUIRED, result.admission)
        assertTrue(result.blockers.isEmpty())
        assertTrue(result.reviewNotes.any { it.contains("real device") })
        assertEquals(false, result.report?.executedOnRealDevice)
    }

    @Test
    fun aFullyBoundRunOnAnAuthorizedDeviceIsAdmitted() {
        val result = OcrEvaluationRunValidator.validate(
            evaluationRun().copy(binding = binding(executedOnRealDevice = true)),
            TODAY,
        )

        assertEquals(OcrEvaluationAdmission.ADMITTED, result.admission)
        assertTrue(result.blockers.isEmpty())
    }

    @Test
    fun duplicateEvaluatedRecordsAreRejected() {
        val duplicated = evaluationRun().copy(
            records = evaluationRun().records.map { it.copy(corpusRecordId = "record-1") },
        )

        val result = OcrEvaluationRunValidator.validate(duplicated, TODAY)

        assertEquals(OcrEvaluationAdmission.REJECTED, result.admission)
        assertTrue(result.blockers.any { it.contains("Duplicate evaluated record ids") })
    }

    private fun binding(executedOnRealDevice: Boolean = false) = OcrEngineBinding(
        engine = OcrEngine.ANDROID_ML_KIT_TEXT_RECOGNITION,
        engineVersion = "16.0.1",
        modelVersion = "chinese-2026-06",
        deviceModel = "Pixel 8",
        osVersion = "Android 16",
        recognitionLanguages = listOf("zh-Hant-TW"),
        executedOnRealDevice = executedOnRealDevice,
    )

    private fun evaluationRun() = OcrEvaluationRun(
        runId = "run-1",
        binding = binding(),
        evaluatedAtIsoDate = "2026-08-18",
        records = listOf(
            OcrEvaluatedRecord(
                corpusRecordId = "record-1",
                consentState = ConsentState.ACTIVE,
                observations = listOf(
                    OcrFieldObservation(OcrFieldType.UNIT, expected = "毫克", observed = "毫克"),
                    OcrFieldObservation(
                        OcrFieldType.AMOUNT,
                        expected = "15",
                        observed = "1S",
                        corrected = "15",
                    ),
                ),
            ),
            OcrEvaluatedRecord(
                corpusRecordId = "record-2",
                consentState = ConsentState.ACTIVE,
                observations = listOf(
                    OcrFieldObservation(OcrFieldType.UNIT, expected = "微克", observed = "毫克"),
                ),
            ),
        ),
    )

    private companion object {
        const val TODAY = "2026-08-18"
    }
}

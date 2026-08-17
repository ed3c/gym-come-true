package dev.ed3c.gymcometrue.domain

import kotlinx.serialization.Serializable

/**
 * Issue #25 (TW2) — field-level ML Kit and Apple Vision OCR evaluation contract.
 *
 * State transition delivered here: `CONSENT_CONTRACT_DRAFT -> OCR_EVALUATION_DRAFT`.
 *
 * Three invariants drive this file:
 *
 * 1. An accuracy number without an engine, model, device, and OS binding is not a measurement.
 *    A run that cannot name what produced it cannot be compared against another run.
 * 2. First-pass exact accuracy and user-corrected completion are separate numbers, and correction
 *    never repairs the first-pass number. [OcrMetricCompiler] already enforces this per field; this
 *    file carries it up to the run level.
 * 3. The aggregate that leaves the device carries counts and version strings only. Raw label text,
 *    images, and per-record identifiers are structurally absent from [OcrEvaluationReport] — there
 *    is no field to put them in — and [OcrAggregateLeakScanner] checks the strings that remain.
 *
 * Real device execution and a real consented corpus are external gates. Neither is present in this
 * repository, so no run defined here can reach `ADMITTED` from repository evidence alone.
 */

@Serializable
enum class OcrEngine {
    ANDROID_ML_KIT_TEXT_RECOGNITION,
    APPLE_VISION_RECOGNIZE_TEXT,
}

/**
 * Exactly what produced a set of observations.
 *
 * The version fields are plain strings rather than a parsed type because the honest failure mode is
 * "the harness did not report it", which shows up as blank and is rejected by
 * [OcrEvaluationRunValidator]. Blank is not defaulted to "unknown version" and silently accepted.
 */
@Serializable
data class OcrEngineBinding(
    val engine: OcrEngine,
    val engineVersion: String = "",
    val modelVersion: String = "",
    val deviceModel: String = "",
    val osVersion: String = "",
    val recognitionLanguages: List<String> = emptyList(),
    val executedOnRealDevice: Boolean = false,
)

@Serializable
data class OcrEvaluatedRecord(
    val corpusRecordId: String,
    val consentState: ConsentState = ConsentState.UNKNOWN,
    val observations: List<OcrFieldObservation> = emptyList(),
) {
    init {
        require(corpusRecordId.isNotBlank()) { "An evaluated record requires a corpus record id." }
    }
}

@Serializable
data class OcrEvaluationRun(
    val runId: String,
    val binding: OcrEngineBinding,
    val evaluatedAtIsoDate: String,
    val records: List<OcrEvaluatedRecord> = emptyList(),
) {
    init {
        require(runId.isNotBlank()) { "An evaluation run requires a run id." }
    }
}

/**
 * The only artifact that may leave the evaluation device.
 *
 * Note what is absent and cannot be added by a caller: expected text, observed text, corrected
 * text, corpus record identifiers, file paths, and image references. [recordCount] is a count, not
 * a list.
 */
@Serializable
data class OcrEvaluationReport(
    val runId: String,
    val engine: OcrEngine,
    val engineVersion: String,
    val modelVersion: String,
    val deviceModel: String,
    val osVersion: String,
    val evaluatedAtIsoDate: String,
    val executedOnRealDevice: Boolean,
    val recordCount: Int,
    val observationCount: Int,
    val fieldMetrics: List<OcrFieldMetric>,
    val firstPassExactAccuracy: Double,
    val correctionCompletion: Double?,
)

object OcrEvaluationCompiler {
    /**
     * Projects a run into its aggregate report.
     *
     * The run-level numbers are recomputed from the raw observations rather than averaged from
     * [OcrFieldMetric] rows, because averaging per-field rates would weight a field with three
     * samples the same as a field with three hundred.
     */
    fun compile(run: OcrEvaluationRun): OcrEvaluationReport {
        val observations = run.records.flatMap { it.observations }
        val metrics = OcrMetricCompiler.summarize(observations)
        val total = metrics.sumOf { it.total }
        val exact = metrics.sumOf { it.exactMatches }
        val correctionsRequired = metrics.sumOf { it.correctionsRequired }
        val correctionsCompleted = metrics.sumOf { it.correctionsCompleted }
        return OcrEvaluationReport(
            runId = run.runId,
            engine = run.binding.engine,
            engineVersion = run.binding.engineVersion,
            modelVersion = run.binding.modelVersion,
            deviceModel = run.binding.deviceModel,
            osVersion = run.binding.osVersion,
            evaluatedAtIsoDate = run.evaluatedAtIsoDate,
            executedOnRealDevice = run.binding.executedOnRealDevice,
            recordCount = run.records.size,
            observationCount = total,
            fieldMetrics = metrics,
            firstPassExactAccuracy = if (total == 0) 0.0 else exact.toDouble() / total,
            correctionCompletion = if (correctionsRequired == 0) {
                null
            } else {
                correctionsCompleted.toDouble() / correctionsRequired
            },
        )
    }
}

/**
 * Deterministic check that an aggregate report carries no label content or identifying path.
 *
 * The report type already makes raw observations unrepresentable. This scanner covers the residual
 * surface — the free-text version and identifier strings — where a harness could smuggle a file
 * path, a data URI, or a slice of the label itself.
 */
object OcrAggregateLeakScanner {
    private val han = Regex("[㐀-䶿一-鿿]")

    fun scan(report: OcrEvaluationReport): List<String> {
        val findings = mutableListOf<String>()
        val fields = listOf(
            "runId" to report.runId,
            "engineVersion" to report.engineVersion,
            "modelVersion" to report.modelVersion,
            "deviceModel" to report.deviceModel,
            "osVersion" to report.osVersion,
            "evaluatedAtIsoDate" to report.evaluatedAtIsoDate,
        )
        fields.forEach { (name, value) ->
            if (han.containsMatchIn(value)) {
                findings += "$name contains Han characters; aggregate output must not carry label text."
            }
            if (value.contains('/') || value.contains('\\')) {
                findings += "$name looks like a filesystem path; aggregate output must not carry paths."
            }
            if (value.contains("://") || value.startsWith("data:")) {
                findings += "$name contains a URI; aggregate output must not carry image or file references."
            }
            if (value.length > 64) {
                findings += "$name is longer than 64 characters and may carry embedded content."
            }
        }
        return findings.distinct()
    }
}

@Serializable
enum class OcrEvaluationAdmission {
    REJECTED,
    REVIEW_REQUIRED,
    ADMITTED,
}

@Serializable
data class OcrEvaluationValidationResult(
    val admission: OcrEvaluationAdmission,
    val report: OcrEvaluationReport?,
    val blockers: List<String>,
    val reviewNotes: List<String>,
)

object OcrEvaluationRunValidator {
    /** A run must declare that it recognized Traditional Chinese, not merely "Chinese". */
    private val traditionalChineseTags = setOf("zh-hant", "zh-hant-tw", "zh-tw")

    fun validate(run: OcrEvaluationRun, asOfIsoDate: String): OcrEvaluationValidationResult {
        val blockers = mutableListOf<String>()
        val reviewNotes = mutableListOf<String>()

        val evaluatedAt = run.evaluatedAtIsoDate.taiwanIsoDateKey()
        val asOf = asOfIsoDate.taiwanIsoDateKey()
        if (evaluatedAt == null) blockers += "evaluatedAtIsoDate must be a valid YYYY-MM-DD date."
        if (asOf == null) blockers += "The validation date must be a valid YYYY-MM-DD date."
        if (evaluatedAt != null && asOf != null && evaluatedAt > asOf) {
            blockers += "The run is dated after the validation date."
        }

        val binding = run.binding
        if (binding.engineVersion.isBlank()) blockers += "The run must bind an exact engine version."
        if (binding.modelVersion.isBlank()) blockers += "The run must bind an exact recognition model version."
        if (binding.deviceModel.isBlank()) blockers += "The run must bind the exact device model."
        if (binding.osVersion.isBlank()) blockers += "The run must bind the exact OS version."
        if (binding.recognitionLanguages.none { it.lowercase() in traditionalChineseTags }) {
            blockers += "The run must declare a Traditional Chinese recognition language."
        }

        if (run.records.isEmpty()) blockers += "An evaluation run requires at least one evaluated record."
        val duplicateIds = run.records
            .groupingBy { it.corpusRecordId }
            .eachCount()
            .filterValues { it > 1 }
            .keys
        if (duplicateIds.isNotEmpty()) blockers += "Duplicate evaluated record ids: ${duplicateIds.sorted()}."

        run.records.forEach { record ->
            if (record.observations.isEmpty()) {
                blockers += "Record ${record.corpusRecordId} contributed no field observations."
            }
            if (record.consentState != ConsentState.ACTIVE) {
                blockers += "Record ${record.corpusRecordId} has ${record.consentState.name} consent " +
                    "and may not be evaluated."
            }
        }

        val report = OcrEvaluationCompiler.compile(run)
        blockers += OcrAggregateLeakScanner.scan(report)

        if (!binding.executedOnRealDevice) {
            reviewNotes += "The run did not execute on an authorized real device; it is a contract " +
                "exercise, not a measurement of ML Kit or Apple Vision."
        }

        val admission = when {
            blockers.isNotEmpty() -> OcrEvaluationAdmission.REJECTED
            reviewNotes.isNotEmpty() -> OcrEvaluationAdmission.REVIEW_REQUIRED
            else -> OcrEvaluationAdmission.ADMITTED
        }
        return OcrEvaluationValidationResult(
            admission = admission,
            report = if (admission == OcrEvaluationAdmission.REJECTED) null else report,
            blockers = blockers.distinct(),
            reviewNotes = reviewNotes.distinct(),
        )
    }
}

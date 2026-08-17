package dev.ed3c.gymcometrue.health

import dev.ed3c.gymcometrue.domain.EvidenceStatus
import dev.ed3c.gymcometrue.domain.ScanEvidence
import dev.ed3c.gymcometrue.domain.SupplementLabelParser
import kotlinx.serialization.Serializable

/**
 * Contract for the canonical iOS native capture bridge (Issue #27).
 *
 * The native side owns pixels, permissions, and Vision. This module owns the
 * decision about what may enter the shared evidence ledger. Nothing here can
 * raise evidence above [EvidenceStatus.UNVERIFIED]; only an explicit user
 * confirmation or a reviewed source may do that, elsewhere.
 */
@Serializable
enum class EvidenceCaptureSource {
    PHOTO_LIBRARY,
    CAMERA,
}

/** Mirrors the platform permission states that iOS can actually report. */
@Serializable
enum class CaptureAuthorization {
    NOT_DETERMINED,
    DENIED,
    RESTRICTED,
    LIMITED,
    AUTHORIZED,
}

/**
 * Raw pixel lifecycle declared by the native caller.
 *
 * [PERSISTED_LOCAL] is rejected: retention needs consent, encryption, expiry,
 * deletion, withdrawal, hashes, and provenance, none of which exist yet.
 */
@Serializable
enum class RawPixelRetention {
    TEMPORARY_IN_MEMORY,
    TEMPORARY_FILE_DELETED_AFTER_SCAN,
    PERSISTED_LOCAL,
}

@Serializable
data class NativeScanPayload(
    val source: EvidenceCaptureSource,
    val authorization: CaptureAuthorization,
    val recognizedText: String,
    val rawTextSha256: String,
    val barcode: String?,
    val rawPixelRetention: RawPixelRetention,
)

@Serializable
data class EvidenceHandoffResult(
    val accepted: Boolean,
    val summary: String,
    val evidence: ScanEvidence?,
    val rejections: List<String>,
)

object EvidenceHandoff {
    const val REJECTED_AUTHORIZATION = "CAPTURE_AUTHORIZATION_NOT_GRANTED"
    const val REJECTED_RETENTION = "RAW_PIXEL_RETENTION_NOT_ADMITTED"
    const val REJECTED_DIGEST_SHAPE = "DIGEST_SHAPE_INVALID"
    const val REJECTED_NO_CONTENT = "NO_RECOGNIZED_CONTENT"
    const val REJECTED_UNKNOWN_SOURCE = "UNKNOWN_CAPTURE_SOURCE"
    const val REJECTED_UNKNOWN_AUTHORIZATION = "UNKNOWN_CAPTURE_AUTHORIZATION"
    const val REJECTED_UNKNOWN_RETENTION = "UNKNOWN_RAW_PIXEL_RETENTION"

    /**
     * The digest is produced on device by CryptoKit. Shared code verifies its
     * shape only; recomputation would need a multiplatform SHA-256 and stays
     * NOT_IMPLEMENTED rather than being claimed.
     */
    private val digestShape = Regex("[0-9a-f]{64}")

    fun accept(payload: NativeScanPayload): EvidenceHandoffResult {
        val rejections = mutableListOf<String>()

        if (payload.authorization != CaptureAuthorization.AUTHORIZED &&
            payload.authorization != CaptureAuthorization.LIMITED
        ) {
            rejections += REJECTED_AUTHORIZATION
        }
        if (payload.rawPixelRetention == RawPixelRetention.PERSISTED_LOCAL) {
            rejections += REJECTED_RETENTION
        }
        if (!digestShape.matches(payload.rawTextSha256)) {
            rejections += REJECTED_DIGEST_SHAPE
        }

        val text = payload.recognizedText.trim()
        val barcode = payload.barcode?.trim()?.takeIf { it.isNotEmpty() }
        if (text.isEmpty() && barcode == null) {
            rejections += REJECTED_NO_CONTENT
        }

        if (rejections.isNotEmpty()) {
            return rejected(rejections)
        }

        val candidates = SupplementLabelParser.parse(text)
            .map { it.copy(evidenceStatus = EvidenceStatus.UNVERIFIED) }
        val warnings = buildList {
            add("OCR output is unverified evidence; confirm every field against the physical label.")
            add("The text digest was produced on device and is not recomputed in shared code.")
            if (barcode != null) {
                add("A barcode identifies a candidate product only; it does not verify contents.")
            }
            if (candidates.isEmpty()) {
                add("No ingredient-and-amount pair could be read; enter the label manually or rescan.")
            }
            add("Raw pixels stayed ${payload.rawPixelRetention.name}; no dose was calculated.")
        }

        val evidence = ScanEvidence(
            rawTextSha256 = payload.rawTextSha256,
            barcode = barcode,
            candidates = candidates,
            evidenceStatus = EvidenceStatus.UNVERIFIED,
            warnings = warnings,
        )
        val summary = "Captured ${candidates.size} unverified candidate(s) from " +
            "${payload.source.name} (digest ${payload.rawTextSha256.take(12)}…). " +
            "Confirm against the physical label; no dose was calculated."

        return EvidenceHandoffResult(
            accepted = true,
            summary = summary,
            evidence = evidence,
            rejections = emptyList(),
        )
    }

    /**
     * Primitive-only entry point for the Swift bridge. Unknown identifiers fail
     * closed instead of throwing across the Objective-C boundary.
     */
    fun acceptFromNative(
        sourceId: String,
        authorizationId: String,
        recognizedText: String,
        rawTextSha256: String,
        barcode: String?,
        retentionId: String,
    ): EvidenceHandoffResult {
        val source = EvidenceCaptureSource.entries.firstOrNull { it.name == sourceId }
        val authorization = CaptureAuthorization.entries.firstOrNull { it.name == authorizationId }
        val retention = RawPixelRetention.entries.firstOrNull { it.name == retentionId }

        val unknown = buildList {
            if (source == null) add(REJECTED_UNKNOWN_SOURCE)
            if (authorization == null) add(REJECTED_UNKNOWN_AUTHORIZATION)
            if (retention == null) add(REJECTED_UNKNOWN_RETENTION)
        }
        if (source == null || authorization == null || retention == null) {
            return rejected(unknown)
        }

        return accept(
            NativeScanPayload(
                source = source,
                authorization = authorization,
                recognizedText = recognizedText,
                rawTextSha256 = rawTextSha256,
                barcode = barcode,
                rawPixelRetention = retention,
            ),
        )
    }

    private fun rejected(rejections: List<String>): EvidenceHandoffResult = EvidenceHandoffResult(
        accepted = false,
        summary = "Evidence was not accepted: ${rejections.joinToString(", ")}.",
        evidence = null,
        rejections = rejections,
    )
}

package dev.ed3c.gymcometrue.health

import dev.ed3c.gymcometrue.domain.EvidenceStatus
import dev.ed3c.gymcometrue.domain.MassUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val DIGEST = "6f1b2c3d4e5a60718293a4b5c6d7e8f90a1b2c3d4e5f60718293a4b5c6d7e8f9"

class EvidenceHandoffTest {
    private fun payload(
        source: EvidenceCaptureSource = EvidenceCaptureSource.PHOTO_LIBRARY,
        authorization: CaptureAuthorization = CaptureAuthorization.AUTHORIZED,
        recognizedText: String = "Zinc 15 mg\nCreatine Monohydrate 5 g",
        digest: String = DIGEST,
        barcode: String? = null,
        retention: RawPixelRetention = RawPixelRetention.TEMPORARY_IN_MEMORY,
    ) = NativeScanPayload(
        source = source,
        authorization = authorization,
        recognizedText = recognizedText,
        rawTextSha256 = digest,
        barcode = barcode,
        rawPixelRetention = retention,
    )

    @Test
    fun acceptedEvidenceStaysUnverifiedAndCarriesConfirmationWarnings() {
        val result = EvidenceHandoff.accept(payload(barcode = "4712345678901"))
        val evidence = assertNotNull(result.evidence)

        assertTrue(result.accepted)
        assertEquals(EvidenceStatus.UNVERIFIED, evidence.evidenceStatus)
        assertTrue(evidence.candidates.all { it.evidenceStatus == EvidenceStatus.UNVERIFIED })
        assertEquals(2, evidence.candidates.size)
        assertEquals(MassUnit.MG, evidence.candidates[0].unit)
        assertTrue(evidence.warnings.any { it.contains("physical label") })
        assertTrue(evidence.warnings.any { it.contains("barcode", ignoreCase = true) })
        assertTrue(evidence.warnings.any { it.contains("not recomputed") })
        assertTrue(result.summary.contains("unverified candidate"))
        assertTrue(result.summary.contains("no dose was calculated"))
    }

    @Test
    fun deniedOrUndeterminedCaptureAuthorizationFailsClosed() {
        for (state in listOf(
            CaptureAuthorization.DENIED,
            CaptureAuthorization.RESTRICTED,
            CaptureAuthorization.NOT_DETERMINED,
        )) {
            val result = EvidenceHandoff.accept(payload(authorization = state))
            assertFalse(result.accepted, "authorization $state must not produce evidence")
            assertNull(result.evidence)
            assertTrue(EvidenceHandoff.REJECTED_AUTHORIZATION in result.rejections)
        }
    }

    @Test
    fun limitedPhotoAccessIsStillAnExplicitUserChoice() {
        val result = EvidenceHandoff.accept(payload(authorization = CaptureAuthorization.LIMITED))
        assertTrue(result.accepted)
    }

    @Test
    fun persistedRawPixelsAreRejectedUntilRetentionIsAdmitted() {
        val result = EvidenceHandoff.accept(payload(retention = RawPixelRetention.PERSISTED_LOCAL))

        assertFalse(result.accepted)
        assertTrue(EvidenceHandoff.REJECTED_RETENTION in result.rejections)
    }

    @Test
    fun malformedOrAbsentDigestIsRejectedRatherThanRepaired() {
        for (digest in listOf("", "not-a-digest", DIGEST.uppercase(), DIGEST.dropLast(1))) {
            val result = EvidenceHandoff.accept(payload(digest = digest))
            assertFalse(result.accepted, "digest '$digest' must fail closed")
            assertTrue(EvidenceHandoff.REJECTED_DIGEST_SHAPE in result.rejections)
        }
    }

    @Test
    fun emptyRecognitionIsRejectedButBarcodeOnlyCaptureSurvives() {
        val nothing = EvidenceHandoff.accept(payload(recognizedText = "   ", barcode = "  "))
        assertFalse(nothing.accepted)
        assertTrue(EvidenceHandoff.REJECTED_NO_CONTENT in nothing.rejections)

        val barcodeOnly = EvidenceHandoff.accept(payload(recognizedText = "", barcode = "4712345678901"))
        val evidence = assertNotNull(barcodeOnly.evidence)
        assertTrue(barcodeOnly.accepted)
        assertTrue(evidence.candidates.isEmpty())
        assertTrue(evidence.warnings.any { it.contains("No ingredient-and-amount pair") })
    }

    @Test
    fun unknownNativeIdentifiersFailClosedInsteadOfThrowing() {
        val result = EvidenceHandoff.acceptFromNative(
            sourceId = "SCREENSHOT",
            authorizationId = "AUTHORIZED",
            recognizedText = "Zinc 15 mg",
            rawTextSha256 = DIGEST,
            barcode = null,
            retentionId = "FOREVER",
        )

        assertFalse(result.accepted)
        assertTrue(EvidenceHandoff.REJECTED_UNKNOWN_SOURCE in result.rejections)
        assertTrue(EvidenceHandoff.REJECTED_UNKNOWN_RETENTION in result.rejections)
    }

    @Test
    fun nativeSeamAgreesWithTheTypedContract() {
        val typed = EvidenceHandoff.accept(payload(source = EvidenceCaptureSource.CAMERA))
        val fromNative = EvidenceHandoff.acceptFromNative(
            sourceId = "CAMERA",
            authorizationId = "AUTHORIZED",
            recognizedText = "Zinc 15 mg\nCreatine Monohydrate 5 g",
            rawTextSha256 = DIGEST,
            barcode = null,
            retentionId = "TEMPORARY_IN_MEMORY",
        )

        assertEquals(typed, fromNative)
    }
}

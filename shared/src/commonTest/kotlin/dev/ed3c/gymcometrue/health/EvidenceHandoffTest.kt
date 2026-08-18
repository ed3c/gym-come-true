package dev.ed3c.gymcometrue.health

import dev.ed3c.gymcometrue.domain.EvidenceStatus
import dev.ed3c.gymcometrue.domain.MassUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** sha256("Zinc 15 mg\nCreatine Monohydrate 5 g"), the default [payload] text below. */
private const val DIGEST = "9dd09ce9e259cfd75c112f0c7d00dfaa1db303274bb85063da5163c6446b2086"

/** sha256(""), needed whenever a payload's recognized text trims to empty. */
private const val EMPTY_TEXT_DIGEST = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"

/** NIST/FIPS 180-4 known-answer vector: sha256("abc"). */
private const val SHA256_ABC = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"

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
        assertTrue(evidence.warnings.any { it.contains("recomputed") && it.contains("matched") })
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
    fun declaredDigestNotMatchingTheRecomputedOneFailsClosed() {
        // Valid 64-char hex shape, but not sha256 of the recognized text: the
        // shape check alone would have let this through before Issue #53.
        val wrongButWellShaped = EMPTY_TEXT_DIGEST
        val result = EvidenceHandoff.accept(payload(digest = wrongButWellShaped))

        assertFalse(result.accepted)
        assertNull(result.evidence)
        assertTrue(EvidenceHandoff.REJECTED_DIGEST_MISMATCH in result.rejections)
        assertTrue(EvidenceHandoff.REJECTED_DIGEST_SHAPE !in result.rejections)
    }

    @Test
    fun recomputationAgreesWithAKnownShaVector() {
        val accepted = EvidenceHandoff.accept(payload(recognizedText = "abc", digest = SHA256_ABC))
        assertTrue(accepted.accepted)

        val flipped = SHA256_ABC.replaceFirstChar { if (it == 'b') 'c' else 'b' }
        val rejected = EvidenceHandoff.accept(payload(recognizedText = "abc", digest = flipped))
        assertFalse(rejected.accepted)
        assertTrue(EvidenceHandoff.REJECTED_DIGEST_MISMATCH in rejected.rejections)
    }

    @Test
    fun emptyRecognitionIsRejectedButBarcodeOnlyCaptureSurvives() {
        val nothing = EvidenceHandoff.accept(payload(recognizedText = "   ", barcode = "  "))
        assertFalse(nothing.accepted)
        assertTrue(EvidenceHandoff.REJECTED_NO_CONTENT in nothing.rejections)

        val barcodeOnly = EvidenceHandoff.accept(
            payload(recognizedText = "", digest = EMPTY_TEXT_DIGEST, barcode = "4712345678901"),
        )
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

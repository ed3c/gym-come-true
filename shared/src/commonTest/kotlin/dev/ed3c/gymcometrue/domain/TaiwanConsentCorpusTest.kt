package dev.ed3c.gymcometrue.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Issue #24 (TW1) — consent semantics and deletion completeness. */
class TaiwanConsentCorpusTest {
    @Test
    fun everyConsentStateIsProducedByTheResolver() {
        assertEquals(
            ConsentState.UNKNOWN,
            ConsentResolver.resolve(null, TODAY).state,
        )
        assertEquals(
            ConsentState.ACTIVE,
            ConsentResolver.resolve(grant(), TODAY).state,
        )
        assertEquals(
            ConsentState.EXPIRED,
            ConsentResolver.resolve(grant(), "2027-01-01").state,
        )
        assertEquals(
            ConsentState.WITHDRAWN,
            ConsentResolver.resolve(grant().copy(withdrawnAtIsoDate = "2026-06-01"), TODAY).state,
        )
        assertEquals(
            ConsentState.UNVERIFIABLE,
            ConsentResolver.resolve(grant().copy(consentReceiptSha256 = null), TODAY).state,
        )
    }

    @Test
    fun consentWithoutABoundedWindowIsUnverifiableRatherThanActive() {
        val resolution = ConsentResolver.resolve(grant().copy(expiresAtIsoDate = null), TODAY)

        assertEquals(ConsentState.UNVERIFIABLE, resolution.state)
        assertTrue(resolution.reasons.any { it.contains("unbounded") })
    }

    @Test
    fun anUnparsableEvaluationDateCannotResolveToActiveConsent() {
        assertEquals(ConsentState.UNVERIFIABLE, ConsentResolver.resolve(grant(), "2026-02-30").state)
    }

    @Test
    fun withdrawnExpiredAndUnknownConsentAllDenyCorpusUse() {
        val denied = listOf(
            null,
            grant().copy(withdrawnAtIsoDate = "2026-06-01"),
            grant().copy(expiresAtIsoDate = "2026-06-01"),
            grant().copy(consentTextSha256 = null),
        )

        denied.forEach { candidate ->
            val decision = ConsentedCorpusAdmissionValidator.validate(
                ConsentedCorpusRecord(record = optInRecord(), grant = candidate),
                ConsentScope.OCR_EVALUATION,
                TODAY,
            )
            assertEquals(EvidenceAdmission.DENY, decision.admission, "state=${decision.consentState}")
        }
    }

    @Test
    fun consentGrantedForOnePurposeDoesNotAuthorizeAnother() {
        val decision = ConsentedCorpusAdmissionValidator.validate(
            ConsentedCorpusRecord(
                record = optInRecord(),
                grant = grant().copy(scopes = setOf(ConsentScope.RULE_PACK_TESTING)),
            ),
            ConsentScope.OCR_EVALUATION,
            TODAY,
        )

        assertEquals(EvidenceAdmission.DENY, decision.admission)
        assertEquals(ConsentState.ACTIVE, decision.consentState)
        assertTrue(decision.reasons.any { it.contains("does not cover OCR_EVALUATION") })
    }

    @Test
    fun syntheticRepositoryRecordsNeedNoHumanGrant() {
        val decision = ConsentedCorpusAdmissionValidator.validate(
            ConsentedCorpusRecord(record = syntheticRecord()),
            ConsentScope.OCR_EVALUATION,
            TODAY,
        )

        assertEquals(EvidenceAdmission.ALLOW, decision.admission)
        assertEquals(ConsentState.ACTIVE, decision.consentState)
    }

    @Test
    fun anOpenDeletionRequestDeniesUseEvenWhileConsentIsActive() {
        val decision = ConsentedCorpusAdmissionValidator.validate(
            ConsentedCorpusRecord(
                record = optInRecord(),
                grant = grant(),
                deletionRequest = CorpusDeletionRequest(
                    requestId = "deletion-1",
                    subjectPseudonymousId = "subject-1",
                    requestedAtIsoDate = "2026-08-17",
                    declaredLocations = setOf(CorpusStorageLocation.OBJECT_STORAGE),
                    manifestUpdated = true,
                ),
            ),
            ConsentScope.OCR_EVALUATION,
            TODAY,
        )

        assertEquals(EvidenceAdmission.DENY, decision.admission)
        assertEquals(ConsentState.ACTIVE, decision.consentState)
        assertTrue(decision.reasons.any { it.contains("deletion request is open") })
    }

    @Test
    fun deletionCannotBeManifestOnly() {
        val result = CorpusDeletionValidator.validate(
            CorpusDeletionRequest(
                requestId = "deletion-1",
                subjectPseudonymousId = "subject-1",
                requestedAtIsoDate = "2026-08-17",
                declaredLocations = setOf(
                    CorpusStorageLocation.OBJECT_STORAGE,
                    CorpusStorageLocation.DATABASE_ROW,
                ),
                manifestUpdated = true,
            ),
            TODAY,
        )

        assertEquals(DeletionCompleteness.MANIFEST_ONLY, result.completeness)
        assertTrue(result.blockers.any { it.contains("manifest-only") })
        assertEquals(2, result.unverifiedLocations.size)
    }

    @Test
    fun aDeleteJobThatRanButWasNeverVerifiedIsNotDeletion() {
        val result = CorpusDeletionValidator.validate(
            CorpusDeletionRequest(
                requestId = "deletion-2",
                subjectPseudonymousId = "subject-1",
                requestedAtIsoDate = "2026-08-17",
                declaredLocations = setOf(CorpusStorageLocation.OBJECT_STORAGE),
                receipts = listOf(
                    receipt(CorpusStorageLocation.OBJECT_STORAGE).copy(verifiedAbsent = false),
                ),
            ),
            TODAY,
        )

        assertEquals(DeletionCompleteness.NOT_STARTED, result.completeness)
        assertTrue(result.blockers.any { it.contains("does not confirm the data is absent") })
    }

    @Test
    fun derivedOcrMetricsMustBeDeclaredWhenTheRecordWasEvaluated() {
        val result = CorpusDeletionValidator.validate(
            CorpusDeletionRequest(
                requestId = "deletion-3",
                subjectPseudonymousId = "subject-1",
                requestedAtIsoDate = "2026-08-17",
                declaredLocations = setOf(CorpusStorageLocation.OBJECT_STORAGE),
                usedForOcrEvaluation = true,
                receipts = listOf(receipt(CorpusStorageLocation.OBJECT_STORAGE)),
            ),
            TODAY,
        )

        assertTrue(result.blockers.any { it.contains("DERIVED_OCR_METRICS") })
    }

    @Test
    fun partialErasureIsNeverReportedAsComplete() {
        val result = CorpusDeletionValidator.validate(
            CorpusDeletionRequest(
                requestId = "deletion-4",
                subjectPseudonymousId = "subject-1",
                requestedAtIsoDate = "2026-08-17",
                declaredLocations = setOf(
                    CorpusStorageLocation.OBJECT_STORAGE,
                    CorpusStorageLocation.ENCRYPTED_BACKUP,
                ),
                receipts = listOf(receipt(CorpusStorageLocation.OBJECT_STORAGE)),
            ),
            TODAY,
        )

        assertEquals(DeletionCompleteness.PARTIAL, result.completeness)
        assertEquals(listOf(CorpusStorageLocation.ENCRYPTED_BACKUP), result.unverifiedLocations)
    }

    @Test
    fun everyDeclaredLocationWithAVerifiedReceiptCompletesDeletion() {
        val result = CorpusDeletionValidator.validate(
            CorpusDeletionRequest(
                requestId = "deletion-5",
                subjectPseudonymousId = "subject-1",
                requestedAtIsoDate = "2026-08-17",
                declaredLocations = setOf(
                    CorpusStorageLocation.OBJECT_STORAGE,
                    CorpusStorageLocation.DERIVED_OCR_METRICS,
                ),
                usedForOcrEvaluation = true,
                manifestUpdated = true,
                receipts = listOf(
                    receipt(CorpusStorageLocation.OBJECT_STORAGE),
                    receipt(CorpusStorageLocation.DERIVED_OCR_METRICS),
                ),
            ),
            TODAY,
        )

        assertEquals(DeletionCompleteness.VERIFIED_COMPLETE, result.completeness)
        assertTrue(result.blockers.isEmpty())
        assertTrue(result.unverifiedLocations.isEmpty())
    }

    private fun grant() = ConsentGrant(
        grantId = "grant-1",
        subjectPseudonymousId = "subject-1",
        scopes = setOf(ConsentScope.OCR_EVALUATION),
        consentReceiptSha256 = hash("receipt"),
        consentTextSha256 = hash("wording"),
        grantedAtIsoDate = "2026-01-01",
        expiresAtIsoDate = "2026-12-31",
    )

    private fun receipt(location: CorpusStorageLocation) = DeletionExecutionReceipt(
        location = location,
        executedAtIsoDate = "2026-08-18",
        verifiedAbsent = true,
        verificationEvidenceSha256 = hash("erasure"),
        operatorSignatureSha256 = hash("operator"),
    )

    private fun product() = ProductVariantIdentity(
        productId = "synthetic-zinc-d3",
        productName = "Synthetic Zinc",
        formulation = "capsule",
        labelRevision = "rev-1",
        labelEffectiveDate = "2026-08-15",
        serving = ServingDefinition(amount = 2.0, unit = "capsule"),
    )

    private fun syntheticRecord() = LabelCorpusRecord(
        recordId = "synthetic-1",
        product = product(),
        rawTextSha256 = hash("raw"),
        consent = LabelCorpusConsent.SYNTHETIC,
        retentionPolicy = LabelRetentionPolicy.DO_NOT_STORE_IMAGE,
        provenanceNote = "Repository-authored synthetic label",
    )

    private fun optInRecord() = syntheticRecord().copy(
        recordId = "consented-1",
        consent = LabelCorpusConsent.EXPLICIT_OPT_IN,
        provenanceNote = "Synthetic stand-in for a consented record; no real label bytes exist",
    )

    private fun hash(seed: String): String =
        seed.map { it.code }.sum().toString(16).padStart(64, '0').takeLast(64)

    private companion object {
        const val TODAY = "2026-08-18"
    }
}

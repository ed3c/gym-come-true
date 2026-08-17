package dev.ed3c.gymcometrue.catalog

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MediaAdmissionTest {

    private val asOf = "2026-08-18"

    private fun hash(character: Char): String = character.toString().repeat(64)

    private fun executedScope(
        derivativesAllowed: Boolean = true,
        redistributionAllowed: Boolean = true,
        platforms: Set<String> = setOf("android", "ios", "web"),
        territories: Set<String> = setOf("worldwide"),
    ): MediaRightsScope = MediaRightsScope(
        licenseGrant = LicenseGrantKind.EXECUTED_ASSET_SCOPE,
        licenseEvidenceRef = "private://legal/contracts/vendor/2026-001",
        platforms = platforms,
        territories = territories,
        termStartIsoDate = "2026-01-01",
        termEndIsoDate = "2027-01-01",
        derivativesAllowed = derivativesAllowed,
        redistributionAllowed = redistributionAllowed,
        attributionRequired = false,
        attributionText = null,
    )

    private fun admittedOriginal(sha: String = hash('a')): MediaIntakeRecord = MediaIntakeRecord(
        mediaId = "media-original",
        exerciseId = "gct-bodyweight-squat",
        kind = MediaKind.STILL_IMAGE,
        state = MediaAdmissionState.ADMITTED,
        revocationKey = "rk-vendor-2026-001",
        rights = executedScope(),
        originSha256 = sha,
        byteLength = 2048,
        storageUri = "repo://media/$sha.webp",
        altText = mapOf("en" to "A squat demonstration.", "zh-Hant-TW" to "深蹲示範。"),
        reviewerAttestationSha256 = hash('b'),
    )

    @Test
    fun anAdmittedOriginalWithFullEvidencePasses() {
        val result = MediaAdmissionValidator.validate(admittedOriginal(), asOfIsoDate = asOf)

        assertEquals(emptyList(), result.blockers)
        assertEquals(MediaAdmissionState.ADMITTED, result.effectiveState)
    }

    @Test
    fun anInputManifestCannotSelfDeclareProductionAdmission() {
        val result = MediaAdmissionValidator.validate(
            admittedOriginal().copy(productionAdmitted = true),
            asOfIsoDate = asOf,
        )

        assertTrue(result.blockers.any { it.contains("self-declares productionAdmitted") })
        assertEquals(MediaAdmissionState.QUARANTINED, result.effectiveState)
    }

    @Test
    fun aRemoteUrlCannotSurviveIntake() {
        val result = MediaAdmissionValidator.validate(
            admittedOriginal().copy(remoteUrl = "vendor-cdn-reference"),
            asOfIsoDate = asOf,
        )

        assertTrue(result.blockers.any { it.contains("never hotlinked") })
    }

    @Test
    fun hashVerificationRequiresContentAddressedStorage() {
        val sha = hash('c')
        val result = MediaAdmissionValidator.validate(
            admittedOriginal(sha).copy(
                state = MediaAdmissionState.HASH_VERIFIED,
                storageUri = "repo://media/unrelated-name.webp",
            ),
            asOfIsoDate = asOf,
        )

        assertTrue(result.blockers.any { it.contains("not addressed by its own hash") })
    }

    @Test
    fun hashVerificationIsNotALicence() {
        val sha = hash('d')
        val result = MediaAdmissionValidator.validate(
            MediaIntakeRecord(
                mediaId = "media-hash-only",
                exerciseId = null,
                kind = MediaKind.VECTOR_SCHEMATIC,
                state = MediaAdmissionState.HASH_VERIFIED,
                revocationKey = "rk-first-party",
                rights = MediaRightsScope(licenseGrant = LicenseGrantKind.FIRST_PARTY_OWNERSHIP),
                originSha256 = sha,
                byteLength = 5738,
                storageUri = "repo://assets/schematic.svg#sha256=$sha",
            ),
            asOfIsoDate = asOf,
        )

        assertEquals(emptyList(), result.blockers)
        assertTrue(result.reviewNotes.any { it.contains("hash verification is not a licence") })
    }

    @Test
    fun theRepositoryRootLicenceNeverAuthorizesAnAsset() {
        val result = MediaAdmissionValidator.validate(
            admittedOriginal().copy(
                rights = executedScope().copy(licenseGrant = LicenseGrantKind.REPOSITORY_ROOT_LICENSE),
            ),
            asOfIsoDate = asOf,
        )

        assertTrue(result.blockers.any { it.contains("repository-root licence") })
    }

    @Test
    fun admissionRequiresBilingualAlternativeText() {
        val result = MediaAdmissionValidator.validate(
            admittedOriginal().copy(altText = mapOf("en" to "A squat demonstration.")),
            asOfIsoDate = asOf,
        )

        assertTrue(result.blockers.any { it.contains("needs zh-Hant-TW alternative text") })
    }

    @Test
    fun anExpiredTermBlocksAdmission() {
        val result = MediaAdmissionValidator.validate(
            admittedOriginal().copy(rights = executedScope().copy(termEndIsoDate = "2026-01-31")),
            asOfIsoDate = asOf,
        )

        assertTrue(result.blockers.any { it.contains("term has expired") })
    }

    @Test
    fun aDerivativeInheritsAndCannotExceedItsParent() {
        val parentSha = hash('a')
        val childSha = hash('e')
        val parent = admittedOriginal(parentSha)
        val child = MediaIntakeRecord(
            mediaId = "media-derivative",
            exerciseId = "gct-bodyweight-squat",
            kind = MediaKind.STILL_IMAGE,
            state = MediaAdmissionState.ADMITTED,
            revocationKey = parent.revocationKey,
            rights = executedScope(platforms = setOf("web")),
            originSha256 = childSha,
            byteLength = 512,
            storageUri = "repo://media/$childSha.webp",
            altText = parent.altText,
            reviewerAttestationSha256 = hash('b'),
            derivedFromSha256 = parentSha,
            derivativeTransform = DerivativeTransform.RESIZE,
        )
        val index = mapOf(parentSha to parent, childSha to child)

        assertEquals(
            emptyList(),
            MediaAdmissionValidator.validate(child, index, asOfIsoDate = asOf).blockers,
        )

        val overreach = child.copy(rights = executedScope(platforms = setOf("android", "ios", "web", "tv")))
        assertTrue(
            MediaAdmissionValidator.validate(overreach, index, asOfIsoDate = asOf)
                .blockers.any { it.contains("platforms beyond its parent") },
        )

        val orphan = child.copy(derivedFromSha256 = hash('f'))
        assertTrue(
            MediaAdmissionValidator.validate(orphan, index, asOfIsoDate = asOf)
                .blockers.any { it.contains("unknown parent hash") },
        )

        val escapesTakedown = child.copy(revocationKey = "rk-something-else")
        assertTrue(
            MediaAdmissionValidator.validate(escapesTakedown, index, asOfIsoDate = asOf)
                .blockers.any { it.contains("inherit its parent's revocation key") },
        )
    }

    @Test
    fun aDerivativeOfANonAdmittedParentIsRejected() {
        val parentSha = hash('a')
        val childSha = hash('e')
        val parent = admittedOriginal(parentSha).copy(state = MediaAdmissionState.RIGHTS_REVIEWED)
        val child = admittedOriginal(childSha).copy(
            mediaId = "media-derivative",
            derivedFromSha256 = parentSha,
            derivativeTransform = DerivativeTransform.TRANSCODE,
        )

        val result = MediaAdmissionValidator.validate(
            child,
            mapOf(parentSha to parent, childSha to child),
            asOfIsoDate = asOf,
        )

        assertTrue(result.blockers.any { it.contains("which is not admitted") })
    }

    @Test
    fun aTransformWithoutAParentIsRejected() {
        val result = MediaAdmissionValidator.validate(
            admittedOriginal().copy(derivativeTransform = DerivativeTransform.CROP),
            asOfIsoDate = asOf,
        )

        assertTrue(result.blockers.any { it.contains("transform without a parent hash") })
    }

    @Test
    fun takedownReachesEveryDerivativeOfTheWithdrawnOriginal() {
        val parentSha = hash('a')
        val childSha = hash('e')
        val grandchildSha = hash('9')
        val parent = admittedOriginal(parentSha)
        val child = admittedOriginal(childSha).copy(
            mediaId = "media-derivative",
            derivedFromSha256 = parentSha,
            derivativeTransform = DerivativeTransform.RESIZE,
        )
        // Deliberately carries a stale key: takedown must reach it through the derivative graph,
        // not through the key it happens to be labelled with.
        val grandchild = admittedOriginal(grandchildSha).copy(
            mediaId = "media-thumbnail",
            revocationKey = "rk-stale-key",
            derivedFromSha256 = childSha,
            derivativeTransform = DerivativeTransform.CROP,
        )
        val unrelated = admittedOriginal(hash('7')).copy(
            mediaId = "media-unrelated",
            revocationKey = "rk-other-vendor",
        )

        val result = MediaTakedown.apply(
            listOf(parent, child, grandchild, unrelated),
            "rk-vendor-2026-001",
        )

        assertFalse(result.keyNotFound)
        assertEquals(
            listOf("media-derivative", "media-original", "media-thumbnail"),
            result.revokedMediaIds,
        )
        assertTrue(
            result.records
                .filter { it.mediaId != "media-unrelated" }
                .all { it.state == MediaAdmissionState.REVOKED },
        )
        assertEquals(
            MediaAdmissionState.ADMITTED,
            result.records.single { it.mediaId == "media-unrelated" }.state,
        )
    }

    @Test
    fun anUnmatchedTakedownKeyIsReportedRatherThanSucceedingSilently() {
        val result = MediaTakedown.apply(listOf(admittedOriginal()), "rk-not-in-this-ledger")

        assertTrue(result.keyNotFound)
        assertEquals(emptyList(), result.revokedMediaIds)
        assertEquals(MediaAdmissionState.ADMITTED, result.records.single().state)
    }

    @Test
    fun aRevokedRecordCanNeverBeServed() {
        val result = MediaAdmissionValidator.validate(
            admittedOriginal().copy(state = MediaAdmissionState.REVOKED),
            asOfIsoDate = asOf,
        )

        assertEquals(listOf("Media media-original is revoked and cannot be served."), result.blockers)
    }

    @Test
    fun theLedgerOnlyEverReturnsFullyEvidencedMedia() {
        val good = admittedOriginal(hash('a'))
        val missingAttestation = admittedOriginal(hash('2')).copy(
            mediaId = "media-no-attestation",
            reviewerAttestationSha256 = null,
        )
        val quarantined = admittedOriginal(hash('3')).copy(
            mediaId = "media-quarantined",
            state = MediaAdmissionState.QUARANTINED,
        )

        assertEquals(
            setOf("media-original"),
            MediaAdmissionLedger.admittedMediaIds(
                listOf(good, missingAttestation, quarantined),
                asOf,
            ),
        )
    }
}

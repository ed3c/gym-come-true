package dev.ed3c.gymcometrue.catalog

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExerciseCatalogContractTest {

    private fun provenance(
        fields: Set<CatalogField> = FieldProvenanceValidator.ALWAYS_REQUIRED,
        authorship: AuthorshipMethod = AuthorshipMethod.FIRST_PARTY_AGENT_DRAFTED,
        grant: LicenseGrantKind = LicenseGrantKind.FIRST_PARTY_OWNERSHIP,
        review: ContentReviewState = ContentReviewState.DRAFT,
    ): List<FieldProvenance> = fields.map { field ->
        FieldProvenance(
            field = field,
            authorship = authorship,
            licenseGrant = grant,
            provenanceRecordId = "prov-test-record",
            reviewState = review,
        )
    }

    private fun record(slug: String = "bodyweight-squat"): RawExerciseRecord = RawExerciseRecord(
        id = "gct-$slug",
        slug = slug,
        name = mapOf("en" to "Bodyweight Squat", "zh-Hant-TW" to "徒手深蹲"),
        summary = mapOf(
            "en" to "A squat performed without external load.",
            "zh-Hant-TW" to "不使用外加負重完成的蹲起動作。",
        ),
        movementPattern = "SQUAT",
        mechanics = "COMPOUND",
        force = "PUSH",
        laterality = "BILATERAL",
        skillLevel = "BEGINNER",
        equipment = listOf("BODYWEIGHT"),
        muscleEngagement = listOf(
            RawMuscleEngagement("QUADRICEPS", "PRIMARY"),
            RawMuscleEngagement("GLUTEUS_MAXIMUS", "PRIMARY"),
            RawMuscleEngagement("ADDUCTORS", "SECONDARY"),
            RawMuscleEngagement("ABDOMINALS", "STABILIZER"),
        ),
        steps = mapOf(
            "en" to listOf("Stand in a stable position.", "Descend under control.", "Return to standing."),
            "zh-Hant-TW" to listOf("採取穩定站姿。", "有控制地下降。", "回到站立位置。"),
        ),
        commonErrors = mapOf(
            "en" to listOf("Lifting the heels off the floor."),
            "zh-Hant-TW" to listOf("腳跟離地。"),
        ),
        safetyNoteRef = "general",
        fieldProvenance = provenance(),
    )

    private fun catalog(vararg records: RawExerciseRecord): RawExerciseCatalog = RawExerciseCatalog(
        schemaVersion = CatalogSchema.CURRENT_VERSION,
        catalogId = "test-catalog",
        catalogVersion = "1.0.0-test",
        safetyNotes = mapOf(
            "general" to mapOf("en" to "Stop for pain or dizziness.", "zh-Hant-TW" to "疼痛或暈眩時停止。"),
        ),
        records = records.toList(),
    )

    @Test
    fun validRecordIsAcceptedAsDraftOnly() {
        val result = ExerciseCatalogValidator.validate(catalog(record()))

        assertEquals(emptyList(), result.blockers)
        assertEquals(CatalogAdmission.DRAFT, result.admission)
        assertEquals(1, result.records.size)
        assertEquals(MovementPattern.SQUAT, result.records.first().movementPattern)
    }

    @Test
    fun duplicateIdAndSlugAreRejected() {
        val result = ExerciseCatalogValidator.validate(catalog(record(), record()))

        assertEquals(CatalogAdmission.REJECTED, result.admission)
        assertTrue(result.blockers.any { it.contains("Duplicate exercise id") })
        assertTrue(result.blockers.any { it.contains("Duplicate exercise slug") })
        assertEquals(emptyList(), result.records)
    }

    @Test
    fun unknownTaxonomyTokenIsRejectedNeverCoerced() {
        val broken = record().copy(
            muscleEngagement = listOf(RawMuscleEngagement("GLUTES", "PRIMARY")),
            movementPattern = "SQUATTING",
        )

        val result = ExerciseCatalogValidator.validate(catalog(broken))

        assertEquals(CatalogAdmission.REJECTED, result.admission)
        assertTrue(result.blockers.any { it.contains("Unknown taxonomy token 'GLUTES'") })
        assertTrue(result.blockers.any { it.contains("Unknown taxonomy token 'SQUATTING'") })
    }

    @Test
    fun repositoryRootLicenceNeverAuthorizesARecord() {
        val broken = record().copy(
            fieldProvenance = provenance(
                authorship = AuthorshipMethod.LICENSED_THIRD_PARTY,
                grant = LicenseGrantKind.REPOSITORY_ROOT_LICENSE,
            ),
        )

        val result = ExerciseCatalogValidator.validate(catalog(broken))

        assertEquals(CatalogAdmission.REJECTED, result.admission)
        assertTrue(result.blockers.any { it.contains("repository-root licence") })
    }

    @Test
    fun scrapedTextIsRejectedOutright() {
        val broken = record().copy(
            fieldProvenance = provenance(authorship = AuthorshipMethod.SCRAPED_OR_MIRRORED),
        )

        val result = ExerciseCatalogValidator.validate(catalog(broken))

        assertTrue(result.blockers.any { it.contains("scraped or mirrored") })
    }

    @Test
    fun aMissingLocaleIsABlockerNotAFallback() {
        val broken = record().copy(
            steps = mapOf("en" to listOf("One.", "Two.", "Three.")),
            name = mapOf("en" to "Bodyweight Squat", "zh-Hant" to "徒手深蹲"),
        )

        val result = ExerciseCatalogValidator.validate(catalog(broken))

        assertTrue(result.blockers.any { it.contains("is missing locale zh-Hant-TW") })
        assertTrue(result.blockers.any { it.contains("unsupported locale tag 'zh-Hant'") })
    }

    @Test
    fun stepCountsMustMatchAcrossLocales() {
        val broken = record().copy(
            steps = mapOf(
                "en" to listOf("One.", "Two.", "Three."),
                "zh-Hant-TW" to listOf("一。", "二。", "三。", "四。"),
            ),
        )

        val result = ExerciseCatalogValidator.validate(catalog(broken))

        assertTrue(result.blockers.any { it.contains("different entry count per locale") })
    }

    @Test
    fun medicalClaimWordingIsScreenedInBothLanguages() {
        val english = record().copy(
            summary = mapOf(
                "en" to "This movement prevents injury in every athlete.",
                "zh-Hant-TW" to "不使用外加負重完成的蹲起動作。",
            ),
        )
        val chinese = record("glute-bridge").copy(
            summary = mapOf(
                "en" to "A hip extension performed on the floor.",
                "zh-Hant-TW" to "本動作可以治療下背疼痛。",
            ),
        )

        val result = ExerciseCatalogValidator.validate(catalog(english, chinese))

        assertTrue(result.blockers.any { it.contains("claim phrase 'prevents injury'") })
        assertTrue(result.blockers.any { it.contains("claim phrase '治療'") })
    }

    @Test
    fun safetyDisclaimerWordingIsNotMistakenForAClaim() {
        // The screen runs over instructional fields only, so a disclaimer that legitimately says
        // "not medical advice" must not be flagged the way a claim is.
        assertEquals(emptyList(), MedicalClaimScreen.scan("This entry is not medical advice."))
        assertEquals(emptyList(), MedicalClaimScreen.scan("本條目不是醫療建議。"))
    }

    @Test
    fun catalogTextMayNotCarryALink() {
        val broken = record().copy(
            summary = mapOf(
                "en" to "See https://example.test/squat for a demonstration.",
                "zh-Hant-TW" to "不使用外加負重完成的蹲起動作。",
            ),
        )

        val result = ExerciseCatalogValidator.validate(catalog(broken))

        assertTrue(result.blockers.any { it.contains("contains a URL") })
    }

    @Test
    fun mediaMayNotBeCitedUntilItIsAdmitted() {
        val withMedia = record().copy(
            mediaRefs = listOf("media-squat-still"),
            fieldProvenance = provenance(FieldProvenanceValidator.ALWAYS_REQUIRED + CatalogField.MEDIA),
        )

        val rejected = ExerciseCatalogValidator.validate(catalog(withMedia))
        assertTrue(rejected.blockers.any { it.contains("media media-squat-still that is not admitted") })

        val accepted = ExerciseCatalogValidator.validate(
            catalog = catalog(withMedia),
            admittedMediaIds = setOf("media-squat-still"),
        )
        assertEquals(emptyList(), accepted.blockers)
    }

    @Test
    fun provenanceForAbsentMediaIsRejectedAsWell() {
        // Declaring rights over media a record does not have is the same fabrication as omitting
        // rights over media it does have.
        val broken = record().copy(
            fieldProvenance = provenance(FieldProvenanceValidator.ALWAYS_REQUIRED + CatalogField.MEDIA),
        )

        val result = ExerciseCatalogValidator.validate(catalog(broken))

        assertTrue(result.blockers.any { it.contains("Provenance declared for field MEDIA") })
    }

    @Test
    fun anUnsupportedSchemaVersionFailsClosed() {
        val result = ExerciseCatalogValidator.validate(
            catalog(record()).copy(schemaVersion = CatalogSchema.CURRENT_VERSION + 1),
        )

        assertEquals(CatalogAdmission.REJECTED, result.admission)
        assertTrue(result.blockers.any { it.contains("Unsupported catalog schemaVersion") })
    }

    @Test
    fun anUnknownSafetyNoteReferenceIsRejected() {
        val result = ExerciseCatalogValidator.validate(
            catalog(record().copy(safetyNoteRef = "stop-rule-that-does-not-exist")),
        )

        assertTrue(result.blockers.any { it.contains("unknown safety note") })
    }

    @Test
    fun agentDraftedTextCannotReachProductionWithoutHumanAcceptance() {
        val result = ExerciseCatalogValidator.validate(catalog(record()), production = true)

        assertEquals(CatalogAdmission.REJECTED, result.admission)
        assertTrue(result.blockers.any { it.contains("requires human editorial acceptance") })
        assertTrue(result.blockers.any { it.contains("Production requires ADMITTED provenance") })
    }

    @Test
    fun aRecordWithoutAPrimaryMuscleIsRejected() {
        val result = ExerciseCatalogValidator.validate(
            catalog(record().copy(muscleEngagement = listOf(RawMuscleEngagement("ABDOMINALS", "STABILIZER")))),
        )

        assertTrue(result.blockers.any { it.contains("declares no PRIMARY muscle") })
    }

    @Test
    fun anUnrenderableMuscleIsReportedRatherThanDropped() {
        val result = ExerciseCatalogValidator.validate(catalog(record()))

        assertFalse(MuscleRegionMap.isRenderable(MuscleGroup.ADDUCTORS))
        assertTrue(result.reviewNotes.any { it.contains("ADDUCTORS") && it.contains("text only") })
    }

    @Test
    fun accessibilityLabelIsDerivedDeterministicallyInBothLocales() {
        val validated = ExerciseCatalogValidator.validate(catalog(record())).records.single()

        assertEquals(
            "Bodyweight Squat. primary muscles: Glutes, Quadriceps. " +
                "supporting muscles: Adductors. stabilizing muscles: Abdominals.",
            ExerciseAccessibility.label(validated, CatalogLocale.EN),
        )
        assertEquals(
            "徒手深蹲。主要肌群：股四頭肌、臀大肌。協同肌群：內收肌群。穩定肌群：腹直肌。",
            ExerciseAccessibility.label(validated, CatalogLocale.ZH_HANT_TW),
        )
        assertEquals(
            listOf(MuscleGroup.QUADRICEPS, MuscleGroup.GLUTEUS_MAXIMUS),
            validated.musclesAt(ActivationIntensity.PRIMARY),
        )
    }

    @Test
    fun everyMuscleHasALabelInEveryRequiredLocale() {
        assertTrue(MuscleLabels.isComplete())
        assertEquals(
            MuscleGroup.entries.size,
            MuscleRegionMap.regions.size + MuscleRegionMap.unrenderableMuscles.size,
        )
    }

    @Test
    fun theVisualizationPlanIsOrderedAndWeightedDeterministically() {
        val plan = MuscleVisualizationPlanner.plan(
            engagements = listOf(
                MuscleEngagement(MuscleGroup.QUADRICEPS, ActivationIntensity.PRIMARY),
                MuscleEngagement(MuscleGroup.GLUTEUS_MAXIMUS, ActivationIntensity.PRIMARY),
                MuscleEngagement(MuscleGroup.ADDUCTORS, ActivationIntensity.SECONDARY),
                MuscleEngagement(MuscleGroup.ABDOMINALS, ActivationIntensity.STABILIZER),
            ),
            locale = CatalogLocale.EN,
        )

        assertEquals(
            listOf(
                "muscle-back-left-glute",
                "muscle-back-right-glute",
                "muscle-front-abdominals",
                "muscle-front-left-quadriceps",
                "muscle-front-right-quadriceps",
            ),
            plan.highlights.map { it.regionId },
        )
        assertEquals(listOf(MuscleGroup.ADDUCTORS), plan.unrenderedMuscles)
        assertEquals(MuscleRegionMap.ASSET_PATH, plan.assetPath)
        assertEquals(0.90, plan.highlights.first { it.muscle == MuscleGroup.QUADRICEPS }.opacity, 1e-9)
        assertEquals(0.30, plan.highlights.first { it.muscle == MuscleGroup.ABDOMINALS }.opacity, 1e-9)
        assertTrue(plan.accessibilitySummary.contains("Adductors"))
    }

    @Test
    fun repeatedMuscleEngagementResolvesToTheStrongestIntensity() {
        val plan = MuscleVisualizationPlanner.plan(
            engagements = listOf(
                MuscleEngagement(MuscleGroup.QUADRICEPS, ActivationIntensity.STABILIZER),
                MuscleEngagement(MuscleGroup.QUADRICEPS, ActivationIntensity.PRIMARY),
            ),
            locale = CatalogLocale.EN,
        )

        assertEquals(2, plan.highlights.size)
        assertTrue(plan.highlights.all { it.intensity == ActivationIntensity.PRIMARY })
    }

    @Test
    fun aDuplicateMuscleInOneRecordIsStillRejectedAtTheSource() {
        val result = ExerciseCatalogValidator.validate(
            catalog(
                record().copy(
                    muscleEngagement = listOf(
                        RawMuscleEngagement("QUADRICEPS", "PRIMARY"),
                        RawMuscleEngagement("QUADRICEPS", "SECONDARY"),
                    ),
                ),
            ),
        )

        assertTrue(result.blockers.any { it.contains("repeats muscle QUADRICEPS") })
    }
}

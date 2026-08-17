package dev.ed3c.gymcometrue.health

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HealthReadAccessTest {
    private val samples = listOf(
        HealthSample("s1", HealthReadType.BODY_MASS, 1_700_000_000L, 71.4, "kg"),
        HealthSample("s2", HealthReadType.WORKOUT_MINUTES, 1_700_003_600L, 45.0, "min"),
        HealthSample("s3", HealthReadType.BODY_MASS, 1_699_000_000L, 71.9, "kg"),
    )

    @Test
    fun onlyFeatureJustifiedTypesAreEverRequested() {
        assertEquals(
            setOf(HealthReadType.BODY_MASS),
            HealthReadPolicy.requestedTypes(setOf(HealthFeature.WEIGHT_TREND_CARD)),
        )
        assertEquals(emptySet(), HealthReadPolicy.requestedTypes(emptySet()))

        val everything = HealthReadPolicy.requestedTypes(HealthFeature.entries.toSet())
        assertFalse(
            HealthReadType.ACTIVE_ENERGY_BURNED in everything,
            "an unjustified read type must never enter the request set",
        )
        assertTrue(HealthReadPolicy.justifications.all { it.userVisiblePurpose.isNotBlank() })
    }

    @Test
    fun appleIdentifierSeamIgnoresUnknownFeatureIdsInsteadOfWidening() {
        assertEquals(
            listOf("HKQuantityTypeIdentifierBodyMass"),
            HealthReadPolicy.appleReadIdentifiers(listOf("WEIGHT_TREND_CARD", "SLEEP_STAGES")),
        )
        assertEquals(emptyList(), HealthReadPolicy.appleReadIdentifiers(listOf("EVERYTHING")))
    }

    @Test
    fun emptyReadIsNeverReportedAsNoDataOrAsDenial() {
        val state = HealthReadState(
            availability = HealthAvailability.AVAILABLE,
            authorization = HealthReadAuthorization.REQUEST_PRESENTED_OUTCOME_UNKNOWABLE,
            lastQueryOutcome = HealthQueryOutcome.EMPTY_INDISTINGUISHABLE,
        )
        val line = HealthReadStateMachine.statusLine(state)

        assertFalse(HealthReadStateMachine.mayDisplayMetric(state))
        assertTrue(line.contains("either no samples or no access"))
        assertFalse(line.contains("denied", ignoreCase = true))
    }

    @Test
    fun metricIsDisplayableOnlyWhenSamplesActuallyReturned() {
        val authorized = HealthReadState(
            availability = HealthAvailability.AVAILABLE,
            authorization = HealthReadAuthorization.REQUEST_PRESENTED_OUTCOME_UNKNOWABLE,
            lastQueryOutcome = HealthQueryOutcome.SAMPLES_RETURNED,
        )
        assertTrue(HealthReadStateMachine.mayDisplayMetric(authorized))

        assertFalse(
            HealthReadStateMachine.mayDisplayMetric(
                authorized.copy(availability = HealthAvailability.UNAVAILABLE_ON_DEVICE),
            ),
        )
        assertFalse(
            HealthReadStateMachine.mayDisplayMetric(
                authorized.copy(authorization = HealthReadAuthorization.NOT_DETERMINED),
            ),
        )
        assertFalse(HealthReadStateMachine.mayDisplayMetric(HealthReadState()))
    }

    @Test
    fun disablingOneFeaturePurgesOnlyItsCachedSamples() {
        val state = HealthReadState(
            availability = HealthAvailability.AVAILABLE,
            authorization = HealthReadAuthorization.REQUEST_PRESENTED_OUTCOME_UNKNOWABLE,
            lastQueryOutcome = HealthQueryOutcome.SAMPLES_RETURNED,
        )
        val (next, receipt) = HealthReadStateMachine.disableFeature(
            state = state,
            feature = HealthFeature.WEIGHT_TREND_CARD,
            cachedSamples = samples,
            stillEnabledFeatures = HealthFeature.entries.toSet(),
        )

        assertEquals(listOf("s1", "s3"), receipt.purgedSampleIds)
        assertEquals(listOf("s2"), receipt.retainedSampleIds)
        assertEquals(HealthReadAuthorization.REQUEST_PRESENTED_OUTCOME_UNKNOWABLE, next.authorization)
        assertEquals(HealthQueryOutcome.NOT_RUN, next.lastQueryOutcome)
        assertTrue(receipt.note.contains("does not revoke Health authorization"))
    }

    @Test
    fun disablingTheLastFeaturePurgesEverythingAndStopsReads() {
        val state = HealthReadState(
            availability = HealthAvailability.AVAILABLE,
            authorization = HealthReadAuthorization.REQUEST_PRESENTED_OUTCOME_UNKNOWABLE,
            lastQueryOutcome = HealthQueryOutcome.SAMPLES_RETURNED,
        )
        val (next, receipt) = HealthReadStateMachine.disableFeature(
            state = state,
            feature = HealthFeature.WEIGHT_TREND_CARD,
            cachedSamples = samples,
            stillEnabledFeatures = setOf(HealthFeature.WEIGHT_TREND_CARD),
        )

        assertEquals(listOf("s1", "s2", "s3"), receipt.purgedSampleIds)
        assertEquals(emptyList(), receipt.retainedSampleIds)
        assertEquals(HealthReadAuthorization.USER_DISABLED_IN_APP, next.authorization)
        assertFalse(HealthReadStateMachine.mayDisplayMetric(next))
    }

    @Test
    fun exportIsChronologicalLocalAndCarriesNoInterpretation() {
        val export = HealthReadStateMachine.export(samples, atEpochSeconds = 1_700_010_000L)

        assertEquals(listOf("s3", "s1", "s2"), export.samples.map { it.sampleId })
        assertEquals(1_700_010_000L, export.exportedAtEpochSeconds)
        assertTrue(export.note.contains("never uploaded"))
        assertTrue(export.note.contains("no medical interpretation"))
    }
}

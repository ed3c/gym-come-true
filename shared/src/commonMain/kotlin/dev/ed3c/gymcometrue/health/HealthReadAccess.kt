package dev.ed3c.gymcometrue.health

import kotlinx.serialization.Serializable

/**
 * Least-privilege HealthKit read contract (Issue #28).
 *
 * Two honest facts drive the whole model:
 *
 * 1. iOS never discloses read authorization. After the sheet is presented the
 *    app knows that it asked, not what the user answered.
 * 2. An empty query result therefore cannot distinguish "denied" from
 *    "authorised but no samples exist".
 *
 * Both facts get their own state instead of being collapsed into "no data".
 */
@Serializable
enum class HealthFeature {
    /** Shows the user's own weight trend when they enable the card. */
    WEIGHT_TREND_CARD,

    /** Shows completed workout minutes for protocol adherence. */
    WORKOUT_MINUTES_CARD,
}

@Serializable
enum class HealthReadType(val appleTypeIdentifier: String) {
    BODY_MASS("HKQuantityTypeIdentifierBodyMass"),
    WORKOUT_MINUTES("HKWorkoutTypeIdentifier"),

    /** Deliberately unjustified: no user-visible feature needs it, so it is never requested. */
    ACTIVE_ENERGY_BURNED("HKQuantityTypeIdentifierActiveEnergyBurned"),
}

@Serializable
data class HealthReadJustification(
    val readType: HealthReadType,
    /** `null` means no feature justifies this type; it must never be requested. */
    val feature: HealthFeature?,
    val userVisiblePurpose: String,
)

@Serializable
enum class HealthAvailability {
    UNKNOWN,
    UNAVAILABLE_ON_DEVICE,
    AVAILABLE,
}

@Serializable
enum class HealthReadAuthorization {
    NOT_DETERMINED,

    /** The sheet was presented. iOS does not report the user's read answer. */
    REQUEST_PRESENTED_OUTCOME_UNKNOWABLE,

    RESTRICTED_BY_POLICY,

    /** The user switched the feature off inside this app; reads stop and the cache is purged. */
    USER_DISABLED_IN_APP,
}

@Serializable
enum class HealthQueryOutcome {
    NOT_RUN,
    SAMPLES_RETURNED,

    /** Zero samples: denial and genuine absence look identical from the app. */
    EMPTY_INDISTINGUISHABLE,
}

@Serializable
data class HealthReadState(
    val availability: HealthAvailability = HealthAvailability.UNKNOWN,
    val authorization: HealthReadAuthorization = HealthReadAuthorization.NOT_DETERMINED,
    val lastQueryOutcome: HealthQueryOutcome = HealthQueryOutcome.NOT_RUN,
)

@Serializable
data class HealthSample(
    val sampleId: String,
    val readType: HealthReadType,
    val recordedAtEpochSeconds: Long,
    val value: Double,
    val unit: String,
)

@Serializable
data class HealthExportRecord(
    val exportedAtEpochSeconds: Long,
    val samples: List<HealthSample>,
    val note: String = "Local cache only. Health data is never uploaded and carries no medical interpretation.",
)

@Serializable
data class HealthPurgeReceipt(
    val purgedSampleIds: List<String>,
    val retainedSampleIds: List<String>,
    val note: String = "Purging the local cache does not revoke Health authorization; the user does that in Settings.",
)

object HealthReadPolicy {
    val justifications: List<HealthReadJustification> = listOf(
        HealthReadJustification(
            readType = HealthReadType.BODY_MASS,
            feature = HealthFeature.WEIGHT_TREND_CARD,
            userVisiblePurpose = "Show your own weight trend next to the protocol you logged.",
        ),
        HealthReadJustification(
            readType = HealthReadType.WORKOUT_MINUTES,
            feature = HealthFeature.WORKOUT_MINUTES_CARD,
            userVisiblePurpose = "Show completed workout minutes so protocol adherence is not self-reported only.",
        ),
        HealthReadJustification(
            readType = HealthReadType.ACTIVE_ENERGY_BURNED,
            feature = null,
            userVisiblePurpose = "No user-visible feature justifies energy data; it is never requested.",
        ),
    )

    /** Only types whose justifying feature is enabled right now. */
    fun requestedTypes(enabledFeatures: Set<HealthFeature>): Set<HealthReadType> = justifications
        .filter { justification -> justification.feature?.let { it in enabledFeatures } == true }
        .map { it.readType }
        .toSet()

    /** Primitive-only seam for Swift: unknown feature ids are ignored, never expanded. */
    fun appleReadIdentifiers(enabledFeatureIds: List<String>): List<String> {
        val features = enabledFeatureIds
            .mapNotNull { id -> HealthFeature.entries.firstOrNull { it.name == id } }
            .toSet()
        return requestedTypes(features).map { it.appleTypeIdentifier }.sorted()
    }
}

object HealthReadStateMachine {
    /**
     * A metric may be rendered only when samples actually came back. Every other
     * combination is a state, not a zero.
     */
    fun mayDisplayMetric(state: HealthReadState): Boolean =
        state.availability == HealthAvailability.AVAILABLE &&
            state.authorization == HealthReadAuthorization.REQUEST_PRESENTED_OUTCOME_UNKNOWABLE &&
            state.lastQueryOutcome == HealthQueryOutcome.SAMPLES_RETURNED

    fun statusLine(state: HealthReadState): String = when {
        state.availability == HealthAvailability.UNAVAILABLE_ON_DEVICE ->
            "Health data is unavailable on this device."
        state.availability == HealthAvailability.UNKNOWN ->
            "Health availability has not been checked yet."
        state.authorization == HealthReadAuthorization.RESTRICTED_BY_POLICY ->
            "Health access is restricted by device policy."
        state.authorization == HealthReadAuthorization.USER_DISABLED_IN_APP ->
            "You turned this Health feature off; the local cache was purged."
        state.authorization == HealthReadAuthorization.NOT_DETERMINED ->
            "Health access has not been requested yet."
        state.lastQueryOutcome == HealthQueryOutcome.NOT_RUN ->
            "Health access was requested; no read has run yet."
        state.lastQueryOutcome == HealthQueryOutcome.EMPTY_INDISTINGUISHABLE ->
            "The read returned nothing. iOS does not reveal read permission, " +
                "so this can mean either no samples or no access."
        else -> "Health samples were read on this device and stay on it."
    }

    /**
     * Revocation path. iOS cannot notify the app that read access was withdrawn,
     * so the in-app switch is the only revocation this code can observe.
     */
    fun disableFeature(
        state: HealthReadState,
        feature: HealthFeature,
        cachedSamples: List<HealthSample>,
        stillEnabledFeatures: Set<HealthFeature>,
    ): Pair<HealthReadState, HealthPurgeReceipt> {
        val stillNeeded = HealthReadPolicy.requestedTypes(stillEnabledFeatures - feature)
        val purged = cachedSamples.filterNot { it.readType in stillNeeded }
        val retained = cachedSamples.filter { it.readType in stillNeeded }
        val nextState = if (stillNeeded.isEmpty()) {
            state.copy(
                authorization = HealthReadAuthorization.USER_DISABLED_IN_APP,
                lastQueryOutcome = HealthQueryOutcome.NOT_RUN,
            )
        } else {
            state.copy(lastQueryOutcome = HealthQueryOutcome.NOT_RUN)
        }
        return nextState to HealthPurgeReceipt(
            purgedSampleIds = purged.map { it.sampleId },
            retainedSampleIds = retained.map { it.sampleId },
        )
    }

    fun export(cachedSamples: List<HealthSample>, atEpochSeconds: Long): HealthExportRecord =
        HealthExportRecord(
            exportedAtEpochSeconds = atEpochSeconds,
            samples = cachedSamples.sortedBy { it.recordedAtEpochSeconds },
        )
}

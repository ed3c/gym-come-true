package dev.ed3c.gymcometrue.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient

/**
 * Health Connect availability as observed on this device right now. Never
 * cached and never inferred from app state — call
 * [currentHealthConnectAvailability] again after every permission flow, Play
 * Store update, or app resume.
 */
enum class HealthConnectAvailability {
    /** The Health Connect provider is installed and a client can be created. */
    AVAILABLE,

    /** No Health Connect provider is installed on this device. */
    NOT_INSTALLED,

    /** A Health Connect provider is installed but must be updated before use. */
    UPDATE_REQUIRED,
}

/**
 * Pure classification of [HealthConnectClient.getSdkStatus]. Kept separate
 * from the [Context]-dependent call so it is unit-testable without
 * instrumentation or a device.
 */
fun classifyHealthConnectSdkStatus(sdkStatus: Int): HealthConnectAvailability = when (sdkStatus) {
    HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.AVAILABLE
    HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectAvailability.UPDATE_REQUIRED
    else -> HealthConnectAvailability.NOT_INSTALLED
}

/**
 * Reads the live SDK status. Side-effecting; call from the UI layer in
 * response to an explicit user action or screen entry — never from a
 * background job or scheduled task.
 */
fun currentHealthConnectAvailability(context: Context): HealthConnectAvailability =
    classifyHealthConnectSdkStatus(HealthConnectClient.getSdkStatus(context))

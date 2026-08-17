package dev.ed3c.gymcometrue.health

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.WeightRecord

/**
 * Least-privilege read scope: only the two record types a shipped,
 * user-visible feature can justify today (docs/platform-capability-matrix.md
 * "Health adapters" — "completed workout minutes or body weight when
 * explicitly requested"). No write permission and no other record type is
 * requested. Extending this set requires a named visible feature, not a
 * speculative future one; see docs/android/health-connect.md.
 */
val minimalHealthConnectReadPermissions: Set<String> = setOf(
    HealthPermission.getReadPermission(WeightRecord::class),
    HealthPermission.getReadPermission(ExerciseSessionRecord::class),
)

/** Coarse permission state derived from a live granted set, never cached. */
enum class HealthConnectPermissionState {
    ALL_GRANTED,
    PARTIALLY_GRANTED,
    NONE_GRANTED,
}

/**
 * Pure classification so revocation — a previously granted permission
 * disappearing between two calls — is exercised by a deterministic unit
 * test instead of a device.
 */
fun classifyHealthConnectPermissionState(
    grantedPermissions: Set<String>,
    requestedPermissions: Set<String> = minimalHealthConnectReadPermissions,
): HealthConnectPermissionState = when {
    requestedPermissions.isEmpty() -> HealthConnectPermissionState.NONE_GRANTED
    grantedPermissions.containsAll(requestedPermissions) -> HealthConnectPermissionState.ALL_GRANTED
    grantedPermissions.any { it in requestedPermissions } -> HealthConnectPermissionState.PARTIALLY_GRANTED
    else -> HealthConnectPermissionState.NONE_GRANTED
}

/**
 * Live, uncached read of what the user has actually granted. Call this
 * before every read instead of trusting a cached in-memory flag — Health
 * Connect permissions can be revoked from system settings at any time.
 */
suspend fun currentHealthConnectPermissionState(
    client: HealthConnectClient,
    requestedPermissions: Set<String> = minimalHealthConnectReadPermissions,
): HealthConnectPermissionState {
    val granted = client.permissionController.getGrantedPermissions()
    return classifyHealthConnectPermissionState(granted, requestedPermissions)
}

/**
 * The ActivityResultContract a caller wires to an explicit, user-initiated
 * "connect health data" action. This adapter never requests permissions on
 * its own initiative.
 */
fun healthConnectPermissionRequestContract() = PermissionController.createRequestPermissionResultContract()

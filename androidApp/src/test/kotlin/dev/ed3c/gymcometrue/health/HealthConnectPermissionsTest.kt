package dev.ed3c.gymcometrue.health

import kotlin.test.Test
import kotlin.test.assertEquals

class HealthConnectPermissionsTest {
    private val requested = setOf("READ_WEIGHT", "READ_EXERCISE")

    @Test
    fun allRequestedGrantedIsAllGranted() {
        assertEquals(
            HealthConnectPermissionState.ALL_GRANTED,
            classifyHealthConnectPermissionState(requested, requested),
        )
    }

    @Test
    fun oneOfTwoGrantedIsPartiallyGranted() {
        assertEquals(
            HealthConnectPermissionState.PARTIALLY_GRANTED,
            classifyHealthConnectPermissionState(setOf("READ_WEIGHT"), requested),
        )
    }

    @Test
    fun noneGrantedIsNoneGranted() {
        assertEquals(
            HealthConnectPermissionState.NONE_GRANTED,
            classifyHealthConnectPermissionState(emptySet(), requested),
        )
    }

    @Test
    fun revocationDropsFromAllGrantedToPartiallyGranted() {
        val beforeRevocation = classifyHealthConnectPermissionState(requested, requested)
        val afterRevocation = classifyHealthConnectPermissionState(setOf("READ_EXERCISE"), requested)

        assertEquals(HealthConnectPermissionState.ALL_GRANTED, beforeRevocation)
        assertEquals(HealthConnectPermissionState.PARTIALLY_GRANTED, afterRevocation)
    }

    @Test
    fun unrelatedGrantedPermissionsDoNotCountTowardRequestedScope() {
        assertEquals(
            HealthConnectPermissionState.NONE_GRANTED,
            classifyHealthConnectPermissionState(setOf("READ_STEPS"), requested),
        )
    }

    @Test
    fun emptyRequestedScopeIsNeverReportedAsGranted() {
        assertEquals(
            HealthConnectPermissionState.NONE_GRANTED,
            classifyHealthConnectPermissionState(setOf("READ_WEIGHT"), emptySet()),
        )
    }
}

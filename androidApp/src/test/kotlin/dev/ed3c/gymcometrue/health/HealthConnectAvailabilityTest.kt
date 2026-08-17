package dev.ed3c.gymcometrue.health

import androidx.health.connect.client.HealthConnectClient
import kotlin.test.Test
import kotlin.test.assertEquals

class HealthConnectAvailabilityTest {
    @Test
    fun availableStatusClassifiesAsAvailable() {
        assertEquals(
            HealthConnectAvailability.AVAILABLE,
            classifyHealthConnectSdkStatus(HealthConnectClient.SDK_AVAILABLE),
        )
    }

    @Test
    fun updateRequiredStatusClassifiesAsUpdateRequired() {
        assertEquals(
            HealthConnectAvailability.UPDATE_REQUIRED,
            classifyHealthConnectSdkStatus(HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED),
        )
    }

    @Test
    fun unavailableStatusClassifiesAsNotInstalled() {
        assertEquals(
            HealthConnectAvailability.NOT_INSTALLED,
            classifyHealthConnectSdkStatus(HealthConnectClient.SDK_UNAVAILABLE),
        )
    }

    @Test
    fun unknownStatusFailsClosedToNotInstalled() {
        assertEquals(HealthConnectAvailability.NOT_INSTALLED, classifyHealthConnectSdkStatus(-1))
    }
}

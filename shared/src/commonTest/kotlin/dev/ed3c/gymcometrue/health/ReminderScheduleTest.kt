package dev.ed3c.gymcometrue.health

import dev.ed3c.gymcometrue.domain.DailyProtocolCompiler
import dev.ed3c.gymcometrue.domain.ProtocolCategory
import dev.ed3c.gymcometrue.domain.ProtocolEvent
import dev.ed3c.gymcometrue.domain.ProtocolTime
import dev.ed3c.gymcometrue.domain.TrainingVariant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReminderScheduleTest {
    private fun event(
        id: String,
        hour: Int,
        minute: Int = 0,
        dayOffset: Int = 0,
    ) = ProtocolEvent(
        id = id,
        time = ProtocolTime(hour, minute, dayOffset),
        title = "Checkpoint $id",
        category = ProtocolCategory.SUPPLEMENT_CHECKPOINT,
        note = "Confirm evidence before continuing.",
        requiresConfirmation = true,
    )

    @Test
    fun appleWeekdayMappingMatchesDateComponents() {
        assertEquals(1, ReminderDay.SUNDAY.appleWeekday)
        assertEquals(2, ReminderDay.MONDAY.appleWeekday)
        assertEquals(7, ReminderDay.SATURDAY.appleWeekday)
        assertTrue(ReminderDay.entries.all { it.appleWeekday in 1..7 })
    }

    @Test
    fun confirmationCheckpointsRecurPerSelectedWeekdayInWallClockComponents() {
        val plan = ReminderPlanner.plan(
            variant = TrainingVariant.AFTERNOON_1600,
            days = setOf(ReminderDay.MONDAY, ReminderDay.WEDNESDAY),
            authorization = ReminderAuthorization.AUTHORIZED,
        )

        assertEquals(ReminderDeliveryChannel.LOCAL_NOTIFICATION, plan.channel)
        assertFalse(plan.guaranteedDelivery)
        assertEquals(
            listOf(
                "morning-evidence@MONDAY",
                "evening-safety@MONDAY",
                "morning-evidence@WEDNESDAY",
                "evening-safety@WEDNESDAY",
            ),
            plan.occurrences.map { it.requestId },
        )
        assertEquals(listOf(2, 2, 4, 4), plan.occurrences.map { it.appleWeekday })
        assertEquals(8 to 15, plan.occurrences.first().hour to plan.occurrences.first().minute)
        assertTrue(plan.warnings.any { it.contains("best effort") })
    }

    @Test
    fun deniedOrUnrequestedAuthorizationSchedulesNothingAndPromisesNothing() {
        for (state in listOf(ReminderAuthorization.DENIED, ReminderAuthorization.NOT_DETERMINED)) {
            val plan = ReminderPlanner.plan(
                variant = TrainingVariant.NIGHT_2200,
                days = ReminderDay.entries.toSet(),
                authorization = state,
            )
            assertEquals(ReminderDeliveryChannel.NONE, plan.channel, "authorization $state")
            assertTrue(plan.occurrences.isEmpty())
            assertFalse(plan.guaranteedDelivery)
        }
    }

    @Test
    fun provisionalAuthorizationSchedulesButSaysItIsQuiet() {
        val plan = ReminderPlanner.plan(
            variant = TrainingVariant.AFTERNOON_1600,
            days = setOf(ReminderDay.FRIDAY),
            authorization = ReminderAuthorization.PROVISIONAL,
        )

        assertEquals(ReminderDeliveryChannel.LOCAL_NOTIFICATION, plan.channel)
        assertTrue(plan.warnings.any { it.contains("quietly") })
    }

    @Test
    fun cancellationRemovesExactlyTheMatchingRequestIds() {
        val plan = ReminderPlanner.plan(
            variant = TrainingVariant.AFTERNOON_1600,
            days = setOf(ReminderDay.MONDAY, ReminderDay.WEDNESDAY),
            authorization = ReminderAuthorization.AUTHORIZED,
            cancelledRequestIds = setOf("morning-evidence@MONDAY", "not-scheduled@MONDAY"),
        )

        assertEquals(3, plan.occurrences.size)
        assertFalse(plan.occurrences.any { it.requestId == "morning-evidence@MONDAY" })
        assertEquals(
            listOf("morning-evidence@MONDAY", "not-scheduled@MONDAY"),
            plan.cancelledRequestIds,
        )
    }

    @Test
    fun emptyWeekdaySelectionYieldsNoChannelInsteadOfADailyDefault() {
        val plan = ReminderPlanner.plan(
            variant = TrainingVariant.AFTERNOON_1600,
            days = emptySet(),
            authorization = ReminderAuthorization.AUTHORIZED,
        )

        assertEquals(ReminderDeliveryChannel.NONE, plan.channel)
        assertTrue(plan.occurrences.isEmpty())
        assertTrue(plan.warnings.any { it.contains("No weekday") })
    }

    @Test
    fun afterMidnightEventRecursOnTheFollowingWeekday() {
        val sleep = DailyProtocolCompiler.compile(TrainingVariant.NIGHT_2200).first { it.id == "b-sleep" }
        val plan = ReminderPlanner.planForEvents(
            events = listOf(sleep),
            days = setOf(ReminderDay.MONDAY, ReminderDay.SATURDAY),
            authorization = ReminderAuthorization.AUTHORIZED,
        )

        assertEquals(1, sleep.time.dayOffset)
        assertEquals(
            listOf(ReminderDay.SUNDAY.appleWeekday, ReminderDay.TUESDAY.appleWeekday),
            plan.occurrences.map { it.appleWeekday }.sorted(),
        )
        assertTrue(plan.occurrences.all { it.hour == 0 && it.minute == 15 })
    }

    @Test
    fun overnightHoursCarryADaylightSavingWarning() {
        val plan = ReminderPlanner.planForEvents(
            events = listOf(event("dst-edge", hour = 2, minute = 30)),
            days = setOf(ReminderDay.SUNDAY),
            authorization = ReminderAuthorization.AUTHORIZED,
        )

        assertTrue(plan.warnings.any { it.contains("daylight-saving") })
    }

    @Test
    fun pendingRequestCeilingTruncatesDeterministicallyAndSaysSo() {
        val events = (0 until 10).map { event(id = "e$it", hour = it) }
        val plan = ReminderPlanner.planForEvents(
            events = events,
            days = ReminderDay.entries.toSet(),
            authorization = ReminderAuthorization.AUTHORIZED,
        )

        assertEquals(ReminderPlanner.MAX_PENDING_REQUESTS, plan.occurrences.size)
        assertTrue(plan.warnings.any { it.contains("at most 64 pending requests") })
        assertEquals(
            plan.occurrences.map { it.sortKey }.sorted(),
            plan.occurrences.map { it.sortKey },
        )
    }

    @Test
    fun nativeSeamAgreesWithTypedPlanAndFailsClosedOnUnknownIdentifiers() {
        val typed = ReminderPlanner.plan(
            variant = TrainingVariant.NIGHT_2200,
            days = setOf(ReminderDay.TUESDAY),
            authorization = ReminderAuthorization.AUTHORIZED,
        )
        val fromNative = ReminderPlanner.planForNative(
            variantId = "NIGHT_2200",
            dayIds = listOf("TUESDAY", "FUNDAY"),
            authorizationId = "AUTHORIZED",
            cancelledRequestIds = emptyList(),
        )
        assertEquals(typed, fromNative)

        val unknown = ReminderPlanner.planForNative(
            variantId = "MORNING_0600",
            dayIds = listOf("TUESDAY"),
            authorizationId = "AUTHORIZED",
            cancelledRequestIds = emptyList(),
        )
        assertEquals(ReminderDeliveryChannel.NONE, unknown.channel)
        assertTrue(unknown.occurrences.isEmpty())
    }

    @Test
    fun alarmCapabilityStaysHonestAboutDeliveryAndStopControls() {
        val assessment = AlarmCapabilityAssessment.current()

        assertFalse(assessment.frameworkLinked)
        assertFalse(assessment.guaranteedDelivery)
        assertFalse(assessment.challengeToDismissAdmitted)
        assertTrue(assessment.systemStopControlsRetained)
        assertEquals("NOT_IMPLEMENTED", assessment.capabilityState)
        assertTrue("REAL_DEVICE_MEASUREMENT_ABSENT" in assessment.blockingGates)

        val claim = AlarmCapabilityAssessment.claim(assessment)
        assertFalse(claim.contains("guarantee", ignoreCase = true))
        assertTrue(claim.contains("best-effort"))
        assertTrue(claim.contains("stop control"))

        assertEquals(
            ReminderDeliveryChannel.NONE,
            AlarmCapabilityAssessment.channelFor(ReminderAuthorization.DENIED),
        )
        assertEquals(
            ReminderDeliveryChannel.LOCAL_NOTIFICATION,
            AlarmCapabilityAssessment.channelFor(ReminderAuthorization.AUTHORIZED),
        )
    }
}

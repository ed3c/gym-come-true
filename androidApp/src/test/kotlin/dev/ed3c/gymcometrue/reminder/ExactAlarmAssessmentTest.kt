package dev.ed3c.gymcometrue.reminder

import kotlin.test.Test
import kotlin.test.assertEquals

class ExactAlarmAssessmentTest {
    @Test
    fun emptyEvidenceDefaultsToNotNeeded() {
        assertEquals(ExactAlarmAssessment.NOT_NEEDED, assessExactAlarmNeed(emptyList()))
    }

    @Test
    fun consistentlyOnTimeDeliveryIsNotNeeded() {
        val delays = List(10) { 1_000L }
        assertEquals(ExactAlarmAssessment.NOT_NEEDED, assessExactAlarmNeed(delays))
    }

    @Test
    fun frequentLateDeliveryNeedsHumanReview() {
        val delays = List(5) { 20 * 60_000L } + List(5) { 1_000L }
        assertEquals(ExactAlarmAssessment.NEEDS_HUMAN_REVIEW, assessExactAlarmNeed(delays))
    }

    @Test
    fun occasionalLateDeliveryStaysNotNeeded() {
        val delays = List(1) { 20 * 60_000L } + List(9) { 1_000L }
        assertEquals(ExactAlarmAssessment.NOT_NEEDED, assessExactAlarmNeed(delays))
    }
}

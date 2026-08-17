package dev.ed3c.gymcometrue.reminder

enum class ExactAlarmAssessment {
    /** Default outcome. No evidence of unreliable inexact delivery. */
    NOT_NEEDED,

    /**
     * Observed delivery delays cross the threshold often enough to be worth
     * a human decision. This NEVER auto-requests
     * `android.permission.SCHEDULE_EXACT_ALARM` by itself — it only unblocks
     * a Human Admit UI path that can request it. See
     * HONEST_ALARM_SEMANTICS in AGENTS.md.
     */
    NEEDS_HUMAN_REVIEW,
}

/**
 * Pure, evidence-driven assessment over recorded delivery delays (intended
 * AlarmManager trigger time minus actual [ProtocolReminderReceiver.onReceive]
 * time, from [DeliveryDelayLog]). No default exact-alarm access: an empty or
 * mostly-on-time log always resolves to [ExactAlarmAssessment.NOT_NEEDED].
 */
fun assessExactAlarmNeed(
    observedDeliveryDelaysMillis: List<Long>,
    lateThresholdMillis: Long = 15L * 60 * 1_000,
    lateFractionThreshold: Double = 0.2,
): ExactAlarmAssessment {
    if (observedDeliveryDelaysMillis.isEmpty()) return ExactAlarmAssessment.NOT_NEEDED

    val lateFraction = observedDeliveryDelaysMillis.count { it >= lateThresholdMillis }
        .toDouble() / observedDeliveryDelaysMillis.size

    return if (lateFraction >= lateFractionThreshold) {
        ExactAlarmAssessment.NEEDS_HUMAN_REVIEW
    } else {
        ExactAlarmAssessment.NOT_NEEDED
    }
}

package dev.ed3c.gymcometrue.health

import dev.ed3c.gymcometrue.domain.DailyProtocolCompiler
import dev.ed3c.gymcometrue.domain.ProtocolEvent
import dev.ed3c.gymcometrue.domain.TrainingVariant
import kotlinx.serialization.Serializable

/**
 * Reminder recurrence and alarm-capability contract (Issue #29).
 *
 * Occurrences are wall-clock local components (weekday/hour/minute), which is
 * exactly what `UNCalendarNotificationTrigger` consumes. No absolute instant is
 * ever computed here: the system re-resolves the components after a time-zone
 * change, a DST transition, or a reboot. A fixed time-interval trigger cannot,
 * which is why it is not part of this contract.
 *
 * Nothing in this file may be read as a delivery guarantee. Local notifications
 * are reminders.
 */
@Serializable
enum class ReminderDay(val appleWeekday: Int) {
    SUNDAY(1),
    MONDAY(2),
    TUESDAY(3),
    WEDNESDAY(4),
    THURSDAY(5),
    FRIDAY(6),
    SATURDAY(7),
    ;

    /** Weekly recurrence: a `+1d` protocol event lands on the next weekday. */
    fun shifted(days: Int): ReminderDay {
        val all = ReminderDay.entries
        return all[(((ordinal + days) % all.size) + all.size) % all.size]
    }
}

@Serializable
enum class ReminderAuthorization {
    NOT_DETERMINED,
    DENIED,

    /** Quiet delivery straight to the notification centre. */
    PROVISIONAL,

    AUTHORIZED,
}

@Serializable
enum class ReminderDeliveryChannel {
    /** Nothing is scheduled and nothing may be promised. */
    NONE,

    /** `UNUserNotificationCenter`; best effort, user-silenceable, not an alarm. */
    LOCAL_NOTIFICATION,
}

@Serializable
data class ReminderOccurrence(
    val requestId: String,
    val appleWeekday: Int,
    val hour: Int,
    val minute: Int,
    val title: String,
    val body: String,
) {
    init {
        require(appleWeekday in 1..7)
        require(hour in 0..23)
        require(minute in 0..59)
    }

    val sortKey: Int = appleWeekday * 24 * 60 + hour * 60 + minute
}

@Serializable
data class ReminderPlan(
    val channel: ReminderDeliveryChannel,
    val occurrences: List<ReminderOccurrence>,
    val cancelledRequestIds: List<String>,
    val guaranteedDelivery: Boolean = false,
    val warnings: List<String> = emptyList(),
)

@Serializable
data class AlarmKitAssessment(
    val frameworkLinked: Boolean,
    val capabilityState: String,
    val guaranteedDelivery: Boolean,
    val systemStopControlsRetained: Boolean,
    val challengeToDismissAdmitted: Boolean,
    val blockingGates: List<String>,
)

object ReminderPlanner {
    /**
     * iOS keeps only the 64 soonest pending local notification requests and
     * silently drops the rest, so the plan truncates deterministically and says so.
     */
    const val MAX_PENDING_REQUESTS = 64

    fun plan(
        variant: TrainingVariant,
        days: Set<ReminderDay>,
        authorization: ReminderAuthorization,
        cancelledRequestIds: Set<String> = emptySet(),
    ): ReminderPlan = planForEvents(
        events = DailyProtocolCompiler.compile(variant).filter { it.requiresConfirmation },
        days = days,
        authorization = authorization,
        cancelledRequestIds = cancelledRequestIds,
    )

    fun planForEvents(
        events: List<ProtocolEvent>,
        days: Set<ReminderDay>,
        authorization: ReminderAuthorization,
        cancelledRequestIds: Set<String> = emptySet(),
    ): ReminderPlan {
        val warnings = mutableListOf(
            "Local notifications are reminders. Delivery is best effort and the user can silence them.",
        )

        if (authorization == ReminderAuthorization.DENIED ||
            authorization == ReminderAuthorization.NOT_DETERMINED
        ) {
            warnings += if (authorization == ReminderAuthorization.DENIED) {
                "Notification authorization was denied; nothing was scheduled."
            } else {
                "Notification authorization was never requested; ask in context before scheduling."
            }
            return ReminderPlan(
                channel = ReminderDeliveryChannel.NONE,
                occurrences = emptyList(),
                cancelledRequestIds = cancelledRequestIds.sorted(),
                warnings = warnings,
            )
        }
        if (authorization == ReminderAuthorization.PROVISIONAL) {
            warnings += "Provisional authorization delivers quietly to the notification centre only."
        }
        if (days.isEmpty()) {
            warnings += "No weekday was selected; nothing was scheduled."
        }

        val all = events
            .flatMap { event -> days.map { day -> occurrenceFor(event, day) } }
            .filterNot { it.requestId in cancelledRequestIds }
            .sortedWith(compareBy<ReminderOccurrence> { it.sortKey }.thenBy { it.requestId })

        val kept = all.take(MAX_PENDING_REQUESTS)
        if (all.size > kept.size) {
            warnings += "iOS keeps at most $MAX_PENDING_REQUESTS pending requests; " +
                "${all.size - kept.size} later reminder(s) were not scheduled."
        }
        if (kept.any { it.hour in 1..3 }) {
            warnings += "Reminders between 01:00 and 03:59 can shift or be skipped on " +
                "daylight-saving transition days."
        }

        return ReminderPlan(
            channel = if (kept.isEmpty()) {
                ReminderDeliveryChannel.NONE
            } else {
                ReminderDeliveryChannel.LOCAL_NOTIFICATION
            },
            occurrences = kept,
            cancelledRequestIds = cancelledRequestIds.sorted(),
            warnings = warnings,
        )
    }

    /** Primitive-only seam for Swift. Unknown identifiers fail closed to an empty plan. */
    fun planForNative(
        variantId: String,
        dayIds: List<String>,
        authorizationId: String,
        cancelledRequestIds: List<String>,
    ): ReminderPlan {
        val variant = TrainingVariant.entries.firstOrNull { it.name == variantId }
        val authorization = ReminderAuthorization.entries.firstOrNull { it.name == authorizationId }
        if (variant == null || authorization == null) {
            return ReminderPlan(
                channel = ReminderDeliveryChannel.NONE,
                occurrences = emptyList(),
                cancelledRequestIds = cancelledRequestIds.sorted(),
                warnings = listOf("Unknown training variant or authorization identifier; nothing was scheduled."),
            )
        }
        val days = dayIds.mapNotNull { id -> ReminderDay.entries.firstOrNull { it.name == id } }.toSet()
        return plan(variant, days, authorization, cancelledRequestIds.toSet())
    }

    private fun occurrenceFor(event: ProtocolEvent, day: ReminderDay): ReminderOccurrence {
        val target = day.shifted(event.time.dayOffset)
        return ReminderOccurrence(
            requestId = "${event.id}@${day.name}",
            appleWeekday = target.appleWeekday,
            hour = event.time.hour,
            minute = event.time.minute,
            title = event.title,
            body = event.note,
        )
    }
}

object AlarmCapabilityAssessment {
    /**
     * AlarmKit is assessed, not adopted. The framework is not linked, no usage
     * description exists, and no device measurement has been taken, so the only
     * honest delivery channel remains a local notification.
     */
    fun current(): AlarmKitAssessment = AlarmKitAssessment(
        frameworkLinked = false,
        capabilityState = "NOT_IMPLEMENTED",
        guaranteedDelivery = false,
        systemStopControlsRetained = true,
        challengeToDismissAdmitted = false,
        blockingGates = listOf(
            "ALARMKIT_FRAMEWORK_NOT_LINKED",
            "ALARMKIT_USAGE_DESCRIPTION_ABSENT",
            "REAL_DEVICE_MEASUREMENT_ABSENT",
            "APP_STORE_POLICY_EVIDENCE_ABSENT",
        ),
    )

    /**
     * AlarmKit is not a channel here. Even when it is eventually linked the system
     * keeps a stop control, so the honest channel is decided by authorization alone.
     */
    fun channelFor(authorization: ReminderAuthorization): ReminderDeliveryChannel = when (authorization) {
        ReminderAuthorization.DENIED,
        ReminderAuthorization.NOT_DETERMINED,
        -> ReminderDeliveryChannel.NONE
        ReminderAuthorization.PROVISIONAL,
        ReminderAuthorization.AUTHORIZED,
        -> ReminderDeliveryChannel.LOCAL_NOTIFICATION
    }

    fun claim(assessment: AlarmKitAssessment): String = buildString {
        append("Reminders are best-effort local notifications. ")
        append("AlarmKit capability is ${assessment.capabilityState}; ")
        append("blocking gates: ${assessment.blockingGates.joinToString(", ")}. ")
        append("The system always keeps a stop control, so no challenge-to-dismiss behaviour is claimed.")
    }
}

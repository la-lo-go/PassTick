package org.ligi.passandroid.reminder

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.threeten.bp.Instant

class NotificationPolicyTest {
    @Test
    fun `upcoming notification includes time location and available actions`() {
        val result = evaluate(
            reminder(
                locationLabel = "Central station",
                hasBarcode = true,
                latitude = 40.4,
                longitude = -3.7,
            ),
            "2026-09-09T10:00:00Z",
        )

        assertThat(result.phase).isEqualTo(NotificationPhase.UPCOMING)
        assertThat(result.body).isEqualTo("Starts in 1 hour · Central station")
        assertThat(result.actions).containsExactlyInAnyOrder(
            NotificationAction.OPEN_CODE,
            NotificationAction.DIRECTIONS,
            NotificationAction.SNOOZE,
        )
    }

    @Test
    fun `access notification uses minutes and schedules active update`() {
        val result = evaluate(reminder(hasBarcode = true), "2026-09-09T10:50:00Z")

        assertThat(result.phase).isEqualTo(NotificationPhase.ACCESS)
        assertThat(result.body).isEqualTo("Starts in 10 minutes")
        assertThat(result.nextTriggerAtMillis).isEqualTo(time("2026-09-09T11:00:00Z"))
    }

    @Test
    fun `active notification removes snooze and schedules cancellation`() {
        val result = evaluate(
            reminder(hasBarcode = true, locationLabel = "Hall A"),
            "2026-09-09T11:15:00Z",
        )

        assertThat(result.phase).isEqualTo(NotificationPhase.ACTIVE)
        assertThat(result.body).isEqualTo("Happening now · Hall A")
        assertThat(result.actions).containsExactlyInAnyOrder(
            NotificationAction.OPEN_CODE,
            NotificationAction.DIRECTIONS,
        )
        assertThat(result.nextTriggerAtMillis).isEqualTo(time("2026-09-09T12:00:00Z"))
    }

    @Test
    fun `ended notification is cancelled`() {
        val result = evaluate(reminder(), "2026-09-09T12:00:00Z")

        assertThat(result.disposition).isEqualTo(NotificationDisposition.CANCEL)
        assertThat(result.nextTriggerAtMillis).isNull()
    }

    @Test
    fun `protected notification hides title and location on lock screen`() {
        val result = evaluate(
            reminder(title = "Private journey", locationLabel = "Home", isProtected = true),
            "2026-09-09T10:30:00Z",
        )

        assertThat(result.title).isEqualTo("Private journey")
        assertThat(result.body).contains("Home")
        assertThat(result.publicTitle).isEqualTo("Pass reminder")
        assertThat(result.publicBody).isEqualTo("Starts in 30 minutes")
    }

    @Test
    fun `missing optional content produces a useful generic notification`() {
        val result = evaluate(reminder(), "2026-09-09T10:30:00Z")

        assertThat(result.body).isEqualTo("Starts in 30 minutes")
        assertThat(result.actions).containsExactly(NotificationAction.SNOOZE)
    }

    @Test
    fun `disabled contextual actions produce no actions`() {
        val result = evaluate(
            reminder(hasBarcode = true, locationLabel = "Station"),
            "2026-09-09T10:30:00Z",
            NotificationPolicySettings(actionsEnabled = false),
        )

        assertThat(result.actions).isEmpty()
    }

    @Test
    fun `per pass actions restrict available actions`() {
        val result = evaluate(
            reminder(hasBarcode = true, locationLabel = "Station").copy(
                enabledActions = setOf(NotificationAction.OPEN_CODE),
            ),
            "2026-09-09T10:30:00Z",
        )

        assertThat(result.actions).containsExactly(NotificationAction.OPEN_CODE)
    }

    @Test
    fun `missing per pass action override inherits defaults`() {
        val result = evaluate(
            reminder(hasBarcode = true),
            "2026-09-09T10:30:00Z",
        )

        assertThat(result.actions).containsExactlyInAnyOrder(NotificationAction.OPEN_CODE, NotificationAction.SNOOZE)
    }

    @Test
    fun `per pass exact timing intent is returned without platform policy`() {
        val result = evaluate(
            reminder(),
            "2026-09-09T09:00:00Z",
            NotificationPolicySettings(exactTiming = true),
        )

        assertThat(result.disposition).isEqualTo(NotificationDisposition.SCHEDULE)
        assertThat(result.nextTriggerAtMillis).isEqualTo(time("2026-09-09T10:00:00Z"))
        assertThat(result.exactTiming).isTrue()
    }

    @Test
    fun `per pass exact timing overrides a disabled global setting`() {
        val result = NotificationPolicy.evaluate(
            reminder().copy(exactTiming = true),
            time("2026-09-09T09:00:00Z"),
            NotificationPolicySettings(exactTiming = false),
        )

        assertThat(result.exactTiming).isTrue()
    }

    @Test
    fun `custom access window controls the phase boundary`() {
        val reminder = reminder()

        assertThat(
            evaluate(reminder, "2026-09-09T10:40:00Z", NotificationPolicySettings(accessWindowMinutes = 30)).phase,
        ).isEqualTo(NotificationPhase.ACCESS)
        assertThat(
            evaluate(reminder, "2026-09-09T10:40:00Z", NotificationPolicySettings(accessWindowMinutes = 15)).phase,
        ).isEqualTo(NotificationPhase.UPCOMING)
    }

    @Test
    fun `disabled event start update schedules cancellation after access`() {
        val result = evaluate(
            reminder(),
            "2026-09-09T10:50:00Z",
            NotificationPolicySettings(updateAtEventStart = false),
        )

        assertThat(result.phase).isEqualTo(NotificationPhase.ACCESS)
        assertThat(result.nextTriggerAtMillis).isEqualTo(time("2026-09-09T12:00:00Z"))
    }

    @Test
    fun `lock screen detail is part of the policy result`() {
        val result = evaluate(
            reminder(isProtected = true),
            "2026-09-09T10:30:00Z",
            NotificationPolicySettings(lockScreenDetail = NotificationLockScreenDetail.HIDDEN),
        )

        assertThat(result.lockScreenDetail).isEqualTo(NotificationLockScreenDetail.HIDDEN)
    }

    @Test
    fun `secondary reminder does not schedule lifecycle updates`() {
        val result = NotificationPolicy.evaluate(
            reminder().copy(triggerAtMillis = time("2026-09-09T10:30:00Z"), ownsLifecycle = false),
            time("2026-09-09T10:30:00Z"),
        )

        assertThat(result.disposition).isEqualTo(NotificationDisposition.SHOW)
        assertThat(result.nextTriggerAtMillis).isNull()
    }

    private fun evaluate(
        reminder: PassReminder,
        now: String,
        settings: NotificationPolicySettings = NotificationPolicySettings(),
    ) = NotificationPolicy.evaluate(reminder, time(now), settings)

    private fun reminder(
        title: String = "Train ticket",
        locationLabel: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        hasBarcode: Boolean = false,
        isProtected: Boolean = false,
    ) = PassReminder(
        id = "pass:event:60",
        passId = "pass",
        title = title,
        eventAtMillis = time("2026-09-09T11:00:00Z"),
        triggerAtMillis = time("2026-09-09T10:00:00Z"),
        endAtMillis = time("2026-09-09T12:00:00Z"),
        locationLabel = locationLabel,
        latitude = latitude,
        longitude = longitude,
        hasBarcode = hasBarcode,
        isProtected = isProtected,
    )

    private fun time(value: String) = Instant.parse(value).toEpochMilli()
}

package org.ligi.passandroid.reminder

interface ReminderScheduler {
    fun sync(reminders: List<PassReminder>, settings: NotificationPolicySettings = NotificationPolicySettings())

    data object None : ReminderScheduler {
        override fun sync(reminders: List<PassReminder>, settings: NotificationPolicySettings) = Unit
    }
}

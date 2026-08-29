package org.ligi.passandroid

import timber.log.Timber

interface Tracker {
    fun trackException(message: String, error: Throwable, fatal: Boolean)

    fun trackException(message: String, fatal: Boolean)

    fun trackEvent(category: String?, action: String?, label: String?, value: Long?)
}

class LocalTracker : Tracker {
    override fun trackException(message: String, error: Throwable, fatal: Boolean) {
        Timber.w(error, "%s: %s", severity(fatal), message)
    }

    override fun trackException(message: String, fatal: Boolean) {
        Timber.w("%s: %s", severity(fatal), message)
    }

    override fun trackEvent(category: String?, action: String?, label: String?, value: Long?) = Unit

    private fun severity(fatal: Boolean) = if (fatal) "Fatal exception" else "Non-fatal exception"
}

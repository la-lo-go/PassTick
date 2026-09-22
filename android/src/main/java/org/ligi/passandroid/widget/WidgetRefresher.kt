package org.ligi.passandroid.widget

import android.content.Context
import androidx.glance.appwidget.updateAll

/** Refreshes the installed widgets after a change that does not republish the snapshot. */
fun interface WidgetRefresher {
    suspend fun refresh()
}

class PassWidgetRefresher(private val context: Context) : WidgetRefresher {
    override suspend fun refresh() {
        PassOverviewWidget().updateAll(context)
        PassCodeWidget().updateAll(context)
    }
}

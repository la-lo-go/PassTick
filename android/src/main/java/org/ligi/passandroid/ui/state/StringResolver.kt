package org.ligi.passandroid.ui.state

import androidx.annotation.StringRes

fun interface StringResolver {
    fun resolve(@StringRes id: Int, vararg args: Any): String
}

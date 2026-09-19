package org.ligi.passandroid.ui.compose

import android.net.Uri
import org.ligi.passandroid.BuildConfig

internal const val PROJECT_REPOSITORY_URL = "https://github.com/la-lo-go/PassTick"
internal const val PRIVACY_POLICY_URL = "https://github.com/la-lo-go/PassTick/blob/main/privacy_policy.txt"
internal const val BUG_REPORT_EMAIL = "apps@lalogo.dev"

private val BUG_REPORT_TEMPLATE = """
    ### What happened

    ### Steps to reproduce
    1.
    2.
    3.

    ### What I expected

    ### Device details
    - PassTick version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})
    - Android version:
""".trimIndent()

internal val BUG_REPORT_URL = buildString {
    append("https://github.com/la-lo-go/PassTick/issues/new")
    append("?title=")
    append(Uri.encode("Bug report"))
    append("&body=")
    append(Uri.encode(BUG_REPORT_TEMPLATE))
}

internal val BUG_REPORT_MAIL_URL = buildString {
    append("mailto:")
    append(BUG_REPORT_EMAIL)
    append("?subject=")
    append(Uri.encode("PassTick bug report"))
    append("&body=")
    append(Uri.encode(BUG_REPORT_TEMPLATE))
}

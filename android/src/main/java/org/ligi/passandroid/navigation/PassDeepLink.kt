package org.ligi.passandroid.navigation

import android.net.Uri

private const val PassScheme = "passandroid"
private const val PassHost = "pass"

fun passDeepLink(passId: String): Uri = Uri.Builder()
    .scheme(PassScheme)
    .authority(PassHost)
    .appendPath(passId)
    .build()

fun Uri.passIdOrNull(): String? = takeIf { scheme == PassScheme && host == PassHost }
    ?.pathSegments
    ?.singleOrNull()
    ?.takeIf(String::isNotBlank)

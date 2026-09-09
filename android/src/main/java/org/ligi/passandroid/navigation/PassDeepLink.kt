package org.ligi.passandroid.navigation

import android.net.Uri

private const val PassScheme = "passtick"
private const val PassHost = "pass"

data class PassDeepLinkRequest(val passId: String, val showCode: Boolean)

fun passDeepLink(passId: String): Uri = Uri.Builder()
    .scheme(PassScheme)
    .authority(PassHost)
    .appendPath(passId)
    .build()

fun Uri.passDeepLinkRequestOrNull(): PassDeepLinkRequest? {
    if (scheme != PassScheme || host != PassHost) return null
    val passId = pathSegments.singleOrNull()?.takeIf(String::isNotBlank) ?: return null
    return PassDeepLinkRequest(passId, showCode = getQueryParameter("view") == "code")
}

package org.ligi.passandroid.ui.quirk_fix

import android.net.Uri
import org.ligi.passandroid.Tracker
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder

private const val CHARSET = "UTF-8"

class URLRewriteController(private val tracker: Tracker) {

    fun getUrlByUri(uri: Uri): String? = getUrl(uri.toString())

    fun getUrl(rawUrl: String): String? {
        val uri = URI(rawUrl)

        if (uri.scheme != null && uri.authority != null && uri.authority == "import") {
            when (uri.scheme) {
                "pass2u" -> return "$uri".substring("pass2u://import/".length)
                "passandroid" -> return "$uri".substring("passandroid://import/".length)
            }
        }

        val host = uri.host
        if (host != null && host.endsWith(".virginaustralia.com")) { // mobile. or checkin.
            return getVirginAustraliaURL(uri)
        }

        return when (host) {
            "pass-cloud.appspot.com" -> uri.queryParameter("url")
            "m.aircanada.ca", "services.aircanada.com" -> getAirCanada(uri)
            "mci.aircanada.com" -> getAirCanada2(uri)
            "www.cathaypacific.com" -> getCathay(uri)
            "mbp.swiss.com", "prod.wap.ncrwebhost.mobi" -> getNrcWebHost(uri)
            else -> null
        }
    }

    private fun getAirCanada(uri: URI) = "$uri?appDetection=false"
    private fun getAirCanada2(uri: URI) = "$uri.pkpass"

    private fun getVirginAustraliaURL(uri: URI): String? {

        val passId: String?
        if ("$uri".contains("CheckInApiIntegration")) {
            passId = uri.queryParameter("key")
            tracker.trackEvent("quirk_fix", "redirect_attempt", "virgin_australia2", null)
        } else {
            tracker.trackEvent("quirk_fix", "redirect_attempt", "virgin_australia1", null)
            passId = uri.queryParameter("c")
        }

        if (passId == null) {
            return null
        }

        tracker.trackEvent("quirk_fix", "redirect", "virgin_australia", null)

        return "https://mobile.virginaustralia.com/boarding/pass.pkpass?key=" + URLEncoder.encode(passId, CHARSET)
    }

    private fun getCathay(uri: URI): String? {
        val passId = uri.queryParameter("v")

        tracker.trackEvent("quirk_fix", "redirect_attempt", "cathay", null)

        if (passId == null) {
            return null
        }

        tracker.trackEvent("quirk_fix", "redirect", "cathay", null)

        return "https://www.cathaypacific.com/icheckin2/PassbookServlet?v=" + URLEncoder.encode(passId, CHARSET)
    }

    private fun getNrcWebHost(uri: URI): String? {
        var url = "$uri"
        if (url.endsWith("/")) {
            url = url.dropLast(1)
        }

        val split = url.split("/".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()

        if (split.size < 6) {
            return null
        }

        return "http://prod.wap.ncrwebhost.mobi/mobiqa/wap/" + split[split.size - 2] + "/" + split[split.size - 1] + "/passbook"
    }
}

private fun URI.queryParameter(name: String): String? = rawQuery
    ?.split('&')
    ?.asSequence()
    ?.map { it.substringBefore('=') to it.substringAfter('=', "") }
    ?.firstOrNull { it.first == name }
    ?.second
    ?.let { URLDecoder.decode(it, CHARSET) }

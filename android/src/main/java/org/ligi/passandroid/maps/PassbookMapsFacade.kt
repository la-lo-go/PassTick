package org.ligi.passandroid.maps

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import org.ligi.passandroid.model.pass.PassLocation

object PassbookMapsFacade {
    fun openLocation(context: Context, location: PassLocation) {
        val uri = geoUri(location).toUri()
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }
}

internal fun geoUri(location: PassLocation) =
    "geo:${location.lat},${location.lon}?q=${location.lat},${location.lon}"

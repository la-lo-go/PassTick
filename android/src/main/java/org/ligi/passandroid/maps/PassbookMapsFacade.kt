package org.ligi.passandroid.maps

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
fun openLocation(context: Context, latitude: Double, longitude: Double) {
    context.startActivity(Intent(Intent.ACTION_VIEW, geoUri(latitude, longitude).toUri()))
}

internal fun geoUri(latitude: Double, longitude: Double) =
    "geo:$latitude,$longitude?q=$latitude,$longitude"

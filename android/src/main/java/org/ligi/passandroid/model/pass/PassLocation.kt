package org.ligi.passandroid.model.pass

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
class PassLocation {

    var name: String? = null

    var lat: Double = 0.toDouble()
    var lon: Double = 0.toDouble()

    fun getNameWithFallback(pass: Pass) = if (name.isNullOrBlank()) {
        // Some pass formats omit a location label.
        pass.description
    } else {
        name
    }

    fun getCommaSeparated() = "$lat,$lon"
}

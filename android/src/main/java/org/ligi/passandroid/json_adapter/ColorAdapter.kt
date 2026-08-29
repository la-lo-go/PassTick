package org.ligi.passandroid.json_adapter

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import org.ligi.passandroid.model.pass.PassImpl

class ColorAdapter {
    @ToJson
    internal fun toJson(@PassImpl.HexColor rgb: Int) = String.format("#%06x", rgb)

    @FromJson
    @PassImpl.HexColor
    internal fun fromJson(rgb: String): Int {
        require(rgb.startsWith('#')) { "Stored colors must use hexadecimal notation." }
        val value = rgb.drop(1).toLong(16)
        return when (rgb.length) {
            7 -> (value or 0xFF000000L).toInt()
            9 -> value.toInt()
            else -> error("Stored colors must use #RRGGBB or #AARRGGBB.")
        }
    }

}

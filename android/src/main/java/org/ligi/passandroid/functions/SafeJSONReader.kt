package org.ligi.passandroid.functions

import org.json.JSONException
import org.json.JSONObject

private val invalidJsonRepairs = listOf(
    "" to "",
    ",[\n\r\t ]*\\}" to "}",
    ",[\n\r\t ]*\\]" to "]",
    ":[ ]*,[\n\r\t ]*\"" to ":\"\",",
    ",[\n\r\t ]*," to ",",
)

@Throws(JSONException::class)
fun readJSONSafely(input: String?): JSONObject? {
    val source = input ?: return null
    var repaired = source
    for ((pattern, replacement) in invalidJsonRepairs) {
        val regex = pattern.toRegex()
        repaired = repaired.replace(regex, replacement)
        try {
            return JSONObject(source.replace(regex, replacement))
        } catch (_: JSONException) { }
    }
    return JSONObject(repaired)
}

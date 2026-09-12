package org.ligi.passandroid.functions

import android.content.res.Resources
import org.ligi.passandroid.R
import org.ligi.passandroid.model.pass.Pass
import org.ligi.passandroid.model.pass.PassField
import org.ligi.passandroid.model.pass.PassImpl
import org.ligi.passandroid.model.pass.PassType
import java.util.*

const val APP = "passandroid"

fun createPassForImageImport(resources: Resources): Pass {
    return createBasePass().apply {
        description = resources.getString(R.string.image_import)

        fields = mutableListOf(
                PassField.create(R.string.field_source, R.string.field_source_image, resources),
                PassField.create(R.string.field_advice_label, R.string.field_advice_text, resources),
                PassField.create(R.string.field_note, R.string.field_note_image, resources, true)
        )
    }
}

fun createPassForPDFImport(resources: Resources): Pass {
    return createBasePass().apply {
        description = resources.getString(R.string.pdf_import)

        fields = mutableListOf(
                PassField.create(R.string.field_source, R.string.field_source_pdf, resources),
                PassField.create(R.string.field_advice_label, R.string.field_advice_text, resources),
                PassField.create(R.string.field_note, R.string.field_note_pdf, resources, true)
        )
    }
}

private fun createBasePass(): PassImpl {
    val pass = PassImpl(UUID.randomUUID().toString())
    pass.accentColor = 0xFF0000FF.toInt()
    pass.app = APP
    pass.type = PassType.EVENT
    return pass
}

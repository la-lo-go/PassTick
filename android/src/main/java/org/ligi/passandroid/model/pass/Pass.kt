package org.ligi.passandroid.model.pass

import android.graphics.Bitmap
import androidx.annotation.StringDef
import org.ligi.passandroid.imports.ImportSource
import org.ligi.passandroid.model.PassBitmapDefinitions.BITMAP_FOOTER
import org.ligi.passandroid.model.PassBitmapDefinitions.BITMAP_ICON
import org.ligi.passandroid.model.PassBitmapDefinitions.BITMAP_LOGO
import org.ligi.passandroid.model.PassBitmapDefinitions.BITMAP_STRIP
import org.ligi.passandroid.model.PassBitmapDefinitions.BITMAP_THUMBNAIL
import org.ligi.passandroid.model.PassStore


interface Pass {

    @StringDef(BITMAP_ICON, BITMAP_THUMBNAIL, BITMAP_STRIP, BITMAP_LOGO, BITMAP_FOOTER)
    @Retention(AnnotationRetention.SOURCE)
    annotation class PassBitmap

    val description: String?

    var type: PassType

    val fields: List<PassField>

    val locations: List<PassLocation>

    var accentColor: Int

    val id: String

    val creator: String?

    fun getSource(passStore: PassStore): String?

    var barCode: BarCode?

    /** Additional codes shown after [barCode]; imported documents can carry several. */
    val barCodes: List<BarCode> get() = emptyList()

    val webServiceURL: String?

    val authToken: String?

    val serial: String?

    val passIdent: String?

    val app: String?

    /** Set when the pass was imported from a photo, camera capture, or PDF. */
    val importSource: ImportSource? get() = null

    /** Page count of the stored PDF document, 0 when the pass has no document. */
    val documentPageCount: Int get() = 0

    val validTimespans: List<PassImpl.TimeSpan>?
    var calendarTimespan: PassImpl.TimeSpan?

    fun getBitmap(passStore: PassStore, passBitmap: String): Bitmap?

}

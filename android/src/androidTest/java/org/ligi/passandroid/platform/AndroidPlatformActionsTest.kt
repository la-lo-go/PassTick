package org.ligi.passandroid.platform

import android.content.Intent
import android.net.Uri
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class AndroidPlatformActionsTest {
    @Test
    fun sharesContentUriWithTemporaryReadAccess() {
        val uri = Uri.parse("content://org.example.fileprovider/share/pass.espass")

        val intent = createShareIntent(uri, "application/vnd.espass-espass+zip")

        assertThat(intent.action).isEqualTo(Intent.ACTION_SEND)
        assertThat(intent.type).isEqualTo("application/vnd.espass-espass+zip")
        assertThat(intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)).isEqualTo(uri)
        assertThat(intent.flags.and(Intent.FLAG_GRANT_READ_URI_PERMISSION)).isNotZero
    }
}

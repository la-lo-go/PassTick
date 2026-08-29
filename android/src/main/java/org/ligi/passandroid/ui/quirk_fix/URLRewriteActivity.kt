package org.ligi.passandroid.ui.quirk_fix

import android.content.Intent
import android.os.Bundle
import androidx.core.net.toUri
import org.ligi.passandroid.ui.AlertFragment
import org.ligi.passandroid.ui.PassAndroidActivity
import org.ligi.passandroid.ui.PassImportActivity

class URLRewriteActivity : PassAndroidActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val rewrittenUrl = intent.data?.let { URLRewriteController(tracker).getUrlByUri(it) }
        if (rewrittenUrl == null) {
            supportFragmentManager.beginTransaction().add(AlertFragment(), "AlertFrag").commit()
            return
        }

        startActivity(Intent(this, PassImportActivity::class.java).apply { data = rewrittenUrl.toUri() })
        finish()
    }
}

package org.ligi.passandroid

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.graphics.drawable.toDrawable
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.ligi.passandroid.navigation.PassDeepLinkRequest
import org.ligi.passandroid.navigation.passDeepLinkRequestOrNull
import org.ligi.passandroid.repository.StartupAppearanceStore
import org.ligi.passandroid.repository.isBackupUri
import org.ligi.passandroid.ui.PassTickApp
import org.ligi.passandroid.ui.state.AppAction
import org.ligi.passandroid.ui.state.MainViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModel()
    private val deepLinkRequest = MutableStateFlow<PassDeepLinkRequest?>(null)
    private val documentImportRequest = MutableStateFlow<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val startupAppearance = StartupAppearanceStore.read(this)
        window.setBackgroundDrawable(startupAppearance.backgroundColor(this).toDrawable())
        enableEdgeToEdge()
        deepLinkRequest.value = intent.data?.passDeepLinkRequestOrNull()
        if (savedInstanceState == null) importFrom(intent)
        setContent {
            PassTickApp(
                activity = this,
                viewModel = viewModel,
                deepLinkRequest = deepLinkRequest,
                documentImportRequest = documentImportRequest,
                startupAppearance = startupAppearance,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkRequest.value = intent.data?.passDeepLinkRequestOrNull()
        importFrom(intent)
    }

    private fun importFrom(intent: Intent) {
        val uris = intent.importUris()
        if (uris.isEmpty()) return
        // A shared backup restores through the single-request channel; passes import in bulk.
        val singleRequest = uris.singleOrNull()
            ?.takeIf { isDocument(it, intent.type) || isBackupUri(this, it) }
        if (singleRequest != null) {
            documentImportRequest.value = singleRequest
        } else {
            viewModel.onAction(AppAction.ImportFiles(uris))
        }
    }

    private fun isDocument(uri: Uri, fallbackType: String?): Boolean {
        val mimeType = contentResolver.getType(uri) ?: fallbackType ?: return false
        return mimeType == "application/pdf" || mimeType.startsWith("image/")
    }
}

@Suppress("DEPRECATION")
private fun Intent.importUris(): List<Uri> = buildList {
    data?.let(::add)
    clipData?.let { clip ->
        repeat(clip.itemCount) { index -> clip.getItemAt(index).uri?.let(::add) }
    }
    if (action == Intent.ACTION_SEND_MULTIPLE) {
        getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)?.let(::addAll)
    } else {
        getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let(::add)
    }
}.filter { it.scheme == "content" }.distinct()

package org.ligi.passandroid

import android.content.Intent
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
import org.ligi.passandroid.ui.PassTickApp
import org.ligi.passandroid.ui.state.AppAction
import org.ligi.passandroid.ui.state.MainViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModel()
    private val deepLinkRequest = MutableStateFlow<PassDeepLinkRequest?>(null)

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
        if (uris.isNotEmpty()) viewModel.onAction(AppAction.ImportFiles(uris))
    }
}

@Suppress("DEPRECATION")
private fun Intent.importUris(): List<android.net.Uri> = buildList {
    data?.let(::add)
    clipData?.let { clip ->
        repeat(clip.itemCount) { index -> clip.getItemAt(index).uri?.let(::add) }
    }
    if (action == Intent.ACTION_SEND_MULTIPLE) {
        getParcelableArrayListExtra<android.net.Uri>(Intent.EXTRA_STREAM)?.let(::addAll)
    } else {
        getParcelableExtra<android.net.Uri>(Intent.EXTRA_STREAM)?.let(::add)
    }
}.filter { it.scheme == "content" }.distinct()

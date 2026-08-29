package org.ligi.passandroid.ui.quirk_fix

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.webkit.WebResourceErrorCompat
import androidx.webkit.WebViewClientCompat
import org.ligi.passandroid.functions.IPHONE_USER_AGENT

class OpenIphoneWebView : Activity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val data = intent.data ?: return
        val webView = WebView(this)
        webView.settings.userAgentString = IPHONE_USER_AGENT

        webView.settings.javaScriptEnabled = true

        val progress = ProgressBar(this)
        val content = FrameLayout(this).apply {
            addView(webView, FrameLayout.LayoutParams(-1, -1))
            addView(progress, FrameLayout.LayoutParams(-2, -2, android.view.Gravity.CENTER))
        }
        setContentView(content)

        webView.webViewClient = object : WebViewClientCompat() {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                progress.visibility = View.GONE
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceErrorCompat) {
                progress.visibility = View.GONE
                Toast.makeText(this@OpenIphoneWebView, "Page load failed", Toast.LENGTH_LONG).show()
            }
        }
        webView.loadUrl(data.toString())
    }
}

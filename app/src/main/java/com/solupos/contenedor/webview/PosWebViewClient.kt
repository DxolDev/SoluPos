package com.solupos.contenedor.webview

import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebView
import android.webkit.WebViewClient

open class PosWebViewClient : WebViewClient() {
    @Deprecated("Deprecated in Java")
    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
        handler?.proceed()
    }
}

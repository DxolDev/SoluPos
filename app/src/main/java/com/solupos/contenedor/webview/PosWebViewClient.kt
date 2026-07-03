package com.solupos.contenedor.webview

import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * Cliente WebView del contenedor POS.
 *
 * Ante un error de certificado NO se continúa automáticamente (eso lo rechaza
 * Google Play). En su lugar el evento se delega a [onSslError] para que la UI
 * pida confirmación explícita al usuario. El valor por defecto cancela la carga,
 * que es el comportamiento seguro si nadie provee callback.
 */
open class PosWebViewClient(
    private val onSslError: (SslErrorHandler, SslError) -> Unit = { handler, _ -> handler.cancel() }
) : WebViewClient() {
    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
        if (handler != null && error != null) onSslError(handler, error) else handler?.cancel()
    }
}

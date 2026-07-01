package com.solupos.contenedor.webview

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.JavascriptInterface
import android.webkit.WebView

class WebAppInterface(private val context: Context, private val webView: WebView) {

    @JavascriptInterface
    fun print() {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val adapter = webView.createPrintDocumentAdapter("Recibo")
        printManager.print("Impresión POS", adapter, PrintAttributes.Builder().build())
    }
}

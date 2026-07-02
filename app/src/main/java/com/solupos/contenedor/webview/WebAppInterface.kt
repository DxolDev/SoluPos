package com.solupos.contenedor.webview

import android.graphics.Bitmap
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.solupos.contenedor.data.preferences.UserPreferences
import com.solupos.contenedor.printer.ReceiptCapture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class PrintOutcome {
    object Success : PrintOutcome()
    object NotConfigured : PrintOutcome()
    object CaptureFailed : PrintOutcome()
    data class Error(val message: String) : PrintOutcome()
}

class WebAppInterface(
    private val webView: WebView,
    private val scope: CoroutineScope,
    private val userPreferences: UserPreferences,
    private val onPreview: (Bitmap, UserPreferences.PrinterConfig) -> Unit,
    private val onResult: (PrintOutcome) -> Unit
) {
    @JavascriptInterface
    fun print() {
        scope.launch {
            val config = userPreferences.printerConfig.first()
            if (config == null) {
                onResult(PrintOutcome.NotConfigured)
                return@launch
            }
            val bitmap = ReceiptCapture.capturePrintable(webView, config.paperWidthMm)
            if (bitmap == null) {
                onResult(PrintOutcome.CaptureFailed)
                return@launch
            }
            // No se imprime aquí: se entrega el recibo capturado a la UI para
            // que muestre la previsualización y el usuario confirme.
            onPreview(bitmap, config)
        }
    }
}

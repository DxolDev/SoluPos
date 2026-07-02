package com.solupos.contenedor.webview

import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.solupos.contenedor.data.preferences.UserPreferences
import com.solupos.contenedor.printer.BluetoothPrinterManager
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
    private val printerManager: BluetoothPrinterManager,
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
            val bitmap = ReceiptCapture.captureVisibleWebView(webView, config.paperWidthMm)
            if (bitmap == null) {
                onResult(PrintOutcome.CaptureFailed)
                return@launch
            }
            val result = printerManager.printBitmap(config, bitmap)
            onResult(result.fold({ PrintOutcome.Success }, { PrintOutcome.Error(it.message ?: "Error desconocido") }))
        }
    }
}

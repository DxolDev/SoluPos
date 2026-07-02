package com.solupos.contenedor.printer

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.webkit.WebView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Captura lo que el WebView tiene renderizado en pantalla en el momento en que
 * PHP POS invoca window.print() (normalmente una vista/preview del recibo ya
 * visible). Usa PixelCopy sobre el Window de la Activity (API pública, no
 * restringida) en vez de intentar pilotar PrintDocumentAdapter a mano: sus
 * callbacks (LayoutResultCallback/WriteResultCallback) tienen constructor
 * package-private en android.print y no se pueden instanciar desde la app.
 * PixelCopy tampoco tiene overload directo para View arbitraria (solo Surface,
 * SurfaceView o Window), así que se recorta la región del WebView dentro del
 * Window de la Activity que lo contiene.
 */
object ReceiptCapture {

    private const val DPI = 203

    suspend fun captureVisibleWebView(webView: WebView, paperWidthMm: Int): Bitmap? {
        val raw = capturePixels(webView) ?: return null
        val trimmed = trimWhitespace(raw)
        return scaleToPaperWidth(trimmed, paperWidthMm)
    }

    private suspend fun capturePixels(webView: WebView): Bitmap? = withContext(Dispatchers.Main) {
        val window = webView.context.findActivity()?.window ?: return@withContext null
        val width = webView.width
        val height = webView.height
        if (width <= 0 || height <= 0) return@withContext null

        val location = IntArray(2)
        webView.getLocationInWindow(location)
        val srcRect = Rect(location[0], location[1], location[0] + width, location[1] + height)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val success = suspendCancellableCoroutine { cont ->
            PixelCopy.request(window, srcRect, bitmap, { result ->
                if (cont.isActive) cont.resume(result == PixelCopy.SUCCESS)
            }, Handler(Looper.getMainLooper()))
        }
        if (success) bitmap else null
    }

    private fun Context.findActivity(): Activity? {
        var ctx = this
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    private fun trimWhitespace(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var top = -1
        for (y in 0 until height) {
            if ((0 until width).any { x -> !isWhite(pixels[y * width + x]) }) { top = y; break }
        }
        if (top < 0) return bitmap // todo blanco: nada que recortar

        var bottom = height - 1
        for (y in height - 1 downTo 0) {
            if ((0 until width).any { x -> !isWhite(pixels[y * width + x]) }) { bottom = y; break }
        }

        val margin = 16
        val y0 = (top - margin).coerceAtLeast(0)
        val y1 = (bottom + margin).coerceAtMost(height - 1)
        if (y0 == 0 && y1 == height - 1) return bitmap
        return Bitmap.createBitmap(bitmap, 0, y0, width, y1 - y0 + 1)
    }

    private fun scaleToPaperWidth(bitmap: Bitmap, paperWidthMm: Int): Bitmap {
        val targetWidthDots = ((paperWidthMm / 25.4) * DPI).toInt()
        if (targetWidthDots <= 0 || bitmap.width == targetWidthDots) return bitmap
        val scale = targetWidthDots.toFloat() / bitmap.width
        val targetHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetWidthDots, targetHeight, true)
    }

    private fun isWhite(pixel: Int): Boolean {
        return Color.red(pixel) > 245 && Color.green(pixel) > 245 && Color.blue(pixel) > 245
    }
}

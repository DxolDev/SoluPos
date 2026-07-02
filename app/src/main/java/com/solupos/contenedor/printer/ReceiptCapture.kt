package com.solupos.contenedor.printer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.webkit.WebView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Genera el bitmap de lo que el sitio realmente imprime (no una captura de la
 * pantalla). PHP POS no imprime la vista en pantalla: usa reglas CSS
 * `@media print` que ocultan el formulario/controles y dejan solo el contenido
 * imprimible (recibo, etiqueta de código de barras, etc.). Al hacer una simple
 * captura de pantalla se obtenía el formulario completo en vez del recibo.
 *
 * Estrategia:
 *  1. Emular impresión: recolectar todas las reglas dentro de bloques
 *     `@media print` y aplicarlas en pantalla, de modo que el WebView renderice
 *     igual que en el diálogo de impresión del navegador.
 *  2. Dibujar el contenido COMPLETO del WebView (no solo el viewport) a un
 *     bitmap, para no truncar recibos más largos que la pantalla. Requiere que
 *     el WebView esté en LAYER_TYPE_SOFTWARE (ya configurado en WebViewScreen).
 *  3. Restaurar la vista normal.
 *  4. Recortar el blanco sobrante y escalar al ancho del papel.
 */
object ReceiptCapture {

    private const val DPI = 203
    private const val REFLOW_DELAY_MS = 350L
    private const val MAX_HEIGHT_PX = 8000
    private const val BW_THRESHOLD = 180 // < = negro; el resto blanco. Sube grises tenues a negro.

    suspend fun capturePrintable(webView: WebView, paperWidthMm: Int): Bitmap? {
        runJs(webView, EMULATE_PRINT_JS)
        // Forzar el contenido a lo ancho del ticket para que se maquete angosto:
        // así, al escalar a los puntos del papel, el texto no se reduce (o crece)
        // y sale a buen tamaño en vez de diminuto.
        runJs(webView, widthConstraintJs(paperCssPx(paperWidthMm)))
        delay(REFLOW_DELAY_MS) // deja que el WebView remaquete
        val raw = drawFullContent(webView)
        runJs(webView, RESTORE_JS)
        if (raw == null) return null
        val trimmed = trimWhitespace(raw)
        val scaled = scaleToPaperWidth(trimmed, paperWidthMm)
        // Binarizar a blanco/negro puro: elimina los grises del render (que salían
        // tenues en térmica) → texto negro sólido y nítido.
        return binarize(scaled)
    }

    private fun binarize(bitmap: Bitmap): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        for (i in pixels.indices) {
            val p = pixels[i]
            val lum = (Color.red(p) * 299 + Color.green(p) * 587 + Color.blue(p) * 114) / 1000
            pixels[i] = if (lum < BW_THRESHOLD) Color.BLACK else Color.WHITE
        }
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(pixels, 0, w, 0, 0, w, h)
        return out
    }

    /**
     * Ancho CSS al que se maqueta el recibo antes de capturar. Más angosto =
     * texto más grande al escalar al ancho del papel. Afinado empíricamente.
     */
    private fun paperCssPx(paperWidthMm: Int): Int = if (paperWidthMm <= 58) 150 else 240

    private fun widthConstraintJs(cssWidth: Int): String = """
        (function() {
            var s = document.getElementById('solupos-width');
            if (!s) { s = document.createElement('style'); s.id = 'solupos-width'; document.head.appendChild(s); }
            // width en html/body fija el ancho del ticket; max-width:100% en todo
            // evita que el logo u otros elementos de ancho fijo se desborden y se
            // corten; img height:auto mantiene la proporción del logo al escalar.
            s.textContent = 'html,body{width:${cssWidth}px !important;min-width:0 !important;' +
                'max-width:${cssWidth}px !important;margin:0 auto !important;}' +
                '*{max-width:100% !important;box-sizing:border-box !important;}' +
                'img{height:auto !important;}';
        })();
    """.trimIndent()

    private suspend fun runJs(webView: WebView, js: String) = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { cont ->
            webView.evaluateJavascript(js) { if (cont.isActive) cont.resume(Unit) }
        }
    }

    private suspend fun drawFullContent(webView: WebView): Bitmap? = withContext(Dispatchers.Main) {
        val width = webView.width
        if (width <= 0) return@withContext null
        val density = webView.resources.displayMetrics.density
        val height = (webView.contentHeight * density).toInt()
            .coerceIn(1, MAX_HEIGHT_PX)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        webView.draw(canvas)
        bitmap
    }

    private fun trimWhitespace(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // El contenido imprimible (texto, barras) siempre es OSCURO; bordes y
        // fondos tenues son claros y deben ignorarse. Detectar por "oscuridad"
        // (no por "no-blanco") evita que una línea gris tenue en un borde impida
        // recortar. Una fila/columna cuenta como contenido si tiene varios
        // píxeles oscuros (minRun) para ignorar motas sueltas.
        val minRun = 2

        fun rowHasContent(y: Int): Boolean {
            var c = 0
            for (x in 0 until width) {
                if (isDark(pixels[y * width + x])) { c++; if (c >= minRun) return true }
            }
            return false
        }

        var top = -1
        for (y in 0 until height) { if (rowHasContent(y)) { top = y; break } }
        if (top < 0) return bitmap // sin contenido oscuro: nada que recortar

        var bottom = height - 1
        for (y in height - 1 downTo 0) { if (rowHasContent(y)) { bottom = y; break } }

        fun colHasContent(x: Int): Boolean {
            var c = 0
            for (y in top..bottom) {
                if (isDark(pixels[y * width + x])) { c++; if (c >= minRun) return true }
            }
            return false
        }

        // Recorte horizontal: sin esto el bitmap conserva todo el ancho de la
        // página (con la etiqueta a un lado) y al escalarlo al ancho del papel
        // el contenido sale pequeño y descentrado. Recortando a la caja real, el
        // contenido llena el ancho del papel al escalar.
        var left = 0
        for (x in 0 until width) { if (colHasContent(x)) { left = x; break } }
        var right = width - 1
        for (x in width - 1 downTo 0) { if (colHasContent(x)) { right = x; break } }

        val marginV = 16
        val marginH = 8
        val x0 = (left - marginH).coerceAtLeast(0)
        val x1 = (right + marginH).coerceAtMost(width - 1)
        val y0 = (top - marginV).coerceAtLeast(0)
        val y1 = (bottom + marginV).coerceAtMost(height - 1)
        return Bitmap.createBitmap(bitmap, x0, y0, x1 - x0 + 1, y1 - y0 + 1)
    }

    private fun scaleToPaperWidth(bitmap: Bitmap, paperWidthMm: Int): Bitmap {
        // Mismo ancho imprimible real que usa la impresora, para que la imagen
        // coincida con el ancho físico y [C] la centre exacta.
        val targetWidthDots = BluetoothPrinterManager.printableDots(paperWidthMm)
        if (targetWidthDots <= 0 || bitmap.width == targetWidthDots) return bitmap
        val scale = targetWidthDots.toFloat() / bitmap.width
        val targetHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetWidthDots, targetHeight, true)
    }

    private fun isDark(pixel: Int): Boolean {
        val r = Color.red(pixel)
        val g = Color.green(pixel)
        val b = Color.blue(pixel)
        val luminance = (r * 299 + g * 587 + b * 114) / 1000
        return luminance < 160
    }

    /**
     * Recolecta todas las reglas dentro de bloques @media print y las aplica en
     * pantalla mediante un <style> inyectado, para que el render coincida con lo
     * que se imprimiría. Las hojas de estilo de otros orígenes lanzan al leer
     * cssRules y simplemente se omiten.
     */
    private val EMULATE_PRINT_JS = """
        (function() {
            try {
                var out = [];
                for (var i = 0; i < document.styleSheets.length; i++) {
                    var rules;
                    try { rules = document.styleSheets[i].cssRules; } catch (e) { continue; }
                    if (!rules) continue;
                    for (var j = 0; j < rules.length; j++) {
                        var r = rules[j];
                        if (r.type === 4 /* MEDIA_RULE */ && /print/i.test(r.media.mediaText)) {
                            for (var k = 0; k < r.cssRules.length; k++) {
                                var cr = r.cssRules[k];
                                if (cr.type === 1 /* STYLE_RULE */) out.push(cr.cssText);
                            }
                        }
                    }
                }
                var s = document.getElementById('solupos-print-emu');
                if (!s) { s = document.createElement('style'); s.id = 'solupos-print-emu'; document.head.appendChild(s); }
                s.textContent = out.join('\n');
                window.scrollTo(0, 0);
            } catch (e) {}
        })();
    """.trimIndent()

    private val RESTORE_JS = """
        (function() {
            ['solupos-print-emu', 'solupos-width'].forEach(function(id) {
                var s = document.getElementById(id);
                if (s) s.remove();
            });
        })();
    """.trimIndent()
}

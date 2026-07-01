package com.solupos.contenedor.webview

object BarcodeInjector {
    fun buildScript(barcode: String): String {
        val safe = barcode.replace("\\", "\\\\").replace("'", "\\'")
        return """
            (function(value) {
                var el = document.activeElement;
                if (!el || (el.tagName !== 'INPUT' && el.tagName !== 'TEXTAREA')) {
                    el = document.querySelector(
                        'input[type="text"]:not([disabled]):not([readonly]),' +
                        'input[type="search"]:not([disabled]):not([readonly]),' +
                        'input:not([type]):not([disabled]):not([readonly])'
                    );
                }
                if (!el) return;
                var nativeSetter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value');
                if (nativeSetter && nativeSetter.set) {
                    nativeSetter.set.call(el, value);
                } else {
                    el.value = value;
                }
                el.dispatchEvent(new Event('input', { bubbles: true }));
                el.dispatchEvent(new Event('change', { bubbles: true }));
                el.dispatchEvent(new KeyboardEvent('keyup', { bubbles: true, key: 'Enter' }));
                if (window.$ || window.jQuery) {
                    (window.$ || window.jQuery)(el).trigger('change').trigger('input');
                }
            })('$safe');
        """.trimIndent()
    }
}

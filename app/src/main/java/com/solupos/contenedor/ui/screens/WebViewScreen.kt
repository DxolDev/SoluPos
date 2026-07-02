package com.solupos.contenedor.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.os.Message
import android.view.inputmethod.InputMethodManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.solupos.contenedor.data.preferences.UserPreferences
import com.solupos.contenedor.data.repository.StoreRepository
import com.solupos.contenedor.printer.BluetoothPrinterManager
import com.solupos.contenedor.webview.BarcodeInjector
import com.solupos.contenedor.webview.PosWebViewClient
import com.solupos.contenedor.webview.PrintOutcome
import com.solupos.contenedor.webview.WebAppInterface
import kotlinx.coroutines.launch

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(
    storeId: String,
    repository: StoreRepository,
    userPreferences: UserPreferences,
    printerManager: BluetoothPrinterManager,
    onBack: () -> Unit,
    onOpenPrinterSettings: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var url by remember { mutableStateOf<String?>(null) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var showScanner by remember { mutableStateOf(false) }

    LaunchedEffect(storeId) {
        url = repository.getById(storeId)?.url
    }

    BackHandler(enabled = showScanner) {
        showScanner = false
    }
    BackHandler(enabled = !showScanner) {
        val wv = webViewRef
        if (wv != null && wv.canGoBack()) wv.goBack() else onBack()
    }

    fun handlePrintOutcome(outcome: PrintOutcome) {
        coroutineScope.launch {
            val message = when (outcome) {
                PrintOutcome.Success -> "Ticket enviado a la impresora"
                PrintOutcome.NotConfigured -> "No hay impresora configurada"
                PrintOutcome.CaptureFailed -> "No se pudo capturar el recibo"
                is PrintOutcome.Error -> "Error al imprimir: ${outcome.message}"
            }
            val actionLabel = if (outcome == PrintOutcome.NotConfigured) "Configurar" else null
            val result = snackbarHostState.showSnackbar(message, actionLabel = actionLabel)
            if (result == SnackbarResult.ActionPerformed) onOpenPrinterSettings()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            url?.let { storeUrl ->
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            addJavascriptInterface(
                                WebAppInterface(
                                    webView = this,
                                    scope = coroutineScope,
                                    userPreferences = userPreferences,
                                    printerManager = printerManager,
                                    onResult = ::handlePrintOutcome
                                ),
                                "Android"
                            )
                            webViewClient = object : PosWebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    view?.evaluateJavascript(
                                        """
                                        window.print = function() { Android.print(); };
                                        (function() {
                                            var opener = document.getElementById('open-left');
                                            var leftBar = document.querySelector('.left-bar');
                                            if (!opener || !leftBar) return;

                                            // El menú lateral original (.left-bar) es position:fixed
                                            // con z-index máximo (2147483647) y se posiciona fuera de
                                            // pantalla (margin-left:-225). En el WebView de Android su
                                            // capa de render queda "colgada": el fondo se pinta pero el
                                            // contenido (los ítems) NUNCA se rasteriza, aunque el DOM,
                                            // la geometría, el color y la visibilidad sean correctos
                                            // (confirmado con DevTools: elementFromPoint sobre el texto
                                            // devuelve el contenedor, no el texto). Ni software
                                            // rendering, ni repintados, ni bajar el z-index lo
                                            // resuelven, y mover el nodo real rompe el JS del sitio.
                                            // Solución: construir un panel propio, nuevo (que el
                                            // WebView sí pinta) con el contenido del menú, y delegar
                                            // cada clic al enlace ORIGINAL para conservar navegación y
                                            // handlers (incl. submenús, re-sincronizando tras el clic).
                                            if (!document.getElementById('solupos-menu-css')) {
                                                var css = document.createElement('style');
                                                css.id = 'solupos-menu-css';
                                                css.textContent =
                                                    '#solupos-menu-ov{position:fixed;inset:0;background:rgba(0,0,0,.5);z-index:2147483646;display:none;}' +
                                                    '#solupos-menu-ov.open{display:block;}' +
                                                    '#solupos-menu{position:fixed;top:0;left:0;width:260px;max-width:85%;height:100%;background:#373942;overflow-y:auto;z-index:2147483647;transform:translateX(-100%);transition:transform .2s ease;box-shadow:2px 0 8px rgba(0,0,0,.4);}' +
                                                    '#solupos-menu.open{transform:translateX(0);}' +
                                                    '#solupos-menu,#solupos-menu *{color:#fff !important;-webkit-text-fill-color:#fff !important;box-sizing:border-box;}' +
                                                    '#solupos-menu ul{list-style:none;margin:0;padding:0;}' +
                                                    '#solupos-menu li{display:block;}' +
                                                    '#solupos-menu a{display:block;padding:12px 16px;text-decoration:none;border-bottom:1px solid rgba(255,255,255,.08);}' +
                                                    '#solupos-menu img{max-width:100%;height:auto;}';
                                                document.head.appendChild(css);
                                            }

                                            var overlay = document.getElementById('solupos-menu-ov');
                                            if (!overlay) {
                                                overlay = document.createElement('div');
                                                overlay.id = 'solupos-menu-ov';
                                                document.body.appendChild(overlay);
                                                overlay.addEventListener('click', closeMenu);
                                            }
                                            var menu = document.getElementById('solupos-menu');
                                            if (!menu) {
                                                menu = document.createElement('div');
                                                menu.id = 'solupos-menu';
                                                document.body.appendChild(menu);
                                                menu.addEventListener('click', function(e) {
                                                    var t = e.target.closest ? e.target.closest('[data-si]') : null;
                                                    if (!t) return;
                                                    e.preventDefault();
                                                    e.stopPropagation();
                                                    var orig = leftBar.querySelector('a[data-si="' + t.getAttribute('data-si') + '"]');
                                                    if (orig) orig.click();
                                                    // Si fue un despliegue de submenú (no navegó),
                                                    // re-sincroniza el clon para reflejar el cambio.
                                                    var sy = menu.scrollTop;
                                                    setTimeout(function() {
                                                        if (document.body.contains(menu) && menu.classList.contains('open')) {
                                                            syncMenu();
                                                            menu.scrollTop = sy;
                                                        }
                                                    }, 300);
                                                });
                                            }

                                            function syncMenu() {
                                                var links = leftBar.querySelectorAll('a');
                                                for (var i = 0; i < links.length; i++) links[i].setAttribute('data-si', i);
                                                menu.innerHTML = leftBar.innerHTML;
                                            }
                                            function openMenu() {
                                                syncMenu();
                                                overlay.classList.add('open');
                                                menu.classList.add('open');
                                            }
                                            function closeMenu() {
                                                overlay.classList.remove('open');
                                                menu.classList.remove('open');
                                            }

                                            opener.addEventListener('click', function(e) {
                                                e.stopImmediatePropagation();
                                                e.preventDefault();
                                                if (menu.classList.contains('open')) closeMenu(); else openMenu();
                                            }, true);
                                        })();
                                        """.trimIndent(),
                                        null
                                    )
                                }
                            }
                            // Algunas acciones de PHP POS (ej. "Imprimir" de un recibo) abren una
                            // "ventana nueva" (window.open / target=_blank) en vez de llamar a
                            // window.print() directo. El WebView no soporta múltiples ventanas por
                            // defecto, así que sin esto esa navegación simplemente no hace nada.
                            // Se redirige esa URL a este mismo WebView.
                            webChromeClient = object : WebChromeClient() {
                                override fun onCreateWindow(
                                    view: WebView?,
                                    isDialog: Boolean,
                                    isUserGesture: Boolean,
                                    resultMsg: Message?
                                ): Boolean {
                                    val popupWebView = WebView(ctx)
                                    popupWebView.webViewClient = object : WebViewClient() {
                                        override fun shouldOverrideUrlLoading(
                                            popupView: WebView?,
                                            request: WebResourceRequest?
                                        ): Boolean {
                                            request?.url?.let { uri -> loadUrl(uri.toString()) }
                                            return true
                                        }
                                    }
                                    val transport = resultMsg?.obj as? WebView.WebViewTransport
                                    transport?.webView = popupWebView
                                    resultMsg?.sendToTarget()
                                    return true
                                }
                            }
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            settings.javaScriptCanOpenWindowsAutomatically = true
                            settings.setSupportMultipleWindows(true)
                            // Quita la marca "; wv)" que Android WebView agrega al user-agent para
                            // identificarse como navegador embebido. Algunos sitios (como este POS)
                            // sirven contenido incompleto/roto (ej. el menú responsivo) cuando
                            // detectan esa marca, aunque funcionen bien en Chrome/Brave normales.
                            settings.userAgentString = settings.userAgentString.replace("; wv)", ")")
                            // El menú lateral (.left-bar) es position:fixed y contiene un <ul> con
                            // scroll interno (overflow-y:auto). En el WebView acelerado por hardware
                            // esa combinación (fixed + scroll interno) hace que el panel se promueva a
                            // su propia capa que no se repinta: el fondo se dibuja pero el contenido
                            // (los ítems del menú) sale en blanco, aunque el DOM sea correcto. En
                            // Brave/Chrome no pasa. Forzar renderizado por software corrige de forma
                            // fiable este bug de composición de capas del WebView. No afecta la
                            // captura de recibos (ReceiptCapture usa PixelCopy sobre la ventana).
                            setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
                            webViewRef = this
                            loadUrl(storeUrl)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (!showScanner) {
                FloatingActionButton(
                    onClick = {
                        webViewRef?.let { wv ->
                            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                            imm.hideSoftInputFromWindow(wv.windowToken, 0)
                            wv.clearFocus()
                        }
                        showScanner = true
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .imePadding()
                        .padding(bottom = 16.dp, end = 16.dp)
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Escanear código")
                }
            }

            if (showScanner) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    ScannerScreen(
                        onBarcodeScanned = { barcode ->
                            webViewRef?.evaluateJavascript(BarcodeInjector.buildScript(barcode), null)
                            showScanner = false
                        },
                        onCancel = { showScanner = false }
                    )
                }
            }
        }
    }
}

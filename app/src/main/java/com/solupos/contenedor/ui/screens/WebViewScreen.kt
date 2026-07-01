package com.solupos.contenedor.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.solupos.contenedor.data.repository.StoreRepository
import com.solupos.contenedor.ui.navigation.Screen
import com.solupos.contenedor.webview.BarcodeInjector
import com.solupos.contenedor.webview.PosWebViewClient
import com.solupos.contenedor.webview.WebAppInterface

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(
    storeId: String,
    repository: StoreRepository,
    navController: NavController,
    onBack: () -> Unit
) {
    var url by remember { mutableStateOf<String?>(null) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    LaunchedEffect(storeId) {
        url = repository.getById(storeId)?.url
    }

    val barcode by navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow<String?>("barcode", null)
        ?.collectAsState()
        ?: remember { mutableStateOf(null) }

    LaunchedEffect(barcode) {
        barcode?.let { code ->
            webViewRef?.evaluateJavascript(BarcodeInjector.buildScript(code), null)
            navController.currentBackStackEntry?.savedStateHandle?.set("barcode", null)
        }
    }

    BackHandler {
        val wv = webViewRef
        if (wv != null && wv.canGoBack()) wv.goBack() else onBack()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        url?.let { storeUrl ->
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        addJavascriptInterface(WebAppInterface(context, this), "Android")
                        webViewClient = object : PosWebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                view?.evaluateJavascript(
                                    "window.print = function() { Android.print(); };", null
                                )
                            }
                        }
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        webViewRef = this
                        loadUrl(storeUrl)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        FloatingActionButton(
            onClick = { navController.navigate(Screen.Scanner.route) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp)
        ) {
            Icon(Icons.Default.QrCodeScanner, contentDescription = "Escanear código")
        }
    }
}

package com.solupos.contenedor

import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.solupos.contenedor.ui.navigation.AppNavHost
import com.solupos.contenedor.ui.theme.SoluPosTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
        enableEdgeToEdge()
        setContent {
            SoluPosTheme {
                AppNavHost()
            }
        }
    }
}

package com.solupos.contenedor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.solupos.contenedor.ui.navigation.AppNavHost
import com.solupos.contenedor.ui.theme.SoluPosTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SoluPosTheme {
                AppNavHost()
            }
        }
    }
}

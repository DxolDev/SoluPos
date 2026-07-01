package com.solupos.contenedor.ui.navigation

sealed class Screen(val route: String) {
    object StoreList : Screen("storeList")
    object StoreForm : Screen("storeForm/{storeId}") {
        fun createRoute(storeId: String? = null) =
            if (storeId != null) "storeForm/$storeId" else "storeForm/new"
    }
    object WebView : Screen("webview/{storeId}") {
        fun createRoute(storeId: String) = "webview/$storeId"
    }
    object Scanner : Screen("scanner")
}

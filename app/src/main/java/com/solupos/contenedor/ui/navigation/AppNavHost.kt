package com.solupos.contenedor.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.solupos.contenedor.SoluPosApp
import com.solupos.contenedor.ui.screens.*
import com.solupos.contenedor.viewmodel.*

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val app = LocalContext.current.applicationContext as SoluPosApp

    NavHost(navController = navController, startDestination = Screen.StoreList.route) {

        composable(Screen.StoreList.route) {
            val viewModel: StoreListViewModel = viewModel(
                factory = StoreListViewModelFactory(app.storeRepository)
            )
            StoreListScreen(
                viewModel = viewModel,
                onAddStore = { navController.navigate(Screen.StoreForm.createRoute()) },
                onEditStore = { id -> navController.navigate(Screen.StoreForm.createRoute(id)) },
                onOpenStore = { id -> navController.navigate(Screen.WebView.createRoute(id)) }
            )
        }

        composable(
            route = Screen.StoreForm.route,
            arguments = listOf(navArgument("storeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val storeId = backStackEntry.arguments?.getString("storeId")?.takeIf { it != "new" }
            val viewModel: StoreFormViewModel = viewModel(
                factory = StoreFormViewModelFactory(app.storeRepository, storeId)
            )
            StoreFormScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.WebView.route,
            arguments = listOf(navArgument("storeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val storeId = backStackEntry.arguments?.getString("storeId") ?: return@composable
            WebViewScreen(
                storeId = storeId,
                repository = app.storeRepository,
                navController = navController,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Scanner.route) {
            ScannerScreen(
                onBarcodeScanned = { barcode ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("barcode", barcode)
                    navController.popBackStack()
                },
                onCancel = { navController.popBackStack() }
            )
        }
    }
}

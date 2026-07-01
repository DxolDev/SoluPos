package com.solupos.contenedor.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.solupos.contenedor.data.db.StoreEntity
import com.solupos.contenedor.ui.components.StoreCard
import com.solupos.contenedor.viewmodel.StoreListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreListScreen(
    viewModel: StoreListViewModel,
    onAddStore: () -> Unit,
    onEditStore: (String) -> Unit,
    onOpenStore: (String) -> Unit
) {
    val stores by viewModel.stores.collectAsState()
    var storeToDelete by remember { mutableStateOf<StoreEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Tiendas") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddStore) {
                Icon(Icons.Default.Add, contentDescription = "Agregar tienda")
            }
        }
    ) { padding ->
        if (stores.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No hay tiendas guardadas.\nToca + para agregar una.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(stores, key = { it.id }) { store ->
                    StoreCard(
                        store = store,
                        onClick = {
                            viewModel.saveLastStore(store.id)
                            onOpenStore(store.id)
                        },
                        onEdit = { onEditStore(store.id) },
                        onDelete = { storeToDelete = store }
                    )
                }
            }
        }
    }

    storeToDelete?.let { store ->
        AlertDialog(
            onDismissRequest = { storeToDelete = null },
            title = { Text("Eliminar tienda") },
            text = { Text("¿Eliminar \"${store.name}\"?") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteStore(store); storeToDelete = null }) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { storeToDelete = null }) { Text("Cancelar") }
            }
        )
    }
}

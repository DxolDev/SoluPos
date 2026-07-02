package com.solupos.contenedor.ui.screens

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.solupos.contenedor.printer.PrinterConnectionState
import com.solupos.contenedor.ui.components.PrintPreviewDialog
import com.solupos.contenedor.viewmodel.PrinterSettingsViewModel

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PrinterSettingsScreen(
    viewModel: PrinterSettingsViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val testPreview by viewModel.testPreview.collectAsState()
    val context = LocalContext.current

    val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        rememberMultiplePermissionsState(
            listOf(android.Manifest.permission.BLUETOOTH_CONNECT, android.Manifest.permission.BLUETOOTH_SCAN)
        )
    } else {
        null
    }
    val hasBluetoothPermission = bluetoothPermissions == null || bluetoothPermissions.allPermissionsGranted

    LaunchedEffect(hasBluetoothPermission) {
        if (hasBluetoothPermission) viewModel.refreshBondedDevices()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurar impresora") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        if (bluetoothPermissions != null && !hasBluetoothPermission) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text(
                        "Se necesita permiso de Bluetooth para ver y conectar las impresoras vinculadas.",
                        textAlign = TextAlign.Center
                    )
                    Button(onClick = { bluetoothPermissions.launchMultiplePermissionRequest() }) {
                        Text("Permitir Bluetooth")
                    }
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("Impresora Bluetooth", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            if (state.bondedDevices.isEmpty()) {
                Text(
                    "No hay impresoras vinculadas. Empareja la impresora desde Ajustes de Android.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(modifier = Modifier.selectableGroup()) {
                    state.bondedDevices.forEach { device ->
                        val name = remember(device) {
                            try {
                                device.name ?: device.address
                            } catch (e: SecurityException) {
                                device.address
                            }
                        }
                        val selected = state.selectedMac == device.address
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(selected = selected, onClick = { viewModel.selectDevice(device) })
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selected, onClick = { viewModel.selectDevice(device) })
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(name, fontWeight = FontWeight.Medium)
                                Text(
                                    device.address,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Vincular nuevo dispositivo")
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Ancho de papel", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.paperWidthMm == 58,
                    onClick = { viewModel.setPaperWidth(58) },
                    label = { Text("58mm") }
                )
                FilterChip(
                    selected = state.paperWidthMm == 80,
                    onClick = { viewModel.setPaperWidth(80) },
                    label = { Text("80mm") }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            when (val connectionState = state.connectionState) {
                is PrinterConnectionState.Connecting ->
                    Text("Conectando...", color = MaterialTheme.colorScheme.primary)
                is PrinterConnectionState.Connected ->
                    Text("Conectado a ${connectionState.deviceName}", color = MaterialTheme.colorScheme.primary)
                is PrinterConnectionState.Error ->
                    Text("Error: ${connectionState.message}", color = MaterialTheme.colorScheme.error)
                PrinterConnectionState.Idle -> {}
            }

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { viewModel.requestTestPrint() },
                enabled = state.selectedMac != null && state.connectionState !is PrinterConnectionState.Connecting
            ) {
                Text("Imprimir prueba")
            }
        }

        testPreview?.let { preview ->
            PrintPreviewDialog(
                text = preview,
                onConfirm = { viewModel.confirmTestPrint() },
                onDismiss = { viewModel.dismissTestPreview() }
            )
        }
    }
}

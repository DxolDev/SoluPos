package com.solupos.contenedor.viewmodel

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.solupos.contenedor.R
import com.solupos.contenedor.data.preferences.UserPreferences
import com.solupos.contenedor.printer.BluetoothPrinterManager
import com.solupos.contenedor.printer.PrinterConnectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PrinterSettingsUiState(
    val bondedDevices: List<BluetoothDevice> = emptyList(),
    val selectedMac: String? = null,
    val paperWidthMm: Int = UserPreferences.DEFAULT_PAPER_WIDTH_MM,
    val connectionState: PrinterConnectionState = PrinterConnectionState.Idle
)

class PrinterSettingsViewModel(
    private val context: Context,
    private val userPreferences: UserPreferences,
    private val printerManager: BluetoothPrinterManager
) : ViewModel() {

    private val _state = MutableStateFlow(PrinterSettingsUiState())
    val state: StateFlow<PrinterSettingsUiState> = _state.asStateFlow()

    init {
        refreshBondedDevices()
        viewModelScope.launch {
            userPreferences.printerConfig.collect { config ->
                _state.value = _state.value.copy(
                    selectedMac = config?.macAddress,
                    paperWidthMm = config?.paperWidthMm ?: UserPreferences.DEFAULT_PAPER_WIDTH_MM
                )
            }
        }
        viewModelScope.launch {
            printerManager.state.collect { connectionState ->
                _state.value = _state.value.copy(connectionState = connectionState)
            }
        }
    }

    fun refreshBondedDevices() {
        _state.value = _state.value.copy(bondedDevices = printerManager.bondedPrinters())
    }

    fun selectDevice(device: BluetoothDevice) {
        val name = deviceName(device)
        viewModelScope.launch {
            userPreferences.savePrinterConfig(
                UserPreferences.PrinterConfig(device.address, name, _state.value.paperWidthMm)
            )
        }
    }

    fun setPaperWidth(mm: Int) {
        _state.value = _state.value.copy(paperWidthMm = mm)
        viewModelScope.launch {
            userPreferences.printerConfig.first()?.let { current ->
                userPreferences.savePrinterConfig(current.copy(paperWidthMm = mm))
            }
        }
    }

    fun testPrint() {
        viewModelScope.launch {
            val config = userPreferences.printerConfig.first() ?: return@launch
            printerManager.printTicket(config, loadLogoBitmap(), buildTestTicket(config))
        }
    }

    private fun loadLogoBitmap(): Bitmap? =
        try {
            BitmapFactory.decodeResource(context.resources, R.drawable.logo_solupos_print)
        } catch (e: Exception) {
            null
        }

    private fun buildTestTicket(config: UserPreferences.PrinterConfig): String {
        val separator = "-".repeat(if (config.paperWidthMm <= 58) 32 else 48)
        val timestamp = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        return buildString {
            appendLine("[C]La solución completa para tu punto de venta")
            appendLine("[L]$separator")
            appendLine("[L]<b>Prueba de impresión</b>")
            appendLine("[L]Impresora: ${config.name}")
            appendLine("[L]Papel: ${config.paperWidthMm}mm")
            appendLine("[L]Fecha: $timestamp")
            appendLine("[L]$separator")
            appendLine("[C]<b>¡Conexión exitosa!</b>")
            appendLine("[C]Ya puedes imprimir tus recibos")
            appendLine("[L]")
            appendLine("[L]")
        }
    }

    private fun deviceName(device: BluetoothDevice): String =
        try {
            device.name ?: device.address
        } catch (e: SecurityException) {
            device.address
        }
}

class PrinterSettingsViewModelFactory(
    private val context: Context,
    private val userPreferences: UserPreferences,
    private val printerManager: BluetoothPrinterManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        PrinterSettingsViewModel(context, userPreferences, printerManager) as T
}

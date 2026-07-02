package com.solupos.contenedor.printer

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.graphics.Bitmap
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import com.dantsu.escposprinter.textparser.PrinterTextParserImg
import com.solupos.contenedor.data.preferences.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * Conexión Bluetooth clásica (SPP) con una impresora térmica ESC/POS ya
 * emparejada desde Ajustes de Android. No hace discovery activo: solo
 * ofrece los dispositivos ya vinculados (BluetoothAdapter.bondedDevices).
 */
class BluetoothPrinterManager {

    private val _state = MutableStateFlow<PrinterConnectionState>(PrinterConnectionState.Idle)
    val state: StateFlow<PrinterConnectionState> = _state

    fun bondedPrinters(): List<BluetoothDevice> {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
        return try {
            adapter.bondedDevices?.toList() ?: emptyList()
        } catch (e: SecurityException) {
            emptyList()
        }
    }

    suspend fun printText(config: UserPreferences.PrinterConfig, text: String): Result<Unit> =
        withPrinter(config) { printer -> printer.printFormattedTextAndCut(text) }

    suspend fun printBitmap(config: UserPreferences.PrinterConfig, bitmap: Bitmap): Result<Unit> =
        withPrinter(config) { printer ->
            val hex = PrinterTextParserImg.bitmapToHexadecimalString(printer, bitmap, true)
            printer.printFormattedTextAndCut("[C]<img>$hex</img>\n")
        }

    suspend fun printTicket(
        config: UserPreferences.PrinterConfig,
        logo: Bitmap?,
        body: String
    ): Result<Unit> = withPrinter(config) { printer ->
        val content = buildString {
            if (logo != null) {
                val hex = PrinterTextParserImg.bitmapToHexadecimalString(printer, logo, true)
                appendLine("[C]<img>$hex</img>")
            }
            append(body)
        }
        printer.printFormattedTextAndCut(content)
    }

    private suspend fun withPrinter(
        config: UserPreferences.PrinterConfig,
        action: (EscPosPrinter) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        _state.value = PrinterConnectionState.Connecting
        runCatching {
            withTimeout(CONNECT_TIMEOUT_MS) {
                val device = BluetoothAdapter.getDefaultAdapter()
                    ?.bondedDevices
                    ?.firstOrNull { it.address == config.macAddress }
                    ?: throw IllegalStateException("Impresora no vinculada. Empareja la impresora desde Ajustes.")

                val connection = BluetoothConnection(device)
                val printer = EscPosPrinter(
                    connection,
                    DPI,
                    config.paperWidthMm.toFloat(),
                    charsPerLine(config.paperWidthMm)
                )
                try {
                    action(printer)
                    _state.value = PrinterConnectionState.Connected(config.name)
                } finally {
                    connection.disconnect()
                }
            }
        }.onFailure { error ->
            _state.value = PrinterConnectionState.Error(error.message ?: "Error al imprimir")
        }
    }

    private fun charsPerLine(widthMm: Int) = if (widthMm <= 58) 32 else 48

    companion object {
        private const val DPI = 203
        private const val CONNECT_TIMEOUT_MS = 8000L
    }
}

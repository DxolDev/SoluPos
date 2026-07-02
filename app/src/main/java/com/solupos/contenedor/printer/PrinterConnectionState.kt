package com.solupos.contenedor.printer

sealed class PrinterConnectionState {
    object Idle : PrinterConnectionState()
    object Connecting : PrinterConnectionState()
    data class Connected(val deviceName: String) : PrinterConnectionState()
    data class Error(val message: String) : PrinterConnectionState()
}

package com.solupos.contenedor.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPreferences(private val context: Context) {
    private val lastStoreIdKey = stringPreferencesKey("last_store_id")
    private val printerMacKey = stringPreferencesKey("printer_mac")
    private val printerNameKey = stringPreferencesKey("printer_name")
    private val paperWidthMmKey = intPreferencesKey("paper_width_mm")
    private val hasSeenTutorialKey = booleanPreferencesKey("has_seen_tutorial")
    private val hasSeenWebViewTutorialKey = booleanPreferencesKey("has_seen_webview_tutorial")

    val lastStoreId: Flow<String?> = context.dataStore.data.map { it[lastStoreIdKey] }

    suspend fun saveLastStoreId(id: String) {
        context.dataStore.edit { it[lastStoreIdKey] = id }
    }

    val printerConfig: Flow<PrinterConfig?> = context.dataStore.data.map { prefs ->
        val mac = prefs[printerMacKey] ?: return@map null
        PrinterConfig(
            macAddress = mac,
            name = prefs[printerNameKey] ?: "Impresora",
            paperWidthMm = prefs[paperWidthMmKey] ?: DEFAULT_PAPER_WIDTH_MM
        )
    }

    suspend fun savePrinterConfig(config: PrinterConfig) {
        context.dataStore.edit {
            it[printerMacKey] = config.macAddress
            it[printerNameKey] = config.name
            it[paperWidthMmKey] = config.paperWidthMm
        }
    }

    val hasSeenTutorial: Flow<Boolean> = context.dataStore.data.map { it[hasSeenTutorialKey] ?: false }

    suspend fun markTutorialSeen() {
        context.dataStore.edit { it[hasSeenTutorialKey] = true }
    }

    val hasSeenWebViewTutorial: Flow<Boolean> =
        context.dataStore.data.map { it[hasSeenWebViewTutorialKey] ?: false }

    suspend fun markWebViewTutorialSeen() {
        context.dataStore.edit { it[hasSeenWebViewTutorialKey] = true }
    }

    suspend fun resetWebViewTutorial() {
        context.dataStore.edit { it[hasSeenWebViewTutorialKey] = false }
    }

    suspend fun clearPrinterConfig() {
        context.dataStore.edit {
            it.remove(printerMacKey)
            it.remove(printerNameKey)
            it.remove(paperWidthMmKey)
        }
    }

    data class PrinterConfig(
        val macAddress: String,
        val name: String,
        val paperWidthMm: Int
    )

    companion object {
        const val DEFAULT_PAPER_WIDTH_MM = 58
    }
}

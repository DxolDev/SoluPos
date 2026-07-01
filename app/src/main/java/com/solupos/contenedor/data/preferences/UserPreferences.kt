package com.solupos.contenedor.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPreferences(private val context: Context) {
    private val lastStoreIdKey = stringPreferencesKey("last_store_id")

    val lastStoreId: Flow<String?> = context.dataStore.data.map { it[lastStoreIdKey] }

    suspend fun saveLastStoreId(id: String) {
        context.dataStore.edit { it[lastStoreIdKey] = id }
    }
}

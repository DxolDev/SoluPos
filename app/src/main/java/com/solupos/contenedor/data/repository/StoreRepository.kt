package com.solupos.contenedor.data.repository

import com.solupos.contenedor.data.db.StoreDao
import com.solupos.contenedor.data.db.StoreEntity
import com.solupos.contenedor.data.preferences.UserPreferences
import kotlinx.coroutines.flow.Flow

class StoreRepository(
    private val dao: StoreDao,
    private val prefs: UserPreferences
) {
    val stores: Flow<List<StoreEntity>> = dao.getAll()
    val lastStoreId: Flow<String?> = prefs.lastStoreId

    suspend fun getById(id: String): StoreEntity? = dao.getById(id)
    suspend fun insert(store: StoreEntity) = dao.insert(store)
    suspend fun update(store: StoreEntity) = dao.update(store)
    suspend fun delete(store: StoreEntity) = dao.delete(store)
    suspend fun saveLastStoreId(id: String) = prefs.saveLastStoreId(id)
}

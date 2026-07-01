package com.solupos.contenedor

import android.app.Application
import androidx.room.Room
import com.solupos.contenedor.data.db.AppDatabase
import com.solupos.contenedor.data.preferences.UserPreferences
import com.solupos.contenedor.data.repository.StoreRepository

class SoluPosApp : Application() {
    val database: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "solupos.db").build()
    }
    val userPreferences: UserPreferences by lazy { UserPreferences(this) }
    val storeRepository: StoreRepository by lazy {
        StoreRepository(database.storeDao(), userPreferences)
    }
}

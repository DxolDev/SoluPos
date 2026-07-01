package com.solupos.contenedor.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stores")
data class StoreEntity(
    @PrimaryKey val id: String,
    val name: String,
    val url: String,
    val createdAt: Long = System.currentTimeMillis()
)

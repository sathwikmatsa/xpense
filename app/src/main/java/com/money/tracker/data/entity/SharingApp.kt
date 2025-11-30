package com.money.tracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sharing_apps")
data class SharingApp(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val packageName: String,
    val isEnabled: Boolean = true,
    val sortOrder: Int = 0
)

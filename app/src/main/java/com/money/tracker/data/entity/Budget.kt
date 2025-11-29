package com.money.tracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey
    val yearMonth: String, // Format: "2024-01" for January 2024
    val amount: Double
)

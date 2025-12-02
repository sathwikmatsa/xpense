package com.money.tracker.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "budget_preallocations",
    primaryKeys = ["yearMonth", "categoryId"],
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("categoryId")]
)
data class BudgetPreallocation(
    val yearMonth: String, // Format: "2024-01" for January 2024
    val categoryId: Long,
    val amount: Double
)

package com.money.tracker.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "tag_budgets",
    primaryKeys = ["yearMonth", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = Tag::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tagId")]
)
data class TagBudget(
    val yearMonth: String, // Format: "2024-01" for January 2024
    val tagId: Long,
    val amount: Double
)

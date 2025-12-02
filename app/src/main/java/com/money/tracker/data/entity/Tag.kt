package com.money.tracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tags")
data class Tag(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val emoji: String,
    val color: Long // ARGB color value
)

object DefaultTags {
    val list = listOf(
        Tag(name = "Travel", emoji = "✈️", color = 0xFF2196F3), // Blue
        Tag(name = "Party", emoji = "🎉", color = 0xFFFF9800), // Orange
        Tag(name = "Gift", emoji = "🎁", color = 0xFFE91E63), // Pink
        Tag(name = "Holiday", emoji = "🎄", color = 0xFF4CAF50), // Green
        Tag(name = "Work Trip", emoji = "💼", color = 0xFF607D8B) // Blue Grey
    )
}

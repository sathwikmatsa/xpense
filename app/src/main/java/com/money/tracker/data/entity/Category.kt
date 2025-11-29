package com.money.tracker.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("parentId")]
)
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val emoji: String,
    val parentId: Long? = null,
    val isDefault: Boolean = false
)

object DefaultCategories {
    val list = listOf(
        Category(name = "Food & Dining", emoji = "🍽", isDefault = true),
        Category(name = "Shopping", emoji = "🛍", isDefault = true),
        Category(name = "Transport", emoji = "🚗", isDefault = true),
        Category(name = "Bills", emoji = "📄", isDefault = true),
        Category(name = "Entertainment", emoji = "🎬", isDefault = true),
        Category(name = "Health", emoji = "💊", isDefault = true),
        Category(name = "Education", emoji = "📚", isDefault = true),
        Category(name = "Groceries", emoji = "🛒", isDefault = true),
        Category(name = "Salary", emoji = "💰", isDefault = true),
        Category(name = "Investment", emoji = "📈", isDefault = true),
        Category(name = "Rent", emoji = "🏠", isDefault = true),
        Category(name = "Transfer", emoji = "↔", isDefault = true),
        Category(name = "Other", emoji = "•", isDefault = true)
    )
}

package com.money.tracker.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("categoryId"), Index("date")]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val type: TransactionType,
    val description: String,
    val merchant: String? = null,
    val categoryId: Long? = null,
    val source: TransactionSource,
    val date: Long, // epoch millis
    val rawMessage: String? = null, // original SMS/email for reference
    val isManual: Boolean = false,
    val isPending: Boolean = false, // true for SMS-detected transactions awaiting confirmation
    val createdAt: Long = System.currentTimeMillis()
)

enum class TransactionType {
    INCOME,
    EXPENSE
}

enum class TransactionSource {
    UPI,
    BANK_TRANSFER,
    CREDIT_CARD,
    DEBIT_CARD,
    PAYPAL,
    CASH,
    AUTO_DEBIT,
    OTHER
}

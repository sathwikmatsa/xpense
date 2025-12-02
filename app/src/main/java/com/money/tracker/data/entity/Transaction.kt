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
        ),
        ForeignKey(
            entity = Tag::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("categoryId"), Index("date"), Index("tagId")]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double, // For split transactions, this is "my share"
    val type: TransactionType,
    val description: String,
    val merchant: String? = null,
    val categoryId: Long? = null,
    val tagId: Long? = null, // Optional contextual tag (e.g., Travel, Holiday)
    val source: TransactionSource,
    val date: Long, // epoch millis
    val rawMessage: String? = null, // original SMS/email for reference
    val isManual: Boolean = false,
    val isPending: Boolean = false, // true for SMS-detected transactions awaiting confirmation
    val createdAt: Long = System.currentTimeMillis(),
    // Split transaction fields
    val isSplit: Boolean = false,
    val splitNumerator: Int = 1, // e.g., 1 for 1/2, 2 for 2/3
    val splitDenominator: Int = 1, // e.g., 2 for 1/2, 3 for 2/3
    val totalAmount: Double = 0.0, // Original total paid (when split)
    val splitSynced: Boolean = false // Whether synced to Splitwise/etc
)

enum class TransactionType {
    INCOME,
    EXPENSE
}

enum class TransactionSource {
    UPI,
    GOOGLE_PAY,
    PHONEPE,
    CRED,
    JUPITER,
    BANK_TRANSFER,
    CREDIT_CARD,
    DEBIT_CARD,
    PAYPAL,
    CASH,
    AUTO_DEBIT,
    OTHER
}

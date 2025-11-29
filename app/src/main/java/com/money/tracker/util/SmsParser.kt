package com.money.tracker.util

import com.money.tracker.data.entity.Transaction
import com.money.tracker.data.entity.TransactionSource
import com.money.tracker.data.entity.TransactionType

data class ParsedTransaction(
    val amount: Double,
    val type: TransactionType,
    val description: String,
    val merchant: String?,
    val source: TransactionSource,
    val rawMessage: String
)

object SmsParser {

    // Common patterns for Indian bank SMS
    private val amountPatterns = listOf(
        Regex("""Rs\.?\s*([0-9,]+\.?\d*)""", RegexOption.IGNORE_CASE),
        Regex("""INR\.?\s*([0-9,]+\.?\d*)""", RegexOption.IGNORE_CASE),
        Regex("""₹\s*([0-9,]+\.?\d*)""")
    )

    private val debitKeywords = listOf(
        "debited", "debit", "spent", "paid", "payment", "purchase",
        "withdrawn", "withdrawal", "sent", "transfer to", "txn"
    )

    private val creditKeywords = listOf(
        "credited", "credit", "received", "refund", "cashback",
        "deposit", "transfer from", "added"
    )

    private val upiPatterns = listOf(
        Regex("""UPI[-/]([A-Za-z0-9]+)""", RegexOption.IGNORE_CASE),
        Regex("""UPI\s+Ref""", RegexOption.IGNORE_CASE),
        Regex("""@[a-z]+""", RegexOption.IGNORE_CASE) // UPI ID pattern
    )

    private val merchantPatterns = listOf(
        Regex("""(?:to|at|@)\s+([A-Za-z0-9\s]+?)(?:\s+on|\s+ref|\.|\s*$)""", RegexOption.IGNORE_CASE),
        Regex("""VPA\s+([a-z0-9@.]+)""", RegexOption.IGNORE_CASE)
    )

    fun parse(smsBody: String, sender: String): ParsedTransaction? {
        val lowerBody = smsBody.lowercase()

        // Check if this is a transaction SMS
        val isDebit = debitKeywords.any { lowerBody.contains(it) }
        val isCreditMsg = creditKeywords.any { lowerBody.contains(it) }

        if (!isDebit && !isCreditMsg) {
            return null // Not a transaction SMS
        }

        // Extract amount
        val amount = extractAmount(smsBody) ?: return null

        // Determine transaction type
        val type = if (isDebit) TransactionType.EXPENSE else TransactionType.INCOME

        // Determine source
        val source = determineSource(smsBody, sender)

        // Extract merchant
        val merchant = extractMerchant(smsBody)

        // Create description
        val description = createDescription(type, merchant, source)

        return ParsedTransaction(
            amount = amount,
            type = type,
            description = description,
            merchant = merchant,
            source = source,
            rawMessage = smsBody
        )
    }

    private fun extractAmount(text: String): Double? {
        for (pattern in amountPatterns) {
            val match = pattern.find(text)
            if (match != null) {
                val amountStr = match.groupValues[1].replace(",", "")
                return amountStr.toDoubleOrNull()
            }
        }
        return null
    }

    private fun determineSource(text: String, sender: String): TransactionSource {
        val lowerText = text.lowercase()
        val lowerSender = sender.lowercase()

        return when {
            upiPatterns.any { it.containsMatchIn(text) } -> TransactionSource.UPI
            lowerText.contains("credit card") || lowerSender.contains("credit") -> TransactionSource.CREDIT_CARD
            lowerText.contains("debit card") -> TransactionSource.DEBIT_CARD
            lowerText.contains("auto") && lowerText.contains("debit") -> TransactionSource.AUTO_DEBIT
            lowerText.contains("neft") || lowerText.contains("imps") || lowerText.contains("rtgs") -> TransactionSource.BANK_TRANSFER
            lowerText.contains("atm") -> TransactionSource.DEBIT_CARD
            else -> TransactionSource.OTHER
        }
    }

    private fun extractMerchant(text: String): String? {
        for (pattern in merchantPatterns) {
            val match = pattern.find(text)
            if (match != null) {
                return match.groupValues[1].trim().take(50)
            }
        }
        return null
    }

    private fun createDescription(type: TransactionType, merchant: String?, source: TransactionSource): String {
        val action = if (type == TransactionType.EXPENSE) "Payment" else "Received"
        val sourceStr = source.name.replace("_", " ").lowercase()
            .replaceFirstChar { it.uppercase() }

        return when {
            merchant != null -> "$action to $merchant via $sourceStr"
            else -> "$action via $sourceStr"
        }
    }
}

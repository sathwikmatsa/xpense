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
    val rawMessage: String,
    val suggestedCategoryName: String? = null // Category name to auto-assign (e.g., "Credit Card Bill")
)

object SmsParser {

    // Common patterns for Indian bank SMS
    private val amountPatterns = listOf(
        Regex("""Rs\.?\s*([0-9,]+\.?\d*)""", RegexOption.IGNORE_CASE),
        Regex("""INR\.?\s*([0-9,]+\.?\d*)""", RegexOption.IGNORE_CASE),
        Regex("""₹\s*([0-9,]+\.?\d*)"""),
        Regex("""debited by ([0-9,]+\.?\d*)""", RegexOption.IGNORE_CASE), // SBI: "debited by 1553.0"
        Regex("""credited by ([0-9,]+\.?\d*)""", RegexOption.IGNORE_CASE), // SBI credit format
        Regex("""USD\s*([0-9,]+\.?\d*)""", RegexOption.IGNORE_CASE), // Foreign currency
        Regex("""EUR\s*([0-9,]+\.?\d*)""", RegexOption.IGNORE_CASE),
        Regex("""GBP\s*([0-9,]+\.?\d*)""", RegexOption.IGNORE_CASE)
    )

    private val debitKeywords = listOf(
        "debited", "debit", "spent", "paid", "payment", "purchase",
        "withdrawn", "withdrawal", "sent", "transfer to", "txn",
        "successfully executed" // Auto-debit mandate execution
    )

    private val creditKeywords = listOf(
        "credited", "received", "refund", "cashback",
        "deposit", "transfer from", "added"
    )

    // Keywords that indicate this is NOT a transaction (bill reminders, OTPs, promos, etc.)
    private val excludeKeywords = listOf(
        "amt due", "amount due", "due date", "bill due", "pay now", "pay by",
        "minimum due", "min due", "total due", "outstanding", "overdue",
        "payment due", "will be processed", // Upcoming auto-debit reminders
        "reminder", "otp is", "is otp", "otp for", "otp:", "one time password", "one-time password", "verification code", "do not share",
        "offer", "cashback offer", "reward", "win", "earn", "avail",
        "emi available", "convert to emi", "pre-approved", "limit increased",
        "upgrade now", "recharge now", "buy now", "book now", "apply now" // Promotional CTAs
    )

    // Patterns for credit card bill payment confirmations (should be treated as expenses)
    private val creditCardPaymentKeywords = listOf(
        "received towards your credit card",
        "received towards your card",
        "credited to your credit card",
        "credited to your card",
        "payment towards your credit card",
        "payment received towards your credit card"
    )

    private val upiPatterns = listOf(
        Regex("""UPI[-/]([A-Za-z0-9]+)""", RegexOption.IGNORE_CASE),
        Regex("""UPI\s+Ref""", RegexOption.IGNORE_CASE),
        Regex("""via\s+UPI""", RegexOption.IGNORE_CASE),
        Regex("""UPI\s+on""", RegexOption.IGNORE_CASE),
        Regex("""@[a-z]+""", RegexOption.IGNORE_CASE) // UPI ID pattern
    )

    private val merchantPatterns = listOf(
        Regex("""trf to ([A-Za-z0-9\s]+?)\s+Refno""", RegexOption.IGNORE_CASE), // SBI: "trf to CHUNDURU VENKATA Refno"
        Regex("""to\s+([A-Za-z0-9\s\.]+?)\.?Ref""", RegexOption.IGNORE_CASE), // "to PAILA VENKATA S.Ref:"
        Regex("""At\s+([A-Za-z0-9\s&*_-]+?)\s+On\s+\d""", RegexOption.IGNORE_CASE), // "At MERCHANT On 2025-"
        Regex("""towards\s+([A-Za-z0-9\s&*_-]+?)\s+using""", RegexOption.IGNORE_CASE), // "towards AMAZON using"
        Regex("""at\s+([A-Za-z0-9\s&*_-]+?)\s+on\s+[A-Z]""", RegexOption.IGNORE_CASE), // "at AMAZON on ICICI"
        Regex("""(?:to|at)\s+([A-Za-z0-9\s]+?)(?:\s+on|\s+ref|\.|\s*$)""", RegexOption.IGNORE_CASE),
        Regex("""VPA\s+([a-z0-9@.]+)""", RegexOption.IGNORE_CASE)
    )

    fun parse(smsBody: String, sender: String): ParsedTransaction? {
        val lowerBody = smsBody.lowercase()

        // First check if this should be excluded (bill reminders, OTPs, promos)
        if (excludeKeywords.any { lowerBody.contains(it) }) {
            return null // Not a transaction - it's a reminder/promo/OTP
        }

        // Check if this is a credit card bill payment confirmation
        val isCreditCardPayment = creditCardPaymentKeywords.any { lowerBody.contains(it) }

        // Check if this is a transaction SMS
        val isDebit = debitKeywords.any { lowerBody.contains(it) }
        val isCreditMsg = creditKeywords.any { lowerBody.contains(it) }

        if (!isDebit && !isCreditMsg) {
            return null // Not a transaction SMS
        }

        // Extract amount
        val amount = extractAmount(smsBody) ?: return null

        // Determine transaction type
        // Credit card payment confirmations are expenses (you paid the bill)
        val type = if (isCreditCardPayment || isDebit) TransactionType.EXPENSE else TransactionType.INCOME

        // Determine source
        val source = determineSource(smsBody, sender)

        // Extract merchant
        val merchant = extractMerchant(smsBody)

        // Create description
        val description = createDescription(type, merchant, source)

        // Suggest category for credit card bill payments
        val suggestedCategory = if (isCreditCardPayment) "Credit Card Bill" else null

        return ParsedTransaction(
            amount = amount,
            type = type,
            description = description,
            merchant = merchant,
            source = source,
            rawMessage = smsBody,
            suggestedCategoryName = suggestedCategory
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

    private fun determineSource(text: String, @Suppress("UNUSED_PARAMETER") sender: String): TransactionSource {
        val lowerText = text.lowercase()

        return when {
            upiPatterns.any { it.containsMatchIn(text) } ||
                lowerText.contains("trf to") || lowerText.contains("upi user") -> TransactionSource.UPI // SBI UPI format
            lowerText.contains("credit card") || lowerText.contains("cc ") ||
                Regex("""bank\s+card\s+\d""", RegexOption.IGNORE_CASE).containsMatchIn(text) -> TransactionSource.CREDIT_CARD
            lowerText.contains("debit card") -> TransactionSource.DEBIT_CARD
            lowerText.contains("auto") && lowerText.contains("debit") -> TransactionSource.AUTO_DEBIT
            lowerText.contains("nach") -> TransactionSource.AUTO_DEBIT // SBI NACH format
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

    private fun createDescription(type: TransactionType, merchant: String?, @Suppress("UNUSED_PARAMETER") source: TransactionSource): String {
        return when {
            merchant != null -> merchant
            type == TransactionType.EXPENSE -> "Payment"
            else -> "Received"
        }
    }
}

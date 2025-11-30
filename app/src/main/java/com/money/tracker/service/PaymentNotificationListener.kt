package com.money.tracker.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.money.tracker.data.AppDatabase
import com.money.tracker.data.entity.Transaction
import com.money.tracker.data.entity.TransactionSource
import com.money.tracker.data.entity.TransactionType
import com.money.tracker.data.repository.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PaymentNotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "PaymentNotificationListener"

        // Known UPI/payment app packages with friendly names
        private val KNOWN_PAYMENT_APPS = mapOf(
            "com.dreamplug.androidapp" to "CRED",
            "com.google.android.apps.nbu.paisa.user" to "GPay",
            "money.jupiter" to "Jupiter",
            "com.phonepe.app" to "PhonePe",
            "net.one97.paytm" to "Paytm",
            "in.org.npci.upiapp" to "BHIM",
            "in.amazon.mShop.android.shopping" to "Amazon Pay",
            "com.freecharge.android" to "Freecharge",
            "com.mobikwik_new" to "MobiKwik"
        )

        // Patterns to extract amount from notifications
        private val AMOUNT_PATTERNS = listOf(
            Regex("""₹\s*([\d,]+\.?\d*)"""),
            Regex("""Rs\.?\s*([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE),
            Regex("""INR\s*([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE)
        )

        // Keywords indicating incoming payment (not outgoing)
        private val INCOME_KEYWORDS = listOf(
            "sent you", "paid you", "received", "credited", "credit", "money received"
        )

        // Keywords to exclude (outgoing payments, promotions, etc.)
        private val EXCLUDE_KEYWORDS = listOf(
            "you sent", "you paid", "debited", "debit", "paid to", "pay now",
            "offer", "cashback", "reward", "win", "scratch", "coupon", "discount"
        )

        // Pattern to extract sender name
        private val SENDER_PATTERNS = listOf(
            Regex("""^(.+?)\s+(?:sent|paid)\s+you""", RegexOption.IGNORE_CASE),
            Regex("""from\s+(.+?)(?:\s+via|\s+on|\s*$)""", RegexOption.IGNORE_CASE)
        )
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private lateinit var transactionRepository: TransactionRepository

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.getDatabase(this)
        transactionRepository = TransactionRepository(db.transactionDao())
        Log.d(TAG, "Payment notification listener created")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        Log.d(TAG, "Payment notification listener destroyed")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName

        // Only process notifications from known payment apps
        val appName = KNOWN_PAYMENT_APPS[packageName] ?: return

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""

        val fullText = "$title $text $bigText".lowercase()

        // Check if it's an incoming payment notification
        val isIncome = INCOME_KEYWORDS.any { fullText.contains(it) }
        val isExcluded = EXCLUDE_KEYWORDS.any { fullText.contains(it) }

        if (!isIncome || isExcluded) {
            Log.d(TAG, "Ignoring notification from $appName - not an income notification")
            return
        }

        // Extract amount
        val searchText = "$title $text $bigText"
        val amount = extractAmount(searchText)
        if (amount == null || amount <= 0) {
            Log.d(TAG, "Could not extract amount from $appName notification")
            return
        }

        // Extract sender name
        val sender = extractSender(title) ?: extractSender(text)

        // Build description
        val description = buildDescription(sender, appName)

        Log.d(TAG, "Income detected from $appName: ₹$amount from ${sender ?: "unknown"}")

        // Save transaction
        serviceScope.launch {
            try {
                val transaction = Transaction(
                    amount = amount,
                    description = description,
                    type = TransactionType.INCOME,
                    categoryId = null, // Will need to be categorized
                    source = getTransactionSource(packageName),
                    date = System.currentTimeMillis(),
                    isManual = false,
                    isPending = true // Mark as pending for user to confirm/categorize
                )
                transactionRepository.insert(transaction)
                Log.d(TAG, "Saved income transaction: $description - ₹$amount")

                // Show notification to user
                showTransactionNotification(amount, description, appName)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save transaction", e)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Not needed for our use case
    }

    private fun extractAmount(text: String): Double? {
        for (pattern in AMOUNT_PATTERNS) {
            val match = pattern.find(text)
            if (match != null) {
                val amountStr = match.groupValues[1].replace(",", "")
                return amountStr.toDoubleOrNull()
            }
        }
        return null
    }

    private fun extractSender(text: String): String? {
        for (pattern in SENDER_PATTERNS) {
            val match = pattern.find(text)
            if (match != null) {
                val sender = match.groupValues[1].trim()
                if (sender.isNotBlank() && sender.length < 100) {
                    return sender
                }
            }
        }
        return null
    }

    private fun buildDescription(sender: String?, appName: String): String {
        return when {
            sender != null -> "Received from $sender via $appName"
            else -> "Payment received via $appName"
        }
    }

    private fun getTransactionSource(packageName: String): TransactionSource {
        return when (packageName) {
            "com.google.android.apps.nbu.paisa.user" -> TransactionSource.GOOGLE_PAY
            "com.phonepe.app" -> TransactionSource.PHONEPE
            "com.dreamplug.androidapp" -> TransactionSource.CRED
            "money.jupiter" -> TransactionSource.JUPITER
            // All other UPI apps map to generic UPI source
            else -> TransactionSource.UPI
        }
    }

    private fun showTransactionNotification(amount: Double, description: String, appName: String) {
        TransactionNotificationHelper.showIncomeNotification(this, amount, description, appName)
    }
}

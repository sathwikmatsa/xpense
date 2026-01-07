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

        // Splitwise package name
        private const val SPLITWISE_PACKAGE = "com.Splitwise.SplitwiseMobile"

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
            "offer", "cashback", "reward", "win", "scratch", "coupon", "discount",
            "earn", "unlock", "claim", "bonus", "expires", "limited time", "special",
            "activate", "eligible", "congratulations", "you've won", "gift",
            "loan", "pre-approved", "credit limit", "emi", "interest rate",
            "voucher", "bills pending", "top up", "we've got you", "get a",
            "bill from", "your bill", "use jewels", "save ₹", "save rs",
            "instant cash", "quick credit", "low interest", "stressing you"
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

    // Track processed Splitwise expenses to avoid duplicates when notifications are bundled
    // Key: expense description (normalized), Value: timestamp when processed
    private val processedSplitwiseExpenses = mutableMapOf<String, Long>()

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.getDatabase(this)
        transactionRepository = TransactionRepository(db.transactionDao(), db.transactionTagDao())
        Log.d(TAG, "Payment notification listener created")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        Log.d(TAG, "Payment notification listener destroyed")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName

        // Handle Splitwise notifications separately
        if (packageName == SPLITWISE_PACKAGE) {
            handleSplitwiseNotification(sbn)
            return
        }

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

    private fun handleSplitwiseNotification(sbn: StatusBarNotification) {
        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val tickerText = notification.tickerText?.toString() ?: ""

        Log.d(TAG, "Splitwise notification - title: $title, text: $text, ticker: $tickerText")

        // Check if this is a bundled notification (multiple expenses)
        val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)

        if (textLines != null && textLines.isNotEmpty()) {
            // Bundled notification - process each expense line
            handleBundledSplitwiseNotification(textLines, tickerText)
        } else {
            // Single notification
            handleSingleSplitwiseNotification(title, text, tickerText)
        }
    }

    private fun handleSingleSplitwiseNotification(title: String, text: String, tickerText: String) {
        val fullText = "$title $text $tickerText"
        val lowerText = fullText.lowercase()

        // Only process "You owe" notifications
        if (!lowerText.contains("you owe")) {
            Log.d(TAG, "Ignoring Splitwise notification - not a 'you owe' notification")
            return
        }

        // Extract expense info from title (format: "Expense Name (₹Total)")
        // Title has the expense name and total, text has "You owe ₹Share"
        val expenseInfo = extractSplitwiseExpenseInfo(title) ?: extractSplitwiseExpenseInfo(tickerText)
        if (expenseInfo == null) {
            Log.d(TAG, "Could not extract expense info from Splitwise notification")
            return
        }

        val (expenseName, totalAmount) = expenseInfo
        // Extract share amount from text (e.g., "– You owe ₹200.50") or fullText
        val shareAmount = extractYouOweAmount(text) ?: extractYouOweAmount(fullText)

        // Check if we've already processed this expense
        val normalizedName = expenseName.lowercase().trim()
        if (processedSplitwiseExpenses.containsKey(normalizedName)) {
            Log.d(TAG, "Already processed Splitwise expense: $expenseName")
            return
        }

        // Calculate split ratio if we have both amounts
        val (numerator, denominator) = if (shareAmount != null && totalAmount > 0) {
            calculateSplitRatio(shareAmount, totalAmount)
        } else {
            Pair(1, 2) // Default to 1/2 split
        }

        val finalAmount = shareAmount ?: (totalAmount / 2) // Use share if available, else half

        Log.d(TAG, "Splitwise expense: $expenseName - Total: ₹$totalAmount, Share: ₹$finalAmount ($numerator/$denominator)")

        saveSplitwiseTransaction(expenseName, finalAmount, totalAmount, numerator, denominator)
        processedSplitwiseExpenses[normalizedName] = System.currentTimeMillis()
    }

    private fun handleBundledSplitwiseNotification(textLines: Array<CharSequence>, tickerText: String) {
        Log.d(TAG, "Processing bundled Splitwise notification with ${textLines.size} items")

        // tickerText contains the latest expense with "You owe" amount
        // e.g., "Coconut (₹140.00) – You owe ₹70.00"
        val latestExpenseInfo = extractSplitwiseExpenseInfo(tickerText)
        val latestShareAmount = extractYouOweAmount(tickerText)
        val latestExpenseName = latestExpenseInfo?.first?.lowercase()?.trim()

        for (line in textLines) {
            val lineStr = line.toString()
            val expenseInfo = extractSplitwiseExpenseInfo(lineStr)

            if (expenseInfo == null) {
                Log.d(TAG, "Could not parse expense line: $lineStr")
                continue
            }

            val (expenseName, totalAmount) = expenseInfo
            val normalizedName = expenseName.lowercase().trim()

            // Check if already processed
            if (processedSplitwiseExpenses.containsKey(normalizedName)) {
                Log.d(TAG, "Already processed: $expenseName")
                continue
            }

            // Determine share amount and split ratio
            val (shareAmount, numerator, denominator) = if (normalizedName == latestExpenseName && latestShareAmount != null) {
                // This is the latest expense, we have the exact share from tickerText
                val (num, den) = calculateSplitRatio(latestShareAmount, totalAmount)
                Triple(latestShareAmount, num, den)
            } else {
                // Older expense in bundle - we don't know the share, default to 1/2
                Triple(totalAmount / 2, 1, 2)
            }

            Log.d(TAG, "Bundled expense: $expenseName - Total: ₹$totalAmount, Share: ₹$shareAmount ($numerator/$denominator)")

            saveSplitwiseTransaction(expenseName, shareAmount, totalAmount, numerator, denominator)
            processedSplitwiseExpenses[normalizedName] = System.currentTimeMillis()
        }

        // Clean up old entries (older than 24 hours) to prevent memory leak
        val oneDayAgo = System.currentTimeMillis() - 24 * 60 * 60 * 1000
        processedSplitwiseExpenses.entries.removeIf { it.value < oneDayAgo }
    }

    private fun extractSplitwiseExpenseInfo(text: String): Pair<String, Double>? {
        // Pattern: "Expense Name (₹Amount)" or "Expense Name (Rs. Amount)"
        val patterns = listOf(
            Regex("""^(.+?)\s*\(₹\s*([\d,]+\.?\d*)\)"""),
            Regex("""^(.+?)\s*\(Rs\.?\s*([\d,]+\.?\d*)\)""", RegexOption.IGNORE_CASE),
            Regex("""^(.+?)\s*\(INR\s*([\d,]+\.?\d*)\)""", RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) {
                val name = match.groupValues[1].trim()
                val amount = match.groupValues[2].replace(",", "").toDoubleOrNull()
                if (amount != null && amount > 0) {
                    return Pair(name, amount)
                }
            }
        }
        return null
    }

    private fun extractYouOweAmount(text: String): Double? {
        val patterns = listOf(
            Regex("""you owe ₹\s*([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE),
            Regex("""you owe Rs\.?\s*([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE),
            Regex("""you owe INR\s*([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) {
                val amount = match.groupValues[1].replace(",", "").toDoubleOrNull()
                if (amount != null && amount > 0) {
                    return amount
                }
            }
        }
        return null
    }

    private fun calculateSplitRatio(share: Double, total: Double): Pair<Int, Int> {
        if (total <= 0 || share <= 0 || share > total) {
            return Pair(1, 2) // Default
        }

        // Common split ratios to check
        val commonDenominators = listOf(2, 3, 4, 5, 6, 7, 8, 9, 10)

        for (den in commonDenominators) {
            for (num in 1 until den) {
                val expectedShare = total * num / den
                // Allow small rounding error (within ₹1)
                if (kotlin.math.abs(expectedShare - share) < 1.0) {
                    return Pair(num, den)
                }
            }
        }

        // If no common ratio found, return approximate ratio
        val ratio = share / total
        val den = 10
        val num = (ratio * den).toInt().coerceIn(1, den - 1)
        return Pair(num, den)
    }

    private fun saveSplitwiseTransaction(
        expenseName: String,
        shareAmount: Double,
        totalAmount: Double,
        numerator: Int,
        denominator: Int
    ) {
        serviceScope.launch {
            try {
                val transaction = Transaction(
                    amount = shareAmount,
                    description = expenseName,
                    type = TransactionType.EXPENSE,
                    categoryId = null,
                    source = TransactionSource.SPLITWISE,
                    date = System.currentTimeMillis(),
                    isManual = false,
                    isPending = true,
                    isSplit = true,
                    splitNumerator = numerator,
                    splitDenominator = denominator,
                    totalAmount = totalAmount,
                    splitSynced = true // Already in Splitwise, no need to sync back
                )
                transactionRepository.insert(transaction)
                Log.d(TAG, "Saved Splitwise expense: $expenseName - ₹$shareAmount (${numerator}/${denominator} of ₹$totalAmount)")

                TransactionNotificationHelper.showExpenseNotification(
                    this@PaymentNotificationListener,
                    shareAmount,
                    expenseName,
                    "Splitwise"
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save Splitwise transaction", e)
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

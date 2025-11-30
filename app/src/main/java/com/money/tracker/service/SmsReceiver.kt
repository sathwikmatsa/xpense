package com.money.tracker.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.money.tracker.MoneyTrackerApp
import com.money.tracker.data.entity.Transaction
import com.money.tracker.data.entity.TransactionType
import com.money.tracker.util.SmsParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val UPI_REMINDER_NOTIFICATION_ID = 9999
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isEmpty()) return

        val app = context.applicationContext as? MoneyTrackerApp ?: return

        for (sms in messages) {
            val sender = sms.displayOriginatingAddress ?: continue
            val body = sms.messageBody ?: continue

            // Parse the SMS
            val parsed = SmsParser.parse(body, sender) ?: continue

            // Save as pending transaction and show notification
            CoroutineScope(Dispatchers.IO).launch {
                // For INCOME transactions, check if notification listener already created it
                // (PaymentNotificationListener handles incoming payments from app notifications)
                if (parsed.type == TransactionType.INCOME &&
                    app.transactionRepository.hasRecentPendingTransaction(parsed.amount)) {
                    // Skip SMS - notification listener already handled this incoming payment
                    return@launch
                }

                val transaction = Transaction(
                    amount = parsed.amount,
                    type = parsed.type,
                    description = parsed.description,
                    merchant = parsed.merchant,
                    categoryId = null,
                    source = parsed.source,
                    date = System.currentTimeMillis(),
                    rawMessage = parsed.rawMessage,
                    isManual = false,
                    isPending = true // Mark as pending until user confirms
                )
                val transactionId = app.transactionRepository.insert(transaction)

                // Dismiss any UPI reminder notification that came in the last 10 seconds
                // (since SMS confirms the payment, no need for UPI reminder)
                val notificationManager = context.getSystemService(NotificationManager::class.java)
                notificationManager.cancel(UPI_REMINDER_NOTIFICATION_ID)

                // Delete recent UPI reminders from the database (within last 10 seconds)
                app.upiReminderRepository.deleteRecentReminders(10_000L)

                // Show notification to prompt user
                TransactionNotificationHelper.showTransactionNotification(
                    context = context,
                    transactionId = transactionId,
                    amount = parsed.amount,
                    type = parsed.type,
                    merchant = parsed.merchant,
                    source = parsed.source.name
                )
            }
        }
    }
}

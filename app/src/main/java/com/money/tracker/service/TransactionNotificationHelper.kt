package com.money.tracker.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.money.tracker.MainActivity
import com.money.tracker.R
import com.money.tracker.data.entity.TransactionType
import java.text.NumberFormat
import java.util.Locale

object TransactionNotificationHelper {

    private const val CHANNEL_ID = "transaction_detected"
    private const val CHANNEL_NAME = "Transaction Detected"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for detected transactions from SMS"
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showTransactionNotification(
        context: Context,
        transactionId: Long,
        amount: Double,
        type: TransactionType,
        merchant: String?,
        source: String
    ) {
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        val amountStr = currencyFormat.format(amount)

        val title = if (type == TransactionType.EXPENSE) {
            "Payment detected: $amountStr"
        } else {
            "Money received: $amountStr"
        }

        val body = buildString {
            if (merchant != null) {
                append(merchant)
                append(" • ")
            }
            append(source)
            append("\nTap to add details")
        }

        // Intent to open app with the pending transaction
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("pending_transaction_id", transactionId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            transactionId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Dismiss action
        val dismissIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_DISMISS
            putExtra("transaction_id", transactionId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            transactionId.toInt() + 10000,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(0, "Dismiss", dismissPendingIntent)
            .addAction(0, "Add", pendingIntent)
            .build()

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.notify(transactionId.toInt(), notification)
    }

    fun cancelNotification(context: Context, transactionId: Long) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.cancel(transactionId.toInt())
    }

    fun showIncomeNotification(
        context: Context,
        amount: Double,
        description: String,
        appName: String
    ) {
        createNotificationChannel(context)

        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        val amountStr = currencyFormat.format(amount)

        val title = "$appName: Received $amountStr"
        val body = "$description\nTap to categorize"

        // Intent to open app with pending transactions
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_pending", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(0, "Categorize", pendingIntent)
            .build()

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    fun showExpenseNotification(
        context: Context,
        amount: Double,
        description: String,
        appName: String
    ) {
        createNotificationChannel(context)

        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        val amountStr = currencyFormat.format(amount)

        val title = "$appName: You owe $amountStr"
        val body = "$description\nTap to categorize"

        // Intent to open app with pending transactions
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_pending", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(0, "Categorize", pendingIntent)
            .build()

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}

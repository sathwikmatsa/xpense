package com.money.tracker.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.money.tracker.MoneyTrackerApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_DISMISS = "com.money.tracker.DISMISS_TRANSACTION"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val transactionId = intent.getLongExtra("transaction_id", -1)
        if (transactionId == -1L) return

        val app = context.applicationContext as? MoneyTrackerApp ?: return

        when (intent.action) {
            ACTION_DISMISS -> {
                // Delete the pending transaction
                CoroutineScope(Dispatchers.IO).launch {
                    app.transactionRepository.deleteById(transactionId)
                }
                TransactionNotificationHelper.cancelNotification(context, transactionId)
            }
        }
    }
}

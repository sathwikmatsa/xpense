package com.money.tracker.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.money.tracker.data.AppDatabase
import com.money.tracker.data.repository.UpiReminderRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UpiNotificationReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_DISMISS = "com.money.tracker.DISMISS_UPI_NOTIFICATION"
        const val EXTRA_REMINDER_ID = "reminder_id"
        private const val NOTIFICATION_ID = 9999
    }

    override fun onReceive(context: Context, intent: Intent) {
        val manager = context.getSystemService(NotificationManager::class.java)

        when (intent.action) {
            ACTION_DISMISS -> {
                // User explicitly dismissed - cancel notification and delete the specific reminder
                manager.cancel(NOTIFICATION_ID)

                val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
                if (reminderId > 0) {
                    val db = AppDatabase.getDatabase(context)
                    val repository = UpiReminderRepository(db.upiReminderDao())

                    CoroutineScope(Dispatchers.IO).launch {
                        repository.deleteById(reminderId)
                    }
                }
            }
        }
    }
}

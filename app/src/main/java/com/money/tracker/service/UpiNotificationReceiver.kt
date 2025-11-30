package com.money.tracker.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.money.tracker.data.AppDatabase
import com.money.tracker.data.entity.UpiReminder
import com.money.tracker.data.repository.UpiReminderRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UpiNotificationReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_DISMISS = "com.money.tracker.DISMISS_UPI_NOTIFICATION"
        const val ACTION_SWIPED = "com.money.tracker.SWIPED_UPI_NOTIFICATION"
        const val EXTRA_PACKAGE_NAME = "package_name"
        const val EXTRA_APP_NAME = "app_name"
        private const val NOTIFICATION_ID = 9999
    }

    override fun onReceive(context: Context, intent: Intent) {
        val manager = context.getSystemService(NotificationManager::class.java)

        when (intent.action) {
            ACTION_DISMISS -> {
                // User explicitly dismissed - just cancel notification
                manager.cancel(NOTIFICATION_ID)
            }
            ACTION_SWIPED -> {
                // User swiped away notification - save reminder
                val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: return
                val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: return

                val db = AppDatabase.getDatabase(context)
                val repository = UpiReminderRepository(db.upiReminderDao())

                CoroutineScope(Dispatchers.IO).launch {
                    repository.insert(UpiReminder(
                        packageName = packageName,
                        appName = appName
                    ))
                }
            }
        }
    }
}

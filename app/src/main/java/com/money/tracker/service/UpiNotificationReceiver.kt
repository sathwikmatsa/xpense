package com.money.tracker.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class UpiNotificationReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_DISMISS = "com.money.tracker.DISMISS_UPI_NOTIFICATION"
        private const val NOTIFICATION_ID = 9999
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_DISMISS) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.cancel(NOTIFICATION_ID)
        }
    }
}

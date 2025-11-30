package com.money.tracker.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat
import com.money.tracker.MainActivity
import com.money.tracker.R

class UpiMonitorService : AccessibilityService() {

    companion object {
        private const val CHANNEL_ID = "upi_monitor"
        private const val NOTIFICATION_ID = 9999
        private const val DEBOUNCE_MS = 30000L // Don't show another notification within 30 seconds
        private const val PIN_NOTIFICATION_DELAY_MS = 3000L // Wait 3 seconds after PIN entry

        // Package names of popular UPI apps
        // Note: Only include apps that don't send SMS with transaction details
        // Apps like Jupiter send SMS, so they're handled by SmsReceiver instead
        private val UPI_PACKAGES = setOf(
            "com.google.android.apps.nbu.paisa.user", // GPay
            "com.phonepe.app", // PhonePe
            "net.one97.paytm", // Paytm
            "in.org.npci.upiapp", // BHIM
            "com.dreamplug.androidapp", // CRED
            "in.amazon.mShop.android.shopping" // Amazon Pay
        )

        // Keywords that indicate a successful payment
        private val SUCCESS_KEYWORDS = listOf(
            "success", "successful", "completed", "paid", "sent", "done",
            "payment of", "debited", "transferred", "rupees", "inr", "₹"
        )

        // PIN patterns (masked PIN characters)
        private val PIN_PATTERN = Regex("[●•\\*]{4,6}")
    }

    private var lastNotificationTime = 0L
    private var pinEntryDetected = false
    private var lastUpiPackage: String? = null
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        createNotificationChannel()

        val info = AccessibilityServiceInfo().apply {
            // Listen for announcements and notifications from UPI apps
            eventTypes = AccessibilityEvent.TYPE_ANNOUNCEMENT or
                    AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = 100
            // Don't filter by package - we'll filter in onAccessibilityEvent
            // Some devices don't deliver events when packageNames is set
        }
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString() ?: return
        if (packageName !in UPI_PACKAGES) return

        // Get text content from the event and source node
        @Suppress("DEPRECATION")
        val textContent = buildString {
            event.text.forEach { append(it).append(" ") }
            event.contentDescription?.let { append(it).append(" ") }

            // Also try to get text from source node
            try {
                event.source?.let { node ->
                    node.text?.let { append(it).append(" ") }
                    node.contentDescription?.let { append(it).append(" ") }

                    // Check child nodes too (one level deep)
                    for (i in 0 until node.childCount) {
                        node.getChild(i)?.let { child ->
                            child.text?.let { append(it).append(" ") }
                            child.contentDescription?.let { append(it).append(" ") }
                            child.recycle()
                        }
                    }
                    node.recycle()
                }
            } catch (_: Exception) {
                // Ignore errors reading source node
            }
        }.lowercase()

        if (textContent.isBlank()) return

        // Check if it looks like a payment success message
        val looksLikePayment = SUCCESS_KEYWORDS.any { textContent.contains(it) }

        // Check if PIN entry is detected (4-6 masked characters like ●●●●●●)
        val isPinEntry = PIN_PATTERN.containsMatchIn(textContent)

        if (isPinEntry && !pinEntryDetected) {
            pinEntryDetected = true
            lastUpiPackage = packageName
            // Schedule notification after delay (to give time for payment to complete)
            handler.removeCallbacksAndMessages(null)
            handler.postDelayed({
                val now = System.currentTimeMillis()
                if (now - lastNotificationTime > DEBOUNCE_MS) {
                    lastNotificationTime = now
                    showTransactionPrompt(lastUpiPackage)
                }
                pinEntryDetected = false
            }, PIN_NOTIFICATION_DELAY_MS)
        }

        if (looksLikePayment) {
            // Cancel PIN-based delayed notification and show immediately
            handler.removeCallbacksAndMessages(null)
            pinEntryDetected = false

            val now = System.currentTimeMillis()
            if (now - lastNotificationTime > DEBOUNCE_MS) {
                lastNotificationTime = now
                showTransactionPrompt(packageName)
            }
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "UPI Payment Reminder",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminds you to add transactions after using UPI apps"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun getAppName(packageName: String): String {
        return when (packageName) {
            "com.google.android.apps.nbu.paisa.user" -> "GPay"
            "com.phonepe.app" -> "PhonePe"
            "net.one97.paytm" -> "Paytm"
            "in.org.npci.upiapp" -> "BHIM"
            "com.dreamplug.androidapp" -> "CRED"
            "in.amazon.mShop.android.shopping" -> "Amazon Pay"
            else -> "UPI App"
        }
    }

    private fun showTransactionPrompt(packageName: String?) {
        val appName = packageName?.let { getAppName(it) } ?: "UPI App"

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_add_transaction", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(this, UpiNotificationReceiver::class.java).apply {
            action = UpiNotificationReceiver.ACTION_DISMISS
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent for when notification is swiped away (save reminder)
        val swipeIntent = Intent(this, UpiNotificationReceiver::class.java).apply {
            action = UpiNotificationReceiver.ACTION_SWIPED
            putExtra(UpiNotificationReceiver.EXTRA_PACKAGE_NAME, packageName ?: "unknown")
            putExtra(UpiNotificationReceiver.EXTRA_APP_NAME, appName)
        }
        val swipePendingIntent = PendingIntent.getBroadcast(
            this,
            1, // Different request code to avoid overwriting
            swipeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Made a payment?")
            .setContentText("Tap to add your $appName transaction")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDeleteIntent(swipePendingIntent)
            .addAction(0, "Dismiss", dismissPendingIntent)
            .addAction(0, "Add", pendingIntent)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun cancelNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.cancel(NOTIFICATION_ID)
    }
}

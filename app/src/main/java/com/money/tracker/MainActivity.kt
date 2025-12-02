package com.money.tracker

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.money.tracker.ui.navigation.MoneyTrackerNavGraph
import com.money.tracker.ui.theme.MoneyTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as MoneyTrackerApp
        val openAddTransaction = intent?.getBooleanExtra("open_add_transaction", false) ?: false
        val upiReminderId = intent?.getLongExtra("upi_reminder_id", -1L) ?: -1L
        val upiPackageName = intent?.getStringExtra("upi_package_name") ?: ""

        setContent {
            MoneyTrackerTheme {
                MoneyTrackerNavGraph(
                    transactionRepository = app.transactionRepository,
                    categoryRepository = app.categoryRepository,
                    budgetRepository = app.budgetRepository,
                    upiReminderRepository = app.upiReminderRepository,
                    sharingAppRepository = app.sharingAppRepository,
                    budgetPreallocationRepository = app.budgetPreallocationRepository,
                    categoryBudgetRepository = app.categoryBudgetRepository,
                    tagRepository = app.tagRepository,
                    openAddTransaction = openAddTransaction,
                    upiReminderId = upiReminderId,
                    upiPackageName = upiPackageName,
                    onOpenUpiApp = { packageName -> openUpiApp(packageName) }
                )
            }
        }
    }

    private fun openUpiApp(packageName: String) {
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launchIntent)
            } else {
                Toast.makeText(this, "App not installed", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open app", Toast.LENGTH_SHORT).show()
        }
    }
}

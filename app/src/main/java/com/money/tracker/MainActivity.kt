package com.money.tracker

import android.os.Bundle
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

        setContent {
            MoneyTrackerTheme {
                MoneyTrackerNavGraph(
                    transactionRepository = app.transactionRepository,
                    categoryRepository = app.categoryRepository,
                    budgetRepository = app.budgetRepository,
                    openAddTransaction = openAddTransaction
                )
            }
        }
    }
}

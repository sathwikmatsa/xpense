package com.money.tracker

import android.app.Application
import com.money.tracker.data.AppDatabase
import com.money.tracker.data.repository.BudgetRepository
import com.money.tracker.data.repository.CategoryRepository
import com.money.tracker.data.repository.TransactionRepository

class MoneyTrackerApp : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }
    val transactionRepository by lazy { TransactionRepository(database.transactionDao()) }
    val categoryRepository by lazy { CategoryRepository(database.categoryDao()) }
    val budgetRepository by lazy { BudgetRepository(database.budgetDao()) }
}

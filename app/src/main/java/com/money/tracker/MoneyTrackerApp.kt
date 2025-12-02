package com.money.tracker

import android.app.Application
import com.money.tracker.data.AppDatabase
import com.money.tracker.data.repository.BudgetPreallocationRepository
import com.money.tracker.data.repository.BudgetRepository
import com.money.tracker.data.repository.CategoryBudgetRepository
import com.money.tracker.data.repository.CategoryRepository
import com.money.tracker.data.repository.SharingAppRepository
import com.money.tracker.data.repository.TagRepository
import com.money.tracker.data.repository.TransactionRepository
import com.money.tracker.data.repository.UpiReminderRepository
import com.money.tracker.service.TransactionNotificationHelper

class MoneyTrackerApp : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }
    val transactionRepository by lazy { TransactionRepository(database.transactionDao()) }
    val categoryRepository by lazy { CategoryRepository(database.categoryDao()) }
    val budgetRepository by lazy { BudgetRepository(database.budgetDao()) }
    val upiReminderRepository by lazy { UpiReminderRepository(database.upiReminderDao()) }
    val sharingAppRepository by lazy { SharingAppRepository(database.sharingAppDao()) }
    val budgetPreallocationRepository by lazy { BudgetPreallocationRepository(database.budgetPreallocationDao()) }
    val categoryBudgetRepository by lazy { CategoryBudgetRepository(database.categoryBudgetDao()) }
    val tagRepository by lazy { TagRepository(database.tagDao(), database.tagBudgetDao()) }

    override fun onCreate() {
        super.onCreate()
        TransactionNotificationHelper.createNotificationChannel(this)
    }
}

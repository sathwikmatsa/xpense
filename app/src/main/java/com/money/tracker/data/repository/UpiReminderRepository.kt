package com.money.tracker.data.repository

import com.money.tracker.data.dao.UpiReminderDao
import com.money.tracker.data.entity.UpiReminder
import kotlinx.coroutines.flow.Flow

class UpiReminderRepository(private val upiReminderDao: UpiReminderDao) {

    val allReminders: Flow<List<UpiReminder>> = upiReminderDao.getAllReminders()

    suspend fun insert(reminder: UpiReminder) {
        upiReminderDao.insert(reminder)
    }

    suspend fun deleteById(id: Long) {
        upiReminderDao.deleteById(id)
    }

    suspend fun deleteAll() {
        upiReminderDao.deleteAll()
    }
}

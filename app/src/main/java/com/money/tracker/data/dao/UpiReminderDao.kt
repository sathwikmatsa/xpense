package com.money.tracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.money.tracker.data.entity.UpiReminder
import kotlinx.coroutines.flow.Flow

@Dao
interface UpiReminderDao {
    @Query("SELECT * FROM upi_reminders ORDER BY timestamp DESC")
    fun getAllReminders(): Flow<List<UpiReminder>>

    @Insert
    suspend fun insert(reminder: UpiReminder)

    @Query("DELETE FROM upi_reminders WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM upi_reminders")
    suspend fun deleteAll()

    @Query("DELETE FROM upi_reminders WHERE timestamp > :since")
    suspend fun deleteRecentReminders(since: Long)
}

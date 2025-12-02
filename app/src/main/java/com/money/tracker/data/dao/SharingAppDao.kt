package com.money.tracker.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.money.tracker.data.entity.SharingApp
import kotlinx.coroutines.flow.Flow

@Dao
interface SharingAppDao {
    @Query("SELECT * FROM sharing_apps WHERE isEnabled = 1 ORDER BY sortOrder ASC")
    fun getEnabledApps(): Flow<List<SharingApp>>

    @Query("SELECT * FROM sharing_apps ORDER BY sortOrder ASC")
    fun getAllApps(): Flow<List<SharingApp>>

    @Insert
    suspend fun insert(app: SharingApp)

    @Update
    suspend fun update(app: SharingApp)

    @Delete
    suspend fun delete(app: SharingApp)

    @Query("SELECT * FROM sharing_apps ORDER BY sortOrder ASC")
    suspend fun getAllAppsSync(): List<SharingApp>

    @Query("DELETE FROM sharing_apps")
    suspend fun deleteAllApps()

    @Insert
    suspend fun insertWithId(app: SharingApp)
}

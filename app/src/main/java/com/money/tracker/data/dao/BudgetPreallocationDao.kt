package com.money.tracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.money.tracker.data.entity.BudgetPreallocation
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetPreallocationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(preallocation: BudgetPreallocation)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(preallocations: List<BudgetPreallocation>)

    @Query("DELETE FROM budget_preallocations WHERE yearMonth = :yearMonth AND categoryId = :categoryId")
    suspend fun delete(yearMonth: String, categoryId: Long)

    @Query("DELETE FROM budget_preallocations WHERE yearMonth = :yearMonth")
    suspend fun deleteAllForMonth(yearMonth: String)

    @Query("SELECT * FROM budget_preallocations WHERE yearMonth = :yearMonth")
    fun getPreallocationsForMonth(yearMonth: String): Flow<List<BudgetPreallocation>>

    @Query("SELECT * FROM budget_preallocations WHERE yearMonth = :yearMonth")
    suspend fun getPreallocationsForMonthSync(yearMonth: String): List<BudgetPreallocation>

    @Query("SELECT SUM(amount) FROM budget_preallocations WHERE yearMonth = :yearMonth")
    fun getTotalPreallocatedForMonth(yearMonth: String): Flow<Double?>

    @Query("SELECT categoryId FROM budget_preallocations WHERE yearMonth = :yearMonth AND amount > 0")
    fun getPreallocatedCategoryIds(yearMonth: String): Flow<List<Long>>
}

package com.money.tracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.money.tracker.data.entity.Budget
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE yearMonth = :yearMonth")
    fun getBudget(yearMonth: String): Flow<Budget?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setBudget(budget: Budget)

    @Query("DELETE FROM budgets WHERE yearMonth = :yearMonth")
    suspend fun deleteBudget(yearMonth: String)
}

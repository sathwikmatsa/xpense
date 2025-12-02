package com.money.tracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.money.tracker.data.entity.CategoryBudget
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryBudgetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(categoryBudget: CategoryBudget)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categoryBudgets: List<CategoryBudget>)

    @Query("DELETE FROM category_budgets WHERE yearMonth = :yearMonth AND categoryId = :categoryId")
    suspend fun delete(yearMonth: String, categoryId: Long)

    @Query("DELETE FROM category_budgets WHERE yearMonth = :yearMonth")
    suspend fun deleteAllForMonth(yearMonth: String)

    @Query("SELECT * FROM category_budgets WHERE yearMonth = :yearMonth")
    fun getCategoryBudgetsForMonth(yearMonth: String): Flow<List<CategoryBudget>>

    @Query("SELECT * FROM category_budgets WHERE yearMonth = :yearMonth")
    suspend fun getCategoryBudgetsForMonthSync(yearMonth: String): List<CategoryBudget>

    @Query("SELECT * FROM category_budgets WHERE yearMonth = :yearMonth AND categoryId = :categoryId")
    suspend fun getCategoryBudget(yearMonth: String, categoryId: Long): CategoryBudget?
}

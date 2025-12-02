package com.money.tracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.money.tracker.data.entity.TagBudget
import kotlinx.coroutines.flow.Flow

@Dao
interface TagBudgetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tagBudget: TagBudget)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tagBudgets: List<TagBudget>)

    @Query("DELETE FROM tag_budgets WHERE yearMonth = :yearMonth AND tagId = :tagId")
    suspend fun delete(yearMonth: String, tagId: Long)

    @Query("DELETE FROM tag_budgets WHERE yearMonth = :yearMonth")
    suspend fun deleteAllForMonth(yearMonth: String)

    @Query("SELECT * FROM tag_budgets WHERE yearMonth = :yearMonth")
    fun getTagBudgetsForMonth(yearMonth: String): Flow<List<TagBudget>>

    @Query("SELECT * FROM tag_budgets WHERE yearMonth = :yearMonth")
    suspend fun getTagBudgetsForMonthSync(yearMonth: String): List<TagBudget>

    @Query("SELECT * FROM tag_budgets WHERE yearMonth = :yearMonth AND tagId = :tagId")
    suspend fun getTagBudget(yearMonth: String, tagId: Long): TagBudget?

    @Query("SELECT * FROM tag_budgets")
    suspend fun getAllTagBudgetsSync(): List<TagBudget>

    @Query("DELETE FROM tag_budgets")
    suspend fun deleteAllTagBudgets()
}

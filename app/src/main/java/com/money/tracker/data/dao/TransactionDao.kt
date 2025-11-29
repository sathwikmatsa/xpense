package com.money.tracker.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.money.tracker.data.entity.Transaction
import com.money.tracker.data.entity.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: Transaction): Long

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): Transaction?

    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getTransactionsBetween(startDate: Long, endDate: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY date DESC")
    fun getByType(type: TransactionType): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId ORDER BY date DESC")
    fun getByCategory(categoryId: Long): Flow<List<Transaction>>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = :type AND date BETWEEN :startDate AND :endDate AND isPending = 0")
    fun getTotalByType(type: TransactionType, startDate: Long, endDate: Long): Flow<Double?>

    @Query("""
        SELECT categoryId, SUM(amount) as total
        FROM transactions
        WHERE type = :type AND date BETWEEN :startDate AND :endDate AND isPending = 0
        GROUP BY categoryId
    """)
    fun getCategoryTotals(type: TransactionType, startDate: Long, endDate: Long): Flow<List<CategoryTotal>>

    @Query("SELECT * FROM transactions WHERE categoryId IS NULL ORDER BY date DESC")
    fun getUncategorized(): Flow<List<Transaction>>

    @Query("SELECT COUNT(*) FROM transactions WHERE categoryId IS NULL")
    fun getUncategorizedCount(): Flow<Int>

    @Query("UPDATE transactions SET categoryId = :newCategoryId WHERE categoryId = :oldCategoryId")
    suspend fun reassignCategory(oldCategoryId: Long, newCategoryId: Long)

    @Query("SELECT COUNT(*) FROM transactions WHERE categoryId = :categoryId")
    suspend fun getTransactionCountByCategory(categoryId: Long): Int

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE transactions SET isPending = 0 WHERE id = :id")
    suspend fun confirmPendingTransaction(id: Long)

    @Query("SELECT * FROM transactions WHERE isPending = 1 ORDER BY date DESC")
    fun getPendingTransactions(): Flow<List<Transaction>>

    @Query("SELECT COUNT(*) FROM transactions WHERE isPending = 1")
    fun getPendingCount(): Flow<Int>
}

data class CategoryTotal(
    val categoryId: Long?,
    val total: Double
)

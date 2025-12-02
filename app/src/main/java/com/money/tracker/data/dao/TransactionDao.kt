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

    // Split transaction queries
    @Query("SELECT * FROM transactions WHERE isSplit = 1 AND splitSynced = 0 ORDER BY date DESC")
    fun getUnsyncedSplitTransactions(): Flow<List<Transaction>>

    @Query("SELECT COUNT(*) FROM transactions WHERE isSplit = 1 AND splitSynced = 0")
    fun getUnsyncedSplitCount(): Flow<Int>

    @Query("UPDATE transactions SET splitSynced = 1 WHERE id = :id")
    suspend fun markSplitSynced(id: Long)

    // For category recommendations - get recent transactions with categories
    @Query("""
        SELECT * FROM transactions
        WHERE categoryId IS NOT NULL AND isPending = 0
        ORDER BY date DESC
        LIMIT :limit
    """)
    suspend fun getRecentCategorizedTransactions(limit: Int = 500): List<Transaction>

    // Check if a similar pending transaction exists (for deduplication)
    @Query("""
        SELECT COUNT(*) FROM transactions
        WHERE isPending = 1 AND isManual = 0 AND createdAt > :since
        AND ABS(amount - :amount) < 0.01
    """)
    suspend fun hasRecentPendingTransaction(since: Long, amount: Double): Int

    // Get expenses excluding preallocated categories (for discretionary budget tracking)
    @Query("""
        SELECT SUM(amount) FROM transactions
        WHERE type = 'EXPENSE' AND date BETWEEN :startDate AND :endDate AND isPending = 0
        AND (categoryId IS NULL OR categoryId NOT IN (:preallocatedCategoryIds))
    """)
    fun getDiscretionaryExpense(startDate: Long, endDate: Long, preallocatedCategoryIds: List<Long>): Flow<Double?>

    // Get expenses in preallocated categories
    @Query("""
        SELECT SUM(amount) FROM transactions
        WHERE type = 'EXPENSE' AND date BETWEEN :startDate AND :endDate AND isPending = 0
        AND categoryId IN (:preallocatedCategoryIds)
    """)
    fun getPreallocatedExpense(startDate: Long, endDate: Long, preallocatedCategoryIds: List<Long>): Flow<Double?>

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    suspend fun getAllTransactionsSync(): List<Transaction>

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWithId(transaction: Transaction)

    // Tag-related queries
    @Query("SELECT * FROM transactions WHERE tagId = :tagId ORDER BY date DESC")
    fun getByTag(tagId: Long): Flow<List<Transaction>>

    @Query("""
        SELECT tagId, SUM(amount) as total
        FROM transactions
        WHERE type = :type AND date BETWEEN :startDate AND :endDate AND isPending = 0
        GROUP BY tagId
    """)
    fun getTagTotals(type: TransactionType, startDate: Long, endDate: Long): Flow<List<TagTotal>>

    @Query("""
        SELECT categoryId, SUM(amount) as total
        FROM transactions
        WHERE type = :type AND date BETWEEN :startDate AND :endDate AND isPending = 0 AND tagId = :tagId
        GROUP BY categoryId
    """)
    fun getCategoryTotalsForTag(type: TransactionType, startDate: Long, endDate: Long, tagId: Long): Flow<List<CategoryTotal>>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate AND tagId = :tagId ORDER BY date DESC")
    fun getTransactionsBetweenWithTag(startDate: Long, endDate: Long, tagId: Long): Flow<List<Transaction>>

    @Query("SELECT COUNT(*) FROM transactions WHERE tagId = :tagId")
    suspend fun getTransactionCountByTag(tagId: Long): Int
}

data class CategoryTotal(
    val categoryId: Long?,
    val total: Double
)

data class TagTotal(
    val tagId: Long?,
    val total: Double
)

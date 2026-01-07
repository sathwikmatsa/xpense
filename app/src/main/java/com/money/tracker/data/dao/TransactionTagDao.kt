package com.money.tracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.money.tracker.data.entity.TransactionTag
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionTagDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transactionTag: TransactionTag)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(transactionTags: List<TransactionTag>)

    @Query("DELETE FROM transaction_tags WHERE transactionId = :transactionId")
    suspend fun deleteAllForTransaction(transactionId: Long)

    @Query("DELETE FROM transaction_tags WHERE transactionId = :transactionId AND tagId = :tagId")
    suspend fun delete(transactionId: Long, tagId: Long)

    @Query("SELECT tagId FROM transaction_tags WHERE transactionId = :transactionId")
    suspend fun getTagIdsForTransaction(transactionId: Long): List<Long>

    @Query("SELECT tagId FROM transaction_tags WHERE transactionId = :transactionId")
    fun getTagIdsForTransactionFlow(transactionId: Long): Flow<List<Long>>

    @Query("SELECT transactionId FROM transaction_tags WHERE tagId = :tagId")
    suspend fun getTransactionIdsForTag(tagId: Long): List<Long>

    @Query("SELECT COUNT(*) FROM transaction_tags WHERE tagId = :tagId")
    suspend fun getTransactionCountByTag(tagId: Long): Int

    @Query("SELECT * FROM transaction_tags")
    fun getAllTransactionTags(): Flow<List<TransactionTag>>
}

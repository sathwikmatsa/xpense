package com.money.tracker.data.repository

import com.money.tracker.data.dao.CategoryTotal
import com.money.tracker.data.dao.TransactionDao
import com.money.tracker.data.entity.Transaction
import com.money.tracker.data.entity.TransactionType
import kotlinx.coroutines.flow.Flow

class TransactionRepository(private val transactionDao: TransactionDao) {

    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()

    val uncategorizedTransactions: Flow<List<Transaction>> = transactionDao.getUncategorized()

    val uncategorizedCount: Flow<Int> = transactionDao.getUncategorizedCount()

    suspend fun insert(transaction: Transaction): Long {
        return transactionDao.insert(transaction)
    }

    suspend fun update(transaction: Transaction) {
        transactionDao.update(transaction)
    }

    suspend fun delete(transaction: Transaction) {
        transactionDao.delete(transaction)
    }

    suspend fun getById(id: Long): Transaction? {
        return transactionDao.getById(id)
    }

    fun getTransactionsBetween(startDate: Long, endDate: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsBetween(startDate, endDate)
    }

    fun getByType(type: TransactionType): Flow<List<Transaction>> {
        return transactionDao.getByType(type)
    }

    fun getByCategory(categoryId: Long): Flow<List<Transaction>> {
        return transactionDao.getByCategory(categoryId)
    }

    fun getTotalIncome(startDate: Long, endDate: Long): Flow<Double?> {
        return transactionDao.getTotalByType(TransactionType.INCOME, startDate, endDate)
    }

    fun getTotalExpense(startDate: Long, endDate: Long): Flow<Double?> {
        return transactionDao.getTotalByType(TransactionType.EXPENSE, startDate, endDate)
    }

    fun getCategoryTotals(type: TransactionType, startDate: Long, endDate: Long): Flow<List<CategoryTotal>> {
        return transactionDao.getCategoryTotals(type, startDate, endDate)
    }

    suspend fun reassignCategory(oldCategoryId: Long, newCategoryId: Long) {
        transactionDao.reassignCategory(oldCategoryId, newCategoryId)
    }

    suspend fun getTransactionCountByCategory(categoryId: Long): Int {
        return transactionDao.getTransactionCountByCategory(categoryId)
    }

    suspend fun deleteById(id: Long) {
        transactionDao.deleteById(id)
    }

    suspend fun confirmPendingTransaction(id: Long) {
        transactionDao.confirmPendingTransaction(id)
    }

    fun getPendingTransactions(): Flow<List<Transaction>> {
        return transactionDao.getPendingTransactions()
    }

    fun getPendingCount(): Flow<Int> {
        return transactionDao.getPendingCount()
    }

    // Split transaction methods
    fun getUnsyncedSplitTransactions(): Flow<List<Transaction>> {
        return transactionDao.getUnsyncedSplitTransactions()
    }

    fun getUnsyncedSplitCount(): Flow<Int> {
        return transactionDao.getUnsyncedSplitCount()
    }

    suspend fun markSplitSynced(id: Long) {
        transactionDao.markSplitSynced(id)
    }

    // For category recommendations
    suspend fun getRecentCategorizedTransactions(limit: Int = 500): List<Transaction> {
        return transactionDao.getRecentCategorizedTransactions(limit)
    }

    // Check if a similar pending transaction exists (for deduplication)
    suspend fun hasRecentPendingTransaction(amount: Double, withinMs: Long = 10_000L): Boolean {
        val since = System.currentTimeMillis() - withinMs
        return transactionDao.hasRecentPendingTransaction(since, amount) > 0
    }
}

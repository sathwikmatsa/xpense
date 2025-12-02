package com.money.tracker.data.repository

import com.money.tracker.data.dao.BudgetPreallocationDao
import com.money.tracker.data.entity.BudgetPreallocation
import kotlinx.coroutines.flow.Flow

class BudgetPreallocationRepository(private val dao: BudgetPreallocationDao) {

    fun getPreallocationsForMonth(yearMonth: String): Flow<List<BudgetPreallocation>> {
        return dao.getPreallocationsForMonth(yearMonth)
    }

    suspend fun getPreallocationsForMonthSync(yearMonth: String): List<BudgetPreallocation> {
        return dao.getPreallocationsForMonthSync(yearMonth)
    }

    fun getTotalPreallocatedForMonth(yearMonth: String): Flow<Double?> {
        return dao.getTotalPreallocatedForMonth(yearMonth)
    }

    fun getPreallocatedCategoryIds(yearMonth: String): Flow<List<Long>> {
        return dao.getPreallocatedCategoryIds(yearMonth)
    }

    suspend fun setPreallocation(yearMonth: String, categoryId: Long, amount: Double) {
        if (amount > 0) {
            dao.insert(BudgetPreallocation(yearMonth, categoryId, amount))
        } else {
            dao.delete(yearMonth, categoryId)
        }
    }

    suspend fun setPreallocations(yearMonth: String, preallocations: List<BudgetPreallocation>) {
        dao.deleteAllForMonth(yearMonth)
        if (preallocations.isNotEmpty()) {
            dao.insertAll(preallocations.filter { it.amount > 0 })
        }
    }

    suspend fun copyFromPreviousMonth(fromYearMonth: String, toYearMonth: String) {
        val previous = dao.getPreallocationsForMonthSync(fromYearMonth)
        val copied = previous.map { it.copy(yearMonth = toYearMonth) }
        dao.insertAll(copied)
    }
}

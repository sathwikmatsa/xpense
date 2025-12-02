package com.money.tracker.data.repository

import com.money.tracker.data.dao.CategoryBudgetDao
import com.money.tracker.data.entity.CategoryBudget
import kotlinx.coroutines.flow.Flow

class CategoryBudgetRepository(private val dao: CategoryBudgetDao) {

    fun getCategoryBudgetsForMonth(yearMonth: String): Flow<List<CategoryBudget>> {
        return dao.getCategoryBudgetsForMonth(yearMonth)
    }

    suspend fun getCategoryBudgetsForMonthSync(yearMonth: String): List<CategoryBudget> {
        return dao.getCategoryBudgetsForMonthSync(yearMonth)
    }

    suspend fun getCategoryBudget(yearMonth: String, categoryId: Long): CategoryBudget? {
        return dao.getCategoryBudget(yearMonth, categoryId)
    }

    suspend fun setCategoryBudget(yearMonth: String, categoryId: Long, amount: Double) {
        if (amount > 0) {
            dao.insert(CategoryBudget(yearMonth, categoryId, amount))
        } else {
            dao.delete(yearMonth, categoryId)
        }
    }

    suspend fun setCategoryBudgets(yearMonth: String, categoryBudgets: List<CategoryBudget>) {
        dao.deleteAllForMonth(yearMonth)
        if (categoryBudgets.isNotEmpty()) {
            dao.insertAll(categoryBudgets.filter { it.amount > 0 })
        }
    }

    suspend fun copyFromPreviousMonth(fromYearMonth: String, toYearMonth: String) {
        val previous = dao.getCategoryBudgetsForMonthSync(fromYearMonth)
        val copied = previous.map { it.copy(yearMonth = toYearMonth) }
        dao.insertAll(copied)
    }
}

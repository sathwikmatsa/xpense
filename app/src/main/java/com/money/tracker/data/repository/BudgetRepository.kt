package com.money.tracker.data.repository

import com.money.tracker.data.dao.BudgetDao
import com.money.tracker.data.entity.Budget
import kotlinx.coroutines.flow.Flow

class BudgetRepository(private val budgetDao: BudgetDao) {

    fun getBudget(yearMonth: String): Flow<Budget?> = budgetDao.getBudget(yearMonth)

    suspend fun setBudget(yearMonth: String, amount: Double) {
        budgetDao.setBudget(Budget(yearMonth, amount))
    }

    suspend fun deleteBudget(yearMonth: String) {
        budgetDao.deleteBudget(yearMonth)
    }
}

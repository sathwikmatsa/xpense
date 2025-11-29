package com.money.tracker.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.money.tracker.data.entity.Category
import com.money.tracker.data.entity.Transaction
import com.money.tracker.data.repository.BudgetRepository
import com.money.tracker.data.repository.CategoryRepository
import com.money.tracker.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class HomeUiState(
    val transactions: List<Transaction> = emptyList(),
    val pendingTransactions: List<Transaction> = emptyList(),
    val categories: Map<Long, Category> = emptyMap(),
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val budget: Double? = null,
    val showIncome: Boolean = false,
    val isLoading: Boolean = true
)

class HomeViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository
) : ViewModel() {

    private val calendar = Calendar.getInstance()
    private val startOfMonth: Long
    private val endOfMonth: Long
    private val currentYearMonth: String

    private val _showIncome = MutableStateFlow(false)

    init {
        val yearMonthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        currentYearMonth = yearMonthFormat.format(calendar.time)

        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        startOfMonth = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        endOfMonth = calendar.timeInMillis
    }

    val uiState: StateFlow<HomeUiState> = combine(
        transactionRepository.getTransactionsBetween(startOfMonth, endOfMonth),
        categoryRepository.allCategories,
        transactionRepository.getTotalIncome(startOfMonth, endOfMonth),
        transactionRepository.getTotalExpense(startOfMonth, endOfMonth),
        budgetRepository.getBudget(currentYearMonth),
        _showIncome,
        transactionRepository.getPendingTransactions()
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val transactions = values[0] as List<Transaction>
        @Suppress("UNCHECKED_CAST")
        val categories = values[1] as List<Category>
        val income = values[2] as Double?
        val expense = values[3] as Double?
        val budget = values[4] as com.money.tracker.data.entity.Budget?
        val showIncome = values[5] as Boolean
        @Suppress("UNCHECKED_CAST")
        val pendingTransactions = values[6] as List<Transaction>

        HomeUiState(
            transactions = transactions.filter { !it.isPending },
            pendingTransactions = pendingTransactions,
            categories = categories.associateBy { it.id },
            totalIncome = income ?: 0.0,
            totalExpense = expense ?: 0.0,
            budget = budget?.amount,
            showIncome = showIncome,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun toggleIncomeVisibility() {
        _showIncome.value = !_showIncome.value
    }

    fun setBudget(amount: Double) {
        viewModelScope.launch {
            budgetRepository.setBudget(currentYearMonth, amount)
        }
    }

    fun clearBudget() {
        viewModelScope.launch {
            budgetRepository.deleteBudget(currentYearMonth)
        }
    }

    fun dismissPendingTransaction(transactionId: Long) {
        viewModelScope.launch {
            transactionRepository.deleteById(transactionId)
        }
    }

    class Factory(
        private val transactionRepository: TransactionRepository,
        private val categoryRepository: CategoryRepository,
        private val budgetRepository: BudgetRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(transactionRepository, categoryRepository, budgetRepository) as T
        }
    }
}

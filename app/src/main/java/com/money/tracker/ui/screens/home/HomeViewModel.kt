package com.money.tracker.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.money.tracker.data.entity.BudgetPreallocation
import com.money.tracker.data.entity.Category
import com.money.tracker.data.entity.CategoryBudget
import com.money.tracker.data.entity.Tag
import com.money.tracker.data.entity.TagBudget
import com.money.tracker.data.entity.Transaction
import com.money.tracker.data.entity.UpiReminder
import com.money.tracker.data.repository.BudgetPreallocationRepository
import com.money.tracker.data.repository.BudgetRepository
import com.money.tracker.data.repository.CategoryBudgetRepository
import com.money.tracker.data.repository.CategoryRepository
import com.money.tracker.data.repository.TagRepository
import com.money.tracker.data.repository.TransactionRepository
import com.money.tracker.data.repository.UpiReminderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class CategoryBudgetWithSpending(
    val categoryBudget: CategoryBudget,
    val spent: Double,
    val category: Category
)

data class TagBudgetWithSpending(
    val tagBudget: TagBudget,
    val spent: Double,
    val tag: Tag
)

data class HomeUiState(
    val transactions: List<Transaction> = emptyList(),
    val pendingTransactions: List<Transaction> = emptyList(),
    val unsyncedSplitTransactions: List<Transaction> = emptyList(),
    val upiReminders: List<UpiReminder> = emptyList(),
    val categories: Map<Long, Category> = emptyMap(),
    val tags: Map<Long, Tag> = emptyMap(),
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val paidForOthers: Double = 0.0,
    val budget: Double? = null,
    val preallocations: List<BudgetPreallocation> = emptyList(),
    val preallocatedBudget: Double = 0.0,
    val tagBudgetTotal: Double = 0.0,
    val discretionaryExpense: Double = 0.0,
    val preallocatedExpense: Double = 0.0,
    val tagExpense: Double = 0.0,
    val categoryBudgets: List<CategoryBudget> = emptyList(),
    val categoryBudgetsWithSpending: List<CategoryBudgetWithSpending> = emptyList(),
    val tagBudgetsWithSpending: List<TagBudgetWithSpending> = emptyList(),
    val showIncome: Boolean = false,
    val isLoading: Boolean = true
)

class HomeViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository,
    private val upiReminderRepository: UpiReminderRepository,
    private val budgetPreallocationRepository: BudgetPreallocationRepository,
    private val categoryBudgetRepository: CategoryBudgetRepository,
    private val tagRepository: TagRepository
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
        transactionRepository.getTransactionsByExpenseDateBetween(startOfMonth, endOfMonth), // Use expense date for budget view
        categoryRepository.allCategories,
        transactionRepository.getTotalIncome(startOfMonth, endOfMonth),
        transactionRepository.getTotalExpense(startOfMonth, endOfMonth),
        budgetRepository.getBudget(currentYearMonth),
        _showIncome,
        transactionRepository.getPendingTransactions(),
        upiReminderRepository.allReminders,
        budgetPreallocationRepository.getPreallocationsForMonth(currentYearMonth)
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
        @Suppress("UNCHECKED_CAST")
        val upiReminders = values[7] as List<UpiReminder>
        @Suppress("UNCHECKED_CAST")
        val preallocations = values[8] as List<BudgetPreallocation>

        // Calculate paid for others from split transactions
        // Only count transactions where YOU paid (not Splitwise where someone else paid)
        val paidForOthers = transactions
            .filter { it.isSplit && !it.isPending && it.source != com.money.tracker.data.entity.TransactionSource.SPLITWISE }
            .sumOf { it.totalAmount - it.amount }

        // Calculate preallocated budget total from monthly preallocations
        val preallocatedBudget = preallocations.sumOf { it.amount }
        val preallocatedCategoryIds = preallocations.map { it.categoryId }.toSet()

        // Find categories excluded from expense calculations (like Settlement)
        val categoriesMap = categories.associateBy { it.id }
        val excludedCategoryIds = categories.filter { it.excludeFromExpense }.map { it.id }.toSet()

        // Calculate discretionary vs preallocated expenses from transactions
        // Exclude transactions in categories marked as excludeFromExpense
        val confirmedTransactions = transactions.filter { !it.isPending }
        val discretionaryExpense = confirmedTransactions
            .filter { it.categoryId == null || it.categoryId !in preallocatedCategoryIds }
            .filter { it.type == com.money.tracker.data.entity.TransactionType.EXPENSE }
            .filter { it.categoryId == null || it.categoryId !in excludedCategoryIds }
            .sumOf { it.amount }
        val preallocatedExpense = confirmedTransactions
            .filter { it.categoryId != null && it.categoryId in preallocatedCategoryIds }
            .filter { it.type == com.money.tracker.data.entity.TransactionType.EXPENSE }
            .filter { it.categoryId !in excludedCategoryIds }
            .sumOf { it.amount }

        // Calculate total expense excluding excluded categories
        val totalExpenseExcluded = confirmedTransactions
            .filter { it.type == com.money.tracker.data.entity.TransactionType.EXPENSE }
            .filter { it.categoryId == null || it.categoryId !in excludedCategoryIds }
            .sumOf { it.amount }

        HomeUiState(
            transactions = confirmedTransactions,
            pendingTransactions = pendingTransactions,
            unsyncedSplitTransactions = emptyList(), // Will be populated separately
            upiReminders = upiReminders,
            categories = categoriesMap,
            totalIncome = income ?: 0.0,
            totalExpense = totalExpenseExcluded, // Use calculated expense excluding Settlement
            paidForOthers = paidForOthers,
            budget = budget?.amount,
            preallocations = preallocations,
            preallocatedBudget = preallocatedBudget,
            discretionaryExpense = discretionaryExpense,
            preallocatedExpense = preallocatedExpense,
            showIncome = showIncome,
            isLoading = false
        )
    }.combine(transactionRepository.getUnsyncedSplitTransactions()) { state, unsyncedSplit ->
        state.copy(unsyncedSplitTransactions = unsyncedSplit)
    }.combine(categoryBudgetRepository.getCategoryBudgetsForMonth(currentYearMonth)) { state, categoryBudgets ->
        // Calculate spending per category with budget
        val categoryBudgetsWithSpending = categoryBudgets.mapNotNull { cb ->
            val category = state.categories[cb.categoryId] ?: return@mapNotNull null
            val spent = state.transactions
                .filter { it.categoryId == cb.categoryId }
                .filter { it.type == com.money.tracker.data.entity.TransactionType.EXPENSE }
                .sumOf { it.amount }
            CategoryBudgetWithSpending(cb, spent, category)
        }
        state.copy(
            categoryBudgets = categoryBudgets,
            categoryBudgetsWithSpending = categoryBudgetsWithSpending
        )
    }.combine(tagRepository.allTags) { state, tags ->
        state.copy(tags = tags.associateBy { it.id })
    }.combine(tagRepository.getTagBudgetsForMonth(currentYearMonth)) { state, tagBudgets ->
        // Calculate spending per tag with budget
        val tagBudgetsWithSpending = tagBudgets.mapNotNull { tb ->
            val tag = state.tags[tb.tagId] ?: return@mapNotNull null
            val spent = state.transactions
                .filter { it.tagId == tb.tagId }
                .filter { it.type == com.money.tracker.data.entity.TransactionType.EXPENSE }
                .sumOf { it.amount }
            TagBudgetWithSpending(tb, spent, tag)
        }
        // Calculate total tag budget allocation
        val tagBudgetTotal = tagBudgets.sumOf { it.amount }
        // Calculate spending on transactions with tags that have budgets
        val tagIdsWithBudget = tagBudgets.map { it.tagId }.toSet()
        val tagExpense = state.transactions
            .filter { it.tagId != null && it.tagId in tagIdsWithBudget }
            .filter { it.type == com.money.tracker.data.entity.TransactionType.EXPENSE }
            .sumOf { it.amount }
        state.copy(
            tagBudgetsWithSpending = tagBudgetsWithSpending,
            tagBudgetTotal = tagBudgetTotal,
            tagExpense = tagExpense
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

    fun dismissUpiReminder(reminderId: Long) {
        viewModelScope.launch {
            upiReminderRepository.deleteById(reminderId)
        }
    }

    fun markSplitSynced(transactionId: Long) {
        viewModelScope.launch {
            transactionRepository.markSplitSynced(transactionId)
        }
    }

    fun setPreallocation(categoryId: Long, amount: Double) {
        viewModelScope.launch {
            budgetPreallocationRepository.setPreallocation(currentYearMonth, categoryId, amount)
        }
    }

    fun savePreallocations(preallocations: List<BudgetPreallocation>) {
        viewModelScope.launch {
            budgetPreallocationRepository.setPreallocations(currentYearMonth, preallocations)
        }
    }

    fun getCurrentYearMonth(): String = currentYearMonth

    fun getPreviousYearMonth(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -1)
        return SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(cal.time)
    }

    suspend fun getPreviousMonthPreallocations(): List<BudgetPreallocation> {
        return budgetPreallocationRepository.getPreallocationsForMonthSync(getPreviousYearMonth())
    }

    fun setCategoryBudget(categoryId: Long, amount: Double) {
        viewModelScope.launch {
            categoryBudgetRepository.setCategoryBudget(currentYearMonth, categoryId, amount)
        }
    }

    fun saveCategoryBudgets(categoryBudgets: List<CategoryBudget>) {
        viewModelScope.launch {
            categoryBudgetRepository.setCategoryBudgets(currentYearMonth, categoryBudgets)
        }
    }

    suspend fun getPreviousMonthCategoryBudgets(): List<CategoryBudget> {
        return categoryBudgetRepository.getCategoryBudgetsForMonthSync(getPreviousYearMonth())
    }

    class Factory(
        private val transactionRepository: TransactionRepository,
        private val categoryRepository: CategoryRepository,
        private val budgetRepository: BudgetRepository,
        private val upiReminderRepository: UpiReminderRepository,
        private val budgetPreallocationRepository: BudgetPreallocationRepository,
        private val categoryBudgetRepository: CategoryBudgetRepository,
        private val tagRepository: TagRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(transactionRepository, categoryRepository, budgetRepository, upiReminderRepository, budgetPreallocationRepository, categoryBudgetRepository, tagRepository) as T
        }
    }
}

package com.money.tracker.ui.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.money.tracker.data.entity.Category
import com.money.tracker.data.entity.Tag
import com.money.tracker.data.entity.Transaction
import com.money.tracker.data.entity.TransactionType
import com.money.tracker.data.repository.CategoryRepository
import com.money.tracker.data.repository.TagRepository
import com.money.tracker.data.repository.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class AnalyticsTimeRange(val label: String, val days: Int) {
    WEEK("7 Days", 7),
    MONTH("30 Days", 30),
    THREE_MONTHS("3 Months", 90),
    SIX_MONTHS("6 Months", 180),
    YEAR("1 Year", 365)
}

enum class TrendTimeRange(val label: String, val days: Int, val barCount: Int) {
    WEEK("Week", 7, 7),
    MONTH("Month", 30, 30),
    THREE_MONTHS("3 Months", 90, 12),
    SIX_MONTHS("6 Months", 180, 6),
    YEAR("Year", 365, 12)
}

data class CategoryBreakdownItem(
    val categoryId: Long,
    val categoryName: String,
    val emoji: String,
    val amount: Double,
    val percentage: Float,
    val isParent: Boolean = false,
    val children: List<CategoryBreakdownItem> = emptyList()
)

data class DailySpending(
    val label: String,
    val amount: Double,
    val date: Long
)

data class AnalyticsUiState(
    val categoryBreakdown: List<CategoryBreakdownItem> = emptyList(),
    val totalExpense: Double = 0.0,
    val transactionCount: Int = 0,
    val selectedTimeRange: AnalyticsTimeRange = AnalyticsTimeRange.MONTH,
    val categories: List<Category> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val selectedTagId: Long? = null,
    val isLoading: Boolean = true
)

data class TrendUiState(
    val dailySpending: List<DailySpending> = emptyList(),
    val maxAmount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val selectedTimeRange: TrendTimeRange = TrendTimeRange.WEEK,
    val selectedCategories: Set<Long> = emptySet(),
    val categories: List<Category> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val selectedTagId: Long? = null
)

data class SplitSummaryItem(
    val label: String,
    val paidForOthers: Double,
    val date: Long
)

data class SplitCategoryItem(
    val categoryId: Long,
    val categoryName: String,
    val emoji: String,
    val amount: Double,
    val percentage: Float
)

data class SplitSummaryState(
    val monthlyData: List<SplitSummaryItem> = emptyList(),
    val categoryBreakdown: List<SplitCategoryItem> = emptyList(),
    val totalPaidForOthers: Double = 0.0,
    val maxAmount: Double = 0.0
)

private data class TrendParams(
    val timeRange: TrendTimeRange,
    val selectedCategories: Set<Long>,
    val selectedTagId: Long?,
    val categories: List<Category>,
    val tags: List<Tag>
)

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val tagRepository: TagRepository
) : ViewModel() {

    private val _selectedTimeRange = MutableStateFlow(AnalyticsTimeRange.MONTH)
    private val _selectedTagId = MutableStateFlow<Long?>(null)
    private val _trendTimeRange = MutableStateFlow(TrendTimeRange.WEEK)
    private val _selectedTrendCategories = MutableStateFlow<Set<Long>>(emptySet())
    private val _selectedTrendTagId = MutableStateFlow<Long?>(null)

    // Split summary: 6-month rolling window
    val splitSummaryState: StateFlow<SplitSummaryState> = combine(
        transactionRepository.allTransactions,
        categoryRepository.allCategories
    ) { transactions, categories ->
        // Get last 6 months of data
        val monthData = (0 until 6).map { monthOffset ->
            val calEnd = Calendar.getInstance()
            calEnd.add(Calendar.MONTH, -monthOffset)
            calEnd.set(Calendar.DAY_OF_MONTH, calEnd.getActualMaximum(Calendar.DAY_OF_MONTH))
            calEnd.set(Calendar.HOUR_OF_DAY, 23)
            calEnd.set(Calendar.MINUTE, 59)
            calEnd.set(Calendar.SECOND, 59)
            val monthEnd = calEnd.timeInMillis

            val calStart = Calendar.getInstance()
            calStart.add(Calendar.MONTH, -monthOffset)
            calStart.set(Calendar.DAY_OF_MONTH, 1)
            calStart.set(Calendar.HOUR_OF_DAY, 0)
            calStart.set(Calendar.MINUTE, 0)
            calStart.set(Calendar.SECOND, 0)
            calStart.set(Calendar.MILLISECOND, 0)
            val monthStart = calStart.timeInMillis

            val paidForOthers = transactions
                .filter { it.isSplit && !it.isPending && it.date in monthStart..monthEnd }
                .sumOf { it.totalAmount - it.amount }

            val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
            SplitSummaryItem(
                label = monthFormat.format(monthStart),
                paidForOthers = paidForOthers,
                date = monthStart
            )
        }.reversed()

        val total = monthData.sumOf { it.paidForOthers }
        val maxAmount = monthData.maxOfOrNull { it.paidForOthers } ?: 0.0

        // Calculate category breakdown for split transactions (last 6 months)
        val sixMonthsAgo = Calendar.getInstance().apply {
            add(Calendar.MONTH, -6)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val splitTransactions = transactions.filter {
            it.isSplit && !it.isPending && it.date >= sixMonthsAgo
        }

        val categoryMap = categories.associateBy { it.id }
        val parentCategories = categories.filter { it.parentId == null }

        // Group by parent category (roll up child categories to parent)
        val categoryTotals = mutableMapOf<Long, Double>()
        splitTransactions.forEach { txn ->
            val category = categoryMap[txn.categoryId]
            // If category has a parent, use parent's ID; otherwise use the category's own ID
            val parentId = (category?.parentId ?: category?.id ?: txn.categoryId) as Long
            val paidForOthers = txn.totalAmount - txn.amount
            categoryTotals.merge(parentId, paidForOthers) { old, new -> old + new }
        }

        val categoryBreakdown = parentCategories.mapNotNull { parent ->
            val amount = categoryTotals[parent.id] ?: 0.0
            if (amount > 0) {
                SplitCategoryItem(
                    categoryId = parent.id,
                    categoryName = parent.name,
                    emoji = parent.emoji,
                    amount = amount,
                    percentage = if (total > 0) (amount / total * 100).toFloat() else 0f
                )
            } else null
        }.sortedByDescending { it.amount }

        SplitSummaryState(
            monthlyData = monthData,
            categoryBreakdown = categoryBreakdown,
            totalPaidForOthers = total,
            maxAmount = maxAmount
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SplitSummaryState()
    )

    private fun getTimeRange(range: AnalyticsTimeRange): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -range.days)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return Pair(calendar.timeInMillis, endTime)
    }

    private fun getTrendTimeRange(range: TrendTimeRange): Pair<Long, Long> {
        val calendarEnd = Calendar.getInstance()
        // Set end time to end of today
        calendarEnd.set(Calendar.HOUR_OF_DAY, 23)
        calendarEnd.set(Calendar.MINUTE, 59)
        calendarEnd.set(Calendar.SECOND, 59)
        calendarEnd.set(Calendar.MILLISECOND, 999)
        val endTime = calendarEnd.timeInMillis

        val calendarStart = Calendar.getInstance()
        // Calculate start time based on range type
        when (range) {
            TrendTimeRange.WEEK -> {
                calendarStart.add(Calendar.DAY_OF_YEAR, -6) // 7 days including today
            }
            TrendTimeRange.MONTH -> {
                calendarStart.add(Calendar.DAY_OF_YEAR, -27) // 4 weeks including current
            }
            TrendTimeRange.THREE_MONTHS -> {
                // Go to first day of (current month - 2)
                calendarStart.add(Calendar.MONTH, -2)
                calendarStart.set(Calendar.DAY_OF_MONTH, 1)
            }
            TrendTimeRange.SIX_MONTHS -> {
                // Go to first day of (current month - 5)
                calendarStart.add(Calendar.MONTH, -5)
                calendarStart.set(Calendar.DAY_OF_MONTH, 1)
            }
            TrendTimeRange.YEAR -> {
                // Go to first day of (current month - 11)
                calendarStart.add(Calendar.MONTH, -11)
                calendarStart.set(Calendar.DAY_OF_MONTH, 1)
            }
        }

        calendarStart.set(Calendar.HOUR_OF_DAY, 0)
        calendarStart.set(Calendar.MINUTE, 0)
        calendarStart.set(Calendar.SECOND, 0)
        calendarStart.set(Calendar.MILLISECOND, 0)
        return Pair(calendarStart.timeInMillis, endTime)
    }

    val uiState: StateFlow<AnalyticsUiState> = combine(
        _selectedTimeRange,
        _selectedTagId
    ) { timeRange, tagId ->
        Pair(timeRange, tagId)
    }.flatMapLatest { (timeRange, selectedTagId) ->
        val (startTime, endTime) = getTimeRange(timeRange)
        combine(
            transactionRepository.getCategoryTotals(TransactionType.EXPENSE, startTime, endTime),
            categoryRepository.allCategories,
            transactionRepository.getTransactionsBetween(startTime, endTime),
            tagRepository.allTags
        ) { categoryTotals, categories, transactions, tags ->
            // Filter by tag if selected
            val filteredTransactions = if (selectedTagId != null) {
                transactions.filter { it.tagId == selectedTagId }
            } else {
                transactions
            }

            val expenseTransactions = filteredTransactions.filter {
                !it.isPending && it.type == TransactionType.EXPENSE
            }
            val totalExpense = expenseTransactions.sumOf { it.amount }

            // Calculate category totals from filtered transactions (not using the pre-calculated totals when tag filter is applied)
            val totalsMap = if (selectedTagId != null) {
                expenseTransactions.groupBy { it.categoryId ?: 0L }
                    .mapValues { it.value.sumOf { txn -> txn.amount } }
            } else {
                categoryTotals.associate { (it.categoryId ?: 0L) to it.total }
            }
            val parentCategories = categories.filter { it.parentId == null }

            val breakdown = parentCategories.mapNotNull { parent ->
                val childCategories = categories.filter { it.parentId == parent.id }
                val parentDirectAmount = totalsMap[parent.id] ?: 0.0

                val childTotals = childCategories.mapNotNull { child ->
                    val childAmount = totalsMap[child.id] ?: 0.0
                    if (childAmount > 0) {
                        CategoryBreakdownItem(
                            categoryId = child.id,
                            categoryName = child.name,
                            emoji = child.emoji,
                            amount = childAmount,
                            percentage = if (totalExpense > 0) (childAmount / totalExpense * 100).toFloat() else 0f
                        )
                    } else null
                }.toMutableList()

                if (parentDirectAmount > 0 && childTotals.isNotEmpty()) {
                    childTotals.add(
                        CategoryBreakdownItem(
                            categoryId = parent.id,
                            categoryName = "Other",
                            emoji = "📦",
                            amount = parentDirectAmount,
                            percentage = if (totalExpense > 0) (parentDirectAmount / totalExpense * 100).toFloat() else 0f
                        )
                    )
                }

                val childrenTotalAmount = childCategories.sumOf { totalsMap[it.id] ?: 0.0 }
                val combinedAmount = parentDirectAmount + childrenTotalAmount

                if (combinedAmount > 0) {
                    CategoryBreakdownItem(
                        categoryId = parent.id,
                        categoryName = parent.name,
                        emoji = parent.emoji,
                        amount = combinedAmount,
                        percentage = if (totalExpense > 0) (combinedAmount / totalExpense * 100).toFloat() else 0f,
                        isParent = childTotals.isNotEmpty(),
                        children = childTotals.sortedByDescending { it.amount }
                    )
                } else null
            }.sortedByDescending { it.amount }

            AnalyticsUiState(
                categoryBreakdown = breakdown,
                totalExpense = totalExpense,
                transactionCount = expenseTransactions.size,
                selectedTimeRange = timeRange,
                categories = categories,
                tags = tags,
                selectedTagId = selectedTagId,
                isLoading = false
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AnalyticsUiState()
    )

    val trendState: StateFlow<TrendUiState> = combine(
        _trendTimeRange,
        _selectedTrendCategories,
        _selectedTrendTagId,
        categoryRepository.allCategories,
        tagRepository.allTags
    ) { timeRange, selectedCategories, selectedTagId, categories, tags ->
        TrendParams(timeRange, selectedCategories, selectedTagId, categories, tags)
    }.flatMapLatest { params ->
        val (startTime, endTime) = getTrendTimeRange(params.timeRange)

        // Build expanded categories (include children of selected parents)
        val expandedCategories = if (params.selectedCategories.isEmpty()) {
            emptySet()
        } else {
            val expanded = params.selectedCategories.toMutableSet()
            params.selectedCategories.forEach { selectedId ->
                params.categories.filter { it.parentId == selectedId }
                    .forEach { child -> expanded.add(child.id) }
            }
            expanded
        }

        transactionRepository.getTransactionsBetween(startTime, endTime).combine(
            kotlinx.coroutines.flow.flowOf(Unit)
        ) { transactions, _ ->
            val filteredTransactions = transactions.filter { txn ->
                !txn.isPending &&
                txn.type == TransactionType.EXPENSE &&
                (expandedCategories.isEmpty() || txn.categoryId in expandedCategories) &&
                (params.selectedTagId == null || txn.tagId == params.selectedTagId)
            }

            // Total is sum of all filtered transactions in the range
            val totalAmount = filteredTransactions.sumOf { it.amount }

            val dailySpending = calculateDailySpending(filteredTransactions, params.timeRange, startTime)
            val maxAmount = dailySpending.maxOfOrNull { it.amount } ?: 0.0

            TrendUiState(
                dailySpending = dailySpending,
                maxAmount = maxAmount,
                totalAmount = totalAmount,
                selectedTimeRange = params.timeRange,
                selectedCategories = params.selectedCategories,
                categories = params.categories,
                tags = params.tags,
                selectedTagId = params.selectedTagId
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TrendUiState()
    )

    private fun calculateDailySpending(
        transactions: List<Transaction>,
        timeRange: TrendTimeRange,
        startTime: Long
    ): List<DailySpending> {
        return when (timeRange) {
            TrendTimeRange.WEEK -> {
                // 7 individual days (including today)
                (0 until 7).map { dayOffset ->
                    val calendarStart = Calendar.getInstance()
                    calendarStart.timeInMillis = startTime
                    calendarStart.add(Calendar.DAY_OF_YEAR, dayOffset)
                    calendarStart.set(Calendar.HOUR_OF_DAY, 0)
                    calendarStart.set(Calendar.MINUTE, 0)
                    calendarStart.set(Calendar.SECOND, 0)
                    calendarStart.set(Calendar.MILLISECOND, 0)
                    val dayStart = calendarStart.timeInMillis

                    val calendarEnd = Calendar.getInstance()
                    calendarEnd.timeInMillis = dayStart
                    calendarEnd.set(Calendar.HOUR_OF_DAY, 23)
                    calendarEnd.set(Calendar.MINUTE, 59)
                    calendarEnd.set(Calendar.SECOND, 59)
                    calendarEnd.set(Calendar.MILLISECOND, 999)
                    val dayEnd = calendarEnd.timeInMillis

                    val amount = transactions.filter { it.date in dayStart..dayEnd }
                        .sumOf { it.amount }

                    val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
                    DailySpending(
                        label = dayFormat.format(dayStart),
                        amount = amount,
                        date = dayStart
                    )
                }
            }
            TrendTimeRange.MONTH -> {
                // 4 weeks
                (0 until 4).map { weekOffset ->
                    val calendarStart = Calendar.getInstance()
                    calendarStart.timeInMillis = startTime
                    calendarStart.add(Calendar.DAY_OF_YEAR, weekOffset * 7)
                    calendarStart.set(Calendar.HOUR_OF_DAY, 0)
                    calendarStart.set(Calendar.MINUTE, 0)
                    calendarStart.set(Calendar.SECOND, 0)
                    calendarStart.set(Calendar.MILLISECOND, 0)
                    val weekStart = calendarStart.timeInMillis

                    val calendarEnd = Calendar.getInstance()
                    calendarEnd.timeInMillis = weekStart
                    calendarEnd.add(Calendar.DAY_OF_YEAR, 6)
                    calendarEnd.set(Calendar.HOUR_OF_DAY, 23)
                    calendarEnd.set(Calendar.MINUTE, 59)
                    calendarEnd.set(Calendar.SECOND, 59)
                    calendarEnd.set(Calendar.MILLISECOND, 999)
                    val weekEnd = calendarEnd.timeInMillis

                    val amount = transactions.filter { it.date in weekStart..weekEnd }
                        .sumOf { it.amount }

                    val weekFormat = SimpleDateFormat("d MMM", Locale.getDefault())
                    DailySpending(
                        label = weekFormat.format(weekStart),
                        amount = amount,
                        date = weekStart
                    )
                }
            }
            TrendTimeRange.THREE_MONTHS -> {
                // 3 months (each bar = 1 month)
                (0 until 3).map { monthOffset ->
                    val calendarStart = Calendar.getInstance()
                    calendarStart.timeInMillis = startTime
                    calendarStart.add(Calendar.MONTH, monthOffset)
                    calendarStart.set(Calendar.DAY_OF_MONTH, 1)
                    calendarStart.set(Calendar.HOUR_OF_DAY, 0)
                    calendarStart.set(Calendar.MINUTE, 0)
                    calendarStart.set(Calendar.SECOND, 0)
                    calendarStart.set(Calendar.MILLISECOND, 0)
                    val monthStart = calendarStart.timeInMillis

                    val calendarEnd = Calendar.getInstance()
                    calendarEnd.timeInMillis = monthStart
                    calendarEnd.add(Calendar.MONTH, 1)
                    calendarEnd.add(Calendar.DAY_OF_YEAR, -1)
                    calendarEnd.set(Calendar.HOUR_OF_DAY, 23)
                    calendarEnd.set(Calendar.MINUTE, 59)
                    calendarEnd.set(Calendar.SECOND, 59)
                    calendarEnd.set(Calendar.MILLISECOND, 999)
                    val monthEnd = calendarEnd.timeInMillis

                    val amount = transactions.filter { it.date in monthStart..monthEnd }
                        .sumOf { it.amount }

                    val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
                    DailySpending(
                        label = monthFormat.format(monthStart),
                        amount = amount,
                        date = monthStart
                    )
                }
            }
            TrendTimeRange.SIX_MONTHS -> {
                // 6 months (each bar = 1 month)
                (0 until 6).map { monthOffset ->
                    val calendarStart = Calendar.getInstance()
                    calendarStart.timeInMillis = startTime
                    calendarStart.add(Calendar.MONTH, monthOffset)
                    calendarStart.set(Calendar.DAY_OF_MONTH, 1)
                    calendarStart.set(Calendar.HOUR_OF_DAY, 0)
                    calendarStart.set(Calendar.MINUTE, 0)
                    calendarStart.set(Calendar.SECOND, 0)
                    calendarStart.set(Calendar.MILLISECOND, 0)
                    val monthStart = calendarStart.timeInMillis

                    val calendarEnd = Calendar.getInstance()
                    calendarEnd.timeInMillis = monthStart
                    calendarEnd.add(Calendar.MONTH, 1)
                    calendarEnd.add(Calendar.DAY_OF_YEAR, -1)
                    calendarEnd.set(Calendar.HOUR_OF_DAY, 23)
                    calendarEnd.set(Calendar.MINUTE, 59)
                    calendarEnd.set(Calendar.SECOND, 59)
                    calendarEnd.set(Calendar.MILLISECOND, 999)
                    val monthEnd = calendarEnd.timeInMillis

                    val amount = transactions.filter { it.date in monthStart..monthEnd }
                        .sumOf { it.amount }

                    val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
                    DailySpending(
                        label = monthFormat.format(monthStart),
                        amount = amount,
                        date = monthStart
                    )
                }
            }
            TrendTimeRange.YEAR -> {
                // 12 months (each bar = 1 month)
                (0 until 12).map { monthOffset ->
                    val calendarStart = Calendar.getInstance()
                    calendarStart.timeInMillis = startTime
                    calendarStart.add(Calendar.MONTH, monthOffset)
                    calendarStart.set(Calendar.DAY_OF_MONTH, 1)
                    calendarStart.set(Calendar.HOUR_OF_DAY, 0)
                    calendarStart.set(Calendar.MINUTE, 0)
                    calendarStart.set(Calendar.SECOND, 0)
                    calendarStart.set(Calendar.MILLISECOND, 0)
                    val monthStart = calendarStart.timeInMillis

                    val calendarEnd = Calendar.getInstance()
                    calendarEnd.timeInMillis = monthStart
                    calendarEnd.add(Calendar.MONTH, 1)
                    calendarEnd.add(Calendar.DAY_OF_YEAR, -1)
                    calendarEnd.set(Calendar.HOUR_OF_DAY, 23)
                    calendarEnd.set(Calendar.MINUTE, 59)
                    calendarEnd.set(Calendar.SECOND, 59)
                    calendarEnd.set(Calendar.MILLISECOND, 999)
                    val monthEnd = calendarEnd.timeInMillis

                    val amount = transactions.filter { it.date in monthStart..monthEnd }
                        .sumOf { it.amount }

                    val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
                    DailySpending(
                        label = monthFormat.format(monthStart),
                        amount = amount,
                        date = monthStart
                    )
                }
            }
        }
    }

    fun setTimeRange(range: AnalyticsTimeRange) {
        _selectedTimeRange.value = range
    }

    fun setTagFilter(tagId: Long?) {
        _selectedTagId.value = tagId
    }

    fun setTrendTimeRange(range: TrendTimeRange) {
        _trendTimeRange.value = range
    }

    fun setTrendTagFilter(tagId: Long?) {
        _selectedTrendTagId.value = tagId
    }

    fun toggleTrendCategory(categoryId: Long) {
        val current = _selectedTrendCategories.value
        _selectedTrendCategories.value = if (categoryId in current) {
            current - categoryId
        } else {
            current + categoryId
        }
    }

    fun clearTrendCategories() {
        _selectedTrendCategories.value = emptySet()
    }

    class Factory(
        private val transactionRepository: TransactionRepository,
        private val categoryRepository: CategoryRepository,
        private val tagRepository: TagRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AnalyticsViewModel(transactionRepository, categoryRepository, tagRepository) as T
        }
    }
}

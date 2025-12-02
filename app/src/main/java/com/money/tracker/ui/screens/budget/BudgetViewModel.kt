package com.money.tracker.ui.screens.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.money.tracker.data.entity.BudgetPreallocation
import com.money.tracker.data.entity.Category
import com.money.tracker.data.entity.CategoryBudget
import com.money.tracker.data.entity.Tag
import com.money.tracker.data.entity.TagBudget
import com.money.tracker.data.repository.BudgetPreallocationRepository
import com.money.tracker.data.repository.BudgetRepository
import com.money.tracker.data.repository.CategoryBudgetRepository
import com.money.tracker.data.repository.CategoryRepository
import com.money.tracker.data.repository.TagRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class BudgetUiState(
    val budget: Double? = null,
    val preallocations: List<BudgetPreallocation> = emptyList(),
    val categoryBudgets: List<CategoryBudget> = emptyList(),
    val tagBudgets: List<TagBudget> = emptyList(),
    val categories: Map<Long, Category> = emptyMap(),
    val tags: Map<Long, Tag> = emptyMap(),
    val isLoading: Boolean = true
)

class BudgetViewModel(
    private val budgetRepository: BudgetRepository,
    private val budgetPreallocationRepository: BudgetPreallocationRepository,
    private val categoryBudgetRepository: CategoryBudgetRepository,
    private val categoryRepository: CategoryRepository,
    private val tagRepository: TagRepository
) : ViewModel() {

    private val calendar = Calendar.getInstance()
    private val yearMonthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    val currentYearMonth: String = yearMonthFormat.format(calendar.time)

    val uiState: StateFlow<BudgetUiState> = combine(
        budgetRepository.getBudget(currentYearMonth),
        budgetPreallocationRepository.getPreallocationsForMonth(currentYearMonth),
        categoryBudgetRepository.getCategoryBudgetsForMonth(currentYearMonth),
        tagRepository.getTagBudgetsForMonth(currentYearMonth),
        categoryRepository.allCategories,
        tagRepository.allTags
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val budget = values[0] as com.money.tracker.data.entity.Budget?
        @Suppress("UNCHECKED_CAST")
        val preallocations = values[1] as List<BudgetPreallocation>
        @Suppress("UNCHECKED_CAST")
        val categoryBudgets = values[2] as List<CategoryBudget>
        @Suppress("UNCHECKED_CAST")
        val tagBudgets = values[3] as List<TagBudget>
        @Suppress("UNCHECKED_CAST")
        val categories = values[4] as List<Category>
        @Suppress("UNCHECKED_CAST")
        val tags = values[5] as List<Tag>

        BudgetUiState(
            budget = budget?.amount,
            preallocations = preallocations,
            categoryBudgets = categoryBudgets,
            tagBudgets = tagBudgets,
            categories = categories.associateBy { it.id },
            tags = tags.associateBy { it.id },
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BudgetUiState()
    )

    private val _editableBudget = MutableStateFlow<Double?>(null)
    val editableBudget: StateFlow<Double?> = _editableBudget

    private val _editablePreallocations = MutableStateFlow<List<BudgetPreallocation>>(emptyList())
    val editablePreallocations: StateFlow<List<BudgetPreallocation>> = _editablePreallocations

    private val _editableCategoryBudgets = MutableStateFlow<List<CategoryBudget>>(emptyList())
    val editableCategoryBudgets: StateFlow<List<CategoryBudget>> = _editableCategoryBudgets

    private val _editableTagBudgets = MutableStateFlow<List<TagBudget>>(emptyList())
    val editableTagBudgets: StateFlow<List<TagBudget>> = _editableTagBudgets

    fun initializeEditableState(state: BudgetUiState) {
        _editableBudget.value = state.budget
        _editablePreallocations.value = state.preallocations.toList()
        _editableCategoryBudgets.value = state.categoryBudgets.toList()
        _editableTagBudgets.value = state.tagBudgets.toList()
    }

    fun setBudgetAmount(amount: Double?) {
        _editableBudget.value = amount
    }

    fun addPreallocation(categoryId: Long) {
        val current = _editablePreallocations.value.toMutableList()
        current.add(BudgetPreallocation(currentYearMonth, categoryId, 0.0))
        _editablePreallocations.value = current
    }

    fun updatePreallocation(categoryId: Long, amount: Double) {
        val current = _editablePreallocations.value.toMutableList()
        val index = current.indexOfFirst { it.categoryId == categoryId }
        if (index >= 0) {
            current[index] = current[index].copy(amount = amount)
            _editablePreallocations.value = current
        }
    }

    fun removePreallocation(categoryId: Long) {
        _editablePreallocations.value = _editablePreallocations.value.filter { it.categoryId != categoryId }
    }

    fun addCategoryBudget(categoryId: Long) {
        val current = _editableCategoryBudgets.value.toMutableList()
        current.add(CategoryBudget(currentYearMonth, categoryId, 0.0))
        _editableCategoryBudgets.value = current
    }

    fun updateCategoryBudget(categoryId: Long, amount: Double) {
        val current = _editableCategoryBudgets.value.toMutableList()
        val index = current.indexOfFirst { it.categoryId == categoryId }
        if (index >= 0) {
            current[index] = current[index].copy(amount = amount)
            _editableCategoryBudgets.value = current
        }
    }

    fun removeCategoryBudget(categoryId: Long) {
        _editableCategoryBudgets.value = _editableCategoryBudgets.value.filter { it.categoryId != categoryId }
    }

    fun addTagBudget(tagId: Long) {
        val current = _editableTagBudgets.value.toMutableList()
        current.add(TagBudget(currentYearMonth, tagId, 0.0))
        _editableTagBudgets.value = current
    }

    fun updateTagBudget(tagId: Long, amount: Double) {
        val current = _editableTagBudgets.value.toMutableList()
        val index = current.indexOfFirst { it.tagId == tagId }
        if (index >= 0) {
            current[index] = current[index].copy(amount = amount)
            _editableTagBudgets.value = current
        }
    }

    fun removeTagBudget(tagId: Long) {
        _editableTagBudgets.value = _editableTagBudgets.value.filter { it.tagId != tagId }
    }

    fun saveBudget() {
        viewModelScope.launch {
            val budget = _editableBudget.value
            if (budget != null && budget > 0) {
                budgetRepository.setBudget(currentYearMonth, budget)
            } else {
                budgetRepository.deleteBudget(currentYearMonth)
            }
            budgetPreallocationRepository.setPreallocations(currentYearMonth, _editablePreallocations.value)
            categoryBudgetRepository.setCategoryBudgets(currentYearMonth, _editableCategoryBudgets.value)
            tagRepository.setTagBudgets(currentYearMonth, _editableTagBudgets.value)
        }
    }

    fun clearBudget() {
        viewModelScope.launch {
            budgetRepository.deleteBudget(currentYearMonth)
            budgetPreallocationRepository.setPreallocations(currentYearMonth, emptyList())
            categoryBudgetRepository.setCategoryBudgets(currentYearMonth, emptyList())
            tagRepository.setTagBudgets(currentYearMonth, emptyList())
        }
    }

    suspend fun getPreviousMonthPreallocations(): List<BudgetPreallocation> {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -1)
        val previousYearMonth = yearMonthFormat.format(cal.time)
        return budgetPreallocationRepository.getPreallocationsForMonthSync(previousYearMonth)
    }

    suspend fun getPreviousMonthCategoryBudgets(): List<CategoryBudget> {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -1)
        val previousYearMonth = yearMonthFormat.format(cal.time)
        return categoryBudgetRepository.getCategoryBudgetsForMonthSync(previousYearMonth)
    }

    suspend fun getPreviousMonthTagBudgets(): List<TagBudget> {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -1)
        val previousYearMonth = yearMonthFormat.format(cal.time)
        return tagRepository.getTagBudgetsForMonthSync(previousYearMonth)
    }

    class Factory(
        private val budgetRepository: BudgetRepository,
        private val budgetPreallocationRepository: BudgetPreallocationRepository,
        private val categoryBudgetRepository: CategoryBudgetRepository,
        private val categoryRepository: CategoryRepository,
        private val tagRepository: TagRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BudgetViewModel(budgetRepository, budgetPreallocationRepository, categoryBudgetRepository, categoryRepository, tagRepository) as T
        }
    }
}

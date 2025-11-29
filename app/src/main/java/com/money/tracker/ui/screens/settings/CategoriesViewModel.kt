package com.money.tracker.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.money.tracker.data.entity.Category
import com.money.tracker.data.repository.CategoryRepository
import com.money.tracker.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CategoryWithChildren(
    val category: Category,
    val children: List<Category> = emptyList(),
    val transactionCount: Int = 0
)

class CategoriesViewModel(
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    val categories: StateFlow<List<Category>> = categoryRepository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _deleteState = MutableStateFlow<DeleteState>(DeleteState.Idle)
    val deleteState: StateFlow<DeleteState> = _deleteState.asStateFlow()

    fun createCategory(name: String, emoji: String, parentId: Long?) {
        viewModelScope.launch {
            categoryRepository.insert(Category(name = name, emoji = emoji, parentId = parentId))
        }
    }

    fun updateCategory(category: Category, name: String, emoji: String, parentId: Long?) {
        viewModelScope.launch {
            categoryRepository.update(category.copy(name = name, emoji = emoji, parentId = parentId))
        }
    }

    fun checkAndDeleteCategory(category: Category) {
        viewModelScope.launch {
            val transactionCount = transactionRepository.getTransactionCountByCategory(category.id)
            if (transactionCount > 0) {
                _deleteState.value = DeleteState.NeedsReassignment(category, transactionCount)
            } else {
                categoryRepository.delete(category)
                _deleteState.value = DeleteState.Idle
            }
        }
    }

    fun deleteWithReassignment(categoryToDelete: Category, reassignTo: Category) {
        viewModelScope.launch {
            transactionRepository.reassignCategory(categoryToDelete.id, reassignTo.id)
            categoryRepository.delete(categoryToDelete)
            _deleteState.value = DeleteState.Idle
        }
    }

    fun cancelDelete() {
        _deleteState.value = DeleteState.Idle
    }

    fun getParentCategories(excludeId: Long? = null): List<Category> {
        return categories.value.filter { it.parentId == null && it.id != excludeId }
    }

    class Factory(
        private val categoryRepository: CategoryRepository,
        private val transactionRepository: TransactionRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CategoriesViewModel(categoryRepository, transactionRepository) as T
        }
    }
}

sealed class DeleteState {
    data object Idle : DeleteState()
    data class NeedsReassignment(val category: Category, val transactionCount: Int) : DeleteState()
}

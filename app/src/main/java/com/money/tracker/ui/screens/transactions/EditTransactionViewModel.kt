package com.money.tracker.ui.screens.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.money.tracker.data.entity.Category
import com.money.tracker.data.entity.Transaction
import com.money.tracker.data.entity.TransactionSource
import com.money.tracker.data.entity.TransactionType
import com.money.tracker.data.repository.CategoryRepository
import com.money.tracker.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EditTransactionUiState(
    val transaction: Transaction? = null,
    val isLoading: Boolean = true,
    val isSaved: Boolean = false,
    val isDeleted: Boolean = false
)

class EditTransactionViewModel(
    private val transactionId: Long,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditTransactionUiState())
    val uiState: StateFlow<EditTransactionUiState> = _uiState.asStateFlow()

    val categories: StateFlow<List<Category>> = categoryRepository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadTransaction()
    }

    private fun loadTransaction() {
        viewModelScope.launch {
            val transaction = transactionRepository.getById(transactionId)
            _uiState.value = EditTransactionUiState(
                transaction = transaction,
                isLoading = false
            )
        }
    }

    fun updateTransaction(
        amount: Double,
        description: String,
        type: TransactionType,
        categoryId: Long,
        source: TransactionSource,
        date: Long
    ) {
        viewModelScope.launch {
            val existing = _uiState.value.transaction ?: return@launch
            val updated = existing.copy(
                amount = amount,
                description = description,
                type = type,
                categoryId = categoryId,
                source = source,
                date = date,
                isPending = false // Mark as confirmed when saved
            )
            transactionRepository.update(updated)
            _uiState.value = _uiState.value.copy(isSaved = true)
        }
    }

    fun createCategory(name: String, emoji: String) {
        viewModelScope.launch {
            categoryRepository.insert(Category(name = name, emoji = emoji))
        }
    }

    fun deleteTransaction() {
        viewModelScope.launch {
            val existing = _uiState.value.transaction ?: return@launch
            transactionRepository.delete(existing)
            _uiState.value = _uiState.value.copy(isDeleted = true)
        }
    }

    class Factory(
        private val transactionId: Long,
        private val transactionRepository: TransactionRepository,
        private val categoryRepository: CategoryRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EditTransactionViewModel(transactionId, transactionRepository, categoryRepository) as T
        }
    }
}

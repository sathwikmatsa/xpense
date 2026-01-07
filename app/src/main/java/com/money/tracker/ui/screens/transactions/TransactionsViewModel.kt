package com.money.tracker.ui.screens.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.money.tracker.data.entity.Category
import com.money.tracker.data.entity.Tag
import com.money.tracker.data.entity.Transaction
import com.money.tracker.data.repository.CategoryRepository
import com.money.tracker.data.repository.TagRepository
import com.money.tracker.data.repository.TransactionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class TransactionsUiState(
    val transactions: List<Transaction> = emptyList(),
    val categories: Map<Long, Category> = emptyMap(),
    val tags: Map<Long, Tag> = emptyMap(),
    val transactionTagIds: Map<Long, List<Long>> = emptyMap(),
    val isLoading: Boolean = true
)

class TransactionsViewModel(
    transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    tagRepository: TagRepository
) : ViewModel() {

    val uiState: StateFlow<TransactionsUiState> = combine(
        transactionRepository.allTransactions,
        categoryRepository.allCategories,
        tagRepository.allTags,
        transactionRepository.getAllTransactionTagsMap()
    ) { transactions, categories, tags, transactionTagIds ->
        TransactionsUiState(
            transactions = transactions,
            categories = categories.associateBy { it.id },
            tags = tags.associateBy { it.id },
            transactionTagIds = transactionTagIds,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionsUiState()
    )

    class Factory(
        private val transactionRepository: TransactionRepository,
        private val categoryRepository: CategoryRepository,
        private val tagRepository: TagRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TransactionsViewModel(transactionRepository, categoryRepository, tagRepository) as T
        }
    }
}

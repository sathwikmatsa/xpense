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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class AddTransactionViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    val categories: Flow<List<Category>> = categoryRepository.allCategories

    fun saveTransaction(
        amount: Double,
        description: String,
        type: TransactionType,
        categoryId: Long,
        source: TransactionSource,
        date: Long
    ) {
        viewModelScope.launch {
            val transaction = Transaction(
                amount = amount,
                description = description,
                type = type,
                categoryId = categoryId,
                source = source,
                date = date,
                isManual = true
            )
            transactionRepository.insert(transaction)
        }
    }

    fun createCategory(name: String, emoji: String) {
        viewModelScope.launch {
            categoryRepository.insert(Category(name = name, emoji = emoji))
        }
    }

    class Factory(
        private val transactionRepository: TransactionRepository,
        private val categoryRepository: CategoryRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AddTransactionViewModel(transactionRepository, categoryRepository) as T
        }
    }
}

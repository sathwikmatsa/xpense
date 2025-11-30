package com.money.tracker.ui.screens.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.money.tracker.data.entity.Category
import com.money.tracker.data.entity.Transaction
import com.money.tracker.data.entity.TransactionSource
import com.money.tracker.data.entity.TransactionType
import com.money.tracker.data.entity.SharingApp
import com.money.tracker.data.repository.CategoryRepository
import com.money.tracker.data.repository.SharingAppRepository
import com.money.tracker.data.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class AddTransactionViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val sharingAppRepository: SharingAppRepository
) : ViewModel() {

    val categories: Flow<List<Category>> = categoryRepository.allCategories
    val sharingApps: Flow<List<SharingApp>> = sharingAppRepository.enabledApps

    fun saveTransaction(
        amount: Double,
        description: String,
        type: TransactionType,
        categoryId: Long,
        source: TransactionSource,
        date: Long,
        isSplit: Boolean = false,
        splitNumerator: Int = 1,
        splitDenominator: Int = 1,
        customMyShare: Double? = null,
        markAsSynced: Boolean = false
    ) {
        viewModelScope.launch {
            // For split transactions, calculate my share
            val myShare = when {
                customMyShare != null -> customMyShare
                isSplit && splitDenominator > 0 -> amount * splitNumerator / splitDenominator
                else -> amount
            }

            val transaction = Transaction(
                amount = myShare,
                description = description,
                type = type,
                categoryId = categoryId,
                source = source,
                date = date,
                isManual = true,
                isSplit = isSplit,
                splitNumerator = if (customMyShare != null) 0 else splitNumerator,
                splitDenominator = if (customMyShare != null) 0 else splitDenominator,
                totalAmount = if (isSplit) amount else 0.0,
                splitSynced = markAsSynced
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
        private val categoryRepository: CategoryRepository,
        private val sharingAppRepository: SharingAppRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AddTransactionViewModel(transactionRepository, categoryRepository, sharingAppRepository) as T
        }
    }
}

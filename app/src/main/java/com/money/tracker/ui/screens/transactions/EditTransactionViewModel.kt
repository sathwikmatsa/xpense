package com.money.tracker.ui.screens.transactions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.money.tracker.data.entity.Category
import com.money.tracker.data.entity.Transaction
import com.money.tracker.data.entity.TransactionSource
import com.money.tracker.data.entity.TransactionType
import com.money.tracker.data.repository.CategoryRepository
import com.money.tracker.data.repository.TransactionRepository
import com.money.tracker.service.TransactionNotificationHelper
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
    application: Application,
    private val transactionId: Long,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : AndroidViewModel(application) {

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
        date: Long,
        isSplit: Boolean = false,
        splitNumerator: Int = 1,
        splitDenominator: Int = 1,
        customMyShare: Double? = null
    ) {
        viewModelScope.launch {
            val existing = _uiState.value.transaction ?: return@launch

            // Calculate my share for split transactions
            val myShare = when {
                customMyShare != null -> customMyShare
                isSplit && splitDenominator > 0 -> amount * splitNumerator / splitDenominator
                else -> amount
            }

            // Reset splitSynced if split details changed
            val splitChanged = existing.isSplit != isSplit ||
                existing.totalAmount != amount ||
                existing.amount != myShare

            val updated = existing.copy(
                amount = myShare,
                description = description,
                type = type,
                categoryId = categoryId,
                source = source,
                date = date,
                isPending = false,
                isSplit = isSplit,
                splitNumerator = if (customMyShare != null) 0 else splitNumerator,
                splitDenominator = if (customMyShare != null) 0 else splitDenominator,
                totalAmount = if (isSplit) amount else 0.0,
                splitSynced = if (isSplit && splitChanged) false else existing.splitSynced
            )
            transactionRepository.update(updated)

            // Cancel the notification if this was a pending transaction from SMS
            if (existing.isPending) {
                TransactionNotificationHelper.cancelNotification(getApplication(), transactionId)
            }

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
        private val application: Application,
        private val transactionId: Long,
        private val transactionRepository: TransactionRepository,
        private val categoryRepository: CategoryRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EditTransactionViewModel(application, transactionId, transactionRepository, categoryRepository) as T
        }
    }
}

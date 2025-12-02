package com.money.tracker.ui.screens.transactions

import android.app.Application
import android.app.NotificationManager
import androidx.lifecycle.AndroidViewModel
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
import com.money.tracker.data.repository.UpiReminderRepository
import com.money.tracker.util.CategoryRecommender
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AddTransactionViewModel(
    application: Application,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val sharingAppRepository: SharingAppRepository,
    private val upiReminderRepository: UpiReminderRepository
) : AndroidViewModel(application) {

    companion object {
        private const val UPI_REMINDER_NOTIFICATION_ID = 9999
    }

    val categories: Flow<List<Category>> = categoryRepository.allCategories
    val sharingApps: Flow<List<SharingApp>> = sharingAppRepository.enabledApps

    private val categoryRecommender = CategoryRecommender()
    private var historicalTransactions: List<Transaction> = emptyList()

    private val _recommendedCategories = MutableStateFlow<List<Category>>(emptyList())
    val recommendedCategories: StateFlow<List<Category>> = _recommendedCategories.asStateFlow()

    init {
        // Load historical transactions for recommendations
        viewModelScope.launch {
            historicalTransactions = transactionRepository.getRecentCategorizedTransactions(500)
        }
    }

    /**
     * Get recommended categories based on transaction details.
     * Call this when transaction details change (amount, description, source, etc.)
     */
    fun updateCategoryRecommendations(
        allCategories: List<Category>,
        merchant: String?,
        description: String,
        amount: Double,
        source: TransactionSource,
        type: TransactionType
    ) {
        val recommended = categoryRecommender.getRecommendedCategories(
            allCategories = allCategories,
            historicalTransactions = historicalTransactions,
            merchant = merchant,
            description = description,
            amount = amount,
            source = source,
            type = type
        )
        _recommendedCategories.value = recommended
    }

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

    fun deleteUpiReminder(reminderId: Long) {
        viewModelScope.launch {
            upiReminderRepository.deleteById(reminderId)
            // Cancel the UPI reminder notification
            val notificationManager = getApplication<Application>().getSystemService(NotificationManager::class.java)
            notificationManager.cancel(UPI_REMINDER_NOTIFICATION_ID)
        }
    }

    fun createCategory(name: String, emoji: String) {
        viewModelScope.launch {
            categoryRepository.insert(Category(name = name, emoji = emoji))
        }
    }

    class Factory(
        private val application: Application,
        private val transactionRepository: TransactionRepository,
        private val categoryRepository: CategoryRepository,
        private val sharingAppRepository: SharingAppRepository,
        private val upiReminderRepository: UpiReminderRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AddTransactionViewModel(application, transactionRepository, categoryRepository, sharingAppRepository, upiReminderRepository) as T
        }
    }
}

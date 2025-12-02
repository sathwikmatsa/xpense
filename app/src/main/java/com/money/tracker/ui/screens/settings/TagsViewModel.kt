package com.money.tracker.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.money.tracker.data.dao.TransactionDao
import com.money.tracker.data.entity.Tag
import com.money.tracker.data.entity.TagBudget
import com.money.tracker.data.repository.TagRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class TagsViewModel(
    private val tagRepository: TagRepository,
    private val transactionDao: TransactionDao
) : ViewModel() {

    val tags: StateFlow<List<Tag>> = tagRepository.allTags
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val currentYearMonth = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))

    val tagBudgets: StateFlow<List<TagBudget>> = tagRepository.getTagBudgetsForMonth(currentYearMonth)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _deleteState = MutableStateFlow<TagDeleteState>(TagDeleteState.Idle)
    val deleteState: StateFlow<TagDeleteState> = _deleteState.asStateFlow()

    fun createTag(name: String, emoji: String, color: Long) {
        viewModelScope.launch {
            tagRepository.insert(Tag(name = name, emoji = emoji, color = color))
        }
    }

    fun updateTag(tag: Tag, name: String, emoji: String, color: Long) {
        viewModelScope.launch {
            tagRepository.update(tag.copy(name = name, emoji = emoji, color = color))
        }
    }

    fun checkAndDeleteTag(tag: Tag) {
        viewModelScope.launch {
            val transactionCount = transactionDao.getTransactionCountByTag(tag.id)
            if (transactionCount > 0) {
                _deleteState.value = TagDeleteState.ConfirmDelete(tag, transactionCount)
            } else {
                tagRepository.delete(tag)
                _deleteState.value = TagDeleteState.Idle
            }
        }
    }

    fun confirmDelete(tag: Tag) {
        viewModelScope.launch {
            tagRepository.delete(tag)
            _deleteState.value = TagDeleteState.Idle
        }
    }

    fun cancelDelete() {
        _deleteState.value = TagDeleteState.Idle
    }

    fun setTagBudget(tagId: Long, amount: Double) {
        viewModelScope.launch {
            if (amount > 0) {
                tagRepository.setTagBudget(TagBudget(currentYearMonth, tagId, amount))
            } else {
                tagRepository.deleteTagBudget(currentYearMonth, tagId)
            }
        }
    }

    fun getTagBudget(tagId: Long): Double {
        return tagBudgets.value.find { it.tagId == tagId }?.amount ?: 0.0
    }

    class Factory(
        private val tagRepository: TagRepository,
        private val transactionDao: TransactionDao
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TagsViewModel(tagRepository, transactionDao) as T
        }
    }
}

sealed class TagDeleteState {
    data object Idle : TagDeleteState()
    data class ConfirmDelete(val tag: Tag, val transactionCount: Int) : TagDeleteState()
}

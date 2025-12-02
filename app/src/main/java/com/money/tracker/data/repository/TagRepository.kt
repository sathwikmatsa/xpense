package com.money.tracker.data.repository

import com.money.tracker.data.dao.TagBudgetDao
import com.money.tracker.data.dao.TagDao
import com.money.tracker.data.entity.Tag
import com.money.tracker.data.entity.TagBudget
import kotlinx.coroutines.flow.Flow

class TagRepository(
    private val tagDao: TagDao,
    private val tagBudgetDao: TagBudgetDao
) {

    val allTags: Flow<List<Tag>> = tagDao.getAllTags()

    suspend fun insert(tag: Tag): Long {
        return tagDao.insert(tag)
    }

    suspend fun update(tag: Tag) {
        tagDao.update(tag)
    }

    suspend fun delete(tag: Tag) {
        tagDao.delete(tag)
    }

    suspend fun getById(id: Long): Tag? {
        return tagDao.getById(id)
    }

    suspend fun getByName(name: String): Tag? {
        return tagDao.getByName(name)
    }

    // Tag budget methods
    fun getTagBudgetsForMonth(yearMonth: String): Flow<List<TagBudget>> {
        return tagBudgetDao.getTagBudgetsForMonth(yearMonth)
    }

    suspend fun setTagBudget(tagBudget: TagBudget) {
        tagBudgetDao.insert(tagBudget)
    }

    suspend fun deleteTagBudget(yearMonth: String, tagId: Long) {
        tagBudgetDao.delete(yearMonth, tagId)
    }

    suspend fun getTagBudget(yearMonth: String, tagId: Long): TagBudget? {
        return tagBudgetDao.getTagBudget(yearMonth, tagId)
    }

    suspend fun setTagBudgets(yearMonth: String, tagBudgets: List<TagBudget>) {
        tagBudgetDao.deleteAllForMonth(yearMonth)
        tagBudgets.filter { it.amount > 0 }.forEach { tagBudgetDao.insert(it) }
    }

    suspend fun getTagBudgetsForMonthSync(yearMonth: String): List<TagBudget> {
        return tagBudgetDao.getTagBudgetsForMonthSync(yearMonth)
    }
}

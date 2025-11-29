package com.money.tracker.data.repository

import com.money.tracker.data.dao.CategoryDao
import com.money.tracker.data.entity.Category
import kotlinx.coroutines.flow.Flow

class CategoryRepository(private val categoryDao: CategoryDao) {

    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()

    val parentCategories: Flow<List<Category>> = categoryDao.getParentCategories()

    fun getChildCategories(parentId: Long): Flow<List<Category>> {
        return categoryDao.getChildCategories(parentId)
    }

    suspend fun insert(category: Category): Long {
        return categoryDao.insert(category)
    }

    suspend fun update(category: Category) {
        categoryDao.update(category)
    }

    suspend fun delete(category: Category) {
        categoryDao.delete(category)
    }

    suspend fun getById(id: Long): Category? {
        return categoryDao.getById(id)
    }

    suspend fun getByName(name: String): Category? {
        return categoryDao.getByName(name)
    }

    suspend fun getChildCategoryIds(parentId: Long): List<Long> {
        return categoryDao.getChildCategoryIds(parentId)
    }
}

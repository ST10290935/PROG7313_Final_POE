package com.fake.cashflow.data

import androidx.lifecycle.LiveData

class CategoryRepository(private val categoryDao: CategoryDao) {
    fun getAllCategories(): LiveData<List<Category>> =
        categoryDao.getAllCategoriesLiveData()
        
    suspend fun getAllCategoriesList(): List<Category> =
        categoryDao.getAllCategories()
    
    suspend fun getCategoryById(categoryId: Long): Category? =
        categoryDao.getCategoryById(categoryId)

    suspend fun insertCategory(category: Category): Long =
        categoryDao.insertCategory(category)
        
    suspend fun updateCategory(category: Category) {
        categoryDao.updateCategory(category)
    }
    
    suspend fun deleteCategory(category: Category) {
        categoryDao.deleteCategory(category)
    }
    
    suspend fun getCategoryCount(): Int =
        categoryDao.getCategoryCount()
}
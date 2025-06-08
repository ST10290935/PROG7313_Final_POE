package com.fake.cashflow.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.fake.cashflow.data.AppDatabase
import com.fake.cashflow.data.Category
import com.fake.cashflow.data.CategoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CategoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CategoryRepository(AppDatabase.getInstance(application).categoryDao())
    val categories: LiveData<List<Category>> = repository.getAllCategories()

    fun insertCategory(category: Category, onComplete: ((Long) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = repository.insertCategory(category)
            if (onComplete != null) {
                withContext(Dispatchers.Main) {
                    onComplete(id)
                }
            }
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateCategory(category)
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCategory(category)
        }
    }
    
    fun getCategoryCount(onResult: (Int) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val count = repository.getCategoryCount()
            launch(Dispatchers.Main) { onResult(count) }
        }
    }
}
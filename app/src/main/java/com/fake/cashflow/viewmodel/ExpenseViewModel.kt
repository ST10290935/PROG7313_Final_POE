package com.fake.cashflow.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.fake.cashflow.data.AppDatabase
import com.fake.cashflow.data.Category
import com.fake.cashflow.data.CategoryRepository
import com.fake.cashflow.data.Expense
import com.fake.cashflow.data.ExpenseRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {
    private val expenseRepository = ExpenseRepository(AppDatabase.getInstance(application).expenseDao())
    private val categoryRepository = CategoryRepository(AppDatabase.getInstance(application).categoryDao())
    
    private val _currentUserId = MutableLiveData<String>()
    
    val expenses: LiveData<List<Expense>>
    val categories: LiveData<List<Category>> = categoryRepository.getAllCategories()

    init {
        // Get current user ID safely
        val firebaseAuth = FirebaseAuth.getInstance()
        val currentUser = firebaseAuth.currentUser
        val userId = currentUser?.uid ?: "dummy"
        _currentUserId.value = userId
        expenses = expenseRepository.getExpenses(userId)
    }
    
    fun updateUserId(userId: String) {
        if (_currentUserId.value != userId) {
            _currentUserId.value = userId
        }
    }
    
    fun getExpensesByCategory(categoryId: Long): LiveData<List<Expense>> {
        val userId = _currentUserId.value ?: "dummy"
        return expenseRepository.getExpensesByCategory(userId, categoryId)
    }

    fun insertExpense(expense: Expense, onComplete: ((Long) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = expenseRepository.insertExpense(expense)
            if (onComplete != null) {
                withContext(Dispatchers.Main) {
                    onComplete(id)
                }
            }
        }
    }
    
    fun updateExpense(expense: Expense) {
        viewModelScope.launch(Dispatchers.IO) {
            expenseRepository.updateExpense(expense)
        }
    }
    
    fun deleteExpense(expense: Expense) {
        viewModelScope.launch(Dispatchers.IO) {
            expenseRepository.deleteExpense(expense)
        }
    }

    fun getExpenseCount(onResult: (Int) -> Unit) {
        val userId = _currentUserId.value ?: "dummy"
        viewModelScope.launch(Dispatchers.IO) {
            val count = expenseRepository.countExpenses(userId)
            withContext(Dispatchers.Main) { onResult(count) }
        }
    }
    
    fun getTotalExpenses(onResult: (Double) -> Unit) {
        val userId = _currentUserId.value ?: "dummy"
        viewModelScope.launch(Dispatchers.IO) {
            val total = expenseRepository.getTotalExpenses(userId)
            withContext(Dispatchers.Main) { onResult(total) }
        }
    }
    
    fun getTotalExpensesByCategory(categoryId: Long, onResult: (Double) -> Unit) {
        val userId = _currentUserId.value ?: "dummy"
        viewModelScope.launch(Dispatchers.IO) {
            val total = expenseRepository.getTotalExpensesByCategory(userId, categoryId)
            withContext(Dispatchers.Main) { onResult(total) }
        }
    }
}

package com.fake.cashflow.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.fake.cashflow.data.AppDatabase
import com.fake.cashflow.data.Budget
import com.fake.cashflow.data.BudgetRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BudgetViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = BudgetRepository(AppDatabase.getInstance(application).budgetDao())
    private val _currentUserId = MutableLiveData<String>()
    
    val budget: LiveData<Budget?>
    
    init {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "dummy"
        _currentUserId.value = userId
        budget = repository.getBudgetByUser(userId)
    }
    
    fun saveBudget(minBudget: Double, maxBudget: Double, onComplete: (() -> Unit)? = null) {
        val userId = _currentUserId.value ?: return
        
        val budget = Budget(
            userId = userId,
            minBudget = minBudget,
            maxBudget = maxBudget
        )
        
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveBudget(budget)
            if (onComplete != null) {
                withContext(Dispatchers.Main) {
                    onComplete()
                }
            }
        }
    }
}
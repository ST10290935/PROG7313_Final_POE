package com.fake.cashflow.data

import androidx.lifecycle.LiveData

class BudgetRepository(private val budgetDao: BudgetDao) {
    fun getBudgetByUser(userId: String): LiveData<Budget?> =
        budgetDao.getBudgetByUser(userId)
        
    suspend fun getBudgetByUserSync(userId: String): Budget? =
        budgetDao.getBudgetByUserSync(userId)
        
    suspend fun saveBudget(budget: Budget) {
        val existingBudget = budgetDao.getBudgetByUserSync(budget.userId)
        if (existingBudget == null) {
            budgetDao.insertBudget(budget)
        } else {
            budgetDao.updateBudget(budget)
        }
    }
}
package com.fake.cashflow.data

import androidx.lifecycle.LiveData

class ExpenseRepository(private val expenseDao: ExpenseDao) {
    fun getExpenses(userId: String): LiveData<List<Expense>> =
        expenseDao.getExpensesByUser(userId)
    
    fun getExpensesByCategory(userId: String, categoryId: Long): LiveData<List<Expense>> =
        expenseDao.getExpensesByCategory(userId, categoryId)

    suspend fun insertExpense(expense: Expense): Long =
        expenseDao.insertExpense(expense)
        
    suspend fun updateExpense(expense: Expense) {
        expenseDao.updateExpense(expense)
    }
    
    suspend fun deleteExpense(expense: Expense) {
        expenseDao.deleteExpense(expense)
    }

    suspend fun countExpenses(userId: String): Int =
        expenseDao.countExpensesByUser(userId).toInt()
        
    suspend fun getTotalExpenses(userId: String): Double {
        // Safely handle null return from DAO
        return expenseDao.getTotalExpensesByUser(userId) ?: 0.0
    }
        
    suspend fun getTotalExpensesByCategory(userId: String, categoryId: Long): Double {
        // Safely handle null return from DAO
        return expenseDao.getTotalExpensesByCategory(userId, categoryId) ?: 0.0
    }
}
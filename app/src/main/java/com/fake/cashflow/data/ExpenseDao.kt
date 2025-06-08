package com.fake.cashflow.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE userId = :userId ORDER BY date DESC")
    fun getExpensesByUser(userId: String): LiveData<List<Expense>>
    
    @Query("SELECT * FROM expenses WHERE userId = :userId")
    suspend fun getExpensesByUserId(userId: String): List<Expense>
    
    @Query("SELECT * FROM expenses WHERE userId = :userId AND categoryId = :categoryId ORDER BY date DESC")
    fun getExpensesByCategory(userId: String, categoryId: Long): LiveData<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<Expense>): List<Long>
    
    @Update
    suspend fun updateExpense(expense: Expense)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateExpense(expense: Expense): Long
    
    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Query("SELECT COUNT(*) FROM expenses WHERE userId = :userId")
    suspend fun countExpensesByUser(userId: String): Long
    
    @Query("SELECT SUM(amount) FROM expenses WHERE userId = :userId")
    suspend fun getTotalExpensesByUser(userId: String): Double?
    
    @Query("SELECT SUM(amount) FROM expenses WHERE userId = :userId AND categoryId = :categoryId")
    suspend fun getTotalExpensesByCategory(userId: String, categoryId: Long): Double?
}

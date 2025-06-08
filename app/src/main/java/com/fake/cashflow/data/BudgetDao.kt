package com.fake.cashflow.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE userId = :userId")
    fun getBudgetByUser(userId: String): LiveData<Budget?>
    
    @Query("SELECT * FROM budgets WHERE userId = :userId")
    suspend fun getBudgetByUserSync(userId: String): Budget?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: Budget)
    
    @Update
    suspend fun updateBudget(budget: Budget)
}
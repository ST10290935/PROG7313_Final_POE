package com.fake.cashflow.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey
    val userId: String,
    val minBudget: Double = 0.0,
    val maxBudget: Double = 0.0
)
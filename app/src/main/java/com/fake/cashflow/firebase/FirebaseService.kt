package com.fake.cashflow.firebase

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

object FirebaseService {
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    val expensesRef: DatabaseReference = database.getReference("expenses")
    val categoriesRef: DatabaseReference = database.getReference("categories")

    fun addExpense(expense: Map<String, Any>) {
        val key = expensesRef.push().key
        if(key != null) {
            expensesRef.child(key).setValue(expense)
        }
    }

    fun addCategory(category: Map<String, Any>) {
        val key = categoriesRef.push().key
        if(key != null) {
            categoriesRef.child(key).setValue(category)
        }
    }
}


package com.fake.cashflow.firebase

import android.net.Uri
import android.util.Log
import com.fake.cashflow.data.AppDatabase
import com.fake.cashflow.data.Category
import com.fake.cashflow.data.Expense
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class FirebaseDataSync(private val db: AppDatabase) {
    // Track active coroutines for cleanup
    private val activeCoroutines = mutableListOf<kotlinx.coroutines.Job>()
    
    // Track active listeners for proper cleanup
    private val activeListeners = mutableListOf<Pair<ValueEventListener, String>>()
    
    private val database by lazy { 
        try {
            FirebaseDatabase.getInstance()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    private val expensesRef by lazy {
        try {
            database?.getReference("expenses")
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    private val categoriesRef by lazy {
        try {
            database?.getReference("categories")
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private val firebaseStorage by lazy {
        try {
            FirebaseStorage.getInstance()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    // Upload an image to Firebase Storage and return the download URL
    suspend fun uploadImageToFirebase(userId: String, localImageUri: String): String? {
        return try {
            if (firebaseStorage == null) {
                Log.e("FirebaseDataSync", "Firebase Storage not initialized")
                return null
            }
            
            Log.d("FirebaseDataSync", "Starting image upload process for URI: $localImageUri")
            
            // Parse the URI
            val imageUri = Uri.parse(localImageUri)
            
            // Generate a unique filename
            val filename = "expense_image_${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg"
            Log.d("FirebaseDataSync", "Generated filename: $filename")
            
            // Get the storage reference
            val storageRef = firebaseStorage?.reference?.child("expenses_images/$userId/$filename")
            Log.d("FirebaseDataSync", "Storage reference path: expenses_images/$userId/$filename")
            
            // Upload the file
            Log.d("FirebaseDataSync", "Starting file upload")
            val uploadTask = storageRef?.putFile(imageUri)?.await()
            Log.d("FirebaseDataSync", "Upload task completed: ${uploadTask != null}")
            
            // Get the download URL
            Log.d("FirebaseDataSync", "Requesting download URL")
            val downloadUrl = storageRef?.downloadUrl?.await()?.toString()
            
            if (downloadUrl != null) {
                Log.d("FirebaseDataSync", "Image uploaded successfully: $downloadUrl")
            } else {
                Log.e("FirebaseDataSync", "Upload succeeded but download URL is null")
            }
            
            downloadUrl
        } catch (e: Exception) {
            Log.e("FirebaseDataSync", "Failed to upload image: ${e.message}", e)
            null
        }
    }
    
    // Sync local expenses to Firebase
    suspend fun syncExpensesToFirebase(userId: String) {
        try {
            if (expensesRef == null) {
                return // Firebase reference not available, skip sync
            }
            
            withContext(Dispatchers.IO) {
                try {
                    // Use raw SQL query directly since we need immediate results, not LiveData
                    val expenses = mutableListOf<Expense>()
                    
                    // Use raw SQL query with proper error handling
                    try {
                        val expenseDao = db.expenseDao()
                        val userExpenses = expenseDao.getExpensesByUserId(userId)
                        expenses.addAll(userExpenses)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // Failed to load expenses, continue with empty expenses list
                    }
                    
                    // Save them to Firebase
                    val expensesMap = mutableMapOf<String, Any>()
                    for (expense in expenses) {
                        val key = expensesRef?.push()?.key ?: continue
                        
                        // For base64 images, we'll just use the base64 string directly
                        // No need to upload to Firebase storage
                        var finalImagePath: String? = expense.imagePath 
                        
                        if (!expense.imagePath.isNullOrEmpty()) {
                            Log.d("FirebaseDataSync", "Using base64 image data directly for expense ${expense.id}")
                            
                            // For base64 images, just log the length for debugging
                            if (expense.imagePath.length > 100 && !expense.imagePath.startsWith("http")) {
                                Log.d("FirebaseDataSync", "Base64 image length: ${expense.imagePath.length}")
                            }
                        }
                        
                        val expenseMap = mapOf(
                            "id" to expense.id,
                            "date" to expense.date,
                            "amount" to expense.amount,
                            "description" to expense.description,
                            "userId" to expense.userId,
                            "categoryId" to (expense.categoryId),
                            "imagePath" to finalImagePath
                        )
                        expensesMap[key] = expenseMap
                    }
                    
                    if (expensesMap.isNotEmpty()) {
                        expensesRef?.child(userId)?.updateChildren(expensesMap)?.await()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Continue with app even if sync fails
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Continue with app even if the outer sync process fails
        }
    }
    
    // Sync Firebase expenses to local database
    fun syncExpensesFromFirebase(userId: String, onComplete: () -> Unit) {
        try {
            if (expensesRef == null) {
                onComplete() // Firebase reference not available, skip sync and complete
                return
            }
            
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val coroutineJob = CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val expenses = mutableListOf<Expense>()
                            
                            for (expenseSnapshot in snapshot.children) {
                                val expenseMap = expenseSnapshot.value
                                if (expenseMap !is Map<*, *>) continue
                                
                                // Safely extract values with proper type checking
                                val dateValue = expenseMap["date"]
                                val date = when (dateValue) {
                                    is Long -> dateValue
                                    is Int -> dateValue.toLong()
                                    is String -> dateValue.toLongOrNull() ?: 0L
                                    else -> 0L
                                }
                                
                                val amountValue = expenseMap["amount"]
                                val amount = when (amountValue) {
                                    is Double -> amountValue
                                    is Float -> amountValue.toDouble()
                                    is Int -> amountValue.toDouble()
                                    is Long -> amountValue.toDouble()
                                    is String -> amountValue.toDoubleOrNull() ?: 0.0
                                    else -> 0.0
                                }
                                
                                val description = expenseMap["description"] as? String ?: ""
                                
                                val categoryIdValue = expenseMap["categoryId"]
                                val categoryId = when (categoryIdValue) {
                                    is Long -> categoryIdValue
                                    is Int -> categoryIdValue.toLong()
                                    is String -> categoryIdValue.toLongOrNull()
                                    else -> null
                                }
                                
                                // Get image path if available
                                val imagePath = expenseMap["imagePath"] as? String
                                
                                val expense = Expense(
                                    id = 0, // Let Room assign new IDs
                                    date = date,
                                    amount = amount,
                                    description = description,
                                    userId = userId,
                                    categoryId = categoryId,
                                    imagePath = imagePath
                                )
                                
                                expenses.add(expense)
                            }
                            
                            try {
                                // Use database transaction for better reliability
                                for (expense in expenses) {
                                    db.expenseDao().insertOrUpdateExpense(expense)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        } catch (e: Exception) {
                            // Log error but still complete the operation
                            e.printStackTrace()
                        } finally {
                            withContext(Dispatchers.Main) {
                                onComplete()
                            }
                            synchronized(activeCoroutines) {
                                activeCoroutines.remove(this@launch as kotlinx.coroutines.Job)
                            }
                        }
                    }
                    
                    synchronized(activeCoroutines) {
                        activeCoroutines.add(coroutineJob)
                    }
                }
                
                override fun onCancelled(error: DatabaseError) {
                    // Log the error
                    error.toException().printStackTrace()
                    onComplete()
                }
            }
            
            // Since we're using addListenerForSingleValueEvent, no need to track for cleanup
            expensesRef?.child(userId)?.addListenerForSingleValueEvent(listener) ?: onComplete()
            
            // Don't track single value event listeners as they auto-remove themselves
        } catch (e: Exception) {
            e.printStackTrace()
            onComplete() // Call completion handler even if we fail to set up sync
        }
    }
    
    // Sync local categories to Firebase
    suspend fun syncCategoriesToFirebase() {
        try {
            if (categoriesRef == null) {
                return // Firebase reference not available, skip sync
            }
            
            withContext(Dispatchers.IO) {
                try {
                    // Use raw SQL query directly since we need immediate results, not LiveData
                    val categories = mutableListOf<Category>()
                    
                    try {
                        // Get categories from DAO instead of raw query
                        val categoryDao = db.categoryDao()
                        val allCategories = categoryDao.getAllCategories()
                        categories.addAll(allCategories)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // Failed to load categories, continue with empty categories list
                    }
                    
                    // Save them to Firebase
                    val categoriesMap = mutableMapOf<String, Any>()
                    for (category in categories) {
                        val key = categoriesRef?.push()?.key ?: continue
                        val categoryMap = mapOf(
                            "id" to category.id,
                            "name" to category.name
                        )
                        categoriesMap[key] = categoryMap
                    }
                    
                    if (categoriesMap.isNotEmpty()) {
                        categoriesRef?.updateChildren(categoriesMap)?.await()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Continue with app even if sync fails
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Continue with app even if the outer sync process fails
        }
    }
    
    // Sync Firebase categories to local database
    fun syncCategoriesFromFirebase(onComplete: () -> Unit) {
        try {
            if (categoriesRef == null) {
                onComplete() // Firebase reference not available, skip sync and complete
                return
            }
            
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val coroutineJob = CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val categories = mutableListOf<Category>()
                            
                            for (categorySnapshot in snapshot.children) {
                                val categoryMap = categorySnapshot.value
                                if (categoryMap !is Map<*, *>) continue
                                
                                val name = categoryMap["name"] as? String ?: ""
                                
                                val category = Category(
                                    id = 0, // Let Room assign new IDs
                                    name = name
                                )
                                
                                categories.add(category)
                            }
                            
                            try {
                                // Use database transaction for better reliability
                                for (category in categories) {
                                    db.categoryDao().insertOrUpdateCategory(category)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        } catch (e: Exception) {
                            // Log error but still complete the operation
                            e.printStackTrace()
                        } finally {
                            withContext(Dispatchers.Main) {
                                onComplete()
                            }
                            synchronized(activeCoroutines) {
                                activeCoroutines.remove(this@launch as kotlinx.coroutines.Job)
                            }
                        }
                    }
                    
                    synchronized(activeCoroutines) {
                        activeCoroutines.add(coroutineJob)
                    }
                }
                
                override fun onCancelled(error: DatabaseError) {
                    // Log the error
                    error.toException().printStackTrace()
                    onComplete()
                }
            }
            
            // Since we're using addListenerForSingleValueEvent, no need to track for cleanup
            categoriesRef?.addListenerForSingleValueEvent(listener) ?: onComplete()
            
            // Don't track single value event listeners as they auto-remove themselves
        } catch (e: Exception) {
            e.printStackTrace()
            onComplete() // Call completion handler even if we fail to set up sync
        }
    }
    
    // Clean up resources when no longer needed
    fun cleanup() {
        // Cancel all active coroutines
        synchronized(activeCoroutines) {
            for (job in activeCoroutines) {
                if (job.isActive) {
                    job.cancel()
                }
            }
            activeCoroutines.clear()
        }
        
        // We no longer need to remove single value event listeners as they auto-remove themselves
        // Only remove persistent listeners if any are added in the future
        synchronized(activeListeners) {
            activeListeners.clear()
        }
    }
    
    companion object {
        @Volatile
        private var instance: FirebaseDataSync? = null
        
        fun getInstance(db: AppDatabase): FirebaseDataSync =
            instance ?: synchronized(this) {
                instance ?: FirebaseDataSync(db).also { instance = it }
            }
    }
}
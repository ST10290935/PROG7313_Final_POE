//Code Attribution:
//For the code below these are the sources I have used to improve my knowledge and implement features:
//Stevdza-San, 2020. ROOM Database - #1 Create Database Schema | Android Studio Tutorial.[video online] Available at: <https://youtu.be/lwAvI3WDXBY?si=mq1S9X37wiOx5aX5> [Accessed 1 April 2025].
//AndroidWithShiv, 2023. Room Database | CRUD Operation in ROOM DB | Android Studio | JAVA | #android #java #database.[online video] Available at: <https://youtu.be/r-ua6f6LmJA?si=NtFoH1Z_Mng2GYrT> [Accessed 1 April 2025].
//Coding Meet, 2023. How to Store and Retrieve Images in Room Database | Android Studio Kotlin Tutorial.[online video] Available at: <https://youtu.be/0NVm3uVRNzg?si=n_sPPOCgiC0pQ8do> [Accessed 4 April 2025].
// how to videos, 2022. Insert image into database and retrieve in navigation drawer (android studio)(android tutorials).[online video] Available at: <https://youtu.be/8_LuejJEF7o?si=Hhr8a8QOxmGuFaAV> [Accessed 4 April 2025].
// Android Knowledge, 2023. RecyclerView in Android Studio using Kotlin | Android Knowledge.[online video] Available at: <https://youtu.be/UDfyZLWyyVM?si=XkKwy4-9apD5AZcW> [Accessed 7 April 2025].

package com.fake.cashflow.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fake.cashflow.R
import com.fake.cashflow.data.AppDatabase
import com.fake.cashflow.data.Category
import com.fake.cashflow.firebase.FirebaseDataSync
import com.fake.cashflow.ui.CategoryAdapter
import com.fake.cashflow.viewmodel.CategoryViewModel
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth

class CategoryActivity : AppCompatActivity() {

    private lateinit var etCategoryName: EditText
    private lateinit var btnAddCategory: Button
    private lateinit var rvCategories: RecyclerView
    private lateinit var categoryViewModel: CategoryViewModel
    private lateinit var categoryAdapter: CategoryAdapter

    private lateinit var btnBack: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category)
        
        // Check if user is authenticated
        if (FirebaseAuth.getInstance().currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        etCategoryName = findViewById(R.id.etCategoryName)
        btnAddCategory = findViewById(R.id.btnAddCategory)
        rvCategories = findViewById(R.id.rvCategories)

        btnBack = findViewById(R.id.btnBack)

        // Initialize the ViewModel
        categoryViewModel = ViewModelProvider(this).get(CategoryViewModel::class.java)
        
        // Set up the RecyclerView and adapter
        rvCategories.layoutManager = LinearLayoutManager(this)
        categoryAdapter = CategoryAdapter(
            onDeleteClick = { category ->
                categoryViewModel.deleteCategory(category)
            }
        )
        rvCategories.adapter = categoryAdapter
        
        // Observe the categories LiveData
        categoryViewModel.categories.observe(this) { categories ->
            categoryAdapter.submitList(categories)
        }

        btnAddCategory.setOnClickListener {
            val categoryName = etCategoryName.text.toString().trim()
            if (categoryName.isEmpty()) {
                Toast.makeText(this, "Enter a category name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val category = Category(name = categoryName)
            categoryViewModel.insertCategory(category) { id ->
                // Also sync to Firebase
                val db = AppDatabase.getInstance(this@CategoryActivity)
                val dataSync = FirebaseDataSync.getInstance(db)
                
                // Sync the category to Firebase
                lifecycleScope.launch {
                    try {
                        dataSync.syncCategoriesToFirebase()
                        Toast.makeText(this@CategoryActivity, "Category added and synced", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@CategoryActivity, "Category added locally", Toast.LENGTH_SHORT).show()
                    } finally {
                        etCategoryName.text.clear()
                    }
                }
            }
        }

        btnBack.setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Check if user is still authenticated
        if (FirebaseAuth.getInstance().currentUser == null) {
            Toast.makeText(this, "Session expired, please login again", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Clean up firebase sync operations
        val db = AppDatabase.getInstance(this)
        val dataSync = FirebaseDataSync.getInstance(db)
        try {
            dataSync.cleanup()
        } catch (e: Exception) {
            // Prevent crash if cleanup method is not properly implemented
        }
    }
}

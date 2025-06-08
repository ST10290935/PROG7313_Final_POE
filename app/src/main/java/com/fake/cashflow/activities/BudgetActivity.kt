//Code Attribution:
//For the code below these are the sources I have used to improve my knowledge and implement features:
//Stevdza-San, 2020. ROOM Database - #1 Create Database Schema | Android Studio Tutorial.[video online] Available at: <https://youtu.be/lwAvI3WDXBY?si=mq1S9X37wiOx5aX5> [Accessed 1 April 2025].
//AndroidWithShiv, 2023. Room Database | CRUD Operation in ROOM DB | Android Studio | JAVA | #android #java #database.[online video] Available at: <https://youtu.be/r-ua6f6LmJA?si=NtFoH1Z_Mng2GYrT> [Accessed 1 April 2025].
//Coding Meet, 2023. How to Store and Retrieve Images in Room Database | Android Studio Kotlin Tutorial.[online video] Available at: <https://youtu.be/0NVm3uVRNzg?si=n_sPPOCgiC0pQ8do> [Accessed 4 April 2025].
// how to videos, 2022. Insert image into database and retrieve in navigation drawer (android studio)(android tutorials).[online video] Available at: <https://youtu.be/8_LuejJEF7o?si=Hhr8a8QOxmGuFaAV> [Accessed 4 April 2025].
// Android Knowledge, 2023. RecyclerView in Android Studio using Kotlin | Android Knowledge.[online video] Available at: <https://youtu.be/UDfyZLWyyVM?si=XkKwy4-9apD5AZcW> [Accessed 7 April 2025].

package com.fake.cashflow.activities

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.content.Intent
import android.graphics.Color
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.fake.cashflow.R
import com.fake.cashflow.viewmodel.BudgetViewModel
import com.fake.cashflow.viewmodel.ExpenseViewModel
import java.text.NumberFormat
import java.util.Locale

class BudgetActivity : AppCompatActivity() {

    private lateinit var etMinBudget: EditText
    private lateinit var etMaxBudget: EditText
    private lateinit var btnSaveBudget: Button
    private lateinit var tvCurrentSpend: TextView
    private lateinit var tvBudgetStatus: TextView
    private lateinit var budgetViewModel: BudgetViewModel
    private lateinit var expenseViewModel: ExpenseViewModel
    //private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))

    private lateinit var btnBack: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_budget)
        
        // Check if user is authenticated
        if (FirebaseAuth.getInstance().currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        etMinBudget = findViewById(R.id.etMinBudget)
        etMaxBudget = findViewById(R.id.etMaxBudget)
        btnSaveBudget = findViewById(R.id.btnSaveBudget)
        tvCurrentSpend = findViewById(R.id.tvCurrentSpend)
        tvBudgetStatus = findViewById(R.id.tvBudgetStatus)

        btnBack = findViewById(R.id.btnBack)

        budgetViewModel = ViewModelProvider(this).get(BudgetViewModel::class.java)
        expenseViewModel = ViewModelProvider(this).get(ExpenseViewModel::class.java)

        // Load existing budget if it exists
        budgetViewModel.budget.observe(this) { budget ->
            if (budget != null) {
                etMinBudget.setText(budget.minBudget.toString())
                etMaxBudget.setText(budget.maxBudget.toString())
                updateBudgetStatus(budget.minBudget, budget.maxBudget)
            }
        }

        // Get current total expenses
        expenseViewModel.getTotalExpenses { totalExpenses ->
            tvCurrentSpend.text = "Current spend: ${currencyFormat.format(totalExpenses)}"
        }

        btnSaveBudget.setOnClickListener {
            val minBudget = etMinBudget.text.toString().toDoubleOrNull()
            val maxBudget = etMaxBudget.text.toString().toDoubleOrNull()

            if (minBudget == null || maxBudget == null) {
                Toast.makeText(this, "Please enter valid budget values", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (minBudget > maxBudget) {
                Toast.makeText(this, "Minimum budget cannot be higher than maximum budget", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            budgetViewModel.saveBudget(minBudget, maxBudget) {
                Toast.makeText(
                    this,
                    "Budget goals set: Min ${currencyFormat.format(minBudget)}, Max ${currencyFormat.format(maxBudget)}",
                    Toast.LENGTH_SHORT
                ).show()
                updateBudgetStatus(minBudget, maxBudget)
            }
        }

        btnBack.setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
        }
    }

    private fun updateBudgetStatus(minBudget: Double, maxBudget: Double) {
        expenseViewModel.getTotalExpenses { totalSpend ->
            val (status, color) = when {
                totalSpend < minBudget -> {
                    "Under budget (Goal: ${currencyFormat.format(minBudget)})" to Color.parseColor("#1976D2") // Blue
                }
                totalSpend > maxBudget -> {
                    "Over budget! (Limit: ${currencyFormat.format(maxBudget)})" to Color.parseColor("#D32F2F") // Red
                }
                else -> {
                    "On track (Between ${currencyFormat.format(minBudget)} and ${currencyFormat.format(maxBudget)})" to Color.parseColor("#388E3C") // Green
                }
            }
            tvBudgetStatus.text = status
            tvBudgetStatus.setTextColor(color)
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
        
        // Refresh expense data
        expenseViewModel.getTotalExpenses { totalExpenses ->
            tvCurrentSpend.text = "Current spend: ${currencyFormat.format(totalExpenses)}"
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // No need to clean up Firebase sync as there is no sync for budgets,
        // but we could add it in the future if needed
    }
}

//Code Attribution:
//For the code below these are the sources I have used to improve my knowledge and implement features:
//Stevdza-San, 2020. ROOM Database - #1 Create Database Schema | Android Studio Tutorial.[video online] Available at: <https://youtu.be/lwAvI3WDXBY?si=mq1S9X37wiOx5aX5> [Accessed 1 April 2025].
//AndroidWithShiv, 2023. Room Database | CRUD Operation in ROOM DB | Android Studio | JAVA | #android #java #database.[online video] Available at: <https://youtu.be/r-ua6f6LmJA?si=NtFoH1Z_Mng2GYrT> [Accessed 1 April 2025].
//Coding Meet, 2023. How to Store and Retrieve Images in Room Database | Android Studio Kotlin Tutorial.[online video] Available at: <https://youtu.be/0NVm3uVRNzg?si=n_sPPOCgiC0pQ8do> [Accessed 4 April 2025].
// how to videos, 2022. Insert image into database and retrieve in navigation drawer (android studio)(android tutorials).[online video] Available at: <https://youtu.be/8_LuejJEF7o?si=Hhr8a8QOxmGuFaAV> [Accessed 4 April 2025].
// Android Knowledge, 2023. RecyclerView in Android Studio using Kotlin | Android Knowledge.[online video] Available at: <https://youtu.be/UDfyZLWyyVM?si=XkKwy4-9apD5AZcW> [Accessed 7 April 2025].

package com.fake.cashflow.activities

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import android.widget.GridLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fake.cashflow.R
import com.fake.cashflow.data.Category
import com.fake.cashflow.data.Expense
import com.fake.cashflow.ui.ExpenseRecyclerAdapter
import com.fake.cashflow.viewmodel.ExpenseViewModel
import java.text.NumberFormat
import java.util.Locale
import com.google.firebase.auth.FirebaseAuth
import com.fake.cashflow.data.AppDatabase
import com.fake.cashflow.firebase.FirebaseDataSync
import androidx.lifecycle.lifecycleScope
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Currency
import java.util.Date

class ExpenseListActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var spinnerCategories: Spinner
    private lateinit var tvTotalAmount: TextView
    private lateinit var tvNoExpenses: TextView
    private lateinit var adapter: ExpenseRecyclerAdapter
    private lateinit var expenseViewModel: ExpenseViewModel
    private var categories = listOf<Category>()
    private var selectedCategoryId: Long? = null
//    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

    //private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "ZA")).apply {
        currency = Currency.getInstance("ZAR")
    }


    private var selectedDateInMillis: Long? = null
    private var startDateInMillis: Long? = null
    private var endDateInMillis: Long? = null

    private var selectedStartDateInMillis: Long? = null
    private var selectedEndDateInMillis: Long? = null


    private lateinit var btnBack: Button
    private lateinit var btnCalculator: Button
    private lateinit var btnCurrencyConverter: Button




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expense_list)

        // Check if user is authenticated
        if (FirebaseAuth.getInstance().currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        recyclerView = findViewById(R.id.recyclerViewExpenses)
        spinnerCategories = findViewById(R.id.spinnerCategoryFilter)
        tvTotalAmount = findViewById(R.id.tvTotalAmount)
        tvNoExpenses = findViewById(R.id.tvNoExpenses)

        btnBack = findViewById(R.id.btnBack)
         btnCalculator = findViewById<Button>(R.id.btnCalculator)
         btnCurrencyConverter = findViewById<Button>(R.id.btnCurrencyConverter)

        btnCalculator.setOnClickListener { showCalculatorDialog() }
        btnCurrencyConverter.setOnClickListener { showCurrencyConverterDialog() }


        // Set up the RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ExpenseRecyclerAdapter(
            expenses = emptyList(),
            categoryNameResolver = { categoryId -> getCategoryName(categoryId) },
            onDeleteClick = { expense -> deleteExpense(expense) }
        )
        recyclerView.adapter = adapter

        // Set up the View Model
        expenseViewModel = ViewModelProvider(this).get(ExpenseViewModel::class.java)

        // Set up the category filter spinner
        setupCategorySpinner()
        
        // First sync with Firebase to ensure we have the latest data including image paths
        FirebaseAuth.getInstance().currentUser?.uid?.let { userId ->
            val db = AppDatabase.getInstance(this)
            val dataSync = FirebaseDataSync.getInstance(db)
            
            // Show loading toast
            Toast.makeText(this, "Loading expense data...", Toast.LENGTH_SHORT).show()
            
            // Sync from Firebase first
            dataSync.syncExpensesFromFirebase(userId) {
                // After sync completes, observe expenses
                runOnUiThread {
                    observeExpenses()
                }
            }
        } ?: run {
            // If not logged in, just observe expenses
            observeExpenses()
        }

        findViewById<Button>(R.id.btnSelectDateRange).setOnClickListener {
            showDateRangePickerDialog()
        }




        btnBack.setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
        }

    }

//    private fun showDatePickerDialog() {
//        val calendar = Calendar.getInstance()
//        val listener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
//            calendar.set(year, month, dayOfMonth, 0, 0, 0)
//            calendar.set(Calendar.MILLISECOND, 0)
//            selectedDateInMillis = calendar.timeInMillis
//
//            findViewById<TextView>(R.id.tvSelectedDate).text =
//                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(selectedDateInMillis!!))
//
//            observeExpenses()
//        }
//
//        DatePickerDialog(
//            this,
//            listener,
//            calendar.get(Calendar.YEAR),
//            calendar.get(Calendar.MONTH),
//            calendar.get(Calendar.DAY_OF_MONTH)
//        ).show()
//    }

    private fun showDateRangePickerDialog() {
        val constraintsBuilder = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointBackward.now()) // To restrict future dates if needed

        val dateRangePicker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Select date range")
            .setCalendarConstraints(constraintsBuilder.build())
            .build()

        dateRangePicker.show(supportFragmentManager, "date_range_picker")

        dateRangePicker.addOnPositiveButtonClickListener { selection ->
            val startDate = selection.first
            val endDate = selection.second

            // Save the selected dates in millis
            selectedStartDateInMillis = startDate
            selectedEndDateInMillis = endDate

            // Format the date range for display
            val formatted = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            findViewById<TextView>(R.id.tvSelectedDateRange).text =
                "${formatted.format(Date(startDate))} - ${formatted.format(Date(endDate))}"

            // Observe expenses after the date selection
            observeExpenses()
        }
    }


    private fun showCustomDateRangeDialog() {
        val inflater = LayoutInflater.from(this)
        val dialogView = inflater.inflate(R.layout.dialog_date_range_picker, null)

        val dpStart = dialogView.findViewById<DatePicker>(R.id.datePickerStart)
        val dpEnd = dialogView.findViewById<DatePicker>(R.id.datePickerEnd)

        AlertDialog.Builder(this)
            .setTitle("Select Date Range")
            .setView(dialogView)
            .setPositiveButton("Apply") { _, _ ->
                val calStart = Calendar.getInstance().apply {
                    set(dpStart.year, dpStart.month, dpStart.dayOfMonth, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val calEnd = Calendar.getInstance().apply {
                    set(dpEnd.year, dpEnd.month, dpEnd.dayOfMonth, 23, 59, 59)
                    set(Calendar.MILLISECOND, 999)
                }

                selectedStartDateInMillis = calStart.timeInMillis
                selectedEndDateInMillis = calEnd.timeInMillis

                val formattedStart = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(calStart.time)
                val formattedEnd = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(calEnd.time)
                findViewById<TextView>(R.id.tvSelectedDate).text = "$formattedStart - $formattedEnd"

                observeExpenses()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }




    private fun setupCategorySpinner() {
        // Observe categories from ViewModel
        expenseViewModel.categories.observe(this) { categoryList ->
            categories = categoryList

            // Create a list of category names for the spinner
            val categoryNames = categoryList.map { it.name }.toMutableList()

            // Add "All Categories" option at the beginning
            categoryNames.add(0, "All Categories")

            // Create and set up adapter
            val adapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                categoryNames
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerCategories.adapter = adapter

            // Set spinner selection listener
            spinnerCategories.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    // First item (position 0) is "All Categories"
                    selectedCategoryId = if (position == 0) {
                        null
                    } else if (position <= categories.size) { // Ensure position is valid
                        // Adjust index for the actual categories list
                        categories[position - 1].id
                    } else {
                        // Handle case when position is out of bounds
                        null
                    }

                    // Re-observe expenses with the new filter
                    observeExpenses()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    selectedCategoryId = null
                    observeExpenses()
                }
            }
        }
    }

    private fun observeExpenses() {
        expenseViewModel.expenses.removeObservers(this)

        expenseViewModel.expenses.observe(this) { expenses ->

            // Filter by category if selected
            var filteredExpenses = if (selectedCategoryId != null) {
                expenses.filter { it.categoryId == selectedCategoryId }
            } else {
                expenses
            }

            // Filter by date range if selected
            filteredExpenses = filteredExpenses.filter { expense ->
                (selectedStartDateInMillis == null || expense.date >= selectedStartDateInMillis!!) &&
                        (selectedEndDateInMillis == null || expense.date <= selectedEndDateInMillis!!)
            }

            updateExpensesList(filteredExpenses)

            // Update total
            val total = filteredExpenses.sumOf { it.amount }
            tvTotalAmount.text = "Total: ${currencyFormat.format(total)}"

            // Save total expenses to SharedPreferences
            val prefs = getSharedPreferences("cashflow_prefs", MODE_PRIVATE)
            prefs.edit().putFloat("total_expenses", total.toFloat()).apply()
        }
    }




    private fun updateExpensesList(expenses: List<Expense>) {
        adapter.updateExpenses(expenses)

        // Show/hide the "No expenses" message
        if (expenses.isEmpty()) {
            tvNoExpenses.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            tvNoExpenses.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun getCategoryName(categoryId: Long?): String {
        if (categoryId == null) return "Uncategorized"

        return categories.find { it.id == categoryId }?.name ?: "Unknown"
    }

    private fun deleteExpense(expense: Expense) {
        expenseViewModel.deleteExpense(expense)

        // Also sync changes to Firebase after deletion
        FirebaseAuth.getInstance().currentUser?.uid?.let { userId ->
            val db = AppDatabase.getInstance(this)
            val dataSync = FirebaseDataSync.getInstance(db)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    dataSync.syncExpensesToFirebase(userId)
                }
            }, 500) // Small delay to let delete operation complete
        }
    }
    private fun showCalculatorDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_calculator, null)
        val display = dialogView.findViewById<TextView>(R.id.calculatorDisplay)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Calculator")
            .setNegativeButton("Close", null)
            .create()

        val buttons = (dialogView as ViewGroup).findViewById<GridLayout>(R.id.buttonGrid)
        var expression = ""

        fun updateDisplay() {
            display.text = expression
        }

        fun evaluateAndShow() {
            try {
                val result = evaluateExpression(expression)
                expression = result.toString()
                updateDisplay()
            } catch (e: Exception) {
                expression = ""
                display.text = "Invalid input"
            }
        }

        for (i in 0 until buttons.childCount) {
            val btn = buttons.getChildAt(i) as Button
            btn.setOnClickListener {
                val value = btn.text.toString()
                when (value) {
                    "=" -> evaluateAndShow()
                    "C" -> {
                        expression = ""
                        updateDisplay()
                    }
                    else -> {
                        expression += value
                        updateDisplay()
                    }
                }
            }
        }

        dialog.show()
    }

    private fun evaluateExpression(expr: String): Double {
        val tokens = Regex("(?<=[-+*/])|(?=[-+*/])").split(expr.replace(" ", ""))
        val numbers = mutableListOf<Double>()
        val operators = mutableListOf<Char>()

        var i = 0
        while (i < tokens.size) {
            val token = tokens[i]
            when {
                token.isEmpty() -> {}
                token in "+-*/" -> operators.add(token[0])
                else -> numbers.add(token.toDouble())
            }
            i++
        }

        // Do */ first
        var index = 0
        while (index < operators.size) {
            val op = operators[index]
            if (op == '*' || op == '/') {
                val a = numbers[index]
                val b = numbers[index + 1]
                val result = if (op == '*') a * b else a / b
                numbers[index] = result
                numbers.removeAt(index + 1)
                operators.removeAt(index)
            } else {
                index++
            }
        }

        // Then do +-
        index = 0
        while (index < operators.size) {
            val op = operators[index]
            val a = numbers[index]
            val b = numbers[index + 1]
            val result = if (op == '+') a + b else a - b
            numbers[index] = result
            numbers.removeAt(index + 1)
            operators.removeAt(index)
        }

        return numbers.first()
    }
    private fun showCurrencyConverterDialog() {
        val layout = layoutInflater.inflate(R.layout.dialog_currency_converter, null)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Currency Converter")
            .setView(layout)
            .setPositiveButton("Convert", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val btnConvert = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val inputAmount = layout.findViewById<EditText>(R.id.etAmount)
            val spinnerCurrency = layout.findViewById<Spinner>(R.id.spinnerCurrency)
            val spinnerDirection = layout.findViewById<Spinner>(R.id.spinnerDirection)

            val currencies = arrayOf("USD", "EUR", "GBP", "ZAR")
            val directions = arrayOf("Foreign → ZAR", "ZAR → Foreign")

            spinnerCurrency.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, currencies)
            spinnerDirection.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, directions)

            btnConvert.setOnClickListener {
                val amount = inputAmount.text.toString().toDoubleOrNull()
                val currency = spinnerCurrency.selectedItem.toString()
                val direction = spinnerDirection.selectedItem.toString()

                if (amount != null) {
                    val rate = getConversionRateToZAR(currency)

                    val result = when (direction) {
                        "Foreign → ZAR" -> amount * rate
                        "ZAR → Foreign" -> if (rate != 0.0) amount / rate else 0.0
                        else -> 0.0
                    }

                    // Format the result text based on conversion direction
                    val resultText = when (direction) {
                        "Foreign → ZAR" -> "$amount $currency = R ${"%.2f".format(result)}"
                        "ZAR → Foreign" -> "R $amount = ${"%.2f".format(result)} $currency"
                        else -> ""
                    }

                    // Show the result in a custom dialog
                    showResultDialog(
                        "Converted Amount",
                        when (direction) {
                            "Foreign → ZAR" -> "$amount $currency = R ${"%.2f".format(result)}"
                            "ZAR → Foreign" -> "R $amount = ${"%.2f".format(result)} $currency"
                            else -> "Invalid conversion direction"
                        }
                    )

                    dialog.dismiss()  // Dismiss the dialog after conversion
                } else {
                    Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show()
                }

            }
        }

        dialog.show()
    }
    private fun getConversionRateToZAR(currencyCode: String): Double {
        return when (currencyCode) {
            "USD" -> 18.5
            "EUR" -> 20.1
            "GBP" -> 23.3
            "ZAR" -> 1.0
            else -> 1.0
        }
    }
    private fun showResultDialog(title: String, message: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_result, null)
        val titleView = dialogView.findViewById<TextView>(R.id.tvResultTitle)
        val bodyView = dialogView.findViewById<TextView>(R.id.tvResultBody)

        titleView.text = title
        bodyView.text = message

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("OK", null)
            .create()
            .show()
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

        // Refresh the expense data in the view model
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        if (userId.isNotEmpty()) {
            expenseViewModel.updateUserId(userId)
        }

        // First sync data from Firebase to ensure latest data
        FirebaseAuth.getInstance().currentUser?.uid?.let { userId ->
            val db = AppDatabase.getInstance(this)
            val dataSync = FirebaseDataSync.getInstance(db)
            
            // Show loading toast
            Toast.makeText(this, "Refreshing expense data...", Toast.LENGTH_SHORT).show()
            
            // Sync from Firebase first
            dataSync.syncExpensesFromFirebase(userId) {
                // After sync completes, re-observe expenses with the current filter
                runOnUiThread {
                    observeExpenses()
                    Toast.makeText(this, "Data refreshed", Toast.LENGTH_SHORT).show()
                }
            }
        } ?: run {
            // If not logged in, just re-observe expenses
            observeExpenses()
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

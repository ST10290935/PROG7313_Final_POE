//Code Attribution:
//For the code below these are the sources I have used to improve my knowledge and implement features:
//Stevdza-San, 2020. ROOM Database - #1 Create Database Schema | Android Studio Tutorial.[video online] Available at: <https://youtu.be/lwAvI3WDXBY?si=mq1S9X37wiOx5aX5> [Accessed 1 April 2025].
//AndroidWithShiv, 2023. Room Database | CRUD Operation in ROOM DB | Android Studio | JAVA | #android #java #database.[online video] Available at: <https://youtu.be/r-ua6f6LmJA?si=NtFoH1Z_Mng2GYrT> [Accessed 1 April 2025].
//Coding Meet, 2023. How to Store and Retrieve Images in Room Database | Android Studio Kotlin Tutorial.[online video] Available at: <https://youtu.be/0NVm3uVRNzg?si=n_sPPOCgiC0pQ8do> [Accessed 4 April 2025].
// how to videos, 2022. Insert image into database and retrieve in navigation drawer (android studio)(android tutorials).[online video] Available at: <https://youtu.be/8_LuejJEF7o?si=Hhr8a8QOxmGuFaAV> [Accessed 4 April 2025].
// Android Knowledge, 2023. RecyclerView in Android Studio using Kotlin | Android Knowledge.[online video] Available at: <https://youtu.be/UDfyZLWyyVM?si=XkKwy4-9apD5AZcW> [Accessed 7 April 2025].

package com.fake.cashflow.activities

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import com.fake.cashflow.R
import com.fake.cashflow.viewmodel.ExpenseViewModel
import com.google.firebase.auth.FirebaseAuth
import com.github.mikephil.charting.charts.LineChart
import com.google.android.material.navigation.NavigationView
import androidx.core.view.GravityCompat
import androidx.appcompat.app.ActionBarDrawerToggle
import java.text.SimpleDateFormat
import java.util.*
import android.widget.Toast
import com.fake.cashflow.data.AppDatabase
import com.fake.cashflow.data.Expense
import com.fake.cashflow.firebase.FirebaseDataSync
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import java.text.NumberFormat

class DashboardActivity : AppCompatActivity() {

    private lateinit var chart: LineChart
    private lateinit var cardAddExpense: CardView
    private lateinit var cardExpenseList: CardView
    private lateinit var cardManageCategories: CardView
    private lateinit var cardSetBudget: CardView
    private lateinit var cardLogout: CardView
    private lateinit var tvExpenseTotal: TextView
    private lateinit var expenseViewModel: ExpenseViewModel
    private lateinit var btnDateRange: Button
    
    // Date range variables
    private var selectedDateRangeType = DateRangeType.ONE_YEAR
    private var customStartDate: Calendar = Calendar.getInstance().apply { 
        add(Calendar.YEAR, -1) 
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    private var customEndDate: Calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }
    
    // Date range enum
    private enum class DateRangeType {
        CUSTOM, ONE_MONTH, THREE_MONTHS, SIX_MONTHS, ONE_YEAR, ALL_TIME
    }







    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Use the DrawerLayout-based layout
        setContentView(R.layout.activity_dashboard)

        // Check if user is authenticated, if not redirect to login
        if (FirebaseAuth.getInstance().currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }




        // Set up the toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Initialize the DrawerLayout and NavigationView
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        val navView = findViewById<NavigationView>(R.id.nav_view)

        // Create and sync the ActionBarDrawerToggle
        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Update navigation header with user info
        val headerView = navView.getHeaderView(0)
        val tvWelcome = headerView.findViewById<android.widget.TextView>(R.id.tvWelcome)
        val tvUserEmail = headerView.findViewById<android.widget.TextView>(R.id.tvUserEmail)
        val switchCategoryTrend = findViewById<Switch>(R.id.switchCategoryTrend)

        switchCategoryTrend.setOnCheckedChangeListener { _, isChecked ->
            updateChart(showCategoryTrends = isChecked)
        }

        //
        tvExpenseTotal = findViewById(R.id.tvExpenseTotal)

        // Initialize ViewModel for accessing expense data

        expenseViewModel = ViewModelProvider(this)[ExpenseViewModel::class.java]


        // Get user details
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            // Display user name
            val displayName = currentUser.displayName
            val email = currentUser.email

            if (!displayName.isNullOrEmpty()) {
                tvWelcome.text = "Welcome, $displayName"
            } else {
                tvWelcome.text = "Welcome, User"
            }

            // Always show email as secondary information if available
            if (!email.isNullOrEmpty()) {
                tvUserEmail.text = email
                tvUserEmail.visibility = android.view.View.VISIBLE
            } else {
                tvUserEmail.visibility = android.view.View.GONE
            }
        } else {
            tvWelcome.text = "Welcome, Guest"
            tvUserEmail.visibility = android.view.View.GONE
        }

        // Set a listener for navigation item selection
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_logout -> {
                    FirebaseAuth.getInstance().signOut()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
                // Handle other navigation items as needed.
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        // Initialize other views from your main content
        chart = findViewById(R.id.chartSpendHabits)
        cardAddExpense = findViewById(R.id.cardAddExpense)
        cardExpenseList = findViewById(R.id.cardExpenseList)
        cardManageCategories = findViewById(R.id.cardManageCategories)
        cardSetBudget = findViewById(R.id.cardSetBudget)
        cardLogout = findViewById(R.id.cardLogout)
        btnDateRange = findViewById(R.id.btnDateRange)

        // Set up click listeners for dashboard buttons
        cardAddExpense.setOnClickListener {
            startActivity(Intent(this, AddExpenseActivity::class.java))
        }
        cardExpenseList.setOnClickListener {
            startActivity(Intent(this, ExpenseListActivity::class.java))
        }
        cardManageCategories.setOnClickListener {
            startActivity(Intent(this, CategoryActivity::class.java))
        }
        cardSetBudget.setOnClickListener {
            startActivity(Intent(this, BudgetActivity::class.java))
        }
        cardLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
        
        // Set up date range selector
        btnDateRange.setOnClickListener {
            showDateRangeDialog()
        }
        
        // Initialize date range button text
        updateDateRangeButtonText()

        // Load the chart data
        updateChart()
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
        updateChart()

        // Fetch and display the user's total expenses on the dashboard
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))

        expenseViewModel.getTotalExpenses { total ->
            tvExpenseTotal.text = "Total Expenses: ${currencyFormat.format(total)}"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up any Firebase sync operations
        val db = AppDatabase.getInstance(this)
        val dataSync = FirebaseDataSync.getInstance(db)
        try {
            dataSync.cleanup()
        } catch (e: Exception) {
            // Prevent crash if cleanup method is not properly implemented
        }
    }
    
    private fun showDateRangeDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_date_range)
        
        // Initialize dialog views
        val radioGroup = dialog.findViewById<RadioGroup>(R.id.radioGroup)
        val layoutCustomDates = dialog.findViewById<LinearLayout>(R.id.layoutCustomDates)
        val btnFromDate = dialog.findViewById<Button>(R.id.btnFromDate)
        val btnToDate = dialog.findViewById<Button>(R.id.btnToDate)
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancel)
        val btnApply = dialog.findViewById<Button>(R.id.btnApply)
        
        // Set initial selection based on current date range
        when (selectedDateRangeType) {
            DateRangeType.CUSTOM -> dialog.findViewById<RadioButton>(R.id.radioCustom).isChecked = true
            DateRangeType.ONE_MONTH -> dialog.findViewById<RadioButton>(R.id.radioOneMonth).isChecked = true
            DateRangeType.THREE_MONTHS -> dialog.findViewById<RadioButton>(R.id.radioThreeMonths).isChecked = true
            DateRangeType.SIX_MONTHS -> dialog.findViewById<RadioButton>(R.id.radioSixMonths).isChecked = true
            DateRangeType.ONE_YEAR -> dialog.findViewById<RadioButton>(R.id.radioOneYear).isChecked = true
            DateRangeType.ALL_TIME -> dialog.findViewById<RadioButton>(R.id.radioAllTime).isChecked = true
        }
        
        // Show custom date fields if custom range is selected
        if (selectedDateRangeType == DateRangeType.CUSTOM) {
            layoutCustomDates.visibility = View.VISIBLE
        }
        
        // Format date buttons
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        btnFromDate.text = dateFormat.format(customStartDate.time)
        btnToDate.text = dateFormat.format(customEndDate.time)
        
        // Set up radio group listener
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            layoutCustomDates.visibility = if (checkedId == R.id.radioCustom) View.VISIBLE else View.GONE
        }
        
        // Set up date pickers
        btnFromDate.setOnClickListener {
            val calendar = customStartDate.clone() as Calendar
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    customStartDate.set(year, month, dayOfMonth, 0, 0, 0)
                    customStartDate.set(Calendar.MILLISECOND, 0)
                    btnFromDate.text = dateFormat.format(customStartDate.time)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
        
        btnToDate.setOnClickListener {
            val calendar = customEndDate.clone() as Calendar
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    customEndDate.set(year, month, dayOfMonth, 23, 59, 59)
                    customEndDate.set(Calendar.MILLISECOND, 999)
                    btnToDate.text = dateFormat.format(customEndDate.time)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
        
        // Set up button listeners
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        btnApply.setOnClickListener {
            // Determine selected range type
            selectedDateRangeType = when (radioGroup.checkedRadioButtonId) {
                R.id.radioCustom -> DateRangeType.CUSTOM
                R.id.radioOneMonth -> DateRangeType.ONE_MONTH
                R.id.radioThreeMonths -> DateRangeType.THREE_MONTHS
                R.id.radioSixMonths -> DateRangeType.SIX_MONTHS
                R.id.radioOneYear -> DateRangeType.ONE_YEAR
                R.id.radioAllTime -> DateRangeType.ALL_TIME
                else -> DateRangeType.ONE_YEAR
            }
            
            // Calculate date range based on selected type
            calculateDateRange()
            
            // Update button text with selected range
            updateDateRangeButtonText()
            
            // Refresh chart
            updateChart()
            
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private fun calculateDateRange() {
        val now = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        
        when (selectedDateRangeType) {
            DateRangeType.ONE_MONTH -> {
                customStartDate = Calendar.getInstance().apply {
                    add(Calendar.MONTH, -1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                customEndDate = now
            }
            DateRangeType.THREE_MONTHS -> {
                customStartDate = Calendar.getInstance().apply {
                    add(Calendar.MONTH, -3)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                customEndDate = now
            }
            DateRangeType.SIX_MONTHS -> {
                customStartDate = Calendar.getInstance().apply {
                    add(Calendar.MONTH, -6)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                customEndDate = now
            }
            DateRangeType.ONE_YEAR -> {
                customStartDate = Calendar.getInstance().apply {
                    add(Calendar.YEAR, -1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                customEndDate = now
            }
            DateRangeType.ALL_TIME -> {
                customStartDate = Calendar.getInstance().apply {
                    set(1970, 0, 1, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                customEndDate = now
            }
            DateRangeType.CUSTOM -> {
                // Custom dates are already set by date pickers
            }
        }
    }
    
    private fun updateDateRangeButtonText() {
        btnDateRange.text = when (selectedDateRangeType) {
            DateRangeType.CUSTOM -> {
                val dateFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
                "${dateFormat.format(customStartDate.time)} - ${dateFormat.format(customEndDate.time)}"
            }
            DateRangeType.ONE_MONTH -> getString(R.string.one_month)
            DateRangeType.THREE_MONTHS -> getString(R.string.three_months)
            DateRangeType.SIX_MONTHS -> getString(R.string.six_months)
            DateRangeType.ONE_YEAR -> getString(R.string.one_year)
            DateRangeType.ALL_TIME -> getString(R.string.all_time)
        }
    }

    private fun updateChart(showCategoryTrends: Boolean = false) {
        try {
            val viewModel = ViewModelProvider(this).get(ExpenseViewModel::class.java)

            // Make sure the user ID is updated in the view model
            val currentUser = FirebaseAuth.getInstance().currentUser
            currentUser?.uid?.let { userId ->
                viewModel.updateUserId(userId)
            }

            // Observe changes to expenses from the viewModel
            viewModel.expenses.observe(this) { expenses ->
                try {
                    // Filter expenses by selected date range
                    val startTime = customStartDate.timeInMillis
                    val endTime = customEndDate.timeInMillis
                    
                    // Filter expenses for the selected date range
                    val filteredExpenses = expenses.filter { 
                        it.date in startTime..endTime 
                    }

                    Log.d("DashboardActivity", "Expense count for selected range: ${filteredExpenses.size}")

                    // Determine how to group expenses based on date range
                    processExpensesForChart(filteredExpenses, showCategoryTrends)
                    
                } catch (e: Exception) {
                    Log.e("DashboardActivity", "Error processing expense data: ${e.message}")
                    Toast.makeText(this@DashboardActivity, "Error displaying chart data", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e("DashboardActivity", "Error setting up chart: ${e.message}")
            Toast.makeText(this, "Error setting up expense chart", Toast.LENGTH_SHORT).show()
        }
    }
    
//    private fun processExpensesForChart(expenses: List<Expense>) {
//        if (expenses.isEmpty()) {
//            // Display empty chart or message
//            updateChartWithData(emptyMap())
//            return
//        }
//
//        // Determine grouping type based on date range span
//        val dateRangeSpan = customEndDate.timeInMillis - customStartDate.timeInMillis
//        val daySpan = dateRangeSpan / (24 * 60 * 60 * 1000)
//
//        when {
//            // Group by day for ranges ≤ 31 days
//            daySpan <= 31 -> {
//                groupExpensesByDay(expenses)
//            }
//            // Group by week for ranges ≤ 120 days
//            daySpan <= 120 -> {
//                groupExpensesByWeek(expenses)
//            }
//            // Group by month for longer ranges
//            else -> {
//                groupExpensesByMonth(expenses)
//            }
//        }
//    }
private fun processExpensesForChart(expenses: List<Expense>, showCategoryTrends: Boolean) {
    if (expenses.isEmpty()) {
        updateChartWithData(emptyMap())
        return
    }

    if (showCategoryTrends) {
        groupExpensesByCategory(expenses)
        return
    }

    val dateRangeSpan = customEndDate.timeInMillis - customStartDate.timeInMillis
    val daySpan = dateRangeSpan / (24 * 60 * 60 * 1000)

    when {
        daySpan <= 31 -> groupExpensesByDay(expenses)
        daySpan <= 120 -> groupExpensesByWeek(expenses)
        else -> groupExpensesByMonth(expenses)
    }
}
    private fun groupExpensesByCategory(expenses: List<Expense>) {
        val expensesByCategory = mutableMapOf<String, Double>()

        for (expense in expenses) {
            val category = expense.categoryId ?: ""
            expensesByCategory[category.toString()] = expensesByCategory.getOrDefault(category, 0.0) + expense.amount
        }

        val entries = mutableListOf<com.github.mikephil.charting.data.BarEntry>()
        val labels = mutableListOf<String>()
        var index = 0

        for ((category, total) in expensesByCategory) {
            entries.add(com.github.mikephil.charting.data.BarEntry(index.toFloat(), total.toFloat()))
            labels.add(category)
            index++
        }

        updateCategoryChart(entries, labels)
    }
    private fun updateCategoryChart(entries: List<Entry>, labels: List<String>) {
        if (!::chart.isInitialized) {
            Log.e("DashboardActivity", "Chart not initialized")
            return
        }

        // Clear previous data to avoid overlap
        chart.clear()

        // Prepare dataset
        val dataSet = LineDataSet(entries, "Spending by Category")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        dataSet.valueTextSize = 12f
        dataSet.lineWidth = 2f
        dataSet.setCircleColor(Color.BLACK)
        dataSet.circleRadius = 4f
        dataSet.setDrawFilled(true)
        dataSet.fillColor = ColorTemplate.getHoloBlue()

        // Set chart data
        val lineData = LineData(dataSet)
        chart.data = lineData

        // Format x-axis labels to display the category names
        chart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                return if (index >= 0 && index < labels.size) labels[index] else ""
            }
        }

        // Enable/Disable specific chart features
        chart.description.isEnabled = false
        chart.legend.isEnabled = true
        chart.setScaleEnabled(false)
        chart.setDrawGridBackground(false)
        chart.xAxis.setDrawGridLines(false)
        chart.axisLeft.setDrawGridLines(true)
        chart.axisRight.isEnabled = false

        // Refresh chart to display updated data
        chart.notifyDataSetChanged()
        chart.invalidate()
    }


    private fun groupExpensesByDay(expenses: List<Expense>) {
        val expensesByDay = mutableMapOf<Triple<Int, Int, Int>, Double>()
        
        for (expense in expenses) {
            val cal = Calendar.getInstance().apply { timeInMillis = expense.date }
            val key = Triple(
                cal.get(Calendar.YEAR), 
                cal.get(Calendar.MONTH), 
                cal.get(Calendar.DAY_OF_MONTH)
            )
            expensesByDay[key] = expensesByDay.getOrDefault(key, 0.0) + expense.amount
        }
        
        // Create daily chart entries
        val entries = mutableListOf<com.github.mikephil.charting.data.Entry>()
        val labels = mutableListOf<String>()
        
        val startCal = customStartDate.clone() as Calendar
        val endCal = customEndDate.clone() as Calendar
        val dayFormat = SimpleDateFormat("dd", Locale.getDefault())
        
        var index = 0
        while (!startCal.after(endCal)) {
            val key = Triple(
                startCal.get(Calendar.YEAR),
                startCal.get(Calendar.MONTH),
                startCal.get(Calendar.DAY_OF_MONTH)
            )
            val total = expensesByDay[key] ?: 0.0
            entries.add(com.github.mikephil.charting.data.Entry(index.toFloat(), total.toFloat()))
            labels.add(dayFormat.format(startCal.time))
            
            startCal.add(Calendar.DAY_OF_MONTH, 1)
            index++
        }
        
        updateChartWithCustomData(entries, labels)
    }
    
    private fun groupExpensesByWeek(expenses: List<Expense>) {
        val expensesByWeek = mutableMapOf<Pair<Int, Int>, Double>()
        
        for (expense in expenses) {
            val cal = Calendar.getInstance().apply { 
                timeInMillis = expense.date 
                set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY) // Start week on Sunday
            }
            val weekOfYear = cal.get(Calendar.WEEK_OF_YEAR)
            val year = cal.get(Calendar.YEAR)
            val key = Pair(year, weekOfYear)
            expensesByWeek[key] = expensesByWeek.getOrDefault(key, 0.0) + expense.amount
        }
        
        // Create weekly chart entries
        val entries = mutableListOf<com.github.mikephil.charting.data.Entry>()
        val labels = mutableListOf<String>()
        
        val startCal = customStartDate.clone() as Calendar
        startCal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        
        val endCal = customEndDate.clone() as Calendar
        val weekFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
        
        var index = 0
        while (!startCal.after(endCal)) {
            val key = Pair(
                startCal.get(Calendar.YEAR),
                startCal.get(Calendar.WEEK_OF_YEAR)
            )
            val total = expensesByWeek[key] ?: 0.0
            entries.add(com.github.mikephil.charting.data.Entry(index.toFloat(), total.toFloat()))
            labels.add(weekFormat.format(startCal.time))
            
            startCal.add(Calendar.WEEK_OF_YEAR, 1)
            index++
        }
        
        updateChartWithCustomData(entries, labels)
    }
    
    private fun groupExpensesByMonth(expenses: List<Expense>) {
        val expensesByMonth = mutableMapOf<Pair<Int, Int>, Double>()
        
        for (expense in expenses) {
            val cal = Calendar.getInstance().apply { timeInMillis = expense.date }
            val key = Pair(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
            expensesByMonth[key] = expensesByMonth.getOrDefault(key, 0.0) + expense.amount
        }
        
        updateChartWithData(expensesByMonth)
    }
    
    private fun updateChartWithCustomData(entries: List<com.github.mikephil.charting.data.Entry>, labels: List<String>) {
        try {
            if (!::chart.isInitialized) {
                Log.e("DashboardActivity", "Chart not initialized")
                return
            }
            
            val dataSet = com.github.mikephil.charting.data.LineDataSet(entries, "Spend Habits")
            dataSet.lineWidth = 2f
            dataSet.circleRadius = 4f
            chart.data = com.github.mikephil.charting.data.LineData(dataSet)
            
            // Set custom labels
            chart.xAxis.valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val index = value.toInt()
                    return if (index >= 0 && index < labels.size) labels[index] else ""
                }
            }
            
            // Configure chart appearance
            chart.description.isEnabled = false
            chart.legend.isEnabled = true
            chart.setScaleEnabled(true)
            chart.setDrawGridBackground(false)
            chart.xAxis.setDrawGridLines(false)
            chart.axisLeft.setDrawGridLines(true)
            chart.axisRight.isEnabled = false
            
            // If we have a lot of labels, only show some of them
            if (labels.size > 12) {
                chart.xAxis.setLabelCount(12, true)
            }
            
            chart.invalidate() // Refresh chart
        } catch (e: Exception) {
            Log.e("DashboardActivity", "Error updating chart: ${e.message}")
        }
    }

    private fun updateChartWithData(expensesByMonth: Map<Pair<Int, Int>, Double>) {
        try {
            if (!::chart.isInitialized) {
                Log.e("DashboardActivity", "Chart not initialized")
                return
            }

            // Determine the number of months to display based on date range
            val monthsBetween = getMonthsBetween(customStartDate, customEndDate)
            val entries = mutableListOf<com.github.mikephil.charting.data.Entry>()
            
            // Create a calendar starting at the start date
            val cal = customStartDate.clone() as Calendar
            
            // Generate entries for each month in the range
            for (i in 0 until monthsBetween) {
                val key = Pair(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
                val total = expensesByMonth[key] ?: 0.0
                entries.add(com.github.mikephil.charting.data.Entry(i.toFloat(), total.toFloat()))
                cal.add(Calendar.MONTH, 1)
            }

            val dataSet = com.github.mikephil.charting.data.LineDataSet(entries, "Spend Habits")
            dataSet.lineWidth = 2f
            dataSet.circleRadius = 4f
            chart.data = com.github.mikephil.charting.data.LineData(dataSet)
            
            // Reset calendar for labels
            cal.timeInMillis = customStartDate.timeInMillis
            
            chart.xAxis.valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val index = value.toInt()
                    if (index >= 0 && index < monthsBetween) {
                        val labelCal = Calendar.getInstance().apply { 
                            timeInMillis = customStartDate.timeInMillis
                            add(Calendar.MONTH, index)
                        }
                        return SimpleDateFormat("MMM", Locale.getDefault()).format(labelCal.time)
                    }
                    return ""
                }
            }
            
            // Configure chart appearance
            chart.description.isEnabled = false
            chart.legend.isEnabled = true
            chart.setScaleEnabled(true)
            chart.setDrawGridBackground(false)
            chart.xAxis.setDrawGridLines(false)
            chart.axisLeft.setDrawGridLines(true)
            chart.axisRight.isEnabled = false
            
            // If we have a lot of months, only show some of them
            if (monthsBetween > 12) {
                chart.xAxis.setLabelCount(12, true)
            }
            
            chart.invalidate() // Refresh chart
        } catch (e: Exception) {
            Log.e("DashboardActivity", "Error updating chart: ${e.message}")
        }
    }
    
    private fun getMonthsBetween(startDate: Calendar, endDate: Calendar): Int {
        val start = startDate.clone() as Calendar
        val end = endDate.clone() as Calendar
        
        // Set both to first day of month for accurate calculation
        start.set(Calendar.DAY_OF_MONTH, 1)
        end.set(Calendar.DAY_OF_MONTH, 1)
        
        var monthCount = 0
        while (!start.after(end)) {
            start.add(Calendar.MONTH, 1)
            monthCount++
        }
        
        return maxOf(1, monthCount) // Ensure at least 1 month is displayed
    }
}

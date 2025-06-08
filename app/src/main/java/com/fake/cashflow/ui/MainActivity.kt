package com.fake.cashflow.ui

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.fake.cashflow.R
import com.fake.cashflow.activities.*
import com.fake.cashflow.data.AppDatabase
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var tvHello: TextView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //try get rid of calc
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish() //if don't want to go back to MainActivity


        try {
            // Set up Toolbar and DrawerToggle
            val toolbar = findViewById<Toolbar>(R.id.toolbar)
            setSupportActionBar(toolbar)

            drawerLayout = findViewById(R.id.drawer_layout)
            navView = findViewById(R.id.nav_view)

            val toggle = ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
            )
            drawerLayout.addDrawerListener(toggle)
            toggle.syncState()

            // Inflate header safely
            val headerView = if (navView.headerCount > 0) {
                navView.getHeaderView(0)
            } else {
                navView.inflateHeaderView(R.layout.nav_header)
            }
            
            try {
                val tvWelcome = headerView.findViewById<TextView>(R.id.tvWelcome)
                try {
                    val currentUser = FirebaseAuth.getInstance().currentUser
                    tvWelcome.text = if (currentUser != null) {
                        "Welcome, ${currentUser.displayName ?: currentUser.email ?: "User"}"
                    } else {
                        "Welcome, Guest"
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    tvWelcome.text = "Welcome, Guest"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Continue even if the welcome text fails
            }

            // Toggle menu items based on login state
            try {
                val menu = navView.menu
                try {
                    val currentUser = FirebaseAuth.getInstance().currentUser
                    if (currentUser != null) {
                        menu.findItem(R.id.nav_login)?.isVisible = false
                        menu.findItem(R.id.nav_register)?.isVisible = false
                        menu.findItem(R.id.nav_profile)?.isVisible = true
                        menu.findItem(R.id.nav_logout)?.isVisible = true
                    } else {
                        menu.findItem(R.id.nav_login)?.isVisible = true
                        menu.findItem(R.id.nav_register)?.isVisible = true
                        menu.findItem(R.id.nav_profile)?.isVisible = false
                        menu.findItem(R.id.nav_logout)?.isVisible = false
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Default to not logged in state
                    menu.findItem(R.id.nav_login)?.isVisible = true
                    menu.findItem(R.id.nav_register)?.isVisible = true
                    menu.findItem(R.id.nav_profile)?.isVisible = false
                    menu.findItem(R.id.nav_logout)?.isVisible = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Continue even if menu setup fails
            }

            // Set up navigation
            try {
                navView.setNavigationItemSelectedListener { menuItem ->
                    try {
                        when (menuItem.itemId) {
                            R.id.nav_dashboard -> startActivity(Intent(this, DashboardActivity::class.java))
                            R.id.nav_add_expense -> startActivity(Intent(this, AddExpenseActivity::class.java))
                            R.id.nav_expense_list -> startActivity(Intent(this, ExpenseListActivity::class.java))
                            R.id.nav_categories -> startActivity(Intent(this, CategoryActivity::class.java))
                            R.id.nav_budget -> startActivity(Intent(this, BudgetActivity::class.java))
                            R.id.nav_login -> startActivity(Intent(this, LoginActivity::class.java))
                            R.id.nav_register -> startActivity(Intent(this, RegisterActivity::class.java))
                            R.id.nav_logout -> {
                                try {
                                    FirebaseAuth.getInstance().signOut()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                startActivity(Intent(this, LoginActivity::class.java))
                                finish()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    try {
                        drawerLayout.closeDrawer(GravityCompat.START)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    true
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Continue even if navigation setup fails
            }

            // Setup calculator view
            tvHello = findViewById(R.id.tvHello)
            
            // Initialize database
            try {
                db = AppDatabase.getInstance(this)
                
                // Load expense count
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        // Get current user ID safely
                        val currentUserId = try {
                            FirebaseAuth.getInstance().currentUser?.uid
                        } catch (e: Exception) {
                            e.printStackTrace()
                            null
                        }
                        
                        val count = try {
                            if (currentUserId != null) {
                                db.expenseDao().countExpensesByUser(currentUserId)
                            } else {
                                // Fallback for guest users - safer query approach
                                try {
                                    // Use a safer SQL query format 
                                    val cursor = db.openHelper.readableDatabase.query(
                                        "SELECT COUNT(*) FROM expenses", 
                                        arrayOf<String>()
                                    )
                                    cursor.use {
                                        if (it.moveToFirst()) {
                                            it.getLong(0)
                                        } else {
                                            0L
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    0L
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            0L
                        }
                        
                        withContext(Dispatchers.Main) {
                            tvHello.text = "Number of expenses in DB: $count"
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        withContext(Dispatchers.Main) {
                            tvHello.text = "Database error. Please try again."
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                tvHello.text = "Could not connect to database."
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Last resort fallback if everything fails
            try {
                tvHello = findViewById(R.id.tvHello)
                tvHello.text = "App initialization error."
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

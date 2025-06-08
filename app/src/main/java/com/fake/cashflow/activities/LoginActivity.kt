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
import com.fake.cashflow.R
import com.fake.cashflow.data.AppDatabase
import com.fake.cashflow.firebase.FirebaseDataSync
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnRegister: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnRegister = findViewById(R.id.btnRegister)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Sync data from Firebase to local database
                    val db = AppDatabase.getInstance(this@LoginActivity)
                    val dataSync = FirebaseDataSync.getInstance(db)
                    
                    val userId = auth.currentUser?.uid ?: ""
                    Toast.makeText(this, "Login successful, syncing data...", Toast.LENGTH_SHORT).show()
                    
                    // Use a more reliable sync mechanism with better user feedback

                    val syncTimeout = 10000L // 10 seconds timeout
                    var syncCompleted = false
                    
                    // Show progress dialog
                    val progressDialog = android.app.ProgressDialog(this@LoginActivity).apply {
                        setMessage("Syncing data from cloud...")
                        setCancelable(false)
                        show()
                    }
                    
                    val timeoutHandler = android.os.Handler(android.os.Looper.getMainLooper())
                    val timeoutRunnable = Runnable {
                        if (!syncCompleted) {
                            syncCompleted = true
                            progressDialog.dismiss()
                            Toast.makeText(this@LoginActivity, "Sync taking longer than expected, proceeding to dashboard", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@LoginActivity, DashboardActivity::class.java))
                            finish()
                        }
                    }
                    
                    timeoutHandler.postDelayed(timeoutRunnable, syncTimeout)
                    
                    // First sync categories, then expenses
                    dataSync.syncCategoriesFromFirebase {
                        dataSync.syncExpensesFromFirebase(userId) {
                            timeoutHandler.removeCallbacks(timeoutRunnable)
                            if (!syncCompleted) {
                                syncCompleted = true
                                progressDialog.dismiss()
                                startActivity(Intent(this@LoginActivity, DashboardActivity::class.java))
                                finish()
                            }
                        }
                    }
                } else {
                    Toast.makeText(this, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}

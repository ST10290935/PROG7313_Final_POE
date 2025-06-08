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
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var btnBackToLogin: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnRegister = findViewById(R.id.btnRegister)
        btnBackToLogin = findViewById(R.id.btnBackToLogin)

        btnRegister.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, DashboardActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        btnBackToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}

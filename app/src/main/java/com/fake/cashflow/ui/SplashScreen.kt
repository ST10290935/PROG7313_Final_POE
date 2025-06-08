package com.fake.cashflow.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.fake.cashflow.R
import com.fake.cashflow.activities.DashboardActivity
import com.google.firebase.auth.FirebaseAuth

class SplashScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)
        
        // Delay for 2 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                // Check if Firebase is initialized and user is logged in
                val auth = FirebaseAuth.getInstance()
                val currentUser = auth.currentUser
                
                if (currentUser != null) {
                    // If user is logged in, navigate to DashboardActivity
                    val intent = Intent(this, DashboardActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(intent)
                } else {
                    // If not logged in, navigate to MainActivity
                    val intent = Intent(this, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(intent)
                }
            } catch (e: Exception) {
                // If any error occurs, log it and go to MainActivity as fallback
                e.printStackTrace()
                try {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(intent)
                } catch (e2: Exception) {
                    e2.printStackTrace()
                    // If MainActivity fails to launch, try to exit cleanly
                    finishAffinity()
                }
            } finally {
                finish()
            }
        }, 2000)
    }
}

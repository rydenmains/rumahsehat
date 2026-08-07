package com.rumahsehat.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.rumahsehat.BuildConfig

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val target = if (BuildConfig.FLAVOR == "admin") {
            Intent(this, AdminDashboardActivity::class.java)
        } else {
            Intent(this, MainActivity::class.java)
        }
        startActivity(target)
        finish()
    }
}
package com.rumahsehat.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.appcompat.app.AppCompatActivity
import com.rumahsehat.R
import com.rumahsehat.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.alpha = 0f
        binding.root.animate().alpha(1f).setDuration(700).start()

        // Logo tumbuh halus dari 85% -> 100% dengan overshoot, sambil spinner loading.
        binding.ivLogo.scaleX = 0.85f
        binding.ivLogo.scaleY = 0.85f
        binding.ivLogo.animate()
            .scaleX(1f).scaleY(1f)
            .setDuration(700)
            .setInterpolator(OvershootInterpolator())
            .start()

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }, 1400)
    }
}
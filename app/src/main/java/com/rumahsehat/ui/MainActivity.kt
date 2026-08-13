package com.rumahsehat.ui

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.rumahsehat.R
import com.rumahsehat.databinding.ActivityMainBinding
import java.util.Calendar

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: AssessmentViewModel by viewModels()
    private var syncSpin: ValueAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = AssessmentAdapter({ assessmentId ->
            ReviewActivity.start(this, assessmentId)
        })
        binding.historyContent.rvAssessments.layoutManager = LinearLayoutManager(this)
        binding.historyContent.rvAssessments.adapter = adapter

        // Setup Home Content Actions
        binding.homeContent.btnStartNew.setOnClickListener {
            startActivity(Intent(this, AssessmentActivity::class.java))
        }
        binding.homeContent.btnSyncNow.setOnClickListener {
            viewModel.syncNow()
        }
        binding.homeContent.btnViewHistory.setOnClickListener {
            switchToTab(R.id.nav_history)
        }

        binding.homeContent.tvGreeting.setText(greetingForHour(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)))

        // Setup History Content Actions
        binding.historyContent.btnSyncAllHistory.setOnClickListener {
            viewModel.syncNow()
        }

        // Setup Bottom Nav Actions
        binding.bottomNav.navHome.setOnClickListener { switchToTab(R.id.nav_home) }
        binding.bottomNav.navInspect.setOnClickListener { 
            startActivity(Intent(this, AssessmentActivity::class.java)) 
        }
        binding.bottomNav.navHistory.setOnClickListener { switchToTab(R.id.nav_history) }

        // Initial Tab Selection
        switchToTab(R.id.nav_home)

        viewModel.isSyncing.observe(this) { syncing ->
            binding.homeContent.btnSyncNow.isEnabled = !syncing
            binding.homeContent.btnSyncNow.text = getString(if (syncing) R.string.sync_in_progress else R.string.sync_now)
            if (syncing) startSyncSpin() else stopSyncSpin()
        }

        viewModel.syncMessage.observe(this) { msg ->
            if (msg != null) {
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNav.root) { v, insets ->
            v.setPadding(0, 0, 0, insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom)
            insets
        }

        viewModel.allAssessments.observe(this) { assessments ->
            adapter.submitList(assessments)
            binding.historyContent.llEmpty.visibility =
                if (assessments.isEmpty()) View.VISIBLE else View.GONE

            val pending = assessments.count { it.syncStatus != "SYNCED" }
            binding.homeContent.llSyncBanner.visibility =
                if (pending == 0) View.GONE else View.VISIBLE
            binding.homeContent.tvPendingInfo.text = getString(R.string.pending_info, pending)
            binding.homeContent.tvTodayCompleted.text = assessments.count { it.syncStatus == "SYNCED" }.toString()
            binding.homeContent.tvTodayPending.text = pending.toString()
        }
    }

    private fun greetingForHour(hour: Int): Int = when (hour) {
        in 5..10 -> R.string.greeting_morning
        in 11..14 -> R.string.greeting_afternoon
        in 15..17 -> R.string.greeting_evening
        else -> R.string.greeting_night
    }

    private fun switchToTab(tabId: Int) {
        // Reset selections
        binding.bottomNav.navHome.isSelected = false
        binding.bottomNav.navInspect.isSelected = false
        binding.bottomNav.navHistory.isSelected = false

        when (tabId) {
            R.id.nav_home -> {
                binding.bottomNav.navHome.isSelected = true
                binding.scrollHome.visibility = View.VISIBLE
                binding.scrollHistory.visibility = View.GONE
            }
            R.id.nav_history -> {
                binding.bottomNav.navHistory.isSelected = true
                binding.scrollHome.visibility = View.GONE
                binding.scrollHistory.visibility = View.VISIBLE
            }
        }
    }

    private fun startSyncSpin() {
        if (syncSpin != null) return
        val icon = binding.homeContent.ivSyncIcon
        val animator = ObjectAnimator.ofFloat(icon, "rotation", 0f, 360f).apply {
            duration = 1000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            start()
        }
        syncSpin = animator
    }

    private fun stopSyncSpin() {
        syncSpin?.cancel()
        binding.homeContent.ivSyncIcon.rotation = 0f
        syncSpin = null
    }
}
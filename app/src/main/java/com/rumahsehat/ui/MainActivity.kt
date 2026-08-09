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

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: AssessmentViewModel by viewModels()
    private var syncSpin: ValueAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        val adapter = AssessmentAdapter({ assessmentId ->
            ReviewActivity.start(this, assessmentId)
        })
        binding.rvAssessments.layoutManager = LinearLayoutManager(this)
        binding.rvAssessments.adapter = adapter

        binding.btnStartNew.setOnClickListener {
            startActivity(Intent(this, AssessmentActivity::class.java))
        }
        binding.btnSyncNow.setOnClickListener {
            viewModel.syncNow()
        }

        binding.btnViewHistory.setOnClickListener {
            binding.tvHistoryTitle.post {
                binding.scroll.smoothScrollTo(0, binding.tvHistoryTitle.top)
            }
        }

        viewModel.isSyncing.observe(this) { syncing ->
            binding.btnSyncNow.isEnabled = !syncing
            binding.btnSyncNow.text = getString(if (syncing) R.string.sync_in_progress else R.string.sync_now)
            if (syncing) startSyncSpin() else stopSyncSpin()
        }

        viewModel.syncMessage.observe(this) { msg ->
            if (msg != null) {
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            }
        }

        binding.bottomNavTabs.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inspect -> {
                    startActivity(Intent(this, AssessmentActivity::class.java))
                    true
                }
                R.id.nav_history -> {
                    binding.tvHistoryTitle.post {
                        binding.scroll.smoothScrollTo(0, binding.tvHistoryTitle.top)
                    }
                    true
                }
                else -> {
                    binding.scroll.smoothScrollTo(0, 0)
                    true
                }
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNavTabs) { v, insets ->
            v.setPadding(0, 0, 0, insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom)
            insets
        }

        viewModel.allAssessments.observe(this) { assessments ->
            adapter.submitList(assessments)
            binding.tvEmpty.visibility =
                if (assessments.isEmpty()) View.VISIBLE else View.GONE

            val pending = assessments.count { it.syncStatus != "SYNCED" }
            binding.llSyncBanner.visibility =
                if (pending == 0) View.GONE else View.VISIBLE
            binding.tvPendingInfo.text = getString(R.string.pending_info, pending)
            binding.tvTodayCompleted.text = assessments.count { it.syncStatus == "SYNCED" }.toString()
            binding.tvTodayPending.text = pending.toString()
        }
    }

    private fun startSyncSpin() {
        if (syncSpin != null) return
        val icon = binding.llSyncBanner.getChildAt(0) as View
        val animator = ObjectAnimator.ofFloat(icon, "rotation", 0f, 360f).apply {
            duration = 900
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            start()
        }
        syncSpin = animator
    }

    private fun stopSyncSpin() {
        syncSpin?.cancel()
        binding.llSyncBanner.getChildAt(0).rotation = 0f
        syncSpin = null
    }
}
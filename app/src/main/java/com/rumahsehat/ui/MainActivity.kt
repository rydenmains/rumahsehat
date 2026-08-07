package com.rumahsehat.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.rumahsehat.R
import com.rumahsehat.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: AssessmentViewModel by viewModels()

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

        viewModel.allAssessments.observe(this) { assessments ->
            adapter.submitList(assessments)
            binding.tvEmpty.visibility =
                if (assessments.isEmpty()) View.VISIBLE else View.GONE

            val pending = assessments.count { it.syncStatus != "SYNCED" }
            binding.tvPendingInfo.visibility =
                if (pending == 0) View.GONE else View.VISIBLE
            binding.tvPendingInfo.text = getString(R.string.pending_info, pending)
        }
    }
}
package com.rumahsehat.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.rumahsehat.BuildConfig
import com.rumahsehat.R
import com.rumahsehat.data.model.FormItemsProvider
import com.rumahsehat.databinding.ActivityReviewBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReviewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityReviewBinding
    private val viewModel: AssessmentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val assessmentId = intent.getStringExtra(EXTRA_ID) ?: run {
            finish()
            return
        }

        val items = FormItemsProvider.getFormItems()
        val adapter = ReviewAdapter(
            titles = items.associate { it.id to it.titleRes },
            weights = items.associate { it.id to it.maxScore }
        )
        binding.rvItems.layoutManager = LinearLayoutManager(this)
        binding.rvItems.adapter = adapter

        viewModel.review.observe(this) { (assessment, scoreItems) ->
            if (assessment == null) return@observe
            binding.tvMeta.text = buildString {
                append(getString(R.string.greeting_assessor)).append(": ").append(assessment.assessorId)
                append("\n").append(assessment.company)
                append("\n").append(
                    SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date(assessment.createdAt))
                )
            }
            binding.tvStatus.setText(
                if (assessment.isHealthy) R.string.status_healthy else R.string.status_unhealthy
            )
            binding.tvStatus.setBackgroundColor(
                if (assessment.isHealthy) getColor(R.color.emerald_accent) else getColor(R.color.error_red)
            )
            // Aplikasi user: tidak menampilkan label sehat/tidak sehat.
            if (BuildConfig.FLAVOR == "admin") {
                binding.tvStatus.visibility = View.VISIBLE
            } else {
                binding.tvStatus.visibility = View.GONE
            }
            adapter.submitList(scoreItems)
        }

        viewModel.loadReview(assessmentId)
    }

    companion object {
        const val EXTRA_ID = "assessment_id"
        fun start(context: android.content.Context, assessmentId: String) {
            context.startActivity(Intent(context, ReviewActivity::class.java).putExtra(EXTRA_ID, assessmentId))
        }
    }
}
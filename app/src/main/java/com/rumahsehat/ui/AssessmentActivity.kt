package com.rumahsehat.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.rumahsehat.R
import com.rumahsehat.databinding.ActivityAssessmentBinding

class AssessmentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAssessmentBinding
    private val viewModel: AssessmentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAssessmentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val totalQuestions = viewModel.getFormItemsCount()
        binding.viewPager.adapter = QuestionPagerAdapter(this, totalQuestions)
        binding.viewPager.isUserInputEnabled = false

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateNavigationButtons(position, totalQuestions)
                binding.progressBar.progress = ((position + 1) * 100) / totalQuestions
            }
        })

        binding.btnNext.setOnClickListener {
            val current = binding.viewPager.currentItem
            if (current < totalQuestions - 1) {
                binding.viewPager.currentItem = current + 1
            }
        }

        binding.btnPrev.setOnClickListener {
            val current = binding.viewPager.currentItem
            if (current > 0) {
                binding.viewPager.currentItem = current - 1
            }
        }

        binding.btnSave.setOnClickListener { onSave() }
    }

    private fun onSave() {
        val assessor = binding.etAssessor.text?.toString()?.trim().orEmpty()
        val company = binding.etCompany.text?.toString()?.trim().orEmpty()

        val issues = mutableListOf<String>()
        if (assessor.isEmpty() || company.isEmpty()) {
            val parts = mutableListOf<String>()
            if (assessor.isEmpty()) parts.add(getString(R.string.assessor_name_label))
            if (company.isEmpty()) parts.add(getString(R.string.company_label))
            issues.add(getString(R.string.msg_missing_section_identity) + ": " + parts.joinToString(", "))
        }

        val missingItems = viewModel.missingItems()
        if (missingItems.isNotEmpty()) {
            issues.add(
                getString(R.string.msg_missing_section_items) + ":\n- " +
                    missingItems.joinToString("\n- ") { getString(it.titleRes) }
            )
        }

        val missingPhotos = viewModel.missingPhotos()
        if (missingPhotos.isNotEmpty()) {
            val names = missingPhotos.map { getString(it.toPhotoNameRes()) }
            issues.add(
                getString(R.string.msg_missing_section_photos) + ":\n- " + names.joinToString("\n- ")
            )
        }

        if (issues.isNotEmpty()) {
            showCustomWarning(issues.joinToString("\n\n"))
            return
        }

        viewModel.saveAssessment(assessor, company)
        Toast.makeText(this, R.string.msg_saved, Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun String.toPhotoNameRes(): Int = when (this) {
        AssessmentViewModel.photoKeys[0] -> R.string.photo_house_front
        AssessmentViewModel.photoKeys[1] -> R.string.photo_sanitation
        else -> R.string.photo_kitchen_spal
    }

    private fun showCustomWarning(message: String) {
        AlertDialog.Builder(this, R.style.AlertDialogCustom)
            .setTitle(R.string.msg_missing_title)
            .setMessage(message)
            .setPositiveButton(getString(R.string.ok)) { _, _ -> }
            .show()
    }

    private fun updateNavigationButtons(position: Int, total: Int) {
        binding.apply {
            btnPrev.visibility = if (position == 0) View.GONE else View.VISIBLE

            if (position == total - 1) {
                btnNext.visibility = View.GONE
                btnSave.visibility = View.VISIBLE
            } else {
                btnNext.visibility = View.VISIBLE
                btnSave.visibility = View.GONE
            }
        }
    }
}
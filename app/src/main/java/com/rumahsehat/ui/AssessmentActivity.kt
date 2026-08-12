package com.rumahsehat.ui

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.viewpager2.widget.ViewPager2
import com.rumahsehat.R
import com.rumahsehat.databinding.ActivityAssessmentBinding
import java.io.File

/**
 * Flow assessment: halaman-0 = identitas (subtitle AppBar), sisanya 17 soal.
 * Tombol kamera bulat di bar bawah muncul hanya di soal pertama tiap section.
 */
class AssessmentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAssessmentBinding
    private val viewModel: AssessmentViewModel by viewModels()

    private var totalQuestions = 0
    private var identitySubtitleShown = false
    private var photoFile: File? = null
    private var currentSectionKey: String = AssessmentViewModel.photoKeys[0]

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && photoFile != null) {
            viewModel.markPhotoTaken(currentSectionKey, photoFile!!.absolutePath)
            updateCameraButton()
            Toast.makeText(this, getString(currentSectionKey.toPhotoNameRes()), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAssessmentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        totalQuestions = viewModel.getFormItemsCount()
        val totalPages = totalQuestions + 1 // +1 = halaman-0 identitas
        binding.viewPager.adapter = QuestionPagerAdapter(this, totalPages)
        binding.viewPager.isUserInputEnabled = false

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateNavigationButtons(position, totalPages)
                if (position == 0) {
                    binding.progressBar.progress = 0
                } else {
                    binding.progressBar.progress = (position * 100) / totalQuestions
                    val sectionPrefix = viewModel.getFormItemAt(position - 1).id.substringBefore('.')
                    binding.progressBar.setIndicatorColor(
                        when (sectionPrefix) {
                            "1" -> getColor(R.color.forest_green)
                            "2" -> getColor(R.color.emerald_accent)
                            else -> getColor(R.color.on_primary_container)
                        }
                    )
                    currentSectionKey = AssessmentViewModel.photoKeyFor(viewModel.getFormItemAt(position - 1).id)
                    updateCameraButton()
                }
                if (position == 1 && !identitySubtitleShown) showIdentitySubtitle()
            }
        })

        binding.btnNext.setOnClickListener {
            val current = binding.viewPager.currentItem
            if (current < totalPages - 1) {
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
        binding.cardCamera.setOnClickListener { capturePhoto() }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            binding.bottomNavigation.updatePadding(bottom = bottom)
            insets
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                AlertDialog.Builder(this@AssessmentActivity, R.style.AlertDialogCustom)
                    .setTitle(R.string.exit_confirm_title)
                    .setMessage(R.string.exit_confirm_message)
                    .setPositiveButton(getString(R.string.exit_confirm_leave)) { _, _ ->
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                    .setNegativeButton(getString(R.string.exit_confirm_stay), null)
                    .show()
            }
        })
    }

    /** Saat masuk halaman pertama soal, identitas "terbang" jadi subtitle header. */
    private fun showIdentitySubtitle() {
        identitySubtitleShown = true
        val name = viewModel.assessorName.trim()
        val company = viewModel.companyName.trim()
        binding.tvIdentitySub.text = when {
            name.isEmpty() -> company
            company.isEmpty() -> name
            else -> "$name • $company"
        }
        binding.tvIdentitySub.visibility = View.VISIBLE
        binding.tvIdentitySub.translationY = dp(18).toFloat()
        binding.tvIdentitySub.alpha = 0f
        binding.tvIdentitySub.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(380)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun capturePhoto() {
        val position = binding.viewPager.currentItem
        if (position == 0) return
        currentSectionKey = AssessmentViewModel.photoKeyFor(viewModel.getFormItemAt(position - 1).id)

        val fileName = "IMG_${viewModel.getFormItemAt(position - 1).id}_${System.currentTimeMillis()}.jpg"
        val storageDir = getExternalFilesDir(null)
        val file = File(storageDir, fileName)
        photoFile = file

        val uri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            file
        )
        takePicture.launch(uri)
    }

    /** Tampilan tombol kamera: ikon + semi-transparan (belum foto) <-> thumbnail + centang (sudah foto). */
    private fun updateCameraButton() {
        val path = viewModel.photoPath(currentSectionKey)
        val taken = path != null
        binding.ivCameraThumb.visibility = if (taken) View.VISIBLE else View.GONE
        binding.btnCameraIcon.visibility = if (taken) View.GONE else View.VISIBLE
        binding.ivCameraCheck.visibility = if (taken) View.VISIBLE else View.GONE
        binding.cardCamera.setCardBackgroundColor(
            getColor(if (taken) R.color.forest_green else R.color.camera_fab_semi)
        )
        if (taken) {
            try {
                // ponytail: decode penuh sekali per navigasi; cukup untuk 1 thumbnail.
                val bmp = BitmapFactory.decodeFile(path)
                if (bmp != null) binding.ivCameraThumb.setImageBitmap(bmp)
            } catch (_: OutOfMemoryError) {
                // biarkan thumbnail kosong, foto tetap tersimpan & terkirim.
            }
        }
    }

    private fun onSave() {
        val assessor = viewModel.assessorName
        val company = viewModel.companyName

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

    private fun updateNavigationButtons(position: Int, totalPages: Int) {
        binding.apply {
            btnPrev.visibility = if (position == 0) View.GONE else View.VISIBLE

            val isLast = position == totalPages - 1
            if (isLast) {
                btnNext.visibility = View.GONE
                btnSave.visibility = View.VISIBLE
            } else {
                btnNext.visibility = View.VISIBLE
                btnSave.visibility = View.GONE
            }

            val isSectionFirst = position > 0 &&
                viewModel.getFormItemAt(position - 1).id.endsWith(".1")
            cameraFab.visibility = if (!isLast && isSectionFirst) View.VISIBLE else View.GONE
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
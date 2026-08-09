package com.rumahsehat.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.rumahsehat.R
import com.rumahsehat.data.model.FormItem
import com.rumahsehat.databinding.FragmentQuestionBinding
import java.io.File

class QuestionFragment : Fragment() {
    private var _binding: FragmentQuestionBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AssessmentViewModel by activityViewModels()

    private var questionIndex: Int = 0
    private lateinit var item: FormItem
    private var photoFile: File? = null
    private var photoCaption: Int = R.string.btn_take_photo
    private var sectionKey: String = AssessmentViewModel.photoKeys[0]

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && photoFile != null) {
            viewModel.markPhotoTaken(sectionKey, photoFile!!.absolutePath)
            updatePhotoUI()
            Toast.makeText(requireContext(), photoCaption, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        questionIndex = requireArguments().getInt(ARG_INDEX, 0)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentQuestionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        item = viewModel.getFormItemAt(questionIndex)

        binding.apply {
            badgeNumber.text = item.id.substringBefore('.')
            tvQuestionNumber.text = getString(
                R.string.question_count,
                questionIndex + 1,
                viewModel.getFormItemsCount()
            )
            tvQuestionTitle.setText(item.titleRes)

            val sectionPrefix = item.id.substringBefore('.')
            val sectionLabelRes = when (sectionPrefix) {
                "1" -> R.string.section_housing_label
                "2" -> R.string.section_sanitation_label
                else -> R.string.section_behavior_label
            }
            val sectionBannerRes = when (sectionPrefix) {
                "1" -> R.string.cat_housing
                "2" -> R.string.cat_sanitation
                else -> R.string.cat_behavior
            }
            tvSectionName.setText(sectionLabelRes)
            tvSectionBanner.setText(sectionBannerRes)
            val isSectionFirst = item.id.endsWith(".1")
            cvSectionBanner.visibility = if (isSectionFirst) View.VISIBLE else View.GONE

            photoCaption = when (sectionPrefix) {
                "1" -> R.string.photo_house_front
                "2" -> R.string.photo_sanitation
                else -> R.string.photo_kitchen_spal
            }
            sectionKey = AssessmentViewModel.photoKeyFor(item.id)
            // Foto hanya 3, diambil sekali per bagian => tampilkan tombol hanya di item pertama (1.1/2.1/3.1).
            tvPhotoCaption.visibility = if (isSectionFirst) View.VISIBLE else View.GONE
            tvPhotoCaption.setText(photoCaption)
            btnPhoto.setText(R.string.btn_take_photo)
            btnPhoto.visibility = if (isSectionFirst) View.VISIBLE else View.GONE

            buildOptions()

            rgOptions.setOnCheckedChangeListener { _, checkedId ->
                item.selectedOptionIndex = item.options.indexOfFirst { it.letter.code == checkedId }
            }

            cbApplicable.setOnCheckedChangeListener { _, isChecked ->
                item.isApplicable = isChecked
                setApplicableUI(isChecked)
            }

            etNotes.setText(item.reason.orEmpty())
            etNotes.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    item.reason = s?.toString()
                }
            })

            btnPhoto.setOnClickListener { capturePhoto() }

            updateUI()
            updatePhotoUI()
        }
    }

    private fun buildOptions() {
        binding.rgOptions.removeAllViews()
        item.options.forEach { option ->
            val rb = RadioButton(requireContext())
            rb.id = option.letter.code
            rb.text = option.label
            rb.textSize = 15f
            rb.setTextColor(requireContext().getColor(R.color.on_surface))
            rb.setBackgroundResource(R.drawable.bg_radio_card)
            rb.setPaddingRelative(dp(14), dp(14), dp(14), dp(14))
            val buttonTint = android.content.res.ColorStateList(
                arrayOf(
                    intArrayOf(android.R.attr.state_checked),
                    intArrayOf()
                ),
                intArrayOf(
                    requireContext().getColor(R.color.forest_green),
                    requireContext().getColor(R.color.outline)
                )
            )
            rb.setButtonTintList(buttonTint)
            val lp = RadioGroup.LayoutParams(
                RadioGroup.LayoutParams.MATCH_PARENT,
                RadioGroup.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = dp(10)
            rb.layoutParams = lp
            binding.rgOptions.addView(rb)
        }
    }

    private fun updateUI() {
        binding.apply {
            cbApplicable.isChecked = item.isApplicable
            setApplicableUI(item.isApplicable)

            if (item.selectedOptionIndex >= 0) {
                rgOptions.check(item.options[item.selectedOptionIndex].letter.code)
            } else {
                rgOptions.clearCheck()
            }
        }
    }

    private fun setApplicableUI(applicable: Boolean) {
        binding.rgOptions.isEnabled = applicable
        binding.etNotes.isEnabled = applicable
    }

    private fun updatePhotoUI() {
        val taken = viewModel.isPhotoTaken(sectionKey)
        binding.btnPhoto.setText(if (taken) R.string.btn_retake_photo else R.string.btn_take_photo)
        binding.tvPhotoHint.visibility =
            if (binding.btnPhoto.visibility == View.VISIBLE) View.VISIBLE else View.GONE
        binding.tvPhotoHint.setText(if (taken) R.string.photo_hint_taken else R.string.photo_hint_missing)
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun capturePhoto() {
        val fileName = "IMG_${item.id}_${System.currentTimeMillis()}.jpg"
        val storageDir = requireContext().getExternalFilesDir(null)
        val file = File(storageDir, fileName)
        photoFile = file

        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )
        takePicture.launch(uri)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_INDEX = "index"
        fun newInstance(index: Int) = QuestionFragment().apply {
            arguments = Bundle().apply { putInt(ARG_INDEX, index) }
        }
    }
}
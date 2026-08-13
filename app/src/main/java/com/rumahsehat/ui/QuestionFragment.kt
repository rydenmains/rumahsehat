package com.rumahsehat.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.rumahsehat.R
import com.rumahsehat.data.model.FormItem
import com.rumahsehat.databinding.FragmentQuestionBinding

class QuestionFragment : Fragment() {
    private var _binding: FragmentQuestionBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AssessmentViewModel by activityViewModels()

    private var questionIndex: Int = 0
    private lateinit var item: FormItem
    private var photoCaption: Int = R.string.photo_house_front
    private var sectionKey: String = AssessmentViewModel.photoKeys[0]

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
            tvSectionBanner.setText(
                when (sectionPrefix) {
                    "1" -> R.string.banner_housing
                    "2" -> R.string.banner_sanitation
                    else -> R.string.banner_behavior
                }
            )
            val isSectionFirst = item.id.endsWith(".1")
            tvSectionBanner.visibility = if (isSectionFirst) View.VISIBLE else View.GONE

            photoCaption = when (sectionPrefix) {
                "1" -> R.string.photo_house_front
                "2" -> R.string.photo_sanitation
                else -> R.string.photo_kitchen_spal
            }
            sectionKey = AssessmentViewModel.photoKeyFor(item.id)
            tvPhotoCaption.visibility = if (isSectionFirst) View.VISIBLE else View.GONE
            tvPhotoCaption.setText(photoCaption)
            tvPhotoHint.visibility = if (isSectionFirst) View.VISIBLE else View.GONE

            buildOptions()

            rgOptions.setOnCheckedChangeListener { _, checkedId ->
                item.selectedOptionIndex = item.options.indexOfFirst { it.letter.code == checkedId }
            }

            etNotes.setText(item.reason.orEmpty())
            etNotes.doOnTextChanged { text, _, _, _ ->
                item.reason = text?.toString()
            }

            updateUI()
            updatePhotoHint()
            // Tombol kamera ada di bar bawah activity; hint ikut status foto di ViewModel.
            viewModel.photoPathsLive.observe(viewLifecycleOwner) { updatePhotoHint() }
        }
    }

    private fun buildOptions() {
        binding.rgOptions.removeAllViews()
        item.options.forEach { option ->
            val rb = RadioButton(requireContext())
            rb.id = option.letter.code
            rb.text = option.label
            rb.setTextAppearance(R.style.TextAppearance_RumahSehat_Body_Medium)
            rb.setTextColor(requireContext().getColor(R.color.on_surface))
            rb.setBackgroundResource(R.drawable.bg_radio_card)
            rb.setPaddingRelative(dp(12), dp(12), dp(12), dp(12))
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
            lp.bottomMargin = dp(8)
            rb.layoutParams = lp
            binding.rgOptions.addView(rb)
        }
    }

    private fun updateUI() {
        if (item.selectedOptionIndex >= 0) {
            binding.rgOptions.check(item.options[item.selectedOptionIndex].letter.code)
        } else {
            binding.rgOptions.clearCheck()
        }
    }

    private fun updatePhotoHint() {
        val path = viewModel.photoPath(sectionKey)
        val taken = path != null
        binding.tvPhotoHint.setText(if (taken) R.string.photo_hint_taken else R.string.photo_hint_missing)
        val preview = if (taken) decodePreview(path) else null
        if (preview != null) {
            binding.ivPhotoPreview.setImageBitmap(preview)
            binding.cardPhotoPreview.visibility = View.VISIBLE
        } else {
            binding.cardPhotoPreview.visibility = View.GONE
        }
    }

    // ponytail: decode takar (inSampleSize), full-res HP lapangan bisa 12MP = 48MB bitmap.
    private fun decodePreview(path: String?): Bitmap? {
        if (path == null) return null
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= 800 || bounds.outHeight / (sample * 2) >= 800) sample *= 2
            BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
        } catch (_: OutOfMemoryError) {
            null
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

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
package com.rumahsehat.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
            tvQuestionNumber.text = getString(
                R.string.question_count,
                questionIndex + 1,
                viewModel.getFormItemsCount()
            )
            tvQuestionTitle.setText(item.titleRes)

            photoCaption = when (item.id.substringBefore('.')) {
                "1" -> R.string.photo_house_front
                "2" -> R.string.photo_sanitation
                else -> R.string.photo_kitchen_spal
            }
            sectionKey = AssessmentViewModel.photoKeyFor(item.id)
            // Foto hanya 3, diambil sekali per bagian => tampilkan tombol hanya di item pertama (1.1/2.1/3.1).
            val isSectionFirst = item.id.endsWith(".1")
            btnPhoto.setText(photoCaption)
            btnPhoto.visibility = if (isSectionFirst) View.VISIBLE else View.GONE

            sliderScore.valueFrom = 0f
            sliderScore.valueTo = 4f
            sliderScore.stepSize = 1f

            sliderScore.addOnChangeListener { _, value, fromUser ->
                if (fromUser) {
                    item.currentScore = scoreForLevel(value, item.maxScore)
                    updateLevelLabel()
                }
            }

            cbApplicable.setOnCheckedChangeListener { _, isChecked ->
                item.isApplicable = isChecked
                sliderScore.isEnabled = isChecked
                etNotes.isEnabled = isChecked
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
        }
    }

    private fun scoreForLevel(level: Float, max: Int): Int = when (level) {
        1f -> (max * 0.25).toInt()
        2f -> (max * 0.50).toInt()
        3f -> (max * 0.75).toInt()
        4f -> max
        else -> 0
    }

    private fun updateLevelLabel() {
        binding.tvLevel.setText(
            if (item.isApplicable) AssessmentLevel.labelRes(item.currentScore, item.maxScore)
            else R.string.item_not_applicable
        )
    }

    private fun updateUI() {
        binding.apply {
            cbApplicable.isChecked = item.isApplicable
            sliderScore.isEnabled = item.isApplicable
            etNotes.isEnabled = item.isApplicable

            sliderScore.value = when (item.currentScore) {
                0 -> 0f
                (item.maxScore * 0.25).toInt() -> 1f
                (item.maxScore * 0.50).toInt() -> 2f
                (item.maxScore * 0.75).toInt() -> 3f
                item.maxScore -> 4f
                else -> 0f
            }
            updateLevelLabel()
        }
    }

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
package com.rumahsehat.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.rumahsehat.databinding.FragmentIdentityBinding

/** Halaman-0: identitas petugas & instansi. Nilai langsung di-sinkron ke ViewModel. */
class IdentityFragment : Fragment() {
    private var _binding: FragmentIdentityBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AssessmentViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIdentityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.etAssessor.setText(viewModel.assessorName)
        binding.etCompany.setText(viewModel.companyName)

        binding.etAssessor.doOnTextChanged { text, _, _, _ ->
            viewModel.assessorName = text?.toString()?.trim().orEmpty()
        }
        binding.etCompany.doOnTextChanged { text, _, _, _ ->
            viewModel.companyName = text?.toString()?.trim().orEmpty()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = IdentityFragment()
    }
}
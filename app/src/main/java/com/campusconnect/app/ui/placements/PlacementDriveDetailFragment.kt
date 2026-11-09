package com.campusconnect.app.ui.placements

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.campusconnect.app.R
import com.campusconnect.app.data.model.PlacementDriveModel
import com.campusconnect.app.databinding.FragmentPlacementDriveDetailBinding
import com.campusconnect.app.utils.Resource
import com.campusconnect.app.viewmodel.PlacementViewModel
import com.campusconnect.app.viewmodel.ViewModelFactory
import com.google.android.material.snackbar.Snackbar

class PlacementDriveDetailFragment : Fragment() {

    private var _binding: FragmentPlacementDriveDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PlacementViewModel by viewModels {
        ViewModelFactory(requireContext())
    }

    private var driveId: String = ""
    private var isEligible: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlacementDriveDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        driveId = arguments?.getString("driveId") ?: ""
        if (driveId.isBlank()) {
            Toast.makeText(requireContext(), "Invalid drive ID", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnApplyNow.setOnClickListener {
            if (!isEligible) {
                Snackbar.make(
                    binding.root,
                    "You do not meet the minimum eligibility criteria for this position.",
                    Snackbar.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            binding.btnApplyNow.isEnabled = false
            binding.btnApplyNow.text = "Submitting..."
            viewModel.submitApplication(driveId)
        }

        observeViewModel()
        viewModel.loadDriveDetail(driveId)
    }

    private fun observeViewModel() {
        viewModel.driveDetail.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Resource.Loading -> {
                    // detail loading
                }
                is Resource.Success -> {
                    result.data?.let { bindDriveData(it) }
                }
                is Resource.Error -> {
                    Toast.makeText(
                        requireContext(),
                        result.message ?: "Failed to load drive details",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        viewModel.eligibility.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Resource.Loading -> {
                    binding.tvEligibilityBadge.text = "Evaluating..."
                }
                is Resource.Success -> {
                    val res = result.data
                    isEligible = res?.isEligible == true
                    if (isEligible) {
                        binding.tvEligibilityBadge.text = "● You are Eligible"
                        binding.tvEligibilityBadge.setBackgroundResource(R.drawable.bg_badge_eligible)
                        binding.tvEligibilityBadge.setTextColor(
                            ContextCompat.getColor(requireContext(), R.color.status_registered_text)
                        )
                        binding.tvEligibilityOverview.text =
                            "Your academic profile meets all criteria specified by the recruiter."
                        binding.layoutIneligibleReasons.visibility = View.GONE
                        binding.btnApplyNow.isEnabled = true
                        binding.btnApplyNow.text = "Submit Application"
                    } else {
                        binding.tvEligibilityBadge.text = "● Not Eligible"
                        binding.tvEligibilityBadge.setBackgroundResource(R.drawable.bg_badge_ineligible)
                        binding.tvEligibilityBadge.setTextColor(
                            ContextCompat.getColor(requireContext(), R.color.status_full_text)
                        )
                        binding.tvEligibilityOverview.text =
                            "You do not meet one or more required recruitment benchmarks."
                        binding.layoutIneligibleReasons.visibility = View.VISIBLE
                        val reasons = res?.reasons?.joinToString("\n• ") ?: "Criteria not satisfied"
                        binding.tvIneligibleReasons.text = "• $reasons"
                        binding.btnApplyNow.isEnabled = false
                        binding.btnApplyNow.text = "Ineligible to Apply"
                    }
                }
                is Resource.Error -> {
                    binding.tvEligibilityBadge.text = "Unknown"
                }
            }
        }

        viewModel.applicationState.observe(viewLifecycleOwner) { state ->
            if (state == null) return@observe

            when (state) {
                is Resource.Loading -> {
                    binding.btnApplyNow.isEnabled = false
                    binding.btnApplyNow.text = "Submitting Application..."
                }
                is Resource.Success -> {
                    binding.btnApplyNow.isEnabled = false
                    binding.btnApplyNow.text = "✓ Application Submitted"
                    Snackbar.make(
                        binding.root,
                        "Application submitted successfully to ${binding.tvCompanyName.text}!",
                        Snackbar.LENGTH_LONG
                    ).show()
                    viewModel.clearApplicationState()
                }
                is Resource.Error -> {
                    binding.btnApplyNow.isEnabled = isEligible
                    binding.btnApplyNow.text = if (isEligible) "Submit Application" else "Ineligible to Apply"
                    Snackbar.make(
                        binding.root,
                        state.message ?: "Failed to submit application",
                        Snackbar.LENGTH_LONG
                    ).show()
                    viewModel.clearApplicationState()
                }
            }
        }
    }

    private fun bindDriveData(drive: PlacementDriveModel) {
        with(binding) {
            tvToolbarTitle.text = drive.jobTitle
            tvJobTitle.text = drive.jobTitle
            tvCompanyName.text = drive.companyName
            tvSalaryPackage.text = drive.salaryPackageCtc
            tvEmploymentType.text = drive.employmentType.replace("_", " ")
            tvJobLocation.text = "📍 Location: ${drive.jobLocation}"
            tvVacancies.text = "👥 Open Vacancies: ${drive.vacancies}"

            val deadline = if (drive.applicationDeadline.isNotBlank()) {
                drive.applicationDeadline.take(10)
            } else {
                "Open for Registration"
            }
            tvDeadline.text = "⏰ Deadline: $deadline"

            tvJobDescription.text = drive.jobDescription

            val criteria = drive.eligibilityCriteria
            tvCriteriaCgpa.text = "• Minimum CGPA Required: ${criteria?.minCgpa ?: 6.0}"
            tvCriteriaBacklogs.text = "• Maximum Active Backlogs: ${criteria?.maxBacklogs ?: 0}"
            
            val skillsText = if (!criteria?.requiredSkills.isNullOrEmpty()) {
                criteria!!.requiredSkills.joinToString(", ")
            } else {
                "General Engineering Skills"
            }
            tvCriteriaSkills.text = "• Required Skills: $skillsText"

            if (drive.companyLogoUrl.isNotBlank()) {
                Glide.with(requireContext())
                    .load(drive.companyLogoUrl)
                    .placeholder(R.drawable.ic_logo)
                    .error(R.drawable.ic_logo)
                    .into(ivCompanyLogo)
            } else {
                ivCompanyLogo.setImageResource(R.drawable.ic_logo)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

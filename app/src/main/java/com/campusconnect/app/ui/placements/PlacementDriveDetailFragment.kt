package com.campusconnect.app.ui.placements

import android.content.res.ColorStateList
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class PlacementDriveDetailFragment : Fragment() {

    private var _binding: FragmentPlacementDriveDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PlacementViewModel by viewModels {
        ViewModelFactory(requireContext())
    }

    private var driveId: String = ""
    private var isEligible: Boolean = false
    private var isEvaluatingEligibility: Boolean = true
    private var isSubmitting: Boolean = false
    private var hasAlreadyApplied: Boolean = false
    private var ineligibilityReasons: List<String> = emptyList()

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

        setupSubmitButton()
        observeViewModel()

        viewModel.loadDriveDetail(driveId)
        viewModel.loadMyApplications()
    }

    private fun setupSubmitButton() {
        binding.btnApplyNow.isClickable = true
        binding.btnApplyNow.isFocusable = true
        binding.btnApplyNow.isEnabled = true

        binding.btnApplyNow.setOnClickListener {
            if (isSubmitting) {
                Snackbar.make(binding.root, "Submission already in progress...", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (hasAlreadyApplied) {
                Snackbar.make(
                    binding.root,
                    "You have already submitted an application for this position.",
                    Snackbar.LENGTH_LONG
                ).setAction("View") {
                    findNavController().navigate(R.id.myApplicationsFragment)
                }.show()
                return@setOnClickListener
            }

            if (isEvaluatingEligibility) {
                Snackbar.make(
                    binding.root,
                    "Evaluating eligibility criteria. Please wait a moment...",
                    Snackbar.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (!isEligible) {
                val reasonsText = if (ineligibilityReasons.isNotEmpty()) {
                    ineligibilityReasons.joinToString("\n• ")
                } else {
                    "Your profile does not satisfy one or more recruitment benchmarks."
                }

                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Eligibility Criteria Not Met")
                    .setMessage("You cannot apply because of the following constraints:\n\n• $reasonsText")
                    .setPositiveButton("Understood", null)
                    .show()
                return@setOnClickListener
            }

            // Eligible and ready to submit
            isSubmitting = true
            binding.btnApplyNow.isEnabled = false
            binding.btnApplyNow.text = "Submitting Application..."
            viewModel.submitApplication(driveId)
        }
    }

    private fun observeViewModel() {
        viewModel.driveDetail.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Resource.Loading -> {}
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
                    isEvaluatingEligibility = true
                    binding.tvEligibilityBadge.text = "Evaluating..."
                    binding.btnApplyNow.text = "Checking Eligibility..."
                }
                is Resource.Success -> {
                    isEvaluatingEligibility = false
                    val res = result.data
                    isEligible = res?.isEligible == true
                    ineligibilityReasons = res?.reasons ?: emptyList()

                    if (hasAlreadyApplied) {
                        applyAlreadySubmittedStyle()
                        return@observe
                    }

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
                        binding.btnApplyNow.backgroundTintList =
                            ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.primary))
                    } else {
                        binding.tvEligibilityBadge.text = "● Not Eligible"
                        binding.tvEligibilityBadge.setBackgroundResource(R.drawable.bg_badge_ineligible)
                        binding.tvEligibilityBadge.setTextColor(
                            ContextCompat.getColor(requireContext(), R.color.status_full_text)
                        )
                        binding.tvEligibilityOverview.text =
                            "You do not meet one or more required recruitment benchmarks."
                        binding.layoutIneligibleReasons.visibility = View.VISIBLE
                        val reasons = ineligibilityReasons.joinToString("\n• ")
                        binding.tvIneligibleReasons.text = "• $reasons"

                        // Keep button enabled and clickable so tap gives immediate feedback dialog
                        binding.btnApplyNow.isEnabled = true
                        binding.btnApplyNow.text = "Submit Application (Ineligible)"
                        binding.btnApplyNow.backgroundTintList =
                            ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.text_muted))
                    }
                }
                is Resource.Error -> {
                    isEvaluatingEligibility = false
                    isEligible = true // Allow submission attempt directly to backend validator
                    binding.tvEligibilityBadge.text = "Criteria Checked"
                    binding.btnApplyNow.isEnabled = true
                    binding.btnApplyNow.text = "Submit Application"
                }
            }
        }

        viewModel.myApplications.observe(viewLifecycleOwner) { result ->
            if (result is Resource.Success) {
                val apps = result.data ?: emptyList()
                val existing = apps.find { it.driveId == driveId && it.status != "WITHDRAWN" }
                if (existing != null) {
                    hasAlreadyApplied = true
                    applyAlreadySubmittedStyle()
                }
            }
        }

        viewModel.applicationState.observe(viewLifecycleOwner) { state ->
            if (state == null) return@observe

            when (state) {
                is Resource.Loading -> {
                    isSubmitting = true
                    binding.btnApplyNow.isEnabled = false
                    binding.btnApplyNow.text = "Submitting Application..."
                }
                is Resource.Success -> {
                    isSubmitting = false
                    hasAlreadyApplied = true
                    applyAlreadySubmittedStyle()

                    Snackbar.make(
                        binding.root,
                        "✓ Application submitted successfully to ${binding.tvCompanyName.text}!",
                        Snackbar.LENGTH_LONG
                    ).setAction("My Applications") {
                        findNavController().navigate(R.id.myApplicationsFragment)
                    }.show()
                    viewModel.clearApplicationState()
                }
                is Resource.Error -> {
                    isSubmitting = false
                    binding.btnApplyNow.isEnabled = true
                    binding.btnApplyNow.text = if (isEligible) "Submit Application" else "Submit Application (Ineligible)"
                    Snackbar.make(
                        binding.root,
                        state.message ?: "Failed to submit application. Please try again.",
                        Snackbar.LENGTH_LONG
                    ).show()
                    viewModel.clearApplicationState()
                }
            }
        }
    }

    private fun applyAlreadySubmittedStyle() {
        binding.btnApplyNow.isEnabled = true
        binding.btnApplyNow.text = "✓ Application Submitted"
        binding.btnApplyNow.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.accent_emerald))
    }

    private fun bindDriveData(drive: PlacementDriveModel) {
        with(binding) {
            tvCompanyName.text = drive.companyName
            tvJobTitle.text = drive.jobTitle
            tvSalaryPackage.text = "₹ ${drive.salaryPackageCtc}"
            tvJobLocation.text = drive.jobLocation
            tvEmploymentType.text = drive.employmentType.replace("_", " ")
            tvVacancies.text = "${drive.vacancies} Vacancies"
            tvDeadline.text = "Deadline: ${if (drive.applicationDeadline.length >= 10) drive.applicationDeadline.take(10) else drive.applicationDeadline}"
            tvJobDescription.text = drive.jobDescription

            if (drive.companyLogoUrl.isNotBlank()) {
                Glide.with(this@PlacementDriveDetailFragment)
                    .load(drive.companyLogoUrl)
                    .placeholder(R.drawable.ic_logo)
                    .into(ivCompanyLogo)
            }

            drive.eligibilityCriteria?.let { crit ->
                tvCriteriaCgpa.text = "Min CGPA: ${crit.minCgpa}"
                tvCriteriaBacklogs.text = "Max Backlogs: ${crit.maxBacklogs}"

                val skills = if (crit.requiredSkills.isNotEmpty()) {
                    crit.requiredSkills.joinToString(", ")
                } else "Open to all relevant tech stacks"
                tvCriteriaSkills.text = "Skills: $skills"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

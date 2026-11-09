package com.campusconnect.app.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.campusconnect.app.R
import com.campusconnect.app.data.model.StudentProfile
import com.campusconnect.app.data.model.User
import com.campusconnect.app.databinding.FragmentProfileBinding
import com.campusconnect.app.utils.Resource
import com.campusconnect.app.viewmodel.AuthViewModel
import com.campusconnect.app.viewmodel.ProfileViewModel
import com.campusconnect.app.viewmodel.ViewModelFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by viewModels { ViewModelFactory(requireContext()) }
    private val profileViewModel: ProfileViewModel by viewModels { ViewModelFactory(requireContext()) }

    private var currentProfile: StudentProfile? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        observeViewModels()

        profileViewModel.loadStudentProfile()
    }

    private fun setupListeners() {
        binding.btnEditProfile.setOnClickListener {
            showEditProfileDialog()
        }

        binding.btnMenuMyApplications.setOnClickListener {
            findNavController().navigate(R.id.myApplicationsFragment)
        }

        binding.btnMenuMyEvents.setOnClickListener {
            findNavController().navigate(R.id.myEventsFragment)
        }

        binding.btnMenuSavedPosts.setOnClickListener {
            findNavController().navigate(R.id.savedPostsFragment)
        }

        binding.btnMenuAdmin.setOnClickListener {
            findNavController().navigate(R.id.adminDashboardFragment)
        }

        binding.btnMenuSuperAdmin.setOnClickListener {
            findNavController().navigate(R.id.superAdminDashboardFragment)
        }

        binding.btnLogout.setOnClickListener {
            findNavController().navigate(
                R.id.loginFragment,
                null,
                androidx.navigation.NavOptions.Builder()
                    .setPopUpTo(R.id.nav_graph, true)
                    .build()
            )
            authViewModel.logout()
        }
    }

    private fun observeViewModels() {
        authViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                binding.tvUserName.text = user.name
                binding.tvUserEmail.text = user.email
                binding.tvStudentId.text = "Student ID: ${user.studentId}"

                if (user.profileImageUrl.isNotBlank()) {
                    Glide.with(requireContext()).load(user.profileImageUrl).into(binding.imgProfileAvatar)
                }

                if (user.role == User.ROLE_SUPER_ADMIN) {
                    binding.btnMenuSuperAdmin.visibility = View.VISIBLE
                    binding.btnMenuAdmin.visibility = View.VISIBLE
                } else if (user.role == User.ROLE_TPO_ADMIN || user.role == User.ROLE_CLUB_ADMIN) {
                    binding.btnMenuAdmin.visibility = View.VISIBLE
                    binding.btnMenuSuperAdmin.visibility = View.GONE
                } else {
                    binding.btnMenuAdmin.visibility = View.GONE
                    binding.btnMenuSuperAdmin.visibility = View.GONE
                }
            }
        }

        profileViewModel.studentProfile.observe(viewLifecycleOwner) { result ->
            if (result is Resource.Success && result.data != null) {
                val profile = result.data
                currentProfile = profile
                bindStudentProfile(profile)
            }
        }

        profileViewModel.updateState.observe(viewLifecycleOwner) { state ->
            if (state == null) return@observe
            when (state) {
                is Resource.Loading -> {}
                is Resource.Success -> {
                    Snackbar.make(binding.root, "Placement details updated successfully!", Snackbar.LENGTH_SHORT).show()
                    profileViewModel.clearUpdateState()
                }
                is Resource.Error -> {
                    Snackbar.make(binding.root, state.message ?: "Update failed", Snackbar.LENGTH_LONG).show()
                    profileViewModel.clearUpdateState()
                }
            }
        }
    }

    private fun bindStudentProfile(profile: StudentProfile) {
        with(binding) {
            tvUserName.text = profile.fullName
            tvStudentId.text = "Reg No: ${profile.registrationNumber}"
            tvProfileCgpa.text = String.format("%.2f", profile.currentCgpa)
            tvProfileBacklogs.text = profile.activeBacklogs.toString()
            tvProfileBatch.text = profile.graduationYear.toString()
            tvProfileDegree.text = "Degree: ${profile.degree}"

            tvRoleBadge.text = if (profile.isVerified) "✓ Verified Student" else "Pending Verification"
            tvRoleBadge.setBackgroundResource(
                if (profile.isVerified) R.drawable.bg_badge_registered else R.drawable.bg_badge_full
            )

            tvProfilePhone.text = "📞 Phone: ${profile.phoneNumber ?: "Not provided"}"
            tvProfileResume.text = "📄 Resume: ${profile.resumeUrl ?: "Not uploaded yet"}"

            val skillsText = if (profile.skills.isNotEmpty()) {
                profile.skills.joinToString(", ")
            } else {
                "None added"
            }
            tvProfileSkills.text = "💡 Skills: $skillsText"
        }
    }

    private fun showEditProfileDialog() {
        val context = requireContext()
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 16)
        }

        val etPhone = EditText(context).apply {
            hint = "Phone number (+91...)"
            setText(currentProfile?.phoneNumber ?: "")
        }
        val etResume = EditText(context).apply {
            hint = "Resume URL (Drive/PDF link)"
            setText(currentProfile?.resumeUrl ?: "")
        }
        val etSkills = EditText(context).apply {
            hint = "Skills (comma separated, e.g. Python, SQL, React)"
            setText(currentProfile?.skills?.joinToString(", ") ?: "")
        }

        layout.addView(etPhone)
        layout.addView(etResume)
        layout.addView(etSkills)

        MaterialAlertDialogBuilder(context)
            .setTitle("Edit Placement Details")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val phone = etPhone.text.toString().trim()
                val resume = etResume.text.toString().trim()
                val skillsRaw = etSkills.text.toString().trim()
                val skillsList = if (skillsRaw.isNotBlank()) {
                    skillsRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                } else null

                profileViewModel.updateProfile(
                    phone = if (phone.isNotBlank()) phone else null,
                    resumeUrl = if (resume.isNotBlank()) resume else null,
                    skills = skillsList
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

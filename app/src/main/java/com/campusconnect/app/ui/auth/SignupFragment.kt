package com.campusconnect.app.ui.auth

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.campusconnect.app.R
import com.campusconnect.app.databinding.FragmentSignupBinding
import com.campusconnect.app.utils.Resource
import com.campusconnect.app.viewmodel.AuthViewModel
import com.campusconnect.app.viewmodel.ViewModelFactory
import com.google.android.material.snackbar.Snackbar

class SignupFragment : Fragment() {
    private var _binding: FragmentSignupBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by viewModels { ViewModelFactory(requireContext()) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSignupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupInputValidation()
        setupListeners()
        observeViewModel()
    }

    private fun setupInputValidation() {
        val clearErrorWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.tilName.error = null
                binding.tilStudentId.error = null
                binding.tilEmail.error = null
                binding.tilPassword.error = null
                binding.tilConfirmPassword.error = null
                binding.tvError.visibility = View.GONE
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        binding.etName.addTextChangedListener(clearErrorWatcher)
        binding.etStudentId.addTextChangedListener(clearErrorWatcher)
        binding.etEmail.addTextChangedListener(clearErrorWatcher)
        binding.etPassword.addTextChangedListener(clearErrorWatcher)
        binding.etConfirmPassword.addTextChangedListener(clearErrorWatcher)
    }

    private fun setupListeners() {
        binding.btnSignup.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val studentId = binding.etStudentId.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()

            // Required field validations with visual feedback
            if (name.isBlank()) {
                binding.tilName.error = "Full Name is required"
                binding.etName.requestFocus()
                return@setOnClickListener
            }

            if (studentId.isBlank()) {
                binding.tilStudentId.error = "Student Registration Number is required"
                binding.etStudentId.requestFocus()
                return@setOnClickListener
            }

            if (email.isBlank()) {
                binding.tilEmail.error = "Email address is required"
                binding.etEmail.requestFocus()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.tilEmail.error = "Please enter a valid email address (e.g. name@university.edu)"
                binding.etEmail.requestFocus()
                return@setOnClickListener
            }

            if (password.length < 6) {
                binding.tilPassword.error = "Password must be at least 6 characters"
                binding.etPassword.requestFocus()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                binding.tilConfirmPassword.error = "Passwords do not match"
                binding.etConfirmPassword.requestFocus()
                return@setOnClickListener
            }

            binding.btnSignup.isEnabled = false
            binding.btnSignup.text = "Creating Account..."
            authViewModel.signup(name, email, password, studentId)
        }

        binding.tvLoginLink.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun observeViewModel() {
        authViewModel.authState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Resource.Loading -> {
                    binding.tvError.visibility = View.GONE
                    binding.btnSignup.isEnabled = false
                    binding.btnSignup.text = "Creating Account..."
                }
                is Resource.Success -> {
                    binding.btnSignup.isEnabled = true
                    binding.btnSignup.text = getString(R.string.create_account)
                    Snackbar.make(binding.root, "Account created successfully! Welcome to CampusConnect.", Snackbar.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_signup_to_home)
                }
                is Resource.Error -> {
                    binding.btnSignup.isEnabled = true
                    binding.btnSignup.text = getString(R.string.create_account)
                    binding.tvError.text = result.message ?: "Registration failed. Please try again."
                    binding.tvError.visibility = View.VISIBLE
                    Snackbar.make(binding.root, result.message ?: "Registration failed", Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

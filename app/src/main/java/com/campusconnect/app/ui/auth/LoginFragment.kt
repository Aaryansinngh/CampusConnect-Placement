package com.campusconnect.app.ui.auth

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.campusconnect.app.R
import com.campusconnect.app.data.model.User
import com.campusconnect.app.databinding.FragmentLoginBinding
import com.campusconnect.app.utils.Resource
import com.campusconnect.app.viewmodel.AuthViewModel
import com.campusconnect.app.viewmodel.ViewModelFactory
import com.google.android.material.snackbar.Snackbar

class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by viewModels { ViewModelFactory(requireContext()) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupInputValidation()
        setupListeners()
        observeViewModel()
    }

    private fun setupInputValidation() {
        val clearWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.tilEmail.error = null
                binding.tilPassword.error = null
                binding.tvError.visibility = View.GONE
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        binding.etEmail.addTextChangedListener(clearWatcher)
        binding.etPassword.addTextChangedListener(clearWatcher)
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isBlank()) {
                binding.tilEmail.error = "Email address is required"
                binding.etEmail.requestFocus()
                return@setOnClickListener
            }

            if (password.isBlank()) {
                binding.tilPassword.error = "Password is required"
                binding.etPassword.requestFocus()
                return@setOnClickListener
            }

            binding.btnLogin.isEnabled = false
            binding.btnLogin.text = "Signing In..."
            authViewModel.login(email, password)
        }

        binding.tvSignup.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_signup)
        }

        binding.tvForgotPassword.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            if (email.isNotBlank()) {
                Snackbar.make(binding.root, "Password reset instructions sent to $email", Snackbar.LENGTH_LONG).show()
            } else {
                binding.tilEmail.error = "Enter your email to reset password"
            }
        }

        binding.btnDemoStudent.setOnClickListener {
            binding.btnLogin.isEnabled = false
            binding.btnLogin.text = "Signing In (Student)..."
            authViewModel.switchDemoRole(User.ROLE_STUDENT)
        }

        binding.btnDemoAdmin.setOnClickListener {
            binding.btnLogin.isEnabled = false
            binding.btnLogin.text = "Signing In (TPO Admin)..."
            authViewModel.switchDemoRole(User.ROLE_CLUB_ADMIN)
        }

        binding.btnDemoSuper.setOnClickListener {
            binding.btnLogin.isEnabled = false
            binding.btnLogin.text = "Signing In (Super Admin)..."
            authViewModel.switchDemoRole(User.ROLE_SUPER_ADMIN)
        }
    }

    private fun observeViewModel() {
        authViewModel.authState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Resource.Loading -> {
                    binding.tvError.visibility = View.GONE
                    binding.btnLogin.isEnabled = false
                    binding.btnLogin.text = "Signing In..."
                }
                is Resource.Success -> {
                    binding.btnLogin.isEnabled = true
                    binding.btnLogin.text = getString(R.string.login)
                    findNavController().navigate(R.id.action_login_to_home)
                }
                is Resource.Error -> {
                    binding.btnLogin.isEnabled = true
                    binding.btnLogin.text = getString(R.string.login)
                    binding.tvError.text = result.message ?: "Authentication failed. Please check credentials."
                    binding.tvError.visibility = View.VISIBLE
                    Snackbar.make(binding.root, result.message ?: "Login failed", Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

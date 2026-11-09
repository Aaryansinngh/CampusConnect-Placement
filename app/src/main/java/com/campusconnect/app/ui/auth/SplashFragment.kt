package com.campusconnect.app.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.campusconnect.app.R
import com.campusconnect.app.databinding.FragmentSplashBinding
import com.campusconnect.app.viewmodel.AuthViewModel
import com.campusconnect.app.viewmodel.ViewModelFactory

class SplashFragment : Fragment() {

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by viewModels {
        ViewModelFactory(requireContext())
    }

    private var hasNavigated = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authViewModel.sessionChecked.observe(viewLifecycleOwner) { checked ->
            if (checked == true && !hasNavigated && isAdded) {
                val navController = findNavController()

                if (navController.currentDestination?.id == R.id.splashFragment) {
                    hasNavigated = true

                    val destination = if (authViewModel.currentUser.value != null) {
                        R.id.action_splash_to_home
                    } else {
                        R.id.action_splash_to_login
                    }

                    navController.navigate(destination)
                }
            }
        }

        authViewModel.checkCurrentUser()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

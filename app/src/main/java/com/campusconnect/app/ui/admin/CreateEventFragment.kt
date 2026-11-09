package com.campusconnect.app.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.campusconnect.app.databinding.FragmentCreateEventBinding
import com.campusconnect.app.utils.Resource
import com.campusconnect.app.viewmodel.AdminViewModel
import com.campusconnect.app.viewmodel.ViewModelFactory
import com.google.android.material.snackbar.Snackbar

class CreateEventFragment : Fragment() {
    private var _binding: FragmentCreateEventBinding? = null
    private val binding get() = _binding!!

    private val adminViewModel: AdminViewModel by viewModels { ViewModelFactory(requireContext()) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCreateEventBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnPublishEvent.setOnClickListener {
            val title = binding.etTitle.text.toString().trim()
            val description = binding.etDescription.text.toString().trim()
            val date = binding.etDate.text.toString().trim()
            val startTime = binding.etStartTime.text.toString().trim()
            val location = binding.etLocation.text.toString().trim()
            val deadline = binding.etDeadline.text.toString().trim()
            val capacityStr = binding.etCapacity.text.toString().trim()
            val capacity = capacityStr.toIntOrNull() ?: 100
            val imageUrl = binding.etImageUrl.text.toString().trim()

            if (title.isBlank()) {
                binding.etTitle.error = "Event title is required"
                binding.etTitle.requestFocus()
                return@setOnClickListener
            }

            if (date.isBlank()) {
                binding.etDate.error = "Event date is required"
                binding.etDate.requestFocus()
                return@setOnClickListener
            }

            if (location.isBlank()) {
                binding.etLocation.error = "Event venue/location is required"
                binding.etLocation.requestFocus()
                return@setOnClickListener
            }

            binding.btnPublishEvent.isEnabled = false
            binding.btnPublishEvent.text = "Creating Event..."

            adminViewModel.createEvent(
                title = title,
                description = description,
                date = date,
                startTime = if (startTime.isNotBlank()) startTime else "10:00 AM",
                endTime = "8:00 PM",
                location = location,
                deadline = if (deadline.isNotBlank()) deadline else date,
                capacity = capacity,
                imageUrl = imageUrl,
                clubId = "club_coding",
                clubName = "MUJ Coding Club"
            )
        }

        adminViewModel.createEventResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Resource.Loading -> {
                    binding.btnPublishEvent.isEnabled = false
                    binding.btnPublishEvent.text = "Creating Event..."
                }
                is Resource.Success -> {
                    binding.btnPublishEvent.isEnabled = true
                    binding.btnPublishEvent.text = "Publish Event"
                    Toast.makeText(requireContext(), "Event published successfully!", Toast.LENGTH_SHORT).show()
                    adminViewModel.clearResults()
                    findNavController().navigateUp()
                }
                is Resource.Error -> {
                    binding.btnPublishEvent.isEnabled = true
                    binding.btnPublishEvent.text = "Publish Event"
                    Snackbar.make(binding.root, result.message ?: "Failed to publish event", Snackbar.LENGTH_LONG).show()
                }
                else -> {}
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

package com.campusconnect.app.ui.interviews

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.campusconnect.app.adapter.InterviewSlotAdapter
import com.campusconnect.app.data.model.InterviewSlotModel
import com.campusconnect.app.databinding.FragmentInterviewBookingBinding
import com.campusconnect.app.utils.Resource
import com.campusconnect.app.viewmodel.PlacementViewModel
import com.campusconnect.app.viewmodel.ViewModelFactory
import com.google.android.material.snackbar.Snackbar

class InterviewBookingFragment : Fragment() {

    private var _binding: FragmentInterviewBookingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PlacementViewModel by viewModels {
        ViewModelFactory(requireContext())
    }

    private lateinit var adapter: InterviewSlotAdapter

    private var applicationId: String = ""
    private var driveId: String = ""
    private var companyName: String = ""
    private var jobTitle: String = ""
    private var selectedSlot: InterviewSlotModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            applicationId = it.getString("applicationId", "")
            driveId = it.getString("driveId", "")
            companyName = it.getString("companyName", "Company Drive")
            jobTitle = it.getString("jobTitle", "Placement Drive")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInterviewBookingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        binding.tvCompanyName.text = companyName
        binding.tvJobTitle.text = jobTitle

        setupRecyclerView()
        setupListeners()
        observeViewModel()

        if (driveId.isNotBlank()) {
            viewModel.loadRounds(driveId)
        } else {
            Toast.makeText(requireContext(), "Invalid drive ID", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerView() {
        adapter = InterviewSlotAdapter { slot ->
            selectedSlot = slot
            binding.btnConfirmBooking.isEnabled = true
            binding.btnConfirmBooking.text = "Confirm Interview Booking"
        }
        binding.rvSlots.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnConfirmBooking.setOnClickListener {
            val slot = selectedSlot ?: return@setOnClickListener
            val notes = binding.etNotes.text?.toString()?.trim().takeIf { !it.isNullOrBlank() }
            viewModel.bookSlot(slot.id, applicationId, notes)
        }
    }

    private fun observeViewModel() {
        viewModel.rounds.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Resource.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.layoutEmptySlots.visibility = View.GONE
                }
                is Resource.Success -> {
                    val rounds = result.data ?: emptyList()
                    if (rounds.isEmpty()) {
                        binding.progressBar.visibility = View.GONE
                        binding.layoutEmptySlots.visibility = View.VISIBLE
                    } else {
                        val activeRound = rounds.first()
                        binding.tvRoundName.text = "${activeRound.roundName} (${activeRound.roundType.replace("_", " ")})"
                        if (!activeRound.instructions.isNullOrBlank()) {
                            binding.tvRoundInstructions.visibility = View.VISIBLE
                            binding.tvRoundInstructions.text = activeRound.instructions
                        }
                        viewModel.loadSlots(activeRound.id)
                    }
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(
                        requireContext(),
                        result.message ?: "Failed to load recruitment rounds",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        viewModel.slots.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Resource.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    val slots = result.data ?: emptyList()
                    if (slots.isEmpty()) {
                        binding.layoutEmptySlots.visibility = View.VISIBLE
                        binding.rvSlots.visibility = View.GONE
                    } else {
                        binding.layoutEmptySlots.visibility = View.GONE
                        binding.rvSlots.visibility = View.VISIBLE
                        adapter.submitList(slots)
                    }
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(
                        requireContext(),
                        result.message ?: "Failed to load slots",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        viewModel.bookingState.observe(viewLifecycleOwner) { state ->
            if (state == null) return@observe

            when (state) {
                is Resource.Loading -> {
                    binding.btnConfirmBooking.isEnabled = false
                    binding.btnConfirmBooking.text = "Reserving Slot..."
                }
                is Resource.Success -> {
                    Toast.makeText(
                        requireContext(),
                        "Interview slot reserved successfully!",
                        Toast.LENGTH_LONG
                    ).show()
                    viewModel.clearBookingState()
                    findNavController().popBackStack()
                }
                is Resource.Error -> {
                    binding.btnConfirmBooking.isEnabled = true
                    binding.btnConfirmBooking.text = "Confirm Interview Booking"
                    Snackbar.make(
                        binding.root,
                        state.message ?: "Booking failed. Slot may have been taken.",
                        Snackbar.LENGTH_LONG
                    ).show()
                    viewModel.clearBookingState()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

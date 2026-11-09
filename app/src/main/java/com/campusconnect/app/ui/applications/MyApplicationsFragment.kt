package com.campusconnect.app.ui.applications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.campusconnect.app.adapter.ApplicationAdapter
import com.campusconnect.app.databinding.FragmentMyApplicationsBinding
import com.campusconnect.app.utils.Resource
import com.campusconnect.app.viewmodel.PlacementViewModel
import com.campusconnect.app.viewmodel.ViewModelFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class MyApplicationsFragment : Fragment() {

    private var _binding: FragmentMyApplicationsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PlacementViewModel by viewModels {
        ViewModelFactory(requireContext())
    }

    private lateinit var adapter: ApplicationAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyApplicationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        setupRecyclerView()
        setupListeners()
        observeViewModel()

        viewModel.loadMyApplications()
    }

    private fun setupRecyclerView() {
        adapter = ApplicationAdapter(
            onWithdrawClick = { app ->
                confirmWithdraw(app.id, app.companyName ?: "the company")
            },
            onBookSlotClick = { app ->
                val bundle = Bundle().apply {
                    putString("applicationId", app.id)
                    putString("driveId", app.driveId)
                    putString("companyName", app.companyName ?: "Company")
                    putString("jobTitle", app.driveTitle ?: "Placement Drive")
                }
                findNavController().navigate(
                    com.campusconnect.app.R.id.action_applications_to_interviewBooking,
                    bundle
                )
            }
        )
        binding.rvApplications.adapter = adapter
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadMyApplications()
        }
    }

    private fun confirmWithdraw(appId: String, companyName: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Withdraw Application")
            .setMessage("Are you sure you want to withdraw your application to $companyName? You will not be able to re-apply once withdrawn.")
            .setPositiveButton("Withdraw") { _, _ ->
                viewModel.withdrawApplication(appId, "Withdrawn by student.")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun observeViewModel() {
        viewModel.myApplications.observe(viewLifecycleOwner) { result ->
            binding.swipeRefresh.isRefreshing = false
            when (result) {
                is Resource.Loading -> {
                    if (adapter.itemCount == 0) {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                    binding.layoutEmpty.visibility = View.GONE
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    val apps = result.data ?: emptyList()
                    if (apps.isEmpty()) {
                        binding.layoutEmpty.visibility = View.VISIBLE
                        binding.rvApplications.visibility = View.GONE
                    } else {
                        binding.layoutEmpty.visibility = View.GONE
                        binding.rvApplications.visibility = View.VISIBLE
                        adapter.submitList(apps)
                    }
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(
                        requireContext(),
                        result.message ?: "Failed to load applications",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        viewModel.withdrawState.observe(viewLifecycleOwner) { state ->
            if (state == null) return@observe

            when (state) {
                is Resource.Loading -> {}
                is Resource.Success -> {
                    Snackbar.make(binding.root, "Application successfully withdrawn.", Snackbar.LENGTH_SHORT).show()
                    viewModel.clearWithdrawState()
                }
                is Resource.Error -> {
                    Snackbar.make(binding.root, state.message ?: "Failed to withdraw application", Snackbar.LENGTH_LONG).show()
                    viewModel.clearWithdrawState()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

package com.campusconnect.app.ui.placements

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.campusconnect.app.R
import com.campusconnect.app.adapter.PlacementDriveAdapter
import com.campusconnect.app.databinding.FragmentPlacementDrivesBinding
import com.campusconnect.app.utils.Resource
import com.campusconnect.app.viewmodel.PlacementViewModel
import com.campusconnect.app.viewmodel.ViewModelFactory

class PlacementDrivesFragment : Fragment() {

    private var _binding: FragmentPlacementDrivesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PlacementViewModel by viewModels {
        ViewModelFactory(requireContext())
    }

    private lateinit var driveAdapter: PlacementDriveAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlacementDrivesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        observeViewModel()

        viewModel.loadPlacementDrives()
    }

    private fun setupRecyclerView() {
        driveAdapter = PlacementDriveAdapter { drive ->
            val bundle = Bundle().apply {
                putString("driveId", drive.id)
            }
            findNavController().navigate(R.id.placementDriveDetailFragment, bundle)
        }
        binding.rvPlacementDrives.adapter = driveAdapter
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            val query = binding.etSearchDrives.text.toString().trim()
            viewModel.loadPlacementDrives(if (query.isNotBlank()) query else null)
        }

        binding.etSearchDrives.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.trim()
                viewModel.loadPlacementDrives(if (!query.isNullOrBlank()) query else null)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun observeViewModel() {
        viewModel.drives.observe(viewLifecycleOwner) { result ->
            binding.swipeRefresh.isRefreshing = false
            when (result) {
                is Resource.Loading -> {
                    if (driveAdapter.itemCount == 0) {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                    binding.layoutEmpty.visibility = View.GONE
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    val drives = result.data ?: emptyList()
                    if (drives.isEmpty()) {
                        binding.layoutEmpty.visibility = View.VISIBLE
                        binding.rvPlacementDrives.visibility = View.GONE
                    } else {
                        binding.layoutEmpty.visibility = View.GONE
                        binding.rvPlacementDrives.visibility = View.VISIBLE
                        driveAdapter.submitList(drives)
                    }
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    if (driveAdapter.itemCount == 0) {
                        binding.layoutEmpty.visibility = View.VISIBLE
                        binding.tvEmptyMessage.text = result.message ?: "Failed to load placement drives."
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

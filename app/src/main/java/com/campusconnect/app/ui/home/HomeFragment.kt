package com.campusconnect.app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.campusconnect.app.R
import com.campusconnect.app.adapter.CategoryAdapter
import com.campusconnect.app.adapter.EventAdapter
import com.campusconnect.app.adapter.PlacementDriveAdapter
import com.campusconnect.app.adapter.PostAdapter
import com.campusconnect.app.data.model.StudentProfile
import com.campusconnect.app.databinding.FragmentHomeBinding
import com.campusconnect.app.utils.Resource
import com.campusconnect.app.viewmodel.HomeViewModel
import com.campusconnect.app.viewmodel.ViewModelFactory

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val homeViewModel: HomeViewModel by viewModels { ViewModelFactory(requireContext()) }
    private lateinit var placementDriveAdapter: PlacementDriveAdapter
    private lateinit var postAdapter: PostAdapter
    private lateinit var eventAdapter: EventAdapter
    private lateinit var categoryAdapter: CategoryAdapter

    private val categoryList = listOf("All", "Technical", "Cultural", "Sports", "Entrepreneurship", "Arts", "Music", "Dance", "Literary")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        setupListeners()
        observeViewModel()

        homeViewModel.loadHomeData()
    }

    private fun setupRecyclerViews() {
        placementDriveAdapter = PlacementDriveAdapter { drive ->
            val bundle = Bundle().apply { putString("driveId", drive.id) }
            findNavController().navigate(R.id.placementDriveDetailFragment, bundle)
        }
        binding.rvPlacementDrives.adapter = placementDriveAdapter

        categoryAdapter = CategoryAdapter(categoryList) { category ->
            homeViewModel.selectCategory(category)
        }
        binding.rvCategories.adapter = categoryAdapter

        eventAdapter = EventAdapter { event ->
            val bundle = Bundle().apply { putString("eventId", event.eventId) }
            findNavController().navigate(R.id.eventDetailFragment, bundle)
        }
        binding.rvUpcomingEvents.adapter = eventAdapter

        postAdapter = PostAdapter(
            onLikeClicked = { post -> homeViewModel.toggleLike(post.postId) },
            onCommentClicked = { post ->
                val bundle = Bundle().apply { putString("postId", post.postId) }
                findNavController().navigate(R.id.postDetailFragment, bundle)
            },
            onSaveClicked = { post -> homeViewModel.toggleSave(post.postId) },
            onPostClicked = { post ->
                val bundle = Bundle().apply { putString("postId", post.postId) }
                findNavController().navigate(R.id.postDetailFragment, bundle)
            }
        )
        binding.rvFeedPosts.adapter = postAdapter
    }

    private fun setupListeners() {
        binding.swipeRefreshHome.setOnRefreshListener {
            homeViewModel.loadHomeData()
        }

        binding.btnQuickExploreDrives.setOnClickListener {
            findNavController().navigate(R.id.placementDrivesFragment)
        }

        binding.btnViewAllDrives.setOnClickListener {
            findNavController().navigate(R.id.placementDrivesFragment)
        }

        binding.btnQuickMyApplications.setOnClickListener {
            findNavController().navigate(R.id.myApplicationsFragment)
        }

        binding.btnNotification.setOnClickListener {
            findNavController().navigate(R.id.notificationsFragment)
        }

        binding.btnViewAllEvents.setOnClickListener {
            findNavController().navigate(R.id.eventsFragment)
        }

        binding.btnExploreClubs.setOnClickListener {
            findNavController().navigate(R.id.exploreClubsFragment)
        }
    }

    private fun observeViewModel() {
        // --- Placement Dashboard LiveData ---
        homeViewModel.studentProfile.observe(viewLifecycleOwner) { result ->
            if (result is Resource.Success && result.data != null) {
                bindStudentProfile(result.data)
            }
        }

        homeViewModel.placementDrives.observe(viewLifecycleOwner) { result ->
            binding.swipeRefreshHome.isRefreshing = false
            if (result is Resource.Success) {
                val drives = result.data ?: emptyList()
                binding.tvActiveDrivesCount.text = "${drives.size} Active"
                placementDriveAdapter.submitList(drives.take(3))
            }
        }

        homeViewModel.applications.observe(viewLifecycleOwner) { result ->
            if (result is Resource.Success) {
                val apps = result.data ?: emptyList()
                binding.tvApplicationsCount.text = "${apps.size} Applied"
            }
        }

        homeViewModel.announcements.observe(viewLifecycleOwner) { result ->
            if (result is Resource.Success) {
                val announcements = result.data ?: emptyList()
                if (announcements.isNotEmpty()) {
                    val first = announcements.first()
                    binding.cardAnnouncement.visibility = View.VISIBLE
                    binding.tvAnnouncementTitle.text = "📢 " + first.title
                    binding.tvAnnouncementContent.text = first.content
                } else {
                    binding.cardAnnouncement.visibility = View.GONE
                }
            }
        }

        // --- Preserved Community Feed & Events ---
        homeViewModel.postsState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Resource.Loading -> {
                    binding.progressLoading.visibility = View.VISIBLE
                    binding.layoutEmptyFeed.visibility = View.GONE
                }
                is Resource.Success -> {
                    binding.progressLoading.visibility = View.GONE
                    val posts = result.data ?: emptyList()
                    if (posts.isEmpty()) {
                        binding.layoutEmptyFeed.visibility = View.VISIBLE
                        binding.rvFeedPosts.visibility = View.GONE
                    } else {
                        binding.layoutEmptyFeed.visibility = View.GONE
                        binding.rvFeedPosts.visibility = View.VISIBLE
                        postAdapter.submitList(posts)
                    }
                }
                is Resource.Error -> {
                    binding.progressLoading.visibility = View.GONE
                }
            }
        }

        homeViewModel.eventsState.observe(viewLifecycleOwner) { result ->
            if (result is Resource.Success) {
                eventAdapter.submitList(result.data ?: emptyList())
            }
        }
    }

    private fun bindStudentProfile(profile: StudentProfile) {
        binding.tvStudentName.text = profile.fullName
        binding.tvDegreeAndReg.text = "${profile.degree} • ${profile.registrationNumber}"
        binding.tvCgpaValue.text = String.format("%.2f", profile.currentCgpa)
        binding.tvBacklogsValue.text = profile.activeBacklogs.toString()
        binding.tvGradYearValue.text = profile.graduationYear.toString()
        binding.tvVerificationBadge.visibility = if (profile.isVerified) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

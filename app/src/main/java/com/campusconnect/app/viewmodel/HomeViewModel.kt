package com.campusconnect.app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusconnect.app.data.model.*
import com.campusconnect.app.data.repository.CampusRepository
import com.campusconnect.app.data.repository.PlacementRepository
import com.campusconnect.app.utils.Resource
import kotlinx.coroutines.launch

class HomeViewModel(
    private val campusRepository: CampusRepository,
    private val placementRepository: PlacementRepository
) : ViewModel() {

    // --- Placement Platform LiveData ---
    private val _studentProfile = MutableLiveData<Resource<StudentProfile>>()
    val studentProfile: LiveData<Resource<StudentProfile>> = _studentProfile

    private val _placementDrives = MutableLiveData<Resource<List<PlacementDriveModel>>>()
    val placementDrives: LiveData<Resource<List<PlacementDriveModel>>> = _placementDrives

    private val _applications = MutableLiveData<Resource<List<ApplicationModel>>>()
    val applications: LiveData<Resource<List<ApplicationModel>>> = _applications

    private val _announcements = MutableLiveData<Resource<List<AnnouncementModel>>>()
    val announcements: LiveData<Resource<List<AnnouncementModel>>> = _announcements

    // --- Preserved Legacy Community LiveData ---
    private val _postsState = MutableLiveData<Resource<List<Post>>>()
    val postsState: LiveData<Resource<List<Post>>> = _postsState

    private val _eventsState = MutableLiveData<Resource<List<Event>>>()
    val eventsState: LiveData<Resource<List<Event>>> = _eventsState

    private val _selectedCategory = MutableLiveData("All")
    val selectedCategory: LiveData<String> = _selectedCategory

    init {
        loadPlacementDashboard()
    }

    fun loadPlacementDashboard() {
        _studentProfile.value = Resource.Loading()
        _placementDrives.value = Resource.Loading()
        _applications.value = Resource.Loading()
        _announcements.value = Resource.Loading()

        viewModelScope.launch {
            _studentProfile.value = placementRepository.getStudentProfile()
            _placementDrives.value = placementRepository.getPlacementDrives()
            _applications.value = placementRepository.getMyApplications()
            _announcements.value = placementRepository.getAnnouncements()
        }
    }

    fun loadHomeData() {
        loadPlacementDashboard()
        _postsState.value = Resource.Loading()
        _eventsState.value = Resource.Loading()
        viewModelScope.launch {
            _postsState.value = campusRepository.getPosts(category = _selectedCategory.value)
            _eventsState.value = campusRepository.getEvents(category = _selectedCategory.value)
        }
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
        loadHomeData()
    }

    fun toggleLike(postId: String) {
        viewModelScope.launch {
            campusRepository.toggleLikePost(postId)
            loadHomeData()
        }
    }

    fun toggleSave(postId: String) {
        viewModelScope.launch {
            campusRepository.toggleSavePost(postId)
            loadHomeData()
        }
    }
}

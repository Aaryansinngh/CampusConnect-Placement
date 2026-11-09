package com.campusconnect.app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusconnect.app.data.model.Post
import com.campusconnect.app.data.model.StudentProfile
import com.campusconnect.app.data.repository.CampusRepository
import com.campusconnect.app.data.repository.PlacementRepository
import com.campusconnect.app.utils.Resource
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val campusRepository: CampusRepository,
    private val placementRepository: PlacementRepository
) : ViewModel() {

    private val _studentProfile = MutableLiveData<Resource<StudentProfile>>()
    val studentProfile: LiveData<Resource<StudentProfile>> = _studentProfile

    private val _updateState = MutableLiveData<Resource<StudentProfile>?>()
    val updateState: LiveData<Resource<StudentProfile>?> = _updateState

    private val _savedPostsState = MutableLiveData<Resource<List<Post>>>()
    val savedPostsState: LiveData<Resource<List<Post>>> = _savedPostsState

    init {
        loadStudentProfile()
    }

    fun loadStudentProfile() {
        _studentProfile.value = Resource.Loading()
        viewModelScope.launch {
            _studentProfile.value = placementRepository.getStudentProfile()
        }
    }

    fun updateProfile(phone: String?, resumeUrl: String?, skills: List<String>?) {
        _updateState.value = Resource.Loading()
        viewModelScope.launch {
            val res = placementRepository.updateStudentProfile(phone, resumeUrl, skills)
            _updateState.value = res
            if (res is Resource.Success) {
                _studentProfile.value = res
            }
        }
    }

    fun clearUpdateState() {
        _updateState.value = null
    }

    fun loadSavedPosts() {
        _savedPostsState.value = Resource.Loading()
        viewModelScope.launch {
            _savedPostsState.value = campusRepository.getSavedPosts()
        }
    }

    fun unsavePost(postId: String) {
        viewModelScope.launch {
            campusRepository.toggleSavePost(postId)
            loadSavedPosts()
        }
    }
}

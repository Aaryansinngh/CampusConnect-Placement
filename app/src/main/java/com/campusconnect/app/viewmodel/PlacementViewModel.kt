package com.campusconnect.app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusconnect.app.data.model.ApplicationModel
import com.campusconnect.app.data.model.EligibilityCheckResult
import com.campusconnect.app.data.model.InterviewBookingModel
import com.campusconnect.app.data.model.InterviewSlotModel
import com.campusconnect.app.data.model.PlacementDriveModel
import com.campusconnect.app.data.model.RecruitmentRoundModel
import com.campusconnect.app.data.repository.PlacementRepository
import com.campusconnect.app.utils.Resource
import kotlinx.coroutines.launch

class PlacementViewModel(private val repository: PlacementRepository) : ViewModel() {

    private val _drives = MutableLiveData<Resource<List<PlacementDriveModel>>>()
    val drives: LiveData<Resource<List<PlacementDriveModel>>> = _drives

    private val _driveDetail = MutableLiveData<Resource<PlacementDriveModel>>()
    val driveDetail: LiveData<Resource<PlacementDriveModel>> = _driveDetail

    private val _eligibility = MutableLiveData<Resource<EligibilityCheckResult>>()
    val eligibility: LiveData<Resource<EligibilityCheckResult>> = _eligibility

    private val _applicationState = MutableLiveData<Resource<ApplicationModel>?>()
    val applicationState: LiveData<Resource<ApplicationModel>?> = _applicationState

    private val _myApplications = MutableLiveData<Resource<List<ApplicationModel>>>()
    val myApplications: LiveData<Resource<List<ApplicationModel>>> = _myApplications

    private val _withdrawState = MutableLiveData<Resource<ApplicationModel>?>()
    val withdrawState: LiveData<Resource<ApplicationModel>?> = _withdrawState

    fun loadPlacementDrives(query: String? = null) {
        _drives.value = Resource.Loading()
        viewModelScope.launch {
            _drives.value = repository.getPlacementDrives(query)
        }
    }

    fun loadDriveDetail(driveId: String) {
        _driveDetail.value = Resource.Loading()
        _eligibility.value = Resource.Loading()
        viewModelScope.launch {
            _driveDetail.value = repository.getPlacementDriveById(driveId)
            _eligibility.value = repository.checkEligibility(driveId)
        }
    }

    fun submitApplication(driveId: String) {
        _applicationState.value = Resource.Loading()
        viewModelScope.launch {
            val result = repository.applyForDrive(driveId)
            _applicationState.value = result
            if (result is Resource.Success) {
                loadMyApplications()
            }
        }
    }

    fun clearApplicationState() {
        _applicationState.value = null
    }

    fun loadMyApplications() {
        _myApplications.value = Resource.Loading()
        viewModelScope.launch {
            _myApplications.value = repository.getMyApplications()
        }
    }

    fun withdrawApplication(applicationId: String, reason: String? = null) {
        _withdrawState.value = Resource.Loading()
        viewModelScope.launch {
            val res = repository.withdrawApplication(applicationId, reason)
            _withdrawState.value = res
            if (res is Resource.Success) {
                loadMyApplications()
            }
        }
    }

    fun clearWithdrawState() {
        _withdrawState.value = null
    }

    // --- Interview Slots & Booking LiveData ---
    private val _rounds = MutableLiveData<Resource<List<RecruitmentRoundModel>>>()
    val rounds: LiveData<Resource<List<RecruitmentRoundModel>>> = _rounds

    private val _slots = MutableLiveData<Resource<List<InterviewSlotModel>>>()
    val slots: LiveData<Resource<List<InterviewSlotModel>>> = _slots

    private val _bookingState = MutableLiveData<Resource<InterviewBookingModel>?>()
    val bookingState: LiveData<Resource<InterviewBookingModel>?> = _bookingState

    private val _myBookings = MutableLiveData<Resource<List<InterviewBookingModel>>>()
    val myBookings: LiveData<Resource<List<InterviewBookingModel>>> = _myBookings

    fun loadRounds(driveId: String) {
        _rounds.value = Resource.Loading()
        viewModelScope.launch {
            _rounds.value = repository.getRoundsForDrive(driveId)
        }
    }

    fun loadSlots(roundId: String) {
        _slots.value = Resource.Loading()
        viewModelScope.launch {
            _slots.value = repository.getSlotsForRound(roundId)
        }
    }

    fun bookSlot(slotId: String, applicationId: String, notes: String? = null) {
        _bookingState.value = Resource.Loading()
        viewModelScope.launch {
            val res = repository.bookSlot(slotId, applicationId, notes)
            _bookingState.value = res
            if (res is Resource.Success) {
                loadMyApplications()
            }
        }
    }

    fun clearBookingState() {
        _bookingState.value = null
    }

    fun loadMyBookings() {
        _myBookings.value = Resource.Loading()
        viewModelScope.launch {
            _myBookings.value = repository.getMyBookings()
        }
    }
}

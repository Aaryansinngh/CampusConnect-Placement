package com.campusconnect.app.data.repository

import android.content.Context
import android.net.Uri
import com.campusconnect.app.data.firebase.FirebaseManager
import com.campusconnect.app.data.model.*
import com.campusconnect.app.data.remote.ApiClient
import com.campusconnect.app.utils.Resource
import com.campusconnect.app.utils.SampleDataGenerator
import com.campusconnect.app.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

interface PlacementRepository {
    suspend fun getPlacementDrives(query: String? = null): Resource<List<PlacementDriveModel>>
    suspend fun getPlacementDriveById(driveId: String): Resource<PlacementDriveModel>
    suspend fun checkEligibility(driveId: String): Resource<EligibilityCheckResult>
    suspend fun applyForDrive(driveId: String): Resource<ApplicationModel>
    suspend fun getMyApplications(): Resource<List<ApplicationModel>>
    suspend fun withdrawApplication(applicationId: String, reason: String? = null): Resource<ApplicationModel>
    suspend fun getStudentProfile(): Resource<StudentProfile>
    suspend fun updateStudentProfile(phone: String?, resumeUrl: String?, skills: List<String>?): Resource<StudentProfile>
    suspend fun uploadResume(fileUri: Uri, fileName: String): Resource<String>
    suspend fun getAnnouncements(): Resource<List<AnnouncementModel>>
    suspend fun getRoundsForDrive(driveId: String): Resource<List<RecruitmentRoundModel>>
    suspend fun getSlotsForRound(roundId: String): Resource<List<InterviewSlotModel>>
    suspend fun bookSlot(slotId: String, applicationId: String, notes: String? = null): Resource<InterviewBookingModel>
    suspend fun getMyBookings(): Resource<List<InterviewBookingModel>>
    suspend fun cancelBooking(bookingId: String): Resource<InterviewBookingModel>
}

class PlacementRepositoryImpl(context: Context) : PlacementRepository {

    private val appContext = context.applicationContext
    private val apiService = ApiClient.getService(appContext)
    private val sessionManager = SessionManager(appContext)

    override suspend fun getPlacementDrives(query: String?): Resource<List<PlacementDriveModel>> =
        withContext(Dispatchers.IO) {
            // 1. Try FastAPI Backend
            try {
                val response = withTimeoutOrNull(4000L) {
                    apiService.getPlacementDrives()
                }
                if (response != null && response.isSuccessful && response.body() != null) {
                    var list = response.body()!!
                    try {
                        FirebaseManager.syncPlacementDrivesToFirestore(list)
                    } catch (_: Exception) {}

                    if (!query.isNullOrBlank()) {
                        list = list.filter {
                            it.jobTitle.contains(query, ignoreCase = true) ||
                                    it.jobDescription.contains(query, ignoreCase = true) ||
                                    it.companyName.contains(query, ignoreCase = true)
                        }
                    }
                    return@withContext Resource.Success(list)
                }
            } catch (_: Exception) {}

            // 2. Try Firestore Cache
            try {
                val firestoreDrives = FirebaseManager.getPlacementDrivesFromFirestore()
                if (firestoreDrives.isNotEmpty()) {
                    var list = firestoreDrives
                    if (!query.isNullOrBlank()) {
                        list = list.filter {
                            it.jobTitle.contains(query, ignoreCase = true) ||
                                    it.companyName.contains(query, ignoreCase = true)
                        }
                    }
                    return@withContext Resource.Success(list)
                }
            } catch (_: Exception) {}

            // 3. Offline / Demo fallback
            var list = SampleDataGenerator.samplePlacementDrives.toList()
            if (!query.isNullOrBlank()) {
                list = list.filter {
                    it.jobTitle.contains(query, ignoreCase = true) ||
                            it.companyName.contains(query, ignoreCase = true)
                }
            }
            Resource.Success(list)
        }

    override suspend fun getPlacementDriveById(driveId: String): Resource<PlacementDriveModel> =
        withContext(Dispatchers.IO) {
            try {
                val response = withTimeoutOrNull(3000L) {
                    apiService.getPlacementDriveById(driveId)
                }
                if (response != null && response.isSuccessful && response.body() != null) {
                    return@withContext Resource.Success(response.body()!!)
                }
            } catch (_: Exception) {}

            val sample = SampleDataGenerator.samplePlacementDrives.find { it.id == driveId }
            if (sample != null) {
                Resource.Success(sample)
            } else {
                Resource.Error("Placement opportunity not found.")
            }
        }

    override suspend fun checkEligibility(driveId: String): Resource<EligibilityCheckResult> =
        withContext(Dispatchers.IO) {
            try {
                val response = withTimeoutOrNull(3000L) {
                    apiService.checkEligibility(driveId)
                }
                if (response != null && response.isSuccessful && response.body() != null) {
                    return@withContext Resource.Success(response.body()!!)
                }
            } catch (_: Exception) {}

            // Fallback calculation using cached student profile
            val profile = sessionManager.getStudentProfile()
            val drive = SampleDataGenerator.samplePlacementDrives.find { it.id == driveId }
            val criteria = drive?.eligibilityCriteria

            if (profile != null && criteria != null) {
                val reasons = mutableListOf<String>()
                if (profile.currentCgpa < criteria.minCgpa) {
                    reasons.add("Current CGPA (${profile.currentCgpa}) is below required minimum of ${criteria.minCgpa}")
                }
                if (profile.activeBacklogs > criteria.maxBacklogs) {
                    reasons.add("Active backlogs (${profile.activeBacklogs}) exceed maximum limit of ${criteria.maxBacklogs}")
                }
                val isEligible = reasons.isEmpty()
                Resource.Success(EligibilityCheckResult(isEligible, reasons))
            } else {
                Resource.Success(EligibilityCheckResult(true, emptyList()))
            }
        }

    override suspend fun applyForDrive(driveId: String): Resource<ApplicationModel> =
        withContext(Dispatchers.IO) {
            var applicationResult: ApplicationModel? = null

            // 1. Submit to FastAPI Backend
            try {
                val response = withTimeoutOrNull(4000L) {
                    apiService.submitApplication(ApplicationSubmissionRequest(driveId))
                }
                if (response != null && response.isSuccessful && response.body() != null) {
                    applicationResult = response.body()!!
                } else if (response != null && response.code() == 400) {
                    return@withContext Resource.Error("You have already applied for this placement drive.")
                } else if (response != null && response.code() == 403) {
                    return@withContext Resource.Error("Application rejected: You do not meet the minimum eligibility requirements.")
                }
            } catch (_: Exception) {}

            // 2. Fallback / Local Application if backend is offline
            if (applicationResult == null) {
                val drive = SampleDataGenerator.samplePlacementDrives.find { it.id == driveId }
                val existing = SampleDataGenerator.sampleApplications.find { it.driveId == driveId }
                if (existing != null) {
                    return@withContext Resource.Error("You have already submitted an application for this drive.")
                }

                applicationResult = ApplicationModel(
                    id = "app_" + System.currentTimeMillis(),
                    driveId = driveId,
                    studentId = sessionManager.getStudentProfile()?.id ?: sessionManager.getUser()?.uid ?: "student_01",
                    status = "APPLIED",
                    appliedAt = "Just now",
                    driveTitle = drive?.jobTitle ?: "Placement Drive",
                    companyName = drive?.companyName ?: "Campus Partner",
                    salaryPackage = drive?.salaryPackageCtc ?: "Competitive"
                )
                SampleDataGenerator.sampleApplications.add(0, applicationResult)
            }

            // 3. Persist to Firestore applications collection
            try {
                val currentStudent = sessionManager.getStudentProfile()
                FirebaseManager.saveApplicationToFirestore(
                    app = applicationResult,
                    studentName = currentStudent?.fullName ?: sessionManager.getUser()?.name,
                    studentRegNo = currentStudent?.registrationNumber ?: sessionManager.getUser()?.studentId
                )
            } catch (_: Exception) {}

            Resource.Success(applicationResult)
        }

    override suspend fun getMyApplications(): Resource<List<ApplicationModel>> =
        withContext(Dispatchers.IO) {
            val studentId = sessionManager.getStudentProfile()?.id ?: sessionManager.getUser()?.uid ?: ""

            // 1. Try FastAPI
            try {
                val response = withTimeoutOrNull(3000L) {
                    apiService.getMyApplications()
                }
                if (response != null && response.isSuccessful && response.body() != null) {
                    val apiApps = response.body()!!
                    return@withContext Resource.Success(apiApps)
                }
            } catch (_: Exception) {}

            // 2. Try Firestore
            if (studentId.isNotBlank()) {
                try {
                    val firestoreApps = FirebaseManager.getApplicationsFromFirestore(studentId)
                    if (firestoreApps.isNotEmpty()) {
                        return@withContext Resource.Success(firestoreApps)
                    }
                } catch (_: Exception) {}
            }

            // 3. Fallback
            Resource.Success(SampleDataGenerator.sampleApplications.toList())
        }

    override suspend fun withdrawApplication(
        applicationId: String,
        reason: String?
    ): Resource<ApplicationModel> = withContext(Dispatchers.IO) {
        try {
            val response = withTimeoutOrNull(3000L) {
                apiService.withdrawApplication(applicationId, reason)
            }
            if (response != null && response.isSuccessful && response.body() != null) {
                return@withContext Resource.Success(response.body()!!)
            }
        } catch (_: Exception) {}

        val index = SampleDataGenerator.sampleApplications.indexOfFirst { it.id == applicationId }
        if (index != -1) {
            val updated = SampleDataGenerator.sampleApplications[index].copy(
                status = "WITHDRAWN",
                withdrawalReason = reason ?: "Withdrawn by student."
            )
            SampleDataGenerator.sampleApplications[index] = updated
            Resource.Success(updated)
        } else {
            Resource.Error("Application record not found.")
        }
    }

    override suspend fun getStudentProfile(): Resource<StudentProfile> =
        withContext(Dispatchers.IO) {
            val uid = sessionManager.getUser()?.uid ?: ""

            // 1. Try FastAPI
            try {
                val response = withTimeoutOrNull(3000L) {
                    apiService.getStudentProfile()
                }
                if (response != null && response.isSuccessful && response.body() != null) {
                    sessionManager.saveStudentProfile(response.body()!!)
                    return@withContext Resource.Success(response.body()!!)
                }
            } catch (_: Exception) {}

            // 2. Try Firestore
            if (uid.isNotBlank()) {
                try {
                    val fbProfile = FirebaseManager.getStudentProfile(uid)
                    if (fbProfile != null) {
                        sessionManager.saveStudentProfile(fbProfile)
                        return@withContext Resource.Success(fbProfile)
                    }
                } catch (_: Exception) {}
            }

            // 3. Cached / Local Fallback
            val localProfile = sessionManager.getStudentProfile()
                ?: SampleDataGenerator.sampleStudentProfiles.firstOrNull()
                ?: StudentProfile(
                    fullName = "Alex Johnson",
                    registrationNumber = "2026-CS-001",
                    degree = "B.Tech Computer Science",
                    currentCgpa = 9.4,
                    activeBacklogs = 0
                )
            Resource.Success(localProfile)
        }

    override suspend fun updateStudentProfile(
        phone: String?,
        resumeUrl: String?,
        skills: List<String>?
    ): Resource<StudentProfile> = withContext(Dispatchers.IO) {
        val uid = sessionManager.getUser()?.uid ?: ""

        // 1. Update FastAPI
        try {
            val response = withTimeoutOrNull(3000L) {
                apiService.updateStudentProfile(
                    StudentProfileUpdateRequest(phone, resumeUrl, skills)
                )
            }
            if (response != null && response.isSuccessful && response.body() != null) {
                sessionManager.saveStudentProfile(response.body()!!)
                if (uid.isNotBlank()) {
                    try {
                        FirebaseManager.updateStudentProfileInFirestore(uid, phone, resumeUrl, skills)
                    } catch (_: Exception) {}
                }
                return@withContext Resource.Success(response.body()!!)
            }
        } catch (_: Exception) {}

        // 2. Update Firestore directly
        if (uid.isNotBlank()) {
            try {
                FirebaseManager.updateStudentProfileInFirestore(uid, phone, resumeUrl, skills)
            } catch (_: Exception) {}
        }

        // 3. Update Local Cache
        val current = sessionManager.getStudentProfile() ?: StudentProfile(
            id = uid,
            fullName = sessionManager.getUser()?.name ?: "Student",
            registrationNumber = sessionManager.getUser()?.studentId ?: ""
        )
        val updated = current.copy(
            phoneNumber = phone ?: current.phoneNumber,
            resumeUrl = resumeUrl ?: current.resumeUrl,
            skills = skills ?: current.skills
        )
        sessionManager.saveStudentProfile(updated)
        Resource.Success(updated)
    }

    override suspend fun uploadResume(fileUri: Uri, fileName: String): Resource<String> = withContext(Dispatchers.IO) {
        val studentId = sessionManager.getStudentProfile()?.id ?: sessionManager.getUser()?.uid ?: "student_01"
        try {
            val downloadUrl = FirebaseManager.uploadResume(studentId, fileUri, fileName)
            try {
                apiService.updateStudentProfile(StudentProfileUpdateRequest(resumeUrl = downloadUrl))
            } catch (_: Exception) {}

            Resource.Success(downloadUrl)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to upload resume to Firebase Storage.")
        }
    }

    override suspend fun getAnnouncements(): Resource<List<AnnouncementModel>> =
        withContext(Dispatchers.IO) {
            try {
                val response = withTimeoutOrNull(3000L) {
                    apiService.getAnnouncements()
                }
                if (response != null && response.isSuccessful && response.body() != null) {
                    return@withContext Resource.Success(response.body()!!)
                }
            } catch (_: Exception) {}

            Resource.Success(SampleDataGenerator.samplePlacementAnnouncements.toList())
        }

    // --- Interview Slot Booking ---

    override suspend fun getRoundsForDrive(driveId: String): Resource<List<RecruitmentRoundModel>> =
        withContext(Dispatchers.IO) {
            try {
                val response = withTimeoutOrNull(3000L) {
                    apiService.getRoundsForDrive(driveId)
                }
                if (response != null && response.isSuccessful && response.body() != null) {
                    return@withContext Resource.Success(response.body()!!)
                }
            } catch (_: Exception) {}

            Resource.Success(
                listOf(
                    RecruitmentRoundModel(
                        id = "round_default_$driveId",
                        driveId = driveId,
                        roundOrder = 1,
                        roundName = "Technical & Coding Interview",
                        roundType = "TECHNICAL_INTERVIEW",
                        instructions = "Please bring your student ID and ensure a stable network connection."
                    )
                )
            )
        }

    override suspend fun getSlotsForRound(roundId: String): Resource<List<InterviewSlotModel>> =
        withContext(Dispatchers.IO) {
            try {
                val response = withTimeoutOrNull(3000L) {
                    apiService.getSlotsForRound(roundId)
                }
                if (response != null && response.isSuccessful && response.body() != null) {
                    return@withContext Resource.Success(response.body()!!)
                }
            } catch (_: Exception) {}

            Resource.Success(
                listOf(
                    InterviewSlotModel(
                        id = "slot_sample_1",
                        roundId = roundId,
                        startTime = "2026-11-12T10:00:00",
                        endTime = "2026-11-12T10:45:00",
                        venueOrLink = "Virtual: Google Meet",
                        totalCapacity = 1,
                        bookedCount = 0
                    ),
                    InterviewSlotModel(
                        id = "slot_sample_2",
                        roundId = roundId,
                        startTime = "2026-11-12T11:00:00",
                        endTime = "2026-11-12T11:45:00",
                        venueOrLink = "Virtual: Google Meet",
                        totalCapacity = 1,
                        bookedCount = 0
                    )
                )
            )
        }

    override suspend fun bookSlot(
        slotId: String,
        applicationId: String,
        notes: String?
    ): Resource<InterviewBookingModel> = withContext(Dispatchers.IO) {
        try {
            val response = withTimeoutOrNull(4000L) {
                apiService.bookInterviewSlot(SlotBookingRequest(slotId, applicationId, notes))
            }
            if (response != null && response.isSuccessful && response.body() != null) {
                return@withContext Resource.Success(response.body()!!)
            } else if (response != null && response.code() == 409) {
                return@withContext Resource.Error("This interview slot is already fully booked. Please select another slot.")
            } else if (response != null && response.code() == 400) {
                return@withContext Resource.Error("You already have an interview slot booked for this recruitment round.")
            }
        } catch (_: Exception) {}

        // Mock confirmation for demo
        val booking = InterviewBookingModel(
            id = "booking_" + System.currentTimeMillis(),
            slotId = slotId,
            applicationId = applicationId,
            studentId = sessionManager.getStudentProfile()?.id ?: "student_01",
            status = "CONFIRMED",
            bookedAt = "Just now",
            notes = notes
        )
        Resource.Success(booking)
    }

    override suspend fun getMyBookings(): Resource<List<InterviewBookingModel>> =
        withContext(Dispatchers.IO) {
            try {
                val response = withTimeoutOrNull(3000L) {
                    apiService.getMyInterviewBookings()
                }
                if (response != null && response.isSuccessful && response.body() != null) {
                    return@withContext Resource.Success(response.body()!!)
                }
            } catch (_: Exception) {}

            Resource.Success(emptyList())
        }

    override suspend fun cancelBooking(bookingId: String): Resource<InterviewBookingModel> =
        withContext(Dispatchers.IO) {
            try {
                val response = withTimeoutOrNull(3000L) {
                    apiService.cancelInterviewBooking(bookingId)
                }
                if (response != null && response.isSuccessful && response.body() != null) {
                    return@withContext Resource.Success(response.body()!!)
                }
            } catch (_: Exception) {}

            Resource.Error("Could not cancel interview booking.")
        }
}

package com.campusconnect.app.data.repository

import android.content.Context
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
            try {
                val response = withTimeoutOrNull(4000L) {
                    apiService.getPlacementDrives()
                }
                if (response != null && response.isSuccessful && response.body() != null) {
                    var list = response.body()!!
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

            // Offline / Demo fallback
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
            try {
                val response = withTimeoutOrNull(4000L) {
                    apiService.submitApplication(ApplicationSubmissionRequest(driveId))
                }
                if (response != null && response.isSuccessful && response.body() != null) {
                    return@withContext Resource.Success(response.body()!!)
                } else if (response != null && response.code() == 400) {
                    return@withContext Resource.Error("You have already applied for this placement drive.")
                } else if (response != null && response.code() == 403) {
                    return@withContext Resource.Error("Application rejected: You do not meet the minimum eligibility requirements.")
                }
            } catch (_: Exception) {}

            // Offline Demo Mock Application
            val drive = SampleDataGenerator.samplePlacementDrives.find { it.id == driveId }
            val existing = SampleDataGenerator.sampleApplications.find { it.driveId == driveId }
            if (existing != null) {
                return@withContext Resource.Error("You have already submitted an application for this drive.")
            }

            val newApp = ApplicationModel(
                id = "app_" + System.currentTimeMillis(),
                driveId = driveId,
                studentId = sessionManager.getStudentProfile()?.id ?: "student_01",
                status = "APPLIED",
                appliedAt = "Just now",
                driveTitle = drive?.jobTitle ?: "Placement Drive",
                companyName = drive?.companyName ?: "Campus Partner",
                salaryPackage = drive?.salaryPackageCtc ?: "Competitive"
            )
            SampleDataGenerator.sampleApplications.add(0, newApp)
            Resource.Success(newApp)
        }

    override suspend fun getMyApplications(): Resource<List<ApplicationModel>> =
        withContext(Dispatchers.IO) {
            try {
                val response = withTimeoutOrNull(3000L) {
                    apiService.getMyApplications()
                }
                if (response != null && response.isSuccessful && response.body() != null) {
                    return@withContext Resource.Success(response.body()!!)
                }
            } catch (_: Exception) {}

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
            try {
                val response = withTimeoutOrNull(3000L) {
                    apiService.getStudentProfile()
                }
                if (response != null && response.isSuccessful && response.body() != null) {
                    sessionManager.saveStudentProfile(response.body()!!)
                    return@withContext Resource.Success(response.body()!!)
                }
            } catch (_: Exception) {}

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
        try {
            val response = withTimeoutOrNull(3000L) {
                apiService.updateStudentProfile(
                    StudentProfileUpdateRequest(phone, resumeUrl, skills)
                )
            }
            if (response != null && response.isSuccessful && response.body() != null) {
                sessionManager.saveStudentProfile(response.body()!!)
                return@withContext Resource.Success(response.body()!!)
            }
        } catch (_: Exception) {}

        val current = sessionManager.getStudentProfile() ?: StudentProfile()
        val updated = current.copy(
            phoneNumber = phone ?: current.phoneNumber,
            resumeUrl = resumeUrl ?: current.resumeUrl,
            skills = skills ?: current.skills
        )
        sessionManager.saveStudentProfile(updated)
        Resource.Success(updated)
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

            val mockRound = RecruitmentRoundModel(
                id = "round_default_$driveId",
                driveId = driveId,
                roundOrder = 1,
                roundName = "Technical & Architecture Interview",
                roundType = "TECHNICAL_INTERVIEW",
                venue = "Google Meet Virtual Room",
                instructions = "Please join 5 minutes prior with camera on and code editor ready."
            )
            Resource.Success(listOf(mockRound))
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

            val mockSlots = listOf(
                InterviewSlotModel(
                    id = "slot_1_$roundId",
                    roundId = roundId,
                    startTime = "10:00 AM",
                    endTime = "10:45 AM",
                    venueOrLink = "https://meet.google.com/xyz-placement",
                    totalCapacity = 1,
                    bookedCount = 0
                ),
                InterviewSlotModel(
                    id = "slot_2_$roundId",
                    roundId = roundId,
                    startTime = "11:30 AM",
                    endTime = "12:15 PM",
                    venueOrLink = "https://meet.google.com/xyz-placement",
                    totalCapacity = 1,
                    bookedCount = 0
                ),
                InterviewSlotModel(
                    id = "slot_3_$roundId",
                    roundId = roundId,
                    startTime = "02:00 PM",
                    endTime = "02:45 PM",
                    venueOrLink = "https://meet.google.com/xyz-placement",
                    totalCapacity = 1,
                    bookedCount = 1
                ),
                InterviewSlotModel(
                    id = "slot_4_$roundId",
                    roundId = roundId,
                    startTime = "03:30 PM",
                    endTime = "04:15 PM",
                    venueOrLink = "https://meet.google.com/xyz-placement",
                    totalCapacity = 1,
                    bookedCount = 0
                )
            )
            Resource.Success(mockSlots)
        }

    override suspend fun bookSlot(
        slotId: String,
        applicationId: String,
        notes: String?
    ): Resource<InterviewBookingModel> = withContext(Dispatchers.IO) {
        try {
            val response = withTimeoutOrNull(4000L) {
                apiService.bookInterviewSlot(
                    SlotBookingRequest(
                        slotId = slotId,
                        applicationId = applicationId,
                        notes = notes
                    )
                )
            }
            if (response != null && response.isSuccessful && response.body() != null) {
                return@withContext Resource.Success(response.body()!!)
            } else if (response != null && response.code() == 409) {
                return@withContext Resource.Error("This interview slot has already been booked by another student.")
            } else if (response != null && response.code() == 400) {
                return@withContext Resource.Error("You already have an active interview slot booked for this round.")
            }
        } catch (_: Exception) {}

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

            val cancelled = InterviewBookingModel(
                id = bookingId,
                status = "CANCELLED"
            )
            Resource.Success(cancelled)
        }
}

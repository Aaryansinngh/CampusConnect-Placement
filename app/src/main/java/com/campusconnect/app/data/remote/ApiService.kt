package com.campusconnect.app.data.remote

import com.campusconnect.app.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // --- Authentication ---
    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<TokenResponse>

    @POST("auth/register/student")
    suspend fun registerStudent(
        @Body request: StudentRegisterRequest
    ): Response<CurrentUserResponse>

    @GET("auth/me")
    suspend fun getCurrentUser(): Response<CurrentUserResponse>

    @POST("auth/logout")
    suspend fun logout(): Response<Map<String, String>>

    // --- Student Profile ---
    @GET("students/profile")
    suspend fun getStudentProfile(): Response<StudentProfile>

    @PUT("students/profile")
    suspend fun updateStudentProfile(
        @Body request: StudentProfileUpdateRequest
    ): Response<StudentProfile>

    // --- Placement Drives ---
    @GET("placement-drives")
    suspend fun getPlacementDrives(
        @Query("status_filter") statusFilter: String? = null
    ): Response<List<PlacementDriveModel>>

    @GET("placement-drives/{id}")
    suspend fun getPlacementDriveById(
        @Path("id") driveId: String
    ): Response<PlacementDriveModel>

    // --- Eligibility Engine ---
    @GET("eligibility/check/{drive_id}")
    suspend fun checkEligibility(
        @Path("drive_id") driveId: String
    ): Response<EligibilityCheckResult>

    // --- Applications ---
    @POST("applications/")
    suspend fun submitApplication(
        @Body request: ApplicationSubmissionRequest
    ): Response<ApplicationModel>

    @GET("applications/my-applications")
    suspend fun getMyApplications(): Response<List<ApplicationModel>>

    @DELETE("applications/{id}")
    suspend fun withdrawApplication(
        @Path("id") applicationId: String,
        @Query("reason") reason: String? = null
    ): Response<ApplicationModel>

    // --- Announcements ---
    @GET("admin/announcements")
    suspend fun getAnnouncements(): Response<List<AnnouncementModel>>

    // --- Interviews & Slot Booking ---
    @GET("interviews/rounds/drive/{drive_id}")
    suspend fun getRoundsForDrive(
        @Path("drive_id") driveId: String
    ): Response<List<RecruitmentRoundModel>>

    @GET("interviews/slots/round/{round_id}")
    suspend fun getSlotsForRound(
        @Path("round_id") roundId: String
    ): Response<List<InterviewSlotModel>>

    @POST("interviews/bookings")
    suspend fun bookInterviewSlot(
        @Body request: SlotBookingRequest
    ): Response<InterviewBookingModel>

    @GET("interviews/bookings/my-bookings")
    suspend fun getMyInterviewBookings(): Response<List<InterviewBookingModel>>

    @DELETE("interviews/bookings/{id}")
    suspend fun cancelInterviewBooking(
        @Path("id") bookingId: String
    ): Response<InterviewBookingModel>
}

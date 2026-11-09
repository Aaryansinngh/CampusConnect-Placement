package com.campusconnect.app.data.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class RecruitmentRoundModel(
    @SerializedName("id") val id: String = "",
    @SerializedName("drive_id") val driveId: String = "",
    @SerializedName("round_order") val roundOrder: Int = 1,
    @SerializedName("round_name") val roundName: String = "",
    @SerializedName("round_type") val roundType: String = "TECHNICAL_INTERVIEW",
    @SerializedName("scheduled_date") val scheduledDate: String? = null,
    @SerializedName("venue") val venue: String? = null,
    @SerializedName("instructions") val instructions: String? = null
) : Serializable

data class InterviewSlotModel(
    @SerializedName("id") val id: String = "",
    @SerializedName("round_id") val roundId: String = "",
    @SerializedName("recruiter_id") val recruiterId: String? = null,
    @SerializedName("start_time") val startTime: String = "",
    @SerializedName("end_time") val endTime: String = "",
    @SerializedName("venue_or_link") val venueOrLink: String = "",
    @SerializedName("total_capacity") val totalCapacity: Int = 1,
    @SerializedName("booked_count") val bookedCount: Int = 0,
    @SerializedName("is_cancelled") val isCancelled: Boolean = false
) : Serializable {
    val isAvailable: Boolean
        get() = !isCancelled && bookedCount < totalCapacity
}

data class SlotBookingRequest(
    @SerializedName("slot_id") val slotId: String,
    @SerializedName("application_id") val applicationId: String,
    @SerializedName("notes") val notes: String? = null
)

data class InterviewBookingModel(
    @SerializedName("id") val id: String = "",
    @SerializedName("slot_id") val slotId: String = "",
    @SerializedName("application_id") val applicationId: String = "",
    @SerializedName("student_id") val studentId: String = "",
    @SerializedName("status") val status: String = "CONFIRMED",
    @SerializedName("booked_at") val bookedAt: String = "",
    @SerializedName("notes") val notes: String? = null,
    // Transient presentation properties
    var roundName: String? = null,
    var companyName: String? = null,
    var startTimeFormatted: String? = null
) : Serializable

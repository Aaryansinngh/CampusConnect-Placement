package com.campusconnect.app.data.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class StudentProfile(
    @SerializedName("id") val id: String = "",
    @SerializedName("user_id") val userId: String = "",
    @SerializedName("university_id") val universityId: String = "",
    @SerializedName("department_id") val departmentId: String = "",
    @SerializedName("full_name") val fullName: String = "",
    @SerializedName("registration_number") val registrationNumber: String = "",
    @SerializedName("degree") val degree: String = "B.Tech",
    @SerializedName("graduation_year") val graduationYear: Int = 2026,
    @SerializedName("current_cgpa") val currentCgpa: Double = 0.0,
    @SerializedName("active_backlogs") val activeBacklogs: Int = 0,
    @SerializedName("skills") val skills: List<String> = emptyList(),
    @SerializedName("phone_number") val phoneNumber: String? = null,
    @SerializedName("resume_url") val resumeUrl: String? = null,
    @SerializedName("is_verified") val isVerified: Boolean = false
) : Serializable

data class StudentProfileUpdateRequest(
    @SerializedName("phone_number") val phoneNumber: String? = null,
    @SerializedName("resume_url") val resumeUrl: String? = null,
    @SerializedName("skills") val skills: List<String>? = null
)

data class EligibilityCriteriaModel(
    @SerializedName("id") val id: String = "",
    @SerializedName("drive_id") val driveId: String = "",
    @SerializedName("min_cgpa") val minCgpa: Double = 6.0,
    @SerializedName("max_backlogs") val maxBacklogs: Int = 0,
    @SerializedName("eligible_department_ids") val eligibleDepartmentIds: List<String> = emptyList(),
    @SerializedName("eligible_graduation_years") val eligibleGraduationYears: List<Int> = emptyList(),
    @SerializedName("required_skills") val requiredSkills: List<String> = emptyList(),
    @SerializedName("min_attendance_percentage") val minAttendancePercentage: Double? = 75.0
) : Serializable

data class PlacementDriveModel(
    @SerializedName("id") val id: String = "",
    @SerializedName("university_id") val universityId: String = "",
    @SerializedName("company_id") val companyId: String = "",
    @SerializedName("job_title") val jobTitle: String = "",
    @SerializedName("job_description") val jobDescription: String = "",
    @SerializedName("salary_package_ctc") val salaryPackageCtc: String = "",
    @SerializedName("job_location") val jobLocation: String = "",
    @SerializedName("employment_type") val employmentType: String = "FULL_TIME",
    @SerializedName("vacancies") val vacancies: Int = 1,
    @SerializedName("status") val status: String = "ACTIVE",
    @SerializedName("application_deadline") val applicationDeadline: String = "",
    @SerializedName("eligibility_criteria") val eligibilityCriteria: EligibilityCriteriaModel? = null,
    // Client-side transient helpers for company display
    var companyName: String = "Technology Partner",
    var companyLogoUrl: String = "",
    var isStudentEligible: Boolean? = null
) : Serializable

data class EligibilityCheckResult(
    @SerializedName("is_eligible") val isEligible: Boolean = false,
    @SerializedName("reasons") val reasons: List<String> = emptyList(),
    @SerializedName("evaluation_details") val evaluationDetails: Map<String, Any> = emptyMap()
)

data class ApplicationSubmissionRequest(
    @SerializedName("drive_id") val driveId: String
)

data class ApplicationModel(
    @SerializedName("id") val id: String = "",
    @SerializedName("drive_id") val driveId: String = "",
    @SerializedName("student_id") val studentId: String = "",
    @SerializedName("status") val status: String = "APPLIED",
    @SerializedName("applied_at") val appliedAt: String = "",
    @SerializedName("withdrawal_reason") val withdrawalReason: String? = null,
    // Transient drive preview
    var driveTitle: String? = null,
    var companyName: String? = null,
    var salaryPackage: String? = null
) : Serializable

data class AnnouncementModel(
    @SerializedName("id") val id: String = "",
    @SerializedName("university_id") val universityId: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("content") val content: String = "",
    @SerializedName("target_departments") val targetDepartments: List<String> = emptyList(),
    @SerializedName("target_batches") val targetBatches: List<Int> = emptyList()
) : Serializable

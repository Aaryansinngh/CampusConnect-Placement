package com.campusconnect.app.data.model

import com.google.gson.annotations.SerializedName

data class TokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String,
    @SerializedName("token_type") val tokenType: String = "bearer",
    @SerializedName("expires_in") val expiresIn: Int = 3600
)

data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class StudentRegisterRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("full_name") val fullName: String,
    @SerializedName("university_code") val universityCode: String,
    @SerializedName("department_code") val departmentCode: String,
    @SerializedName("registration_number") val registrationNumber: String,
    @SerializedName("degree") val degree: String = "B.Tech",
    @SerializedName("graduation_year") val graduationYear: Int,
    @SerializedName("current_cgpa") val currentCgpa: Double,
    @SerializedName("active_backlogs") val activeBacklogs: Int = 0,
    @SerializedName("skills") val skills: List<String> = emptyList(),
    @SerializedName("phone_number") val phoneNumber: String? = null
)

data class CurrentUserResponse(
    @SerializedName("id") val id: String,
    @SerializedName("email") val email: String,
    @SerializedName("role") val role: String,
    @SerializedName("university_id") val universityId: String?,
    @SerializedName("is_active") val isActive: Boolean,
    @SerializedName("is_verified") val isVerified: Boolean,
    @SerializedName("student") val student: StudentProfile? = null
)

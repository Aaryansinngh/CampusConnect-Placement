package com.campusconnect.app.data.repository

import android.content.Context
import com.campusconnect.app.data.model.LoginRequest
import com.campusconnect.app.data.model.StudentProfile
import com.campusconnect.app.data.model.User
import com.campusconnect.app.data.remote.ApiClient
import com.campusconnect.app.utils.Resource
import com.campusconnect.app.utils.SampleDataGenerator
import com.campusconnect.app.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

interface AuthRepository {
    suspend fun login(email: String, pass: String): Resource<User>
    suspend fun signup(
        name: String,
        email: String,
        pass: String,
        studentId: String
    ): Resource<User>

    suspend fun getCurrentUser(): User?
    suspend fun getStudentProfile(): StudentProfile?
    suspend fun logout()
    suspend fun switchDemoRole(role: String): User
}

class AuthRepositoryImpl(context: Context) : AuthRepository {

    private val appContext = context.applicationContext
    private val sessionManager = SessionManager(appContext)
    private val apiService = ApiClient.getService(appContext)

    override suspend fun login(email: String, pass: String): Resource<User> = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank() || pass.isBlank()) {
            return@withContext Resource.Error("Email and password cannot be empty.")
        }

        // 1. Try FastAPI Backend Authentication
        try {
            val response = withTimeoutOrNull(4000L) {
                apiService.login(LoginRequest(trimmedEmail, pass))
            }

            if (response != null && response.isSuccessful) {
                val tokenBody = response.body()
                if (tokenBody != null) {
                    sessionManager.saveAuthTokens(tokenBody.accessToken, tokenBody.refreshToken)

                    // Fetch user and student profile
                    val meResponse = apiService.getCurrentUser()
                    if (meResponse.isSuccessful && meResponse.body() != null) {
                        val me = meResponse.body()!!
                        val user = User(
                            uid = me.id,
                            name = me.student?.fullName ?: me.email.substringBefore("@"),
                            email = me.email,
                            role = me.role,
                            studentId = me.student?.registrationNumber ?: ""
                        )
                        sessionManager.saveUser(user)
                        if (me.student != null) {
                            sessionManager.saveStudentProfile(me.student)
                        }
                        return@withContext Resource.Success(user)
                    }
                }
            } else if (response != null && response.code() == 401) {
                return@withContext Resource.Error("Invalid email or password.")
            }
        } catch (_: Exception) {
            // Backend offline or timeout -> Proceed to fallback
        }

        // 2. Demo & Offline Fallback Mode
        val sampleUser = SampleDataGenerator.sampleUsers.find {
            it.email.equals(trimmedEmail, ignoreCase = true)
        }
        val resultUser = if (sampleUser != null) {
            sampleUser
        } else {
            User(
                uid = "demo_" + System.currentTimeMillis(),
                name = trimmedEmail.substringBefore("@").replace(".", " ")
                    .replaceFirstChar { it.uppercase() },
                email = trimmedEmail,
                role = User.ROLE_STUDENT,
                studentId = "2026-CS-001"
            )
        }

        sessionManager.saveUser(resultUser)
        val defaultProfile = SampleDataGenerator.sampleStudentProfiles.firstOrNull() ?: StudentProfile(
            id = "student_alex_01",
            fullName = resultUser.name,
            registrationNumber = resultUser.studentId,
            currentCgpa = 9.4,
            activeBacklogs = 0
        )
        sessionManager.saveStudentProfile(defaultProfile)

        Resource.Success(resultUser)
    }

    override suspend fun signup(
        name: String,
        email: String,
        pass: String,
        studentId: String
    ): Resource<User> = withContext(Dispatchers.IO) {
        if (name.isBlank() || email.isBlank() || pass.isBlank() || studentId.isBlank()) {
            return@withContext Resource.Error("Please fill out all required fields.")
        }
        if (pass.length < 6) {
            return@withContext Resource.Error("Password must be at least 6 characters.")
        }

        val newUser = User(
            uid = "user_" + System.currentTimeMillis(),
            name = name.trim(),
            email = email.trim(),
            role = User.ROLE_STUDENT,
            studentId = studentId.trim()
        )
        sessionManager.saveUser(newUser)

        val newProfile = StudentProfile(
            id = "st_" + System.currentTimeMillis(),
            userId = newUser.uid,
            fullName = newUser.name,
            registrationNumber = newUser.studentId,
            currentCgpa = 8.0,
            activeBacklogs = 0,
            isVerified = false
        )
        sessionManager.saveStudentProfile(newProfile)

        Resource.Success(newUser)
    }

    override suspend fun getCurrentUser(): User? {
        return sessionManager.getUser()
    }

    override suspend fun getStudentProfile(): StudentProfile? {
        return sessionManager.getStudentProfile()
    }

    override suspend fun logout() = withContext(Dispatchers.IO) {
        try {
            apiService.logout()
        } catch (_: Exception) {}
        sessionManager.logout()
    }

    override suspend fun switchDemoRole(role: String): User {
        val current = sessionManager.getUser()
        val updated = (current ?: User(
            uid = "demo_switch",
            name = "Demo User",
            email = "demo@apex.edu",
            studentId = "2026-CS-001"
        )).copy(role = role)

        sessionManager.saveUser(updated)
        return updated
    }
}

package com.campusconnect.app.data.repository

import android.content.Context
import com.campusconnect.app.data.firebase.FirebaseManager
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

        var loggedInUser: User? = null
        var studentProfile: StudentProfile? = null

        // 1. Authenticate with FastAPI Backend
        try {
            val response = withTimeoutOrNull(4000L) {
                apiService.login(LoginRequest(trimmedEmail, pass))
            }

            if (response != null && response.isSuccessful) {
                val tokenBody = response.body()
                if (tokenBody != null) {
                    sessionManager.saveAuthTokens(tokenBody.accessToken, tokenBody.refreshToken)

                    val meResponse = apiService.getCurrentUser()
                    if (meResponse.isSuccessful && meResponse.body() != null) {
                        val me = meResponse.body()!!
                        loggedInUser = User(
                            uid = me.id,
                            name = me.student?.fullName ?: me.email.substringBefore("@"),
                            email = me.email,
                            role = me.role,
                            studentId = me.student?.registrationNumber ?: ""
                        )
                        studentProfile = me.student
                    }
                }
            } else if (response != null && response.code() == 401) {
                return@withContext Resource.Error("Invalid email or password.")
            }
        } catch (_: Exception) {
            // Backend offline or timeout
        }

        // 2. Synchronize with Firebase Auth & Firestore
        try {
            val (fbUser, fbProfile) = FirebaseManager.loginUser(trimmedEmail, pass)
            if (loggedInUser == null) {
                loggedInUser = fbUser
            }
            if (studentProfile == null && fbProfile != null) {
                studentProfile = fbProfile
            }
        } catch (_: Exception) {
            // Firebase Auth failed or offline
        }

        // 3. Fallback to Demo/Cached credentials if both network services are offline
        if (loggedInUser == null) {
            val sampleUser = SampleDataGenerator.sampleUsers.find {
                it.email.equals(trimmedEmail, ignoreCase = true)
            }
            loggedInUser = sampleUser ?: User(
                uid = "demo_" + System.currentTimeMillis(),
                name = trimmedEmail.substringBefore("@").replace(".", " ")
                    .replaceFirstChar { it.uppercase() },
                email = trimmedEmail,
                role = User.ROLE_STUDENT,
                studentId = "2026-CS-001"
            )
        }

        if (studentProfile == null) {
            studentProfile = SampleDataGenerator.sampleStudentProfiles.firstOrNull() ?: StudentProfile(
                id = loggedInUser.uid,
                fullName = loggedInUser.name,
                registrationNumber = loggedInUser.studentId,
                currentCgpa = 8.5,
                activeBacklogs = 0
            )
        }

        // Save session locally
        sessionManager.saveUser(loggedInUser)
        sessionManager.saveStudentProfile(studentProfile)

        Resource.Success(loggedInUser)
    }

    override suspend fun signup(
        name: String,
        email: String,
        pass: String,
        studentId: String
    ): Resource<User> = withContext(Dispatchers.IO) {
        val trimmedName = name.trim()
        val trimmedEmail = email.trim()
        val trimmedId = studentId.trim()

        if (trimmedName.isBlank() || trimmedEmail.isBlank() || pass.isBlank() || trimmedId.isBlank()) {
            return@withContext Resource.Error("Please fill out all required fields.")
        }
        if (pass.length < 6) {
            return@withContext Resource.Error("Password must be at least 6 characters.")
        }

        var createdUser: User? = null
        var createdProfile: StudentProfile? = null

        // 1. Create account in Firebase Auth & Firestore
        try {
            val (fbUser, fbProfile) = FirebaseManager.registerUser(
                name = trimmedName,
                email = trimmedEmail,
                pass = pass,
                studentId = trimmedId,
                role = User.ROLE_STUDENT
            )
            createdUser = fbUser
            createdProfile = fbProfile
        } catch (e: Exception) {
            val errorMsg = e.message ?: "Registration failed."
            if (errorMsg.contains("email address is already in use", ignoreCase = true)) {
                return@withContext Resource.Error("An account with this email already exists.")
            }
            // If offline, continue to local registration
        }

        // 2. Fallback / Local creation if Firebase is offline
        if (createdUser == null) {
            val uid = "user_" + System.currentTimeMillis()
            createdUser = User(
                uid = uid,
                name = trimmedName,
                email = trimmedEmail,
                role = User.ROLE_STUDENT,
                studentId = trimmedId
            )
            createdProfile = StudentProfile(
                id = uid,
                userId = uid,
                fullName = trimmedName,
                registrationNumber = trimmedId,
                currentCgpa = 8.0,
                activeBacklogs = 0,
                isVerified = false
            )
        }

        sessionManager.saveUser(createdUser)
        if (createdProfile != null) {
            sessionManager.saveStudentProfile(createdProfile)
        }

        Resource.Success(createdUser)
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
        try {
            FirebaseManager.auth.signOut()
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

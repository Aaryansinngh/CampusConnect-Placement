package com.campusconnect.app.utils

import android.content.Context
import android.content.SharedPreferences
import com.campusconnect.app.data.model.StudentProfile
import com.campusconnect.app.data.model.User
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SessionManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("campus_connect_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveAuthTokens(accessToken: String, refreshToken: String) {
        prefs.edit().apply {
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            apply()
        }
    }

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    fun saveUser(user: User) {
        prefs.edit().apply {
            putString(KEY_UID, user.uid)
            putString(KEY_NAME, user.name)
            putString(KEY_EMAIL, user.email)
            putString(KEY_ROLE, user.role)
            putString(KEY_STUDENT_ID, user.studentId)
            putString(KEY_PROFILE_IMAGE, user.profileImageUrl)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }

    fun getUser(): User? {
        if (!isLoggedIn()) return null
        return User(
            uid = prefs.getString(KEY_UID, "") ?: "",
            name = prefs.getString(KEY_NAME, "Student User") ?: "Student User",
            email = prefs.getString(KEY_EMAIL, "student@university.edu") ?: "student@university.edu",
            role = prefs.getString(KEY_ROLE, User.ROLE_STUDENT) ?: User.ROLE_STUDENT,
            studentId = prefs.getString(KEY_STUDENT_ID, "STU12345") ?: "STU12345",
            profileImageUrl = prefs.getString(KEY_PROFILE_IMAGE, "") ?: ""
        )
    }

    fun saveStudentProfile(profile: StudentProfile) {
        prefs.edit().apply {
            putString(KEY_STUDENT_ID, profile.registrationNumber)
            putString(KEY_NAME, profile.fullName)
            putString(KEY_STUDENT_PROFILE_JSON, gson.toJson(profile))
            apply()
        }
    }

    fun getStudentProfile(): StudentProfile? {
        val json = prefs.getString(KEY_STUDENT_PROFILE_JSON, null) ?: return null
        return try {
            gson.fromJson(json, StudentProfile::class.java)
        } catch (_: Exception) {
            null
        }
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun logout() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_IS_LOGGED_IN = "KEY_IS_LOGGED_IN"
        private const val KEY_ACCESS_TOKEN = "KEY_ACCESS_TOKEN"
        private const val KEY_REFRESH_TOKEN = "KEY_REFRESH_TOKEN"
        private const val KEY_UID = "KEY_UID"
        private const val KEY_NAME = "KEY_NAME"
        private const val KEY_EMAIL = "KEY_EMAIL"
        private const val KEY_ROLE = "KEY_ROLE"
        private const val KEY_STUDENT_ID = "KEY_STUDENT_ID"
        private const val KEY_PROFILE_IMAGE = "KEY_PROFILE_IMAGE"
        private const val KEY_STUDENT_PROFILE_JSON = "KEY_STUDENT_PROFILE_JSON"
    }
}

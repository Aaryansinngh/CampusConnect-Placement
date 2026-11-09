package com.campusconnect.app.data.firebase

import android.net.Uri
import com.campusconnect.app.data.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

object FirebaseManager {

    const val COLLECTION_USERS = "users"
    const val COLLECTION_STUDENTS = "students"
    const val COLLECTION_RECRUITERS = "recruiters"
    const val COLLECTION_PLACEMENT_DRIVES = "placement_drives"
    const val COLLECTION_APPLICATIONS = "applications"
    const val COLLECTION_ACADEMIC_RECORDS = "academic_records"
    const val COLLECTION_RESUMES = "resumes"
    const val COLLECTION_NOTIFICATIONS = "notifications"
    const val COLLECTION_APPROVALS = "approvals"

    val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance().apply {
            firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
        }
    }

    val storage: FirebaseStorage by lazy {
        FirebaseStorage.getInstance()
    }

    val currentFirebaseUser: FirebaseUser?
        get() = auth.currentUser

    val currentUserId: String?
        get() = auth.currentUser?.uid

    // --- Authentication & User Profile ---

    suspend fun registerUser(
        name: String,
        email: String,
        pass: String,
        studentId: String,
        role: String = User.ROLE_STUDENT
    ): Pair<User, StudentProfile> = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim()
        val authResult = auth.createUserWithEmailAndPassword(trimmedEmail, pass).await()
        val fbUser = authResult.user ?: throw IllegalStateException("Firebase user creation failed.")
        val uid = fbUser.uid

        val user = User(
            uid = uid,
            name = name.trim(),
            email = trimmedEmail,
            role = role,
            studentId = studentId.trim(),
            createdAt = System.currentTimeMillis()
        )

        // 1. Save in Firestore users collection
        val userMap = hashMapOf(
            "uid" to user.uid,
            "name" to user.name,
            "email" to user.email,
            "role" to user.role,
            "studentId" to user.studentId,
            "createdAt" to user.createdAt
        )
        firestore.collection(COLLECTION_USERS).document(uid).set(userMap).await()

        // 2. Save in Firestore students collection
        val studentProfile = StudentProfile(
            id = uid,
            userId = uid,
            departmentId = "dept_cse",
            fullName = user.name,
            registrationNumber = user.studentId,
            degree = "B.Tech",
            graduationYear = 2026,
            currentCgpa = 8.5,
            activeBacklogs = 0,
            skills = listOf("Java", "Kotlin", "Problem Solving"),
            isVerified = false
        )

        val studentMap = hashMapOf(
            "id" to studentProfile.id,
            "userId" to studentProfile.userId,
            "fullName" to studentProfile.fullName,
            "registrationNumber" to studentProfile.registrationNumber,
            "departmentId" to studentProfile.departmentId,
            "degree" to studentProfile.degree,
            "graduationYear" to studentProfile.graduationYear,
            "currentCgpa" to studentProfile.currentCgpa,
            "activeBacklogs" to studentProfile.activeBacklogs,
            "skills" to studentProfile.skills,
            "resumeUrl" to (studentProfile.resumeUrl ?: ""),
            "phoneNumber" to (studentProfile.phoneNumber ?: ""),
            "isVerified" to studentProfile.isVerified,
            "updatedAt" to System.currentTimeMillis()
        )
        firestore.collection(COLLECTION_STUDENTS).document(uid).set(studentMap).await()

        // 3. Save initial academic record
        val academicRecordMap = hashMapOf(
            "studentId" to uid,
            "registrationNumber" to studentProfile.registrationNumber,
            "cgpa" to studentProfile.currentCgpa,
            "activeBacklogs" to studentProfile.activeBacklogs,
            "graduationYear" to studentProfile.graduationYear,
            "degree" to studentProfile.degree,
            "departmentId" to studentProfile.departmentId,
            "isVerified" to false,
            "createdAt" to System.currentTimeMillis()
        )
        firestore.collection(COLLECTION_ACADEMIC_RECORDS).document(uid).set(academicRecordMap).await()

        // 4. Send initial welcome notification
        sendNotification(
            userId = uid,
            title = "Welcome to CampusConnect!",
            message = "Your placement profile has been registered. Explore active drives to get started.",
            type = "WELCOME"
        )

        Pair(user, studentProfile)
    }

    suspend fun loginUser(email: String, pass: String): Pair<User, StudentProfile?> = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim()
        val authResult = auth.signInWithEmailAndPassword(trimmedEmail, pass).await()
        val fbUser = authResult.user ?: throw IllegalStateException("Firebase sign in failed.")
        val uid = fbUser.uid

        val userDoc = firestore.collection(COLLECTION_USERS).document(uid).get().await()
        val user = if (userDoc.exists()) {
            User(
                uid = uid,
                name = userDoc.getString("name") ?: trimmedEmail.substringBefore("@"),
                email = userDoc.getString("email") ?: trimmedEmail,
                role = userDoc.getString("role") ?: User.ROLE_STUDENT,
                studentId = userDoc.getString("studentId") ?: "",
                createdAt = userDoc.getLong("createdAt") ?: System.currentTimeMillis()
            )
        } else {
            User(
                uid = uid,
                name = trimmedEmail.substringBefore("@"),
                email = trimmedEmail,
                role = User.ROLE_STUDENT,
                studentId = "REG-$uid"
            )
        }

        val studentDoc = firestore.collection(COLLECTION_STUDENTS).document(uid).get().await()
        val studentProfile = if (studentDoc.exists()) {
            StudentProfile(
                id = uid,
                userId = uid,
                departmentId = studentDoc.getString("departmentId") ?: "dept_cse",
                fullName = studentDoc.getString("fullName") ?: user.name,
                registrationNumber = studentDoc.getString("registrationNumber") ?: user.studentId,
                degree = studentDoc.getString("degree") ?: "B.Tech",
                graduationYear = studentDoc.getLong("graduationYear")?.toInt() ?: 2026,
                currentCgpa = studentDoc.getDouble("currentCgpa") ?: 8.5,
                activeBacklogs = studentDoc.getLong("activeBacklogs")?.toInt() ?: 0,
                skills = (studentDoc.get("skills") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
                resumeUrl = studentDoc.getString("resumeUrl"),
                phoneNumber = studentDoc.getString("phoneNumber"),
                isVerified = studentDoc.getBoolean("isVerified") ?: false
            )
        } else null

        Pair(user, studentProfile)
    }

    suspend fun getStudentProfile(uid: String): StudentProfile? = withContext(Dispatchers.IO) {
        val studentDoc = firestore.collection(COLLECTION_STUDENTS).document(uid).get().await()
        if (studentDoc.exists()) {
            StudentProfile(
                id = uid,
                userId = uid,
                departmentId = studentDoc.getString("departmentId") ?: "dept_cse",
                fullName = studentDoc.getString("fullName") ?: "Student",
                registrationNumber = studentDoc.getString("registrationNumber") ?: "",
                degree = studentDoc.getString("degree") ?: "B.Tech",
                graduationYear = studentDoc.getLong("graduationYear")?.toInt() ?: 2026,
                currentCgpa = studentDoc.getDouble("currentCgpa") ?: 8.0,
                activeBacklogs = studentDoc.getLong("activeBacklogs")?.toInt() ?: 0,
                skills = (studentDoc.get("skills") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
                resumeUrl = studentDoc.getString("resumeUrl"),
                phoneNumber = studentDoc.getString("phoneNumber"),
                isVerified = studentDoc.getBoolean("isVerified") ?: false
            )
        } else null
    }

    suspend fun updateStudentProfileInFirestore(
        uid: String,
        phone: String?,
        resumeUrl: String?,
        skills: List<String>?
    ): Boolean = withContext(Dispatchers.IO) {
        val updates = mutableMapOf<String, Any>(
            "updatedAt" to System.currentTimeMillis()
        )
        if (phone != null) updates["phoneNumber"] = phone
        if (resumeUrl != null) updates["resumeUrl"] = resumeUrl
        if (skills != null) updates["skills"] = skills

        firestore.collection(COLLECTION_STUDENTS).document(uid).set(updates, SetOptions.merge()).await()
        true
    }

    // --- Placement Drives ---

    suspend fun syncPlacementDrivesToFirestore(drives: List<PlacementDriveModel>) = withContext(Dispatchers.IO) {
        val batch = firestore.batch()
        for (drive in drives) {
            val docRef = firestore.collection(COLLECTION_PLACEMENT_DRIVES).document(drive.id)
            val driveMap = hashMapOf(
                "id" to drive.id,
                "companyName" to drive.companyName,
                "companyLogoUrl" to drive.companyLogoUrl,
                "jobTitle" to drive.jobTitle,
                "jobDescription" to drive.jobDescription,
                "salaryPackageCtc" to drive.salaryPackageCtc,
                "jobLocation" to drive.jobLocation,
                "employmentType" to drive.employmentType,
                "vacancies" to drive.vacancies,
                "status" to drive.status,
                "applicationDeadline" to drive.applicationDeadline,
                "minCgpa" to (drive.eligibilityCriteria?.minCgpa ?: 0.0),
                "maxBacklogs" to (drive.eligibilityCriteria?.maxBacklogs ?: 0),
                "requiredSkills" to (drive.eligibilityCriteria?.requiredSkills ?: emptyList<String>()),
                "updatedAt" to System.currentTimeMillis()
            )
            batch.set(docRef, driveMap, SetOptions.merge())
        }
        batch.commit().await()
    }

    suspend fun getPlacementDrivesFromFirestore(): List<PlacementDriveModel> = withContext(Dispatchers.IO) {
        val snapshot = firestore.collection(COLLECTION_PLACEMENT_DRIVES)
            .whereIn("status", listOf("APPROVED", "ACTIVE"))
            .get()
            .await()

        snapshot.documents.map { doc ->
            PlacementDriveModel(
                id = doc.getString("id") ?: doc.id,
                companyId = doc.getString("companyId") ?: "",
                companyName = doc.getString("companyName") ?: "Campus Partner",
                companyLogoUrl = doc.getString("companyLogoUrl") ?: "",
                jobTitle = doc.getString("jobTitle") ?: "Placement Drive",
                jobDescription = doc.getString("jobDescription") ?: "",
                salaryPackageCtc = doc.getString("salaryPackageCtc") ?: "Competitive",
                jobLocation = doc.getString("jobLocation") ?: "Campus",
                employmentType = doc.getString("employmentType") ?: "FULL_TIME",
                vacancies = doc.getLong("vacancies")?.toInt() ?: 1,
                status = doc.getString("status") ?: "ACTIVE",
                applicationDeadline = doc.getString("applicationDeadline") ?: "",
                eligibilityCriteria = EligibilityCriteriaModel(
                    minCgpa = doc.getDouble("minCgpa") ?: 6.0,
                    maxBacklogs = doc.getLong("maxBacklogs")?.toInt() ?: 0,
                    requiredSkills = (doc.get("requiredSkills") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                )
            )
        }
    }

    // --- Applications ---

    suspend fun saveApplicationToFirestore(
        app: ApplicationModel,
        studentName: String? = null,
        studentRegNo: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val appMap = hashMapOf(
            "id" to app.id,
            "driveId" to app.driveId,
            "studentId" to app.studentId,
            "studentName" to (studentName ?: ""),
            "studentRegNo" to (studentRegNo ?: ""),
            "driveTitle" to (app.driveTitle ?: ""),
            "companyName" to (app.companyName ?: ""),
            "salaryPackage" to (app.salaryPackage ?: ""),
            "status" to app.status,
            "appliedAt" to app.appliedAt,
            "withdrawalReason" to (app.withdrawalReason ?: ""),
            "timestamp" to System.currentTimeMillis()
        )
        firestore.collection(COLLECTION_APPLICATIONS).document(app.id).set(appMap, SetOptions.merge()).await()

        sendNotification(
            userId = app.studentId,
            title = "Application Submitted",
            message = "Your application to ${app.companyName ?: "Company"} has been recorded.",
            type = "APPLICATION_SUBMITTED"
        )
        true
    }

    suspend fun getApplicationsFromFirestore(studentId: String): List<ApplicationModel> = withContext(Dispatchers.IO) {
        val snapshot = firestore.collection(COLLECTION_APPLICATIONS)
            .whereEqualTo("studentId", studentId)
            .get()
            .await()

        snapshot.documents.map { doc ->
            ApplicationModel(
                id = doc.getString("id") ?: doc.id,
                driveId = doc.getString("driveId") ?: "",
                studentId = doc.getString("studentId") ?: studentId,
                status = doc.getString("status") ?: "APPLIED",
                appliedAt = doc.getString("appliedAt") ?: "",
                withdrawalReason = doc.getString("withdrawalReason"),
                driveTitle = doc.getString("driveTitle"),
                companyName = doc.getString("companyName"),
                salaryPackage = doc.getString("salaryPackage")
            )
        }
    }

    fun listenToStudentApplications(
        studentId: String,
        onUpdate: (List<ApplicationModel>) -> Unit
    ): ListenerRegistration {
        return firestore.collection(COLLECTION_APPLICATIONS)
            .whereEqualTo("studentId", studentId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.map { doc ->
                    ApplicationModel(
                        id = doc.getString("id") ?: doc.id,
                        driveId = doc.getString("driveId") ?: "",
                        studentId = doc.getString("studentId") ?: studentId,
                        status = doc.getString("status") ?: "APPLIED",
                        appliedAt = doc.getString("appliedAt") ?: "",
                        withdrawalReason = doc.getString("withdrawalReason"),
                        driveTitle = doc.getString("driveTitle"),
                        companyName = doc.getString("companyName"),
                        salaryPackage = doc.getString("salaryPackage")
                    )
                }
                onUpdate(list)
            }
    }

    // --- Firebase Storage: Resume Upload ---

    suspend fun uploadResume(studentId: String, fileUri: Uri, fileName: String): String = withContext(Dispatchers.IO) {
        val cleanFileName = fileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
        val storageRef = storage.reference.child("resumes/$studentId/$cleanFileName")

        // Upload file
        storageRef.putFile(fileUri).await()

        // Get download URL
        val downloadUri = storageRef.downloadUrl.await()
        val downloadUrl = downloadUri.toString()

        // Save resume record in Firestore
        val resumeId = UUID.randomUUID().toString()
        val resumeMap = hashMapOf(
            "id" to resumeId,
            "studentId" to studentId,
            "fileName" to cleanFileName,
            "fileUrl" to downloadUrl,
            "storagePath" to storageRef.path,
            "uploadedAt" to System.currentTimeMillis()
        )
        firestore.collection(COLLECTION_RESUMES).document(resumeId).set(resumeMap).await()

        // Update student profile with resumeUrl
        updateStudentProfileInFirestore(studentId, phone = null, resumeUrl = downloadUrl, skills = null)

        downloadUrl
    }

    suspend fun getStudentResumes(studentId: String): List<Map<String, Any>> = withContext(Dispatchers.IO) {
        val snapshot = firestore.collection(COLLECTION_RESUMES)
            .whereEqualTo("studentId", studentId)
            .get()
            .await()
        snapshot.documents.mapNotNull { it.data }
    }

    // --- Academic Records ---

    suspend fun getAcademicRecord(studentId: String): Map<String, Any>? = withContext(Dispatchers.IO) {
        val doc = firestore.collection(COLLECTION_ACADEMIC_RECORDS).document(studentId).get().await()
        if (doc.exists()) doc.data else null
    }

    suspend fun saveAcademicRecord(
        studentId: String,
        registrationNumber: String,
        cgpa: Double,
        activeBacklogs: Int,
        graduationYear: Int,
        degree: String = "B.Tech",
        departmentId: String = "dept_cse",
        isVerified: Boolean = false,
        verifiedBy: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val map = hashMapOf<String, Any>(
            "studentId" to studentId,
            "registrationNumber" to registrationNumber,
            "cgpa" to cgpa,
            "activeBacklogs" to activeBacklogs,
            "graduationYear" to graduationYear,
            "degree" to degree,
            "departmentId" to departmentId,
            "isVerified" to isVerified,
            "updatedAt" to System.currentTimeMillis()
        )
        if (verifiedBy != null) map["verifiedBy"] = verifiedBy
        firestore.collection(COLLECTION_ACADEMIC_RECORDS).document(studentId).set(map, SetOptions.merge()).await()
        true
    }

    // --- Recruiters ---

    suspend fun saveRecruiterProfile(
        recruiterId: String,
        companyName: String,
        contactEmail: String,
        designation: String? = null,
        isVerified: Boolean = false
    ): Boolean = withContext(Dispatchers.IO) {
        val recruiterMap = hashMapOf<String, Any>(
            "recruiterId" to recruiterId,
            "companyName" to companyName,
            "contactEmail" to contactEmail,
            "isVerified" to isVerified,
            "updatedAt" to System.currentTimeMillis()
        )
        if (designation != null) recruiterMap["designation"] = designation
        firestore.collection(COLLECTION_RECRUITERS).document(recruiterId).set(recruiterMap, SetOptions.merge()).await()
        true
    }

    suspend fun getRecruiterProfile(recruiterId: String): Map<String, Any>? = withContext(Dispatchers.IO) {
        val doc = firestore.collection(COLLECTION_RECRUITERS).document(recruiterId).get().await()
        if (doc.exists()) doc.data else null
    }

    // --- Approvals ---

    suspend fun recordApprovalDecision(
        approvalId: String = UUID.randomUUID().toString(),
        targetType: String, // "PLACEMENT_DRIVE" | "COMPANY_REGISTRATION"
        targetId: String,
        decision: String,   // "APPROVED" | "REJECTED"
        reviewedBy: String,
        comments: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val approvalMap = hashMapOf<String, Any>(
            "id" to approvalId,
            "targetType" to targetType,
            "targetId" to targetId,
            "decision" to decision,
            "reviewedBy" to reviewedBy,
            "timestamp" to System.currentTimeMillis()
        )
        if (comments != null) approvalMap["comments"] = comments
        firestore.collection(COLLECTION_APPROVALS).document(approvalId).set(approvalMap).await()
        true
    }

    suspend fun getApprovalsForTarget(targetId: String): List<Map<String, Any>> = withContext(Dispatchers.IO) {
        val snapshot = firestore.collection(COLLECTION_APPROVALS)
            .whereEqualTo("targetId", targetId)
            .get()
            .await()
        snapshot.documents.mapNotNull { it.data }
    }

    // --- Notifications ---

    suspend fun sendNotification(
        userId: String,
        title: String,
        message: String,
        type: String = "GENERAL"
    ) = withContext(Dispatchers.IO) {
        try {
            val notifId = UUID.randomUUID().toString()
            val notifMap = hashMapOf(
                "id" to notifId,
                "userId" to userId,
                "title" to title,
                "message" to message,
                "type" to type,
                "read" to false,
                "createdAt" to System.currentTimeMillis()
            )
            firestore.collection(COLLECTION_NOTIFICATIONS).document(notifId).set(notifMap).await()
        } catch (_: Exception) {}
    }

    suspend fun getUserNotifications(userId: String): List<Map<String, Any>> = withContext(Dispatchers.IO) {
        val snapshot = firestore.collection(COLLECTION_NOTIFICATIONS)
            .whereEqualTo("userId", userId)
            .get()
            .await()
        snapshot.documents.mapNotNull { it.data }
    }
}

# CampusConnect — Firebase Full Integration & Deployment Guide

This document provides a comprehensive operational and architectural reference for the Firebase integration within the **CampusConnect Placement & Recruitment Platform**.

---

## 1. System Architecture: Hybrid Persistence Model

To maintain strict university compliance, data integrity, and real-time mobile updates, CampusConnect implements a **synchronized hybrid architecture**:

```
                              ┌────────────────────────────────────────┐
                              │  CampusConnect Android Native App      │
                              │  (Kotlin / MVVM / Material Design 3)   │
                              └───────────┬─────────────────┬──────────┘
                                          │                 │
                  1. Offline Cache &      │                 │ 2. Core RBAC, Eligibility
                     Real-time Firestore  │                 │    & ACID Transactions
                                          ▼                 ▼
             ┌──────────────────────────────┐     ┌──────────────────────────────┐
             │ Firebase Cloud Services      │     │ Centralized FastAPI Backend  │
             │                              │     │                              │
             │ • Firebase Auth (Sessions)   │     │ • JWT Access & Refresh Tokens│
             │ • Cloud Firestore (9 Clctns) │     │ • PostgreSQL 16 (Relational) │
             │ • Cloud Storage (Resumes)    │     │ • Eligibility Engine         │
             │ • Offline Persistence Enabled│     │ • ACID CAS Slot Booking      │
             └──────────────────────────────┘     └──────────────────────────────┘
```

### Separation of Responsibilities:
- **Centralized FastAPI Backend (Source of Truth)**: Validates institutional eligibility criteria, enforces role-based access control (RBAC), prevents tamper attacks against student CGPA/backlogs, and executes atomic compare-and-swap (CAS) slot reservations.
- **Firebase Authentication**: Manages persistent mobile identity, password resets, and session tokens across app cold-starts and device reboots.
- **Cloud Firestore**: Acts as a high-speed real-time push layer and offline-capable mobile cache for drives, applications, and status notifications.
- **Firebase Storage**: Stores student PDF resumes, portfolio assets, and profile media, returning validated HTTPS download links to Firestore and the FastAPI backend.

---

## 2. Prerequisites & Console Setup

### 2.1. Firebase Project Initialization
1. Navigate to the [Firebase Console](https://console.firebase.google.com/).
2. Select or create project **`campusconnect-b895f`**.
3. Register the Android application with package name:
   - **Android Package Name:** `com.campusconnect.app`
   - **App Nickname:** CampusConnect Placement
   - **Debug Signing Certificate SHA-1:** Obtain by running `.\gradlew signingReport` in the project root.
4. Download the generated `google-services.json` and place it at:
   ```text
   CampusConnect/app/google-services.json
   ```

### 2.2. Authentication Provider
1. In Firebase Console, go to **Build > Authentication > Sign-in method**.
2. Enable **Email/Password** provider (Email link sign-in optional).
3. Save changes.

### 2.3. Cloud Firestore Database
1. In Firebase Console, go to **Build > Firestore Database**.
2. Click **Create database** and select **Production mode**.
3. Choose a server location closest to your target audience (e.g., `asia-south1` for India).
4. Click **Enable**.

### 2.4. Firebase Cloud Storage
1. In Firebase Console, go to **Build > Storage**.
2. Click **Get started**, choose **Production mode**, and select the same location.
3. Click **Done**.

---

## 3. Firestore Database Schema: 9 Collections

Every collection and subdocument used by `FirebaseManager.kt` and `firestore.rules` is strictly structured:

### 1. `users` — User Accounts & Identity
- **Document ID:** Firebase Auth UID (`request.auth.uid`)
```json
{
  "uid": "FIREBASE_UID",
  "name": "Jane Doe",
  "email": "jane.doe@apex.edu",
  "role": "STUDENT",
  "studentId": "2026-CS-104",
  "createdAt": 1731195600000
}
```

### 2. `students` — Detailed Student Placement Profiles
- **Document ID:** Firebase Auth UID
```json
{
  "id": "FIREBASE_UID",
  "userId": "FIREBASE_UID",
  "fullName": "Jane Doe",
  "registrationNumber": "2026-CS-104",
  "departmentId": "dept_cse",
  "degree": "B.Tech",
  "graduationYear": 2026,
  "currentCgpa": 8.8,
  "activeBacklogs": 0,
  "skills": ["Java", "Kotlin", "Python", "SQL"],
  "resumeUrl": "https://firebasestorage.googleapis.com/...",
  "phoneNumber": "+91 9876543210",
  "isVerified": true,
  "updatedAt": 1731195600000
}
```

### 3. `academic_records` — Verified Institutional Academic Records
- **Document ID:** Firebase Auth UID
- **Access:** Read by student/TPO/recruiter; **Write strictly restricted to TPO Admins**.
```json
{
  "studentId": "FIREBASE_UID",
  "registrationNumber": "2026-CS-104",
  "cgpa": 8.8,
  "activeBacklogs": 0,
  "graduationYear": 2026,
  "degree": "B.Tech",
  "departmentId": "dept_cse",
  "isVerified": true,
  "verifiedBy": "tpo@apex.edu",
  "updatedAt": 1731195600000
}
```

### 4. `recruiters` — Corporate Partner Liaison Profiles
- **Document ID:** Recruiter UID
```json
{
  "recruiterId": "RECRUITER_UID",
  "companyName": "Google LLC",
  "contactEmail": "recruiter@google.com",
  "designation": "University Talent Acquisition Lead",
  "isVerified": true,
  "updatedAt": 1731195600000
}
```

### 5. `placement_drives` — Corporate Recruitment Drives & Rules
- **Document ID:** Drive UUID (`drive.id`)
```json
{
  "id": "DRIVE_UUID",
  "companyName": "Google LLC",
  "companyLogoUrl": "https://...",
  "jobTitle": "Software Development Engineer",
  "jobDescription": "Backend distributed systems engineering with high concurrency...",
  "salaryPackageCtc": "32.0 LPA",
  "jobLocation": "Bangalore / Hybrid",
  "employmentType": "FULL_TIME",
  "vacancies": 12,
  "status": "APPROVED",
  "applicationDeadline": "2026-11-25T23:59:59Z",
  "minCgpa": 8.0,
  "maxBacklogs": 0,
  "requiredSkills": ["DSA", "System Design", "Python"],
  "updatedAt": 1731195600000
}
```

### 6. `applications` — Placement Applications & Status Pipeline
- **Document ID:** Application UUID (`app.id`)
```json
{
  "id": "APP_UUID",
  "driveId": "DRIVE_UUID",
  "studentId": "FIREBASE_UID",
  "studentName": "Jane Doe",
  "studentRegNo": "2026-CS-104",
  "companyName": "Google LLC",
  "driveTitle": "Software Development Engineer",
  "salaryPackage": "32.0 LPA",
  "status": "SHORTLISTED",
  "appliedAt": "2026-11-10T01:00:00Z",
  "withdrawalReason": null,
  "timestamp": 1731195600000
}
```

### 7. `resumes` — Document Metadata & Storage Mapping
- **Document ID:** Resume UUID
```json
{
  "id": "RESUME_UUID",
  "studentId": "FIREBASE_UID",
  "fileName": "Jane_Doe_Resume_2026.pdf",
  "fileUrl": "https://firebasestorage.googleapis.com/v0/b/...",
  "storagePath": "resumes/FIREBASE_UID/Jane_Doe_Resume_2026.pdf",
  "uploadedAt": 1731195600000
}
```

### 8. `notifications` — Target User Alerts
- **Document ID:** Notification UUID
```json
{
  "id": "NOTIF_UUID",
  "userId": "FIREBASE_UID",
  "title": "Application Shortlisted",
  "message": "Congratulations! You have been shortlisted by Google for Round 1.",
  "type": "APPLICATION_SHORTLISTED",
  "read": false,
  "createdAt": 1731195600000
}
```

### 9. `approvals` — TPO Clearance Decisions
- **Document ID:** Approval UUID
```json
{
  "id": "APPROVAL_UUID",
  "targetType": "PLACEMENT_DRIVE",
  "targetId": "DRIVE_UUID",
  "decision": "APPROVED",
  "reviewedBy": "tpo@apex.edu",
  "comments": "Drive criteria and compensation approved by Academic Board.",
  "timestamp": 1731195600000
}
```

---

## 4. Security Rules Specification

### 4.1. Firestore Rules (`firestore.rules`)
Enforces RBAC directly at the database engine level:
- Students can read/write only their own user and student profile documents.
- Students **cannot** alter `academic_records` (write allowed only if `isTpoAdmin()`).
- Applications can only be created by the authenticated student owning the `studentId`.
- Drive publication and administrative approvals require `TPO_ADMIN` or `SUPER_ADMIN` credentials.

### 4.2. Storage Rules (`storage.rules`)
- Resumes restricted to `resumes/{studentId}/{fileName}`:
  - Allowed write only if `request.auth.uid == studentId` and file size < 15MB.
- Profile pictures restricted to `profiles/{userId}/{fileName}`:
  - Allowed write only if `request.auth.uid == userId` and file size < 5MB.

---

## 5. Deployment Commands via Firebase CLI

Run the following commands in the project root to authenticate and deploy the security policies:

```powershell
# 1. Install Firebase CLI globally (if not already installed)
npm install -g firebase-tools

# 2. Log in with your Google account
firebase login

# 3. Associate the CLI with your Firebase project
firebase use campusconnect-b895f

# 4. Deploy Cloud Firestore Security Rules
firebase deploy --only firestore:rules

# 5. Deploy Cloud Storage Security Rules
firebase deploy --only storage
```

---

## 6. Android Implementation Architecture

All Android code interfacing with Firebase is structured in clean MVVM layers:

1. **`CampusConnectApp.kt`**: Initializes `FirebaseApp.initializeApp(this)` during Application `onCreate()`.
2. **`FirebaseManager.kt`** (`com.campusconnect.app.data.firebase`):
   - Lazy singleton instances for `FirebaseAuth`, `FirebaseFirestore` (with disk persistence enabled), and `FirebaseStorage`.
   - Comprehensive CRUD methods for all 9 collections.
   - Real-time snapshot listener: `listenToStudentApplications(studentId, onUpdate)`.
3. **`AuthRepositoryImpl.kt`**:
   - `login()`: FastAPI backend → Firebase Auth → Cached demo credentials.
   - `signup()`: Creates Firebase Auth account → writes `users` + `students` + `academic_records` + `notifications` documents atomically.
   - `logout()`: Signs out from backend API and `FirebaseManager.auth.signOut()`.
4. **`PlacementRepositoryImpl.kt`**:
   - `getPlacementDrives()`: Synchronizes active backend drives into Firestore cache.
   - `applyForDrive()`: Validates eligibility via backend → saves application record into Firestore.
   - `uploadResume()`: Streams PDF to Firebase Storage path `resumes/{studentId}/{cleanFileName}`, updates Firestore `resumes` and `students` documents, and syncs URL to backend profile.
5. **`ProfileFragment.kt` & `ProfileViewModel.kt`**:
   - Uses `ActivityResultContracts.GetContent()` for PDF resume selection.
   - Observes LiveData states with real-time Snackbar feedback.

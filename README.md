# 🎓 CampusConnect — University Placement & Recruitment Management Platform

[![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20Web%20%7C%20API-3DDC84?style=flat&logo=android&logoColor=white)](https://developer.android.com/)
[![Backend](https://img.shields.io/badge/Backend-FastAPI%20%7C%20PostgreSQL%20%7C%20SQLAlchemy%202.0-009688?style=flat&logo=fastapi&logoColor=white)](https://fastapi.tiangolo.com/)
[![Web](https://img.shields.io/badge/Web-React%2018%20%7C%20TypeScript%20%7C%20Tailwind%20CSS-61DAFB?style=flat&logo=react&logoColor=white)](https://vitejs.dev/)
[![Android](https://img.shields.io/badge/Android-Kotlin%20%7C%20Material%203%20%7C%20MVVM%20%7C%20Retrofit-7F52FF?style=flat&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

**CampusConnect** is a centralized, production-grade hybrid platform that unites university placement operations across four integrated components:

1. **📱 Native Android Application** — For students to browse verified campus placement drives, automatically evaluate eligibility, apply, track recruitment status, reserve interview slots with ACID concurrency guarantees, and participate in campus clubs and events.
2. **🏢 Recruiter Web Dashboard** — For corporate partners (Google, Microsoft, Amazon, etc.) to publish recruitment drives, specify automated eligibility rules, screen applicants in a multi-stage ATS pipeline, review student resumes, generate interview slots, and download applicant data.
3. **🏛️ TPO / University Admin Web Dashboard** — For Training and Placement Officers (TPO) to authorize/reject incoming company drives, verify corporate partnerships, manage university student registries, modify verified academic records (CGPA, backlogs), track macro placement rate KPIs, and broadcast targeted placement circulars.
4. **⚡ Centralized Backend API** — High-performance asynchronous FastAPI backend powered by PostgreSQL, Alembic migrations, SQLAlchemy 2.0, JWT authentication with RBAC, server-side eligibility evaluation engine, and row-level locking for atomic slot bookings.

---

## 🏛️ Platform Architecture

```text
┌───────────────────────────────────────────────────────────────────────────────────┐
│                                  PRESENTATION LAYER                               │
├─────────────────────────┬───────────────────────────────┬─────────────────────────┤
│   📱 Student Mobile App │   🏢 Recruiter Dashboard      │  🏛️ TPO Admin Dashboard  │
│   (Kotlin / Material 3) │   (React 18 / TypeScript)     │  (React 18 / TypeScript)│
└────────────┬────────────┴───────────────┬───────────────┴────────────┬────────────┘
             │ Retrofit / OkHttp          │ Axios / TanStack Query     │ Axios
             │ (http://10.0.2.2:8000/api) │ (http://127.0.0.1:8000/api)│
             ▼                            ▼                            ▼
┌───────────────────────────────────────────────────────────────────────────────────┐
│                           CENTRALIZED FASTAPI BACKEND                             │
├───────────────────────────────────────────────────────────────────────────────────┤
│ • Security: Passlib (Bcrypt) + JWT Access/Refresh Rotation + Multi-Role RBAC      │
│ • Academic Integrity Guard: 403 Forbidden on student CGPA / backlog mutation      │
│ • Eligibility Engine: Automated CGPA, backlogs, department, batch, & skills check │
│ • ACID Interview Booking: Row-level database locks (select ... with_for_update)   │
└─────────────────────────────────────────┬─────────────────────────────────────────┘
                                          │ SQLAlchemy 2.0 (Async + Sync)
                                          ▼
┌───────────────────────────────────────────────────────────────────────────────────┐
│                      RELATIONAL DATABASE (19 ENTITIES)                            │
├───────────────────────────────────────────────────────────────────────────────────┤
│ • Universities, Departments, Users, Students, StudentSkills, Resumes              │
│ • Companies, Recruiters, PlacementDrives, DriveCriteria, CriteriaDepartments      │
│ • CriteriaAllowedBatches, Applications, ApplicationStatusHistory                  │
│ • RecruitmentRounds, InterviewSlots, InterviewBookings, PlacementNotices          │
└───────────────────────────────────────────────────────────────────────────────────┘
```

---

## 🌟 Key Functional Modules

### 1. 📱 Student Mobile Application (Android)
- **Placement Dashboard**: Live banner for official announcements, active drive count, profile completion indicator, and quick links.
- **Drive Discovery & Search**: Instant full-text search across companies, job roles, package (CTC), and job types (Full-Time, Internship).
- **Automated Eligibility Engine**: Live server-side eligibility evaluation showing a clear checklist of criteria (CGPA threshold, max backlogs, approved branches, passing batch) and explicit eligibility pass/fail reasons.
- **One-Click Application**: Direct submission with automatic resume attachment and status tracking (`APPLIED`, `SHORTLISTED`, `ASSESSMENT`, `TECHNICAL_INTERVIEW`, `HR_INTERVIEW`, `SELECTED`, `REJECTED`, `WITHDRAWN`).
- **Interactive Interview Slot Booking**: Select available time windows, add candidate notes, and reserve slots with real-time capacity feedback.
- **Academic Profile**: Read-only display of verified institutional records (Roll number, Degree, CGPA, Backlogs) alongside editable student contact info, skills, and resume URL.
- **Community Hub**: Preserved campus clubs exploration, club events registration, and campus updates feed.

### 2. 🏢 Recruiter Web Dashboard
- **Executive Recruitment KPIs**: Active drives, candidate applications, Class of 2026 targets, and company verification status.
- **Drive Creation Wizard**: Step-by-step drive publisher specifying CTC, bond terms, job description, and automated eligibility criteria (minimum CGPA, allowed departments, target graduation year, max active backlogs, required technical skill tags).
- **Applicant Tracking System (ATS)**:
  - Interactive pipeline status progression dropdown (`APPLIED` → `SHORTLISTED` → `ASSESSMENT` → `INTERVIEW` → `SELECTED` / `REJECTED`).
  - Search candidates by name, roll number, or email.
  - CGPA threshold filtering (`≥9.0`, `≥8.5`, `≥8.0`, `≥7.5`).
  - Backlog filters (All, Zero backlogs only).
  - Candidate profile modal with quick resume preview.
  - One-click CSV export of candidate rosters.
- **Recruiter Profile**: Company info, corporate liaison details, and placement verification badge.

### 3. 🏛️ TPO Admin Web Dashboard
- **Executive Placement Analytics**:
  - Campus Placement Rate (%) calculation.
  - Placed vs. Unplaced student distribution.
  - Compensation analytics (Highest Package: ₹42.0 LPA, Average Package: ₹9.2 LPA).
  - Clearance alerts for pending company job postings.
- **Drive Clearances Workflow**: Review submitted corporate recruitment drives, verify compensation and criteria, and approve or reject drives with one click.
- **Corporate Partner & Recruiter Verification**: Authorize newly registered companies and approve individual recruiter accounts.
- **University Student Registry**:
  - Searchable student roster filtered by department, batch, and verification status.
  - Academic modification modal: safely edit student CGPA, backlogs, and toggle university verified badge.
  - Export full university student registry as CSV.
- **Placement Circulars & Notices**: Broadcast official placement circulars with target graduation batch filtering.

### 4. ⚡ FastAPI Backend
- **Multi-Tenant Architecture**: Partitioned by `university_id` across academic entities.
- **Strict Role-Based Access Control**: `STUDENT`, `RECRUITER`, `TPO_ADMIN`, `SUPER_ADMIN`.
- **Academic Integrity Guard**: Endpoints explicitly block students from updating academic fields (`cgpa`, `active_backlogs`, `is_verified`), enforcing TPO-only write authorization.
- **Eligibility Engine**: Dedicated `EligibilityService` evaluating complex criteria combinations server-side.
- **ACID Interview Slot Reservation**: Row-level locking (`with_for_update`) preventing race conditions, overbooking beyond capacity, or duplicate bookings per student per round.
- **100% Passing Automated Tests**: Full pytest suite covering auth, RBAC, academic safeguards, eligibility checks, applications, and slot booking.

---

## 🚀 Quick Start Guide

### Prerequisites
- **Python**: 3.10+ (tested with Python 3.12)
- **Node.js**: 18+ (tested with Node.js 22)
- **Java**: JDK / JBR 17 or 21 (Android Studio JBR recommended)
- **Android SDK**: API 34+

---

### Step 1: Start Backend API
```powershell
cd backend
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt

# Run migrations and seed development data
alembic upgrade head
python scripts/seed_data.py

# Start FastAPI server
python -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```
API Documentation available at:
- **Swagger UI**: `http://127.0.0.1:8000/docs`
- **ReDoc**: `http://127.0.0.1:8000/redoc`

Run Backend Automated Tests:
```powershell
python -m pytest tests/ -v
# Output: 15 passed in 10.72s
```

---

### Step 2: Start Web Portals (Recruiter & TPO Admin)
```powershell
cd web
npm install
npm run dev
```
Access Portals:
- **Recruiter Portal**: `http://localhost:5173/login`
- **TPO Admin Portal**: `http://localhost:5173/admin/login`

Production Build:
```powershell
npm run build
# Output: 1687 modules transformed, built in 9.10s
```

---

### Step 3: Run Android Application
1. Open the project root in **Android Studio**.
2. For Android Emulator, the backend is reachable out-of-the-box at `http://10.0.2.2:8000/api/`.
3. To build via command-line:
```powershell
.\gradlew.bat assembleDebug
# Generated APK: app/build/outputs/apk/debug/app-debug.apk
```

---

## 🔑 Demo Credentials

| Role | Portal / Client | Email | Password | Permissions & Features |
| :--- | :--- | :--- | :--- | :--- |
| **TPO Admin** | Web (`/admin/login`) | `tpo@apex.edu` | `Admin@1234` | Full placement administration, drive approvals, student registry, CGPA updates, circulars |
| **Super Admin** | Web (`/admin/login`) | `superadmin@campusconnect.edu` | `Super@1234` | System-wide administrative oversight |
| **Google Recruiter** | Web (`/login`) | `recruiter@google.com` | `Recruiter@1234` | Google drives, ATS candidate pipeline, candidate status progression, CSV export |
| **Microsoft Recruiter**| Web (`/login`) | `recruiter@microsoft.com` | `Recruiter@1234` | Microsoft drives, ATS screening, slot management |
| **Amazon Recruiter** | Web (`/login`) | `recruiter@amazon.com` | `Recruiter@1234` | Amazon drives, candidate review |
| **Eligible Student** | Android App | `eligible.student@test.edu` | `Student@1234` | CGPA 8.8, 0 Backlogs (Eligible for all active drives; interview slot booking) |
| **Ineligible Student**| Android App | `ineligible.student@test.edu` | `Student@1234` | CGPA 6.2, 2 Backlogs (Tests live eligibility rejection feedback) |

---

## 📊 Verification & Test Summary

| Component | Target / Suite | Verification Details | Result |
| :--- | :--- | :--- | :--- |
| **Backend** | `pytest tests/` | 15 test cases (Auth, RBAC, Academic Security, Eligibility, Applications, ACID Slots) | **15/15 PASSED** |
| **Web** | `npm run build` | Full TypeScript & Tailwind bundling for Recruiter and Admin portals | **0 ERRORS (9.10s)** |
| **Android** | `gradlew assembleDebug` | Kotlin, Material 3, ViewBinding, Retrofit, Navigation | **BUILD SUCCESSFUL (1m 20s)** |

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

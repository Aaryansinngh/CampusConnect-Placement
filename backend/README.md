# CampusConnect Backend API & Database Engine

CampusConnect is an enterprise-grade university placement and recruitment management platform. This backend service is powered by **FastAPI**, **SQLAlchemy 2.0 (Async + Sync)**, **PostgreSQL** (with SQLite fallback for local testing), and **Alembic** migrations.

---

## 🏗️ Architecture & Features

- **Multi-Tenant University Isolation:** All academic entities (departments, students, drives, announcements, audit logs) are partitioned by `university_id`.
- **Role-Based Access Control (RBAC):** Strict separation of privileges between `STUDENT`, `RECRUITER`, `TPO_ADMIN`, and `SUPER_ADMIN`.
- **Server-Side Eligibility Engine:** Evaluates minimum CGPA, maximum backlogs, eligible department IDs, eligible graduation years, and required skill keyword matching.
- **Academic Data Protection:** Students cannot modify their CGPA, backlogs, or verification status; academic records are editable only by authorized TPO administrators.
- **Relational Integrity & ACID Slot Locks:** Normalized schema with foreign key cascades, unique constraints (e.g. `(drive_id, student_id)` preventing duplicate applications), and indexed query lookups.
- **JWT Authentication:** Secure password hashing with bcrypt, JWT access tokens (1 hour expiry), refresh token rotation, and in-memory revocation on logout.

---

## 📁 Directory Structure

```text
backend/
├── app/
│   ├── main.py                  # FastAPI entry point, CORS, lifespan, exception handlers
│   ├── core/
│   │   ├── config.py            # Pydantic Settings & environment variables
│   │   ├── database.py          # SQLAlchemy 2.0 async & sync engines and sessionmakers
│   │   ├── security.py          # Bcrypt hashing, JWT generation, validation & revocation
│   │   └── dependencies.py      # OAuth2 scheme, RBAC checkers & tenant resolvers
│   ├── models/                  # 19 SQLAlchemy ORM entities
│   │   ├── university.py, department.py, user.py, student.py, company.py, recruiter.py
│   │   ├── placement_drive.py, eligibility.py, application.py, recruitment_round.py
│   │   ├── round_candidate.py, interview_slot.py, interview_booking.py, resume.py
│   │   ├── offer.py, announcement.py, notification.py, device_token.py, audit_log.py
│   │   └── base.py
│   ├── schemas/                 # Pydantic v2 validation models
│   ├── routes/                  # Modular FastAPI routers
│   │   ├── auth.py, students.py, companies.py, recruiters.py
│   │   ├── placement_drives.py, eligibility.py, applications.py, admin.py
│   │   └── __init__.py
│   ├── services/
│   │   └── eligibility_service.py # Pure domain eligibility engine
│   └── utils/
│       └── custom_types.py      # Cross-dialect GUID and PortableJSON types
├── migrations/                  # Alembic migration environment and versions
│   ├── env.py
│   └── versions/
│       └── 831dffdb8c7e_initial_19_entity_schema.py
├── scripts/
│   └── seed_data.py             # Realistic development seed script
├── tests/                       # Automated Pytest suite
│   ├── conftest.py
│   ├── test_auth.py
│   ├── test_eligibility.py
│   ├── test_applications.py
│   └── test_security_rbac.py
├── requirements.txt
├── alembic.ini
└── README.md
```

---

## ⚡ Quickstart: Running Locally

### 1. Prerequisites
- Python 3.12+
- `pip` package manager

### 2. Install Dependencies
```bash
cd backend
pip install -r requirements.txt
```

### 3. Run Database Migrations
```bash
python -m alembic upgrade head
```

### 4. Seed Development Data
Populates a demo university (*Apex Institute of Technology*), departments, TPO admin, recruiters, 16 students, placement drives, and criteria:
```bash
python scripts/seed_data.py
```

### 5. Launch Development Server
```bash
python -m uvicorn app.main:app --host 127.0.0.1 --port 8000 --reload
```

Interactive Swagger API docs will be available at:
👉 **http://127.0.0.1:8000/docs**
Alternative ReDoc:
👉 **http://127.0.0.1:8000/redoc**

---

## 🔑 Development Seed Credentials

| Role | Email | Password | Access & Purpose |
| :--- | :--- | :--- | :--- |
| **Super Admin** | `superadmin@campusconnect.edu` | `AdminSecure2026!` | Global platform governance & university onboarding |
| **TPO Administrator** | `tpo@apex.edu` | `TpoPassword123!` | Apex Institute TPO: drive/company approvals, analytics, student verification |
| **Recruiter (Google)** | `google.recruiter@google.com` | `Recruiter123!` | SDE-1 placement drive management, applicant tracking |
| **Recruiter (Microsoft)** | `microsoft.recruiter@microsoft.com` | `Recruiter123!` | Cloud solutions drive management |
| **Recruiter (Amazon)** | `amazon.recruiter@amazon.com` | `Recruiter123!` | Operations & systems drive management |
| **Student (Eligible)** | `alex.cs@apex.edu` | `Student123!` | CSE, CGPA 9.4, 0 backlogs (Eligible for all drives) |
| **Student (Eligible)** | `priya.it@apex.edu` | `Student123!` | IT, CGPA 8.8, 0 backlogs |
| **Student (Filtered)** | `devansh.it@apex.edu` | `Student123!` | IT, CGPA 6.8, 2 backlogs (Demonstrates ineligibility handling) |

---

## 🧪 Running Automated Tests

Execute the full automated pytest suite with coverage:
```bash
python -m pytest tests/ -v
```

All 14 unit and integration tests verify:
- Student & Recruiter registration
- Duplicate email prevention
- Password verification & JWT issuance
- Dynamic eligibility calculations (CGPA, backlogs, department, skills)
- Ineligible student application rejection (HTTP 403 with reasons)
- Duplicate application prevention (HTTP 400)
- Student self-tampering prevention on academic records (HTTP 403)
- TPO academic records modification (HTTP 200)
- Role-based authorization across endpoints

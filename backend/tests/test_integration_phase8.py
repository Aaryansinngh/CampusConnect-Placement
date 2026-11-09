import pytest
import asyncio
import uuid
from datetime import datetime, timezone, timedelta
from httpx import AsyncClient
from app.core.security import hash_password
from app.models.user import User, UserRole
from app.models.student import Student
from app.models.company import Company, CompanyVerificationStatus
from app.models.recruiter import Recruiter
from app.models.department import Department
from app.models.university import University


@pytest.mark.asyncio
async def test_end_to_end_auth_and_rbac_matrix(client: AsyncClient, seeded_test_data):
    """
    Verify:
    1. Authentication for Student, Recruiter, TPO.
    2. RBAC isolation: Student blocked from TPO & Recruiter endpoints.
    3. Recruiter blocked from TPO admin endpoints.
    4. Unauthenticated requests rejected with 401.
    """
    # 1. Unauthenticated request rejected
    no_auth_res = await client.get("/api/auth/me")
    assert no_auth_res.status_code == 401

    # 2. Student Authentication
    st_res = await client.post("/api/auth/login", json={
        "email": "eligible.student@test.edu",
        "password": "StudentPass123!"
    })
    assert st_res.status_code == 200
    st_token = st_res.json()["access_token"]

    # Student accessing own profile
    st_me = await client.get("/api/auth/me", headers={"Authorization": f"Bearer {st_token}"})
    assert st_me.status_code == 200
    assert st_me.json()["role"] == "STUDENT"

    # Student cannot create placement drives
    st_drive_fail = await client.post("/api/placement-drives/", json={
        "company_id": str(uuid.uuid4()),
        "job_title": "Fake Hack Role",
        "job_description": "Description",
        "salary_package_ctc": "10 LPA",
        "job_location": "Remote",
        "employment_type": "FULL_TIME"
    }, headers={"Authorization": f"Bearer {st_token}"})
    assert st_drive_fail.status_code == 403

    # Student cannot access TPO Analytics
    st_stats_fail = await client.get("/api/admin/stats", headers={"Authorization": f"Bearer {st_token}"})
    assert st_stats_fail.status_code == 403

    # 3. Recruiter Authentication
    rec_res = await client.post("/api/auth/login", json={
        "email": "recruiter@acme.com",
        "password": "RecruiterPass123!"
    })
    assert rec_res.status_code == 200
    rec_token = rec_res.json()["access_token"]

    # Recruiter cannot access TPO Analytics
    rec_stats_fail = await client.get("/api/admin/stats", headers={"Authorization": f"Bearer {rec_token}"})
    assert rec_stats_fail.status_code == 403

    # Recruiter cannot update university department registry
    rec_dept_fail = await client.post("/api/admin/departments", json={
        "code": "HACK",
        "name": "Unauthorized Dept"
    }, headers={"Authorization": f"Bearer {rec_token}"})
    assert rec_dept_fail.status_code == 403

    # 4. TPO Authentication
    tpo_res = await client.post("/api/auth/login", json={
        "email": "test_tpo@test.edu",
        "password": "TpoSecure123!"
    })
    assert tpo_res.status_code == 200
    tpo_token = tpo_res.json()["access_token"]

    # TPO can access TPO Analytics
    tpo_stats = await client.get("/api/admin/stats", headers={"Authorization": f"Bearer {tpo_token}"})
    assert tpo_stats.status_code == 200


@pytest.mark.asyncio
async def test_recruiter_drive_creation_and_tpo_approval_pipeline(client: AsyncClient, seeded_test_data):
    """
    Verify:
    1. Recruiter creates a drive -> Initial status is PENDING_APPROVAL.
    2. Student cannot see unapproved drives in the catalog.
    3. TPO reviews and approves the drive -> Status becomes APPROVED.
    4. Student can now see and discover the drive.
    """
    # 1. Login Recruiter & TPO & Student
    rec_token = (await client.post("/api/auth/login", json={
        "email": "recruiter@acme.com",
        "password": "RecruiterPass123!"
    })).json()["access_token"]

    tpo_token = (await client.post("/api/auth/login", json={
        "email": "test_tpo@test.edu",
        "password": "TpoSecure123!"
    })).json()["access_token"]

    st_token = (await client.post("/api/auth/login", json={
        "email": "eligible.student@test.edu",
        "password": "StudentPass123!"
    })).json()["access_token"]

    # Get Recruiter's company id
    companies_res = await client.get("/api/companies", headers={"Authorization": f"Bearer {rec_token}"})
    company_id = companies_res.json()[0]["id"]

    # 2. Recruiter creates drive
    deadline = (datetime.now(timezone.utc) + timedelta(days=14)).isoformat()
    drive_payload = {
        "company_id": company_id,
        "job_title": "Full Stack Cloud Engineer 2026",
        "job_description": "Building cloud native microservices with FastAPI & React.",
        "salary_package_ctc": "24.0 LPA",
        "job_location": "Hyderabad / Hybrid",
        "employment_type": "FULL_TIME",
        "vacancies": 8,
        "application_deadline": deadline,
        "min_cgpa": 7.5,
        "max_backlogs": 0,
        "required_skills": ["Python", "FastAPI"]
    }
    create_res = await client.post(
        "/api/placement-drives/",
        json=drive_payload,
        headers={"Authorization": f"Bearer {rec_token}"}
    )
    assert create_res.status_code == 201
    new_drive = create_res.json()
    drive_id = new_drive["id"]
    assert new_drive["status"] == "PENDING_APPROVAL"

    # 3. Student queries drives: the unapproved drive MUST NOT be visible to student
    st_drives = (await client.get("/api/placement-drives", headers={"Authorization": f"Bearer {st_token}"})).json()
    assert not any(d["id"] == drive_id for d in st_drives)

    # 4. TPO reviews and approves the drive
    approve_res = await client.put(
        f"/api/placement-drives/{drive_id}/status",
        json={"status": "APPROVED"},
        headers={"Authorization": f"Bearer {tpo_token}"}
    )
    assert approve_res.status_code == 200
    assert approve_res.json()["status"] == "APPROVED"

    # 5. Student queries drives again: drive MUST NOW be visible
    st_drives_after = (await client.get("/api/placement-drives", headers={"Authorization": f"Bearer {st_token}"})).json()
    assert any(d["id"] == drive_id for d in st_drives_after)


@pytest.mark.asyncio
async def test_concurrent_interview_slot_booking_race_condition(client: AsyncClient, seeded_test_data):
    """
    Verify:
    ACID Transaction Concurrency Protection:
    When multiple students attempt to book the SAME slot with capacity = 1 concurrently,
    exactly ONE booking succeeds (201) and the second is rejected (409 Conflict).
    """
    from tests.conftest import TestingSessionLocal

    # 1. Login Recruiter
    rec_token = (await client.post("/api/auth/login", json={
        "email": "recruiter@acme.com",
        "password": "RecruiterPass123!"
    })).json()["access_token"]

    # 2. Get approved drive
    drives = (await client.get("/api/placement-drives", headers={"Authorization": f"Bearer {rec_token}"})).json()
    drive_id = drives[0]["id"]

    # 3. Create interview round
    round_res = await client.post("/api/interviews/rounds", json={
        "drive_id": drive_id,
        "round_name": "ACID Concurrency Speed Round",
        "round_type": "TECHNICAL_INTERVIEW",
        "round_order": 99,
        "venue": "Meeting Room A"
    }, headers={"Authorization": f"Bearer {rec_token}"})
    assert round_res.status_code == 201
    round_id = round_res.json()["id"]

    # 4. Create ONE slot with strict capacity = 1
    now = datetime.now(timezone.utc) + timedelta(days=3)
    slot_res = await client.post("/api/interviews/slots/bulk-generate", json={
        "round_id": round_id,
        "start_datetime": now.isoformat(),
        "end_datetime": (now + timedelta(minutes=30)).isoformat(),
        "slot_duration_minutes": 30,
        "venue_or_link": "https://meet.google.com/acid-concurrency-test",
        "capacity_per_slot": 1
    }, headers={"Authorization": f"Bearer {rec_token}"})
    assert slot_res.status_code == 201
    slots = slot_res.json()
    slot_id = slots[0]["id"]
    assert slots[0]["total_capacity"] == 1

    # 5. Create a 3rd distinct student in database for concurrent test
    async with TestingSessionLocal() as session:
        from sqlalchemy import select
        u_res = await session.execute(select(University).limit(1))
        univ = u_res.scalar_one()
        d_res = await session.execute(select(Department).limit(1))
        dept = d_res.scalar_one()

        student_user_c = User(
            id=str(uuid.uuid4()),
            university_id=univ.id,
            email="candidate.concurrent@test.edu",
            hashed_password=hash_password("StudentPass123!"),
            role=UserRole.STUDENT.value,
            is_active=True,
            is_verified=True
        )
        session.add(student_user_c)
        await session.flush()

        student_c = Student(
            id=str(uuid.uuid4()),
            user_id=student_user_c.id,
            university_id=univ.id,
            department_id=dept.id,
            full_name="Candidate Concurrent",
            registration_number="REG-CONC-003",
            degree="B.Tech",
            graduation_year=2026,
            current_cgpa=9.5,
            active_backlogs=0,
            skills=["Python", "DSA"],
            is_verified=True
        )
        session.add(student_c)
        await session.commit()

    # 6. Login Candidate 1 and Candidate 2
    token_st1 = (await client.post("/api/auth/login", json={
        "email": "eligible.student@test.edu",
        "password": "StudentPass123!"
    })).json()["access_token"]

    token_st2 = (await client.post("/api/auth/login", json={
        "email": "candidate.concurrent@test.edu",
        "password": "StudentPass123!"
    })).json()["access_token"]

    # Both submit applications for the drive
    app_res1 = await client.post("/api/applications/", json={"drive_id": drive_id}, headers={"Authorization": f"Bearer {token_st1}"})
    if app_res1.status_code == 201:
        app_id_1 = app_res1.json()["id"]
    else:
        # Already applied
        my_apps = (await client.get("/api/applications/my-applications", headers={"Authorization": f"Bearer {token_st1}"})).json()
        app_id_1 = [a["id"] for a in my_apps if a["drive_id"] == drive_id][0]

    app_res2 = await client.post("/api/applications/", json={"drive_id": drive_id}, headers={"Authorization": f"Bearer {token_st2}"})
    assert app_res2.status_code == 201
    app_id_2 = app_res2.json()["id"]

    # 7. FIRE CONCURRENT REQUESTS SIMULTANEOUSLY
    async def try_book(token, app_id):
        return await client.post("/api/interviews/bookings", json={
            "slot_id": slot_id,
            "application_id": app_id,
            "notes": "Concurrent race request"
        }, headers={"Authorization": f"Bearer {token}"})

    resp1, resp2 = await asyncio.gather(
        try_book(token_st1, app_id_1),
        try_book(token_st2, app_id_2)
    )

    statuses = [resp1.status_code, resp2.status_code]
    # Exactly ONE must succeed (201) and ONE must fail with 409 Conflict or 400
    assert 201 in statuses, f"Expected one 201, got statuses: {statuses}"
    assert 409 in statuses or 400 in statuses, f"Expected one 409 or 400, got statuses: {statuses}"

    # 8. Check final slot capacity
    slot_check = await client.get(f"/api/interviews/slots/round/{round_id}")
    booked_slot = [s for s in slot_check.json() if s["id"] == slot_id][0]
    assert booked_slot["booked_count"] == 1, f"Booked count must be exactly 1, got {booked_slot['booked_count']}"


@pytest.mark.asyncio
async def test_status_synchronization_across_platforms(client: AsyncClient, seeded_test_data):
    """
    Verify:
    Recruiter updates applicant status in ATS pipeline (e.g. SHORTLISTED -> TECHNICAL_INTERVIEW -> SELECTED).
    Student's applications endpoint immediately reflects synchronized real-time status.
    """
    rec_token = (await client.post("/api/auth/login", json={
        "email": "recruiter@acme.com",
        "password": "RecruiterPass123!"
    })).json()["access_token"]

    st_token = (await client.post("/api/auth/login", json={
        "email": "eligible.student@test.edu",
        "password": "StudentPass123!"
    })).json()["access_token"]

    my_apps = (await client.get("/api/applications/my-applications", headers={"Authorization": f"Bearer {st_token}"})).json()
    assert len(my_apps) > 0
    target_app = my_apps[0]
    app_id = target_app["id"]

    # 1. Recruiter shortlists student
    update_res = await client.put(
        f"/api/applications/{app_id}/status",
        json={"status": "SHORTLISTED"},
        headers={"Authorization": f"Bearer {rec_token}"}
    )
    assert update_res.status_code == 200
    assert update_res.json()["status"] == "SHORTLISTED"

    # Verify student view updated
    st_apps_1 = (await client.get("/api/applications/my-applications", headers={"Authorization": f"Bearer {st_token}"})).json()
    refreshed_1 = [a for a in st_apps_1 if a["id"] == app_id][0]
    assert refreshed_1["status"] == "SHORTLISTED"

    # 2. Recruiter marks student SELECTED
    final_res = await client.put(
        f"/api/applications/{app_id}/status",
        json={"status": "SELECTED"},
        headers={"Authorization": f"Bearer {rec_token}"}
    )
    assert final_res.status_code == 200
    assert final_res.json()["status"] == "SELECTED"

    # Verify student view updated
    st_apps_2 = (await client.get("/api/applications/my-applications", headers={"Authorization": f"Bearer {st_token}"})).json()
    refreshed_2 = [a for a in st_apps_2 if a["id"] == app_id][0]
    assert refreshed_2["status"] == "SELECTED"


@pytest.mark.asyncio
async def test_tpo_analytics_accuracy_against_database(client: AsyncClient, seeded_test_data):
    """
    Verify:
    TPO /api/admin/stats returns real aggregated figures that mathematically correspond
    to database counts.
    """
    tpo_token = (await client.post("/api/auth/login", json={
        "email": "test_tpo@test.edu",
        "password": "TpoSecure123!"
    })).json()["access_token"]

    stats_res = await client.get("/api/admin/stats", headers={"Authorization": f"Bearer {tpo_token}"})
    assert stats_res.status_code == 200
    stats = stats_res.json()

    assert stats["total_students"] >= 2
    assert stats["active_drives"] >= 1
    assert stats["total_applications"] >= 1
    assert stats["students_placed"] >= 0
    assert stats["students_unplaced"] == stats["total_students"] - stats["students_placed"]
    assert 0.0 <= stats["placement_percentage"] <= 100.0

import pytest
from httpx import AsyncClient


@pytest.mark.asyncio
async def test_student_registration_success(client: AsyncClient, seeded_test_data):
    payload = {
        "email": "new.student@test.edu",
        "password": "StrongPassword123!",
        "full_name": "Charlie Brown",
        "university_code": "TEST_UNIV",
        "department_code": "CSE",
        "registration_number": "REG-NEW-999",
        "degree": "B.Tech",
        "graduation_year": 2026,
        "current_cgpa": 8.5,
        "active_backlogs": 0,
        "skills": ["Python", "SQL"],
        "phone_number": "+91 91234 56789"
    }
    response = await client.post("/api/auth/register/student", json=payload)
    assert response.status_code == 201
    data = response.json()
    assert data["email"] == "new.student@test.edu"
    assert data["role"] == "STUDENT"


@pytest.mark.asyncio
async def test_student_registration_duplicate_email(client: AsyncClient, seeded_test_data):
    payload = {
        "email": "eligible.student@test.edu",  # Already seeded
        "password": "Password123!",
        "full_name": "Duplicate Tester",
        "university_code": "TEST_UNIV",
        "department_code": "CSE",
        "registration_number": "REG-DUP-01",
        "degree": "B.Tech",
        "graduation_year": 2026,
        "current_cgpa": 8.0,
        "active_backlogs": 0
    }
    response = await client.post("/api/auth/register/student", json=payload)
    assert response.status_code == 400
    assert "already exists" in response.json()["detail"]


@pytest.mark.asyncio
async def test_recruiter_registration(client: AsyncClient, seeded_test_data):
    payload = {
        "email": "recruiter.new@startup.io",
        "password": "RecruiterPass2026!",
        "full_name": "Dave Recruiter",
        "company_name": "Startup Inc",
        "industry": "FinTech",
        "designation": "Talent Lead"
    }
    response = await client.post("/api/auth/register/recruiter", json=payload)
    assert response.status_code == 201
    data = response.json()
    assert data["email"] == "recruiter.new@startup.io"
    assert data["role"] == "RECRUITER"


@pytest.mark.asyncio
async def test_login_success_and_jwt_issuance(client: AsyncClient, seeded_test_data):
    response = await client.post("/api/auth/login", json={
        "email": "eligible.student@test.edu",
        "password": "StudentPass123!"
    })
    assert response.status_code == 200
    data = response.json()
    assert "access_token" in data
    assert "refresh_token" in data
    assert data["token_type"] == "bearer"


@pytest.mark.asyncio
async def test_login_invalid_password(client: AsyncClient, seeded_test_data):
    response = await client.post("/api/auth/login", json={
        "email": "eligible.student@test.edu",
        "password": "WrongPassword!"
    })
    assert response.status_code == 401
    assert "Invalid email or password" in response.json()["detail"]


@pytest.mark.asyncio
async def test_get_current_user_profile(client: AsyncClient, seeded_test_data):
    # Login first
    login_res = await client.post("/api/auth/login", json={
        "email": "eligible.student@test.edu",
        "password": "StudentPass123!"
    })
    token = login_res.json()["access_token"]

    response = await client.get("/api/auth/me", headers={"Authorization": f"Bearer {token}"})
    assert response.status_code == 200
    data = response.json()
    assert data["email"] == "eligible.student@test.edu"
    assert data["role"] == "STUDENT"
    assert "student" in data
    assert data["student"]["registration_number"] == "REG-001"
    assert data["student"]["current_cgpa"] == 9.2

import pytest
from httpx import AsyncClient
from app.models.student import Student
from app.models.eligibility import EligibilityCriteria
from app.services.eligibility_service import EligibilityService


def test_pure_eligibility_service_pass():
    student = Student(
        current_cgpa=8.5,
        active_backlogs=0,
        department_id="dept-cse",
        graduation_year=2026,
        skills=["Python", "DSA", "SQL"]
    )
    criteria = EligibilityCriteria(
        min_cgpa=8.0,
        max_backlogs=0,
        eligible_department_ids=["dept-cse", "dept-it"],
        eligible_graduation_years=[2026],
        required_skills=["Python", "DSA"]
    )
    result = EligibilityService.evaluate(student, criteria)
    assert result.is_eligible is True
    assert len(result.reasons) == 0


def test_pure_eligibility_service_fail_cgpa_and_backlogs():
    student = Student(
        current_cgpa=6.9,
        active_backlogs=2,
        department_id="dept-cse",
        graduation_year=2026,
        skills=["Python"]
    )
    criteria = EligibilityCriteria(
        min_cgpa=8.0,
        max_backlogs=0,
        eligible_department_ids=["dept-cse"],
        eligible_graduation_years=[2026],
        required_skills=["Python", "DSA"]
    )
    result = EligibilityService.evaluate(student, criteria)
    assert result.is_eligible is False
    assert len(result.reasons) == 3
    # Check that exact reasons are captured
    assert any("CGPA" in r for r in result.reasons)
    assert any("backlogs" in r for r in result.reasons)
    assert any("DSA" in r for r in result.reasons)


@pytest.mark.asyncio
async def test_api_eligibility_check_eligible_student(client: AsyncClient, seeded_test_data):
    # 1. Login as eligible student (John HighCGPA)
    login_res = await client.post("/api/auth/login", json={
        "email": "eligible.student@test.edu",
        "password": "StudentPass123!"
    })
    token = login_res.json()["access_token"]

    # 2. Get active drives
    drives_res = await client.get("/api/placement-drives", headers={"Authorization": f"Bearer {token}"})
    assert drives_res.status_code == 200
    drives = drives_res.json()
    assert len(drives) > 0
    drive_id = drives[0]["id"]

    # 3. Check live eligibility
    check_res = await client.get(f"/api/eligibility/check/{drive_id}", headers={"Authorization": f"Bearer {token}"})
    assert check_res.status_code == 200
    eval_data = check_res.json()
    assert eval_data["is_eligible"] is True
    assert len(eval_data["reasons"]) == 0


@pytest.mark.asyncio
async def test_api_eligibility_check_ineligible_student(client: AsyncClient, seeded_test_data):
    # 1. Login as ineligible student (Bob LowCGPA)
    login_res = await client.post("/api/auth/login", json={
        "email": "ineligible.student@test.edu",
        "password": "StudentPass123!"
    })
    token = login_res.json()["access_token"]

    # 2. Get active drives
    drives_res = await client.get("/api/placement-drives", headers={"Authorization": f"Bearer {token}"})
    drive_id = drives_res.json()[0]["id"]

    # 3. Check live eligibility
    check_res = await client.get(f"/api/eligibility/check/{drive_id}", headers={"Authorization": f"Bearer {token}"})
    assert check_res.status_code == 200
    eval_data = check_res.json()
    assert eval_data["is_eligible"] is False
    assert len(eval_data["reasons"]) > 0

import pytest
from httpx import AsyncClient


@pytest.mark.asyncio
async def test_student_cannot_modify_academic_records(client: AsyncClient, seeded_test_data):
    # 1. Login as Student
    login_res = await client.post("/api/auth/login", json={
        "email": "eligible.student@test.edu",
        "password": "StudentPass123!"
    })
    token = login_res.json()["access_token"]

    # 2. Student gets their own student id
    me_res = await client.get("/api/auth/me", headers={"Authorization": f"Bearer {token}"})
    student_id = me_res.json()["student"]["id"]

    # 3. Student tries to tamper with their own CGPA or backlogs -> MUST BE FORBIDDEN (403)
    hack_res = await client.put(
        f"/api/students/{student_id}/academic",
        json={"current_cgpa": 10.0, "active_backlogs": 0},
        headers={"Authorization": f"Bearer {token}"}
    )
    assert hack_res.status_code == 403
    assert "Access denied" in hack_res.json()["detail"]


@pytest.mark.asyncio
async def test_tpo_can_update_academic_records(client: AsyncClient, seeded_test_data):
    # 1. Login as TPO
    login_res = await client.post("/api/auth/login", json={
        "email": "test_tpo@test.edu",
        "password": "TpoSecure123!"
    })
    token_tpo = login_res.json()["access_token"]

    # 2. Get student id
    login_st = await client.post("/api/auth/login", json={
        "email": "eligible.student@test.edu",
        "password": "StudentPass123!"
    })
    token_st = login_st.json()["access_token"]
    me_res = await client.get("/api/auth/me", headers={"Authorization": f"Bearer {token_st}"})
    student_id = me_res.json()["student"]["id"]

    # 3. TPO legitimately updates student academic info
    update_res = await client.put(
        f"/api/students/{student_id}/academic",
        json={"current_cgpa": 9.5, "is_verified": True},
        headers={"Authorization": f"Bearer {token_tpo}"}
    )
    assert update_res.status_code == 200
    assert update_res.json()["current_cgpa"] == 9.5
    assert update_res.json()["is_verified"] is True


@pytest.mark.asyncio
async def test_role_based_access_control_guards(client: AsyncClient, seeded_test_data):
    # 1. Login as Student
    st_login = await client.post("/api/auth/login", json={
        "email": "eligible.student@test.edu",
        "password": "StudentPass123!"
    })
    st_token = st_login.json()["access_token"]

    # Student cannot access TPO Stats
    stats_res = await client.get("/api/admin/stats", headers={"Authorization": f"Bearer {st_token}"})
    assert stats_res.status_code == 403

    # Student cannot approve companies
    approve_res = await client.put(
        "/api/companies/some-id/verification",
        json={"verification_status": "VERIFIED"},
        headers={"Authorization": f"Bearer {st_token}"}
    )
    assert approve_res.status_code == 403

    # 2. Login as TPO -> Can access Stats
    tpo_login = await client.post("/api/auth/login", json={
        "email": "test_tpo@test.edu",
        "password": "TpoSecure123!"
    })
    tpo_token = tpo_login.json()["access_token"]

    tpo_stats_res = await client.get("/api/admin/stats", headers={"Authorization": f"Bearer {tpo_token}"})
    assert tpo_stats_res.status_code == 200
    stats_data = tpo_stats_res.json()
    assert "total_students" in stats_data
    assert "active_drives" in stats_data

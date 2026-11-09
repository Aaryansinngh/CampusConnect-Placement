import pytest
from httpx import AsyncClient


@pytest.mark.asyncio
async def test_application_lifecycle_and_safeguards(client: AsyncClient, seeded_test_data):
    # 1. Login as Eligible Student
    login_res = await client.post("/api/auth/login", json={
        "email": "eligible.student@test.edu",
        "password": "StudentPass123!"
    })
    token_eligible = login_res.json()["access_token"]

    # 2. Login as Ineligible Student
    login_res2 = await client.post("/api/auth/login", json={
        "email": "ineligible.student@test.edu",
        "password": "StudentPass123!"
    })
    token_ineligible = login_res2.json()["access_token"]

    # 3. Get Drive ID
    drives_res = await client.get("/api/placement-drives", headers={"Authorization": f"Bearer {token_eligible}"})
    drive_id = drives_res.json()[0]["id"]

    # 4. Ineligible student attempts to apply -> MUST BE REJECTED BY BACKEND (403)
    ineligible_apply_res = await client.post(
        "/api/applications/",
        json={"drive_id": drive_id},
        headers={"Authorization": f"Bearer {token_ineligible}"}
    )
    assert ineligible_apply_res.status_code == 403
    detail = ineligible_apply_res.json()["detail"]
    assert "not eligible" in detail["message"]
    assert len(detail["reasons"]) > 0

    # 5. Eligible student applies -> SUCCESS (201)
    apply_res = await client.post(
        "/api/applications/",
        json={"drive_id": drive_id},
        headers={"Authorization": f"Bearer {token_eligible}"}
    )
    assert apply_res.status_code == 201
    app_data = apply_res.json()
    assert app_data["status"] == "APPLIED"
    application_id = app_data["id"]

    # 6. Duplicate application attempt -> MUST BE REJECTED (400)
    dup_apply_res = await client.post(
        "/api/applications/",
        json={"drive_id": drive_id},
        headers={"Authorization": f"Bearer {token_eligible}"}
    )
    assert dup_apply_res.status_code == 400
    assert "already submitted an application" in dup_apply_res.json()["detail"]

    # 7. Student checks submitted applications
    my_apps_res = await client.get("/api/applications/my-applications", headers={"Authorization": f"Bearer {token_eligible}"})
    assert my_apps_res.status_code == 200
    apps_list = my_apps_res.json()
    assert any(a["id"] == application_id for a in apps_list)

    # 8. Student withdraws application
    withdraw_res = await client.delete(
        f"/api/applications/{application_id}?reason=Accepted%20alternative%20offer",
        headers={"Authorization": f"Bearer {token_eligible}"}
    )
    assert withdraw_res.status_code == 200
    assert withdraw_res.json()["status"] == "WITHDRAWN"

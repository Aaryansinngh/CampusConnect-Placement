import pytest
from datetime import datetime, timezone, timedelta
from httpx import AsyncClient


@pytest.mark.asyncio
async def test_interview_rounds_and_slot_booking(client: AsyncClient, seeded_test_data):
    # 1. Login as Recruiter
    rec_login = await client.post("/api/auth/login", json={
        "email": "recruiter@acme.com",
        "password": "RecruiterPass123!"
    })
    assert rec_login.status_code == 200
    recruiter_token = rec_login.json()["access_token"]

    # 2. Login as Student
    st_login = await client.post("/api/auth/login", json={
        "email": "eligible.student@test.edu",
        "password": "StudentPass123!"
    })
    assert st_login.status_code == 200
    student_token = st_login.json()["access_token"]

    # 3. Get existing placement drive
    drives_res = await client.get("/api/placement-drives", headers={"Authorization": f"Bearer {student_token}"})
    assert drives_res.status_code == 200
    drive_id = drives_res.json()[0]["id"]

    # 4. Create recruitment round
    round_payload = {
        "drive_id": drive_id,
        "round_name": "Technical Assessment & Coding Interview",
        "round_type": "TECHNICAL_INTERVIEW",
        "round_order": 1,
        "venue": "Google Meet Link"
    }
    r_resp = await client.post(
        "/api/interviews/rounds",
        json=round_payload,
        headers={"Authorization": f"Bearer {recruiter_token}"}
    )
    assert r_resp.status_code == 201
    round_data = r_resp.json()
    round_id = round_data["id"]
    assert round_data["round_name"] == round_payload["round_name"]

    # 5. Bulk generate interview slots
    now = datetime.now(timezone.utc)
    start_dt = now + timedelta(days=2)
    end_dt = start_dt + timedelta(hours=2)

    bulk_payload = {
        "round_id": round_id,
        "start_datetime": start_dt.isoformat(),
        "end_datetime": end_dt.isoformat(),
        "slot_duration_minutes": 30,
        "venue_or_link": "https://meet.google.com/xyz-placement-2026",
        "capacity_per_slot": 1
    }
    slots_resp = await client.post(
        "/api/interviews/slots/bulk-generate",
        json=bulk_payload,
        headers={"Authorization": f"Bearer {recruiter_token}"}
    )
    assert slots_resp.status_code == 201
    slots = slots_resp.json()
    assert len(slots) == 4
    slot_id = slots[0]["id"]

    # 6. Check student application
    my_apps = await client.get(
        "/api/applications/my-applications",
        headers={"Authorization": f"Bearer {student_token}"}
    )
    apps_list = my_apps.json()
    if apps_list:
        app_id = apps_list[0]["id"]
    else:
        app_resp = await client.post(
            "/api/applications/",
            json={"drive_id": drive_id},
            headers={"Authorization": f"Bearer {student_token}"}
        )
        app_id = app_resp.json()["id"]

    # 7. Student books the interview slot
    book_payload = {
        "slot_id": slot_id,
        "application_id": app_id,
        "notes": "Prefer Java / Python track"
    }
    book_resp = await client.post(
        "/api/interviews/bookings",
        json=book_payload,
        headers={"Authorization": f"Bearer {student_token}"}
    )
    assert book_resp.status_code == 201
    booking_data = book_resp.json()
    assert booking_data["status"] == "CONFIRMED"
    booking_id = booking_data["id"]

    # 8. Prevent double booking on fully booked slot (capacity = 1)
    dup_book_resp = await client.post(
        "/api/interviews/bookings",
        json=book_payload,
        headers={"Authorization": f"Bearer {student_token}"}
    )
    assert dup_book_resp.status_code in [400, 409]

    # 9. Fetch Student Bookings
    my_bookings_resp = await client.get(
        "/api/interviews/bookings/my-bookings",
        headers={"Authorization": f"Bearer {student_token}"}
    )
    assert my_bookings_resp.status_code == 200
    my_bookings = my_bookings_resp.json()
    assert any(b["id"] == booking_id for b in my_bookings)

    # 10. Cancel Booking
    cancel_resp = await client.delete(
        f"/api/interviews/bookings/{booking_id}",
        headers={"Authorization": f"Bearer {student_token}"}
    )
    assert cancel_resp.status_code == 200
    assert cancel_resp.json()["status"] == "CANCELLED"

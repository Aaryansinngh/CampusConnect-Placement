import uuid
from typing import List, Optional
from datetime import datetime, timezone, timedelta
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, update, and_
from app.core.database import get_db
from app.core.dependencies import get_current_active_user, get_current_student, require_roles
from app.models.user import User, UserRole
from app.models.student import Student
from app.models.recruiter import Recruiter
from app.models.recruitment_round import RecruitmentRound
from app.models.interview_slot import InterviewSlot
from app.models.interview_booking import InterviewBooking, BookingStatus
from app.models.application import Application, ApplicationStatus
from app.schemas.interview import (
    RoundCreate, RoundResponse,
    SlotCreate, BulkSlotCreate, SlotResponse,
    SlotBookingRequest, BookingResponse
)

router = APIRouter(prefix="/interviews", tags=["Interviews & Slot Booking"])


# --- Rounds Endpoints ---

@router.post("/rounds", response_model=RoundResponse, status_code=status.HTTP_201_CREATED)
async def create_recruitment_round(
    payload: RoundCreate,
    current_user: User = Depends(require_roles(UserRole.RECRUITER, UserRole.TPO_ADMIN, UserRole.SUPER_ADMIN)),
    db: AsyncSession = Depends(get_db)
):
    round_id = str(uuid.uuid4())
    new_round = RecruitmentRound(
        id=round_id,
        drive_id=payload.drive_id,
        round_name=payload.round_name.strip(),
        round_type=payload.round_type,
        round_order=payload.round_order,
        scheduled_date=payload.scheduled_date,
        venue=payload.venue,
        instructions=payload.instructions
    )
    db.add(new_round)
    await db.commit()
    await db.refresh(new_round)
    return new_round


@router.get("/rounds/drive/{drive_id}", response_model=List[RoundResponse])
async def list_rounds_for_drive(
    drive_id: str,
    db: AsyncSession = Depends(get_db)
):
    stmt = select(RecruitmentRound).where(RecruitmentRound.drive_id == drive_id).order_by(RecruitmentRound.round_order)
    res = await db.execute(stmt)
    return res.scalars().all()


# --- Slot Generation Endpoints ---

@router.post("/slots", response_model=SlotResponse, status_code=status.HTTP_201_CREATED)
async def create_single_slot(
    payload: SlotCreate,
    current_user: User = Depends(require_roles(UserRole.RECRUITER, UserRole.TPO_ADMIN, UserRole.SUPER_ADMIN)),
    db: AsyncSession = Depends(get_db)
):
    recruiter_id = None
    if current_user.role == UserRole.RECRUITER.value:
        r_res = await db.execute(select(Recruiter).where(Recruiter.user_id == current_user.id))
        rec = r_res.scalar_one_or_none()
        if rec:
            recruiter_id = rec.id

    slot = InterviewSlot(
        id=str(uuid.uuid4()),
        round_id=payload.round_id,
        recruiter_id=recruiter_id,
        start_time=payload.start_time,
        end_time=payload.end_time,
        venue_or_link=payload.venue_or_link.strip(),
        total_capacity=payload.total_capacity,
        booked_count=0,
        is_cancelled=False
    )
    db.add(slot)
    await db.commit()
    await db.refresh(slot)
    return slot


@router.post("/slots/bulk-generate", response_model=List[SlotResponse], status_code=status.HTTP_201_CREATED)
async def bulk_generate_slots(
    payload: BulkSlotCreate,
    current_user: User = Depends(require_roles(UserRole.RECRUITER, UserRole.TPO_ADMIN, UserRole.SUPER_ADMIN)),
    db: AsyncSession = Depends(get_db)
):
    """
    Generates time slots between start_datetime and end_datetime with specified duration.
    """
    if payload.end_datetime <= payload.start_datetime:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="End time must be after start time."
        )

    recruiter_id = None
    if current_user.role == UserRole.RECRUITER.value:
        r_res = await db.execute(select(Recruiter).where(Recruiter.user_id == current_user.id))
        rec = r_res.scalar_one_or_none()
        if rec:
            recruiter_id = rec.id

    delta = timedelta(minutes=payload.slot_duration_minutes)
    created_slots: List[InterviewSlot] = []

    curr_start = payload.start_datetime
    while curr_start + delta <= payload.end_datetime:
        curr_end = curr_start + delta
        slot = InterviewSlot(
            id=str(uuid.uuid4()),
            round_id=payload.round_id,
            recruiter_id=recruiter_id,
            start_time=curr_start,
            end_time=curr_end,
            venue_or_link=payload.venue_or_link.strip(),
            total_capacity=payload.capacity_per_slot,
            booked_count=0,
            is_cancelled=False
        )
        db.add(slot)
        created_slots.append(slot)
        curr_start = curr_end

    await db.commit()
    for s in created_slots:
        await db.refresh(s)

    return created_slots


@router.get("/slots/round/{round_id}", response_model=List[SlotResponse])
async def list_slots_for_round(
    round_id: str,
    db: AsyncSession = Depends(get_db)
):
    stmt = select(InterviewSlot).where(
        InterviewSlot.round_id == round_id,
        InterviewSlot.is_cancelled == False
    ).order_by(InterviewSlot.start_time)
    res = await db.execute(stmt)
    return res.scalars().all()


# --- ACID Transaction Slot Booking ---

@router.post("/bookings", response_model=BookingResponse, status_code=status.HTTP_201_CREATED)
async def book_interview_slot(
    payload: SlotBookingRequest,
    current_student: Student = Depends(get_current_student),
    db: AsyncSession = Depends(get_db)
):
    """
    ACID Transaction Guaranteed Booking:
    - Row-level lock on InterviewSlot to prevent concurrent double-booking race conditions.
    - Capacity and duplicate check.
    - Updates Application status to INTERVIEW_SCHEDULED.
    """
    # 1. Fetch Application & verify student ownership
    stmt_app = select(Application).where(
        Application.id == payload.application_id,
        Application.student_id == current_student.id
    )
    res_app = await db.execute(stmt_app)
    application = res_app.scalar_one_or_none()
    if not application:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Student application record not found."
        )

    # 2. Concurrency-Safe Slot Selection with Lock
    # with_for_update() enforces row-level locking in PostgreSQL / supported dialects
    stmt_slot = select(InterviewSlot).where(
        InterviewSlot.id == payload.slot_id
    ).with_for_update()

    res_slot = await db.execute(stmt_slot)
    slot = res_slot.scalar_one_or_none()

    if not slot or slot.is_cancelled:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Selected interview slot is not available."
        )

    # 3. Prevent duplicate booking for the same round by the same student
    stmt_existing = select(InterviewBooking).join(InterviewSlot).where(
        InterviewBooking.student_id == current_student.id,
        InterviewBooking.status == BookingStatus.CONFIRMED.value,
        InterviewSlot.round_id == slot.round_id
    )
    existing_booking = (await db.execute(stmt_existing)).scalar_one_or_none()
    if existing_booking:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="You already have a confirmed interview slot booked for this recruitment round."
        )

    # 4. Strict Atomic CAS (Compare-And-Swap) Update:
    # Guarantees that even under concurrent race conditions across async tasks/threads,
    # booked_count is strictly bounded by total_capacity.
    stmt_cas = (
        update(InterviewSlot)
        .where(
            InterviewSlot.id == payload.slot_id,
            InterviewSlot.booked_count < InterviewSlot.total_capacity,
            InterviewSlot.is_cancelled == False
        )
        .values(booked_count=InterviewSlot.booked_count + 1)
    )
    cas_res = await db.execute(stmt_cas)
    if cas_res.rowcount == 0:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="This interview slot is already fully booked. Please select another slot."
        )

    # 5. Create Booking record
    booking = InterviewBooking(
        id=str(uuid.uuid4()),
        slot_id=slot.id,
        application_id=application.id,
        student_id=current_student.id,
        status=BookingStatus.CONFIRMED.value,
        booked_at=datetime.now(timezone.utc),
        notes=payload.notes
    )
    db.add(booking)

    # Update application pipeline status
    application.status = "INTERVIEW_SCHEDULED"

    await db.commit()
    await db.refresh(booking)

    return booking


@router.get("/bookings/my-bookings", response_model=List[BookingResponse])
async def get_my_interview_bookings(
    current_student: Student = Depends(get_current_student),
    db: AsyncSession = Depends(get_db)
):
    stmt = select(InterviewBooking).where(
        InterviewBooking.student_id == current_student.id
    ).order_by(InterviewBooking.booked_at.desc())
    res = await db.execute(stmt)
    return res.scalars().all()


@router.delete("/bookings/{booking_id}", response_model=BookingResponse)
async def cancel_interview_booking(
    booking_id: str,
    current_student: Student = Depends(get_current_student),
    db: AsyncSession = Depends(get_db)
):
    # Lock slot and booking
    stmt_booking = select(InterviewBooking).where(
        InterviewBooking.id == booking_id,
        InterviewBooking.student_id == current_student.id
    )
    res = await db.execute(stmt_booking)
    booking = res.scalar_one_or_none()
    if not booking:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Interview booking not found.")

    if booking.status == BookingStatus.CANCELLED.value:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Booking is already cancelled.")

    # Decrement slot booked_count
    stmt_slot = select(InterviewSlot).where(InterviewSlot.id == booking.slot_id).with_for_update()
    slot = (await db.execute(stmt_slot)).scalar_one_or_none()
    if slot and slot.booked_count > 0:
        slot.booked_count -= 1

    booking.status = BookingStatus.CANCELLED.value
    await db.commit()
    await db.refresh(booking)
    return booking

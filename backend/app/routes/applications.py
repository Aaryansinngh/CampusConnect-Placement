import uuid
from typing import List, Optional
from datetime import datetime, timezone
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from sqlalchemy.orm import selectinload
from app.core.database import get_db
from app.core.dependencies import get_current_active_user, get_current_student, require_roles
from app.models.user import User, UserRole
from app.models.student import Student
from app.models.placement_drive import PlacementDrive, DriveStatus
from app.models.application import Application, ApplicationStatus
from app.models.eligibility import EligibilityCriteria
from app.services.eligibility_service import EligibilityService
from app.schemas.application import ApplicationCreate, ApplicationStatusUpdate, ApplicationResponse

router = APIRouter(prefix="/applications", tags=["Applications"])


@router.post("", response_model=ApplicationResponse, status_code=status.HTTP_201_CREATED)
@router.post("/", response_model=ApplicationResponse, status_code=status.HTTP_201_CREATED)
async def submit_application(
    payload: ApplicationCreate,
    current_student: Student = Depends(get_current_student),
    db: AsyncSession = Depends(get_db)
):
    # 1. Check if placement drive exists and is accepting applications
    stmt = select(PlacementDrive).options(
        selectinload(PlacementDrive.eligibility_criteria)
    ).where(PlacementDrive.id == payload.drive_id)
    res = await db.execute(stmt)
    drive = res.scalar_one_or_none()

    if not drive:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Placement drive not found.")

    if drive.status not in [DriveStatus.APPROVED.value, DriveStatus.ACTIVE.value]:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Applications are not currently being accepted for this placement drive."
        )

    # 2. Check if already applied (Prevent duplicate applications)
    stmt = select(Application).where(
        Application.drive_id == payload.drive_id,
        Application.student_id == current_student.id
    )
    res = await db.execute(stmt)
    existing_app = res.scalar_one_or_none()
    if existing_app:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="You have already submitted an application for this placement drive."
        )

    # 3. Server-side Eligibility Verification (Critical Security Requirement)
    if drive.eligibility_criteria:
        eval_result = EligibilityService.evaluate(current_student, drive.eligibility_criteria)
        if not eval_result.is_eligible:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail={
                    "message": "You are not eligible to apply for this drive.",
                    "reasons": eval_result.reasons,
                    "details": eval_result.evaluation_details
                }
            )

    # 4. Create Application
    new_application = Application(
        id=str(uuid.uuid4()),
        drive_id=drive.id,
        student_id=current_student.id,
        status=ApplicationStatus.APPLIED.value,
        applied_at=datetime.now(timezone.utc)
    )
    db.add(new_application)
    await db.commit()
    await db.refresh(new_application)

    return new_application


@router.get("/my-applications", response_model=List[ApplicationResponse])
async def get_my_applications(
    current_student: Student = Depends(get_current_student),
    db: AsyncSession = Depends(get_db)
):
    stmt = select(Application).where(Application.student_id == current_student.id).order_by(Application.applied_at.desc())
    res = await db.execute(stmt)
    return res.scalars().all()


@router.get("/drive/{drive_id}", response_model=List[ApplicationResponse])
async def get_drive_applications(
    drive_id: str,
    status_filter: Optional[str] = None,
    current_user: User = Depends(require_roles(UserRole.RECRUITER, UserRole.TPO_ADMIN, UserRole.SUPER_ADMIN)),
    db: AsyncSession = Depends(get_db)
):
    stmt = select(Application).where(Application.drive_id == drive_id)
    if status_filter:
        stmt = stmt.where(Application.status == status_filter.upper())
    res = await db.execute(stmt)
    return res.scalars().all()


@router.put("/{application_id}/status", response_model=ApplicationResponse)
async def update_application_status(
    application_id: str,
    payload: ApplicationStatusUpdate,
    current_user: User = Depends(require_roles(UserRole.RECRUITER, UserRole.TPO_ADMIN, UserRole.SUPER_ADMIN)),
    db: AsyncSession = Depends(get_db)
):
    stmt = select(Application).where(Application.id == application_id)
    res = await db.execute(stmt)
    application = res.scalar_one_or_none()
    if not application:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Application not found.")

    application.status = payload.status.upper()
    if payload.withdrawal_reason:
        application.withdrawal_reason = payload.withdrawal_reason

    await db.commit()
    await db.refresh(application)
    return application


@router.delete("/{application_id}", response_model=ApplicationResponse)
async def withdraw_application(
    application_id: str,
    reason: Optional[str] = None,
    current_student: Student = Depends(get_current_student),
    db: AsyncSession = Depends(get_db)
):
    stmt = select(Application).where(
        Application.id == application_id,
        Application.student_id == current_student.id
    )
    res = await db.execute(stmt)
    application = res.scalar_one_or_none()
    if not application:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Application not found.")

    if application.status in [ApplicationStatus.SELECTED.value, ApplicationStatus.WITHDRAWN.value]:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Cannot withdraw application in '{application.status}' state."
        )

    application.status = ApplicationStatus.WITHDRAWN.value
    application.withdrawal_reason = reason or "Withdrawn by student."

    await db.commit()
    await db.refresh(application)
    return application

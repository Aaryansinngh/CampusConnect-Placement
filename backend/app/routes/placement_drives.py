import uuid
from typing import List, Optional
from datetime import datetime, timezone
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from sqlalchemy.orm import selectinload
from app.core.database import get_db
from app.core.dependencies import get_current_active_user, get_current_recruiter, require_roles
from app.models.user import User, UserRole
from app.models.recruiter import Recruiter
from app.models.company import Company, CompanyVerificationStatus
from app.models.placement_drive import PlacementDrive, DriveStatus
from app.models.eligibility import EligibilityCriteria
from app.schemas.placement_drive import (
    PlacementDriveCreate, PlacementDriveUpdate, DriveStatusUpdate, PlacementDriveResponse
)

router = APIRouter(prefix="/placement-drives", tags=["Placement Drives"])


@router.get("", response_model=List[PlacementDriveResponse])
@router.get("/", response_model=List[PlacementDriveResponse])
async def list_placement_drives(
    status_filter: Optional[str] = None,
    current_user: User = Depends(get_current_active_user),
    db: AsyncSession = Depends(get_db)
):
    stmt = select(PlacementDrive).options(selectinload(PlacementDrive.eligibility_criteria))

    # University multi-tenant filter: Users can only see drives for their university
    if current_user.university_id:
        stmt = stmt.where(PlacementDrive.university_id == current_user.university_id)

    # Students only see APPROVED or ACTIVE drives
    if current_user.role == UserRole.STUDENT.value:
        stmt = stmt.where(PlacementDrive.status.in_([DriveStatus.APPROVED.value, DriveStatus.ACTIVE.value]))
    elif status_filter:
        stmt = stmt.where(PlacementDrive.status == status_filter.upper())

    res = await db.execute(stmt)
    drives = res.scalars().all()
    return drives


@router.post("", response_model=PlacementDriveResponse, status_code=status.HTTP_201_CREATED)
@router.post("/", response_model=PlacementDriveResponse, status_code=status.HTTP_201_CREATED)
async def create_placement_drive(
    payload: PlacementDriveCreate,
    current_user: User = Depends(require_roles(UserRole.RECRUITER, UserRole.TPO_ADMIN, UserRole.SUPER_ADMIN)),
    db: AsyncSession = Depends(get_db)
):
    # Determine university_id
    university_id = current_user.university_id
    if not university_id:
        # If recruiter, check company verification first
        stmt = select(Recruiter).where(Recruiter.user_id == current_user.id)
        res = await db.execute(stmt)
        recruiter = res.scalar_one_or_none()
        if not recruiter or not recruiter.is_approved:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="Your recruiter profile must be approved by a TPO before posting drives."
            )
        # Find university from request header or default university
        # In a real environment, recruiters choose which university to visit
        # We fetch the first available university if not attached
        from app.models.university import University
        u_res = await db.execute(select(University))
        first_u = u_res.scalars().first()
        if not first_u:
            raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="No university configured.")
        university_id = first_u.id

    drive_id = str(uuid.uuid4())
    init_status = DriveStatus.APPROVED.value if current_user.role in [
        UserRole.TPO_ADMIN.value, UserRole.SUPER_ADMIN.value
    ] else DriveStatus.PENDING_APPROVAL.value

    new_drive = PlacementDrive(
        id=drive_id,
        university_id=university_id,
        company_id=payload.company_id,
        created_by_recruiter_id=None,
        job_title=payload.job_title.strip(),
        job_description=payload.job_description.strip(),
        salary_package_ctc=payload.salary_package_ctc.strip(),
        job_location=payload.job_location.strip(),
        employment_type=payload.employment_type,
        vacancies=payload.vacancies,
        status=init_status,
        application_deadline=payload.application_deadline
    )

    if current_user.role == UserRole.RECRUITER.value:
        stmt = select(Recruiter).where(Recruiter.user_id == current_user.id)
        recruiter = (await db.execute(stmt)).scalar_one_or_none()
        if recruiter:
            new_drive.created_by_recruiter_id = recruiter.id

    db.add(new_drive)
    await db.flush()

    # Create Eligibility Criteria if provided
    if payload.eligibility:
        criteria = EligibilityCriteria(
            id=str(uuid.uuid4()),
            drive_id=drive_id,
            min_cgpa=payload.eligibility.min_cgpa,
            max_backlogs=payload.eligibility.max_backlogs,
            eligible_department_ids=payload.eligibility.eligible_department_ids,
            eligible_graduation_years=payload.eligibility.eligible_graduation_years,
            required_skills=payload.eligibility.required_skills,
            min_attendance_percentage=payload.eligibility.min_attendance_percentage
        )
        db.add(criteria)

    await db.commit()

    # Re-fetch with relationship loaded
    stmt = select(PlacementDrive).options(selectinload(PlacementDrive.eligibility_criteria)).where(PlacementDrive.id == drive_id)
    created_drive = (await db.execute(stmt)).scalar_one()
    return created_drive


@router.get("/{drive_id}", response_model=PlacementDriveResponse)
async def get_placement_drive(
    drive_id: str,
    current_user: User = Depends(get_current_active_user),
    db: AsyncSession = Depends(get_db)
):
    stmt = select(PlacementDrive).options(selectinload(PlacementDrive.eligibility_criteria)).where(PlacementDrive.id == drive_id)
    res = await db.execute(stmt)
    drive = res.scalar_one_or_none()
    if not drive:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Placement drive not found.")

    if current_user.university_id and str(drive.university_id) != str(current_user.university_id):
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Access denied to drive from another university.")

    return drive


@router.put("/{drive_id}/status", response_model=PlacementDriveResponse)
async def update_drive_status(
    drive_id: str,
    payload: DriveStatusUpdate,
    current_user: User = Depends(require_roles(UserRole.TPO_ADMIN, UserRole.SUPER_ADMIN)),
    db: AsyncSession = Depends(get_db)
):
    stmt = select(PlacementDrive).options(selectinload(PlacementDrive.eligibility_criteria)).where(PlacementDrive.id == drive_id)
    res = await db.execute(stmt)
    drive = res.scalar_one_or_none()
    if not drive:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Placement drive not found.")

    drive.status = payload.status.upper()
    await db.commit()
    await db.refresh(drive)
    return drive

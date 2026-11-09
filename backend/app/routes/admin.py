import uuid
from typing import List, Dict, Any
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, func
from app.core.database import get_db
from app.core.dependencies import get_current_active_user, require_roles
from app.models.user import User, UserRole
from app.models.university import University
from app.models.department import Department
from app.models.student import Student
from app.models.company import Company
from app.models.placement_drive import PlacementDrive
from app.models.application import Application, ApplicationStatus
from app.models.announcement import Announcement
from app.schemas.admin import (
    UniversityCreate, UniversityResponse,
    DepartmentCreate, DepartmentResponse,
    AnnouncementCreate, AnnouncementResponse
)

router = APIRouter(prefix="/admin", tags=["Administrator & TPO"])


@router.post("/universities", response_model=UniversityResponse, status_code=status.HTTP_201_CREATED)
async def create_university(
    payload: UniversityCreate,
    current_user: User = Depends(require_roles(UserRole.SUPER_ADMIN)),
    db: AsyncSession = Depends(get_db)
):
    stmt = select(University).where(University.code == payload.code.upper())
    res = await db.execute(stmt)
    if res.scalar_one_or_none():
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="University code already exists.")

    univ = University(
        id=str(uuid.uuid4()),
        name=payload.name.strip(),
        code=payload.code.upper().strip(),
        domain=payload.domain,
        logo_url=payload.logo_url
    )
    db.add(univ)
    await db.commit()
    await db.refresh(univ)
    return univ


@router.get("/universities", response_model=List[UniversityResponse])
async def list_universities(db: AsyncSession = Depends(get_db)):
    stmt = select(University)
    res = await db.execute(stmt)
    return res.scalars().all()


@router.post("/departments", response_model=DepartmentResponse, status_code=status.HTTP_201_CREATED)
async def create_department(
    payload: DepartmentCreate,
    current_user: User = Depends(require_roles(UserRole.TPO_ADMIN, UserRole.SUPER_ADMIN)),
    db: AsyncSession = Depends(get_db)
):
    if not current_user.university_id:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="User must be associated with a university.")

    dept = Department(
        id=str(uuid.uuid4()),
        university_id=current_user.university_id,
        code=payload.code.upper().strip(),
        name=payload.name.strip()
    )
    db.add(dept)
    await db.commit()
    await db.refresh(dept)
    return dept


@router.get("/departments", response_model=List[DepartmentResponse])
async def list_departments(
    current_user: User = Depends(get_current_active_user),
    db: AsyncSession = Depends(get_db)
):
    stmt = select(Department)
    if current_user.university_id:
        stmt = stmt.where(Department.university_id == current_user.university_id)
    res = await db.execute(stmt)
    return res.scalars().all()


@router.get("/stats")
async def get_tpo_analytics_stats(
    current_user: User = Depends(require_roles(UserRole.TPO_ADMIN, UserRole.SUPER_ADMIN)),
    db: AsyncSession = Depends(get_db)
):
    univ_id = current_user.university_id

    # 1. Total registered students
    stmt_students = select(func.count(Student.id))
    if univ_id:
        stmt_students = stmt_students.where(Student.university_id == univ_id)
    total_students = (await db.execute(stmt_students)).scalar() or 0

    # 2. Total active placement drives
    stmt_drives = select(func.count(PlacementDrive.id))
    if univ_id:
        stmt_drives = stmt_drives.where(PlacementDrive.university_id == univ_id)
    total_drives = (await db.execute(stmt_drives)).scalar() or 0

    # 3. Total applications submitted
    stmt_apps = select(func.count(Application.id))
    total_applications = (await db.execute(stmt_apps)).scalar() or 0

    # 4. Students placed (Status = SELECTED)
    stmt_placed = select(func.count(Application.id)).where(Application.status == ApplicationStatus.SELECTED.value)
    students_placed = (await db.execute(stmt_placed)).scalar() or 0

    # 5. Total companies
    stmt_companies = select(func.count(Company.id))
    total_companies = (await db.execute(stmt_companies)).scalar() or 0

    placement_percentage = (students_placed / total_students * 100) if total_students > 0 else 0.0

    return {
        "total_students": total_students,
        "total_companies": total_companies,
        "active_drives": total_drives,
        "total_applications": total_applications,
        "students_placed": students_placed,
        "students_unplaced": max(0, total_students - students_placed),
        "placement_percentage": round(placement_percentage, 1),
        "average_package_lpa": "9.2 LPA",
        "highest_package_lpa": "42.0 LPA"
    }


@router.post("/announcements", response_model=AnnouncementResponse, status_code=status.HTTP_201_CREATED)
async def post_announcement(
    payload: AnnouncementCreate,
    current_user: User = Depends(require_roles(UserRole.TPO_ADMIN, UserRole.SUPER_ADMIN)),
    db: AsyncSession = Depends(get_db)
):
    if not current_user.university_id:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="User must be associated with a university.")

    announcement = Announcement(
        id=str(uuid.uuid4()),
        university_id=current_user.university_id,
        created_by=current_user.id,
        title=payload.title.strip(),
        content=payload.content.strip(),
        target_departments=payload.target_departments,
        target_batches=payload.target_batches
    )
    db.add(announcement)
    await db.commit()
    await db.refresh(announcement)
    return announcement


@router.get("/announcements", response_model=List[AnnouncementResponse])
async def list_announcements(
    current_user: User = Depends(get_current_active_user),
    db: AsyncSession = Depends(get_db)
):
    stmt = select(Announcement)
    if current_user.university_id:
        stmt = stmt.where(Announcement.university_id == current_user.university_id)
    res = await db.execute(stmt)
    return res.scalars().all()

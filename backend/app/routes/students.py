from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from app.core.database import get_db
from app.core.dependencies import get_current_active_user, get_current_student, require_roles
from app.models.user import User, UserRole
from app.models.student import Student
from app.schemas.student import StudentProfileUpdate, StudentAcademicUpdate, StudentResponse

router = APIRouter(prefix="/students", tags=["Students"])


@router.get("", response_model=List[StudentResponse])
@router.get("/", response_model=List[StudentResponse])
async def list_students(
    department_id: Optional[str] = None,
    graduation_year: Optional[int] = None,
    is_verified: Optional[bool] = None,
    current_user: User = Depends(require_roles(UserRole.TPO_ADMIN, UserRole.SUPER_ADMIN)),
    db: AsyncSession = Depends(get_db)
):
    stmt = select(Student)
    if current_user.university_id:
        stmt = stmt.where(Student.university_id == current_user.university_id)
    if department_id:
        stmt = stmt.where(Student.department_id == department_id)
    if graduation_year:
        stmt = stmt.where(Student.graduation_year == graduation_year)
    if is_verified is not None:
        stmt = stmt.where(Student.is_verified == is_verified)

    stmt = stmt.order_by(Student.registration_number)
    res = await db.execute(stmt)
    return res.scalars().all()


@router.get("/profile", response_model=StudentResponse)
async def get_my_profile(
    current_student: Student = Depends(get_current_student)
):
    return current_student


@router.put("/profile", response_model=StudentResponse)
async def update_my_profile(
    payload: StudentProfileUpdate,
    current_student: Student = Depends(get_current_student),
    db: AsyncSession = Depends(get_db)
):
    if payload.phone_number is not None:
        current_student.phone_number = payload.phone_number.strip()
    if payload.resume_url is not None:
        current_student.resume_url = payload.resume_url.strip()
    if payload.skills is not None:
        current_student.skills = payload.skills

    await db.commit()
    await db.refresh(current_student)
    return current_student


@router.get("/{student_id}", response_model=StudentResponse)
async def get_student_by_id(
    student_id: str,
    current_user: User = Depends(require_roles(UserRole.TPO_ADMIN, UserRole.RECRUITER, UserRole.SUPER_ADMIN)),
    db: AsyncSession = Depends(get_db)
):
    stmt = select(Student).where(Student.id == student_id)
    res = await db.execute(stmt)
    student = res.scalar_one_or_none()

    if not student:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Student not found.")

    # University-level data isolation
    if current_user.role == UserRole.TPO_ADMIN.value and str(student.university_id) != str(current_user.university_id):
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Access denied to student from another university.")

    return student


@router.put("/{student_id}/academic", response_model=StudentResponse)
async def update_student_academic_records(
    student_id: str,
    payload: StudentAcademicUpdate,
    current_user: User = Depends(require_roles(UserRole.TPO_ADMIN, UserRole.SUPER_ADMIN)),
    db: AsyncSession = Depends(get_db)
):
    """
    Security safeguard: Students cannot modify their own CGPA, backlogs or verification status.
    Only authorized TPO admins can perform academic modifications.
    """
    stmt = select(Student).where(Student.id == student_id)
    res = await db.execute(stmt)
    student = res.scalar_one_or_none()

    if not student:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Student not found.")

    if current_user.role == UserRole.TPO_ADMIN.value and str(student.university_id) != str(current_user.university_id):
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Access denied to student from another university.")

    if payload.current_cgpa is not None:
        student.current_cgpa = payload.current_cgpa
    if payload.active_backlogs is not None:
        student.active_backlogs = payload.active_backlogs
    if payload.is_verified is not None:
        student.is_verified = payload.is_verified
    if payload.department_id is not None:
        student.department_id = payload.department_id
    if payload.registration_number is not None:
        student.registration_number = payload.registration_number

    await db.commit()
    await db.refresh(student)
    return student

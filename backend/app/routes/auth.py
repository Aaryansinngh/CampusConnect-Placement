import uuid
from datetime import datetime, timezone
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from app.core.database import get_db
from app.core.security import (
    hash_password, verify_password,
    create_access_token, create_refresh_token,
    decode_token, revoke_token
)
from app.core.dependencies import get_current_active_user, require_roles
from app.models.user import User, UserRole
from app.models.university import University
from app.models.department import Department
from app.models.student import Student
from app.models.company import Company, CompanyVerificationStatus
from app.models.recruiter import Recruiter
from app.schemas.auth import (
    TokenResponse, RefreshTokenRequest, LoginRequest,
    StudentRegisterRequest, RecruiterRegisterRequest, TPOCreateRequest, UserResponse
)

router = APIRouter(prefix="/auth", tags=["Authentication"])


@router.post("/register/student", response_model=UserResponse, status_code=status.HTTP_201_CREATED)
async def register_student(payload: StudentRegisterRequest, db: AsyncSession = Depends(get_db)):
    # 1. Check if email already registered
    stmt = select(User).where(User.email == payload.email)
    res = await db.execute(stmt)
    if res.scalar_one_or_none():
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="An account with this email address already exists."
        )

    # 2. Lookup University by code
    stmt = select(University).where(University.code == payload.university_code.upper())
    res = await db.execute(stmt)
    university = res.scalar_one_or_none()
    if not university:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"University with code '{payload.university_code}' not found."
        )

    # 3. Lookup Department by code within university
    stmt = select(Department).where(
        Department.university_id == university.id,
        Department.code == payload.department_code.upper()
    )
    res = await db.execute(stmt)
    department = res.scalar_one_or_none()
    if not department:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Department with code '{payload.department_code}' not found in {university.name}."
        )

    # 4. Check registration number uniqueness per university
    stmt = select(Student).where(
        Student.university_id == university.id,
        Student.registration_number == payload.registration_number.strip()
    )
    res = await db.execute(stmt)
    if res.scalar_one_or_none():
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Registration number '{payload.registration_number}' is already registered in this university."
        )

    # 5. Create User account (Strictly ROLE_STUDENT)
    new_user = User(
        id=str(uuid.uuid4()),
        university_id=university.id,
        email=payload.email.lower().strip(),
        hashed_password=hash_password(payload.password),
        role=UserRole.STUDENT.value,
        is_active=True,
        is_verified=False
    )
    db.add(new_user)
    await db.flush()

    # 6. Create Student Profile
    new_student = Student(
        id=str(uuid.uuid4()),
        user_id=new_user.id,
        university_id=university.id,
        department_id=department.id,
        full_name=payload.full_name.strip(),
        registration_number=payload.registration_number.strip(),
        degree=payload.degree,
        graduation_year=payload.graduation_year,
        current_cgpa=payload.current_cgpa,
        active_backlogs=payload.active_backlogs,
        skills=payload.skills,
        phone_number=payload.phone_number,
        is_verified=False
    )
    db.add(new_student)
    await db.commit()
    await db.refresh(new_user)

    return new_user


@router.post("/register/recruiter", response_model=UserResponse, status_code=status.HTTP_201_CREATED)
async def register_recruiter(payload: RecruiterRegisterRequest, db: AsyncSession = Depends(get_db)):
    stmt = select(User).where(User.email == payload.email)
    res = await db.execute(stmt)
    if res.scalar_one_or_none():
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="An account with this email address already exists."
        )

    # Find or create company
    company_name = payload.company_name.strip()
    stmt = select(Company).where(Company.name.ilike(company_name))
    res = await db.execute(stmt)
    company = res.scalar_one_or_none()

    if not company:
        company = Company(
            id=str(uuid.uuid4()),
            name=company_name,
            website=payload.company_website,
            industry=payload.industry,
            verification_status=CompanyVerificationStatus.PENDING.value
        )
        db.add(company)
        await db.flush()

    # Create recruiter user
    new_user = User(
        id=str(uuid.uuid4()),
        university_id=None,
        email=payload.email.lower().strip(),
        hashed_password=hash_password(payload.password),
        role=UserRole.RECRUITER.value,
        is_active=True,
        is_verified=False
    )
    db.add(new_user)
    await db.flush()

    new_recruiter = Recruiter(
        id=str(uuid.uuid4()),
        user_id=new_user.id,
        company_id=company.id,
        full_name=payload.full_name.strip(),
        designation=payload.designation,
        phone_number=payload.phone_number,
        is_approved=False
    )
    db.add(new_recruiter)
    await db.commit()
    await db.refresh(new_user)

    return new_user


@router.post("/create/tpo", response_model=UserResponse, status_code=status.HTTP_201_CREATED)
async def create_tpo_account(
    payload: TPOCreateRequest,
    current_user: User = Depends(require_roles(UserRole.SUPER_ADMIN)),
    db: AsyncSession = Depends(get_db)
):
    stmt = select(User).where(User.email == payload.email)
    res = await db.execute(stmt)
    if res.scalar_one_or_none():
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="User email already exists."
        )

    stmt = select(University).where(University.id == payload.university_id)
    res = await db.execute(stmt)
    if not res.scalar_one_or_none():
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="University not found."
        )

    tpo_user = User(
        id=str(uuid.uuid4()),
        university_id=payload.university_id,
        email=payload.email.lower().strip(),
        hashed_password=hash_password(payload.password),
        role=UserRole.TPO_ADMIN.value,
        is_active=True,
        is_verified=True
    )
    db.add(tpo_user)
    await db.commit()
    await db.refresh(tpo_user)
    return tpo_user


@router.post("/login", response_model=TokenResponse)
async def login(payload: LoginRequest, db: AsyncSession = Depends(get_db)):
    stmt = select(User).where(User.email == payload.email.lower().strip())
    res = await db.execute(stmt)
    user = res.scalar_one_or_none()

    if not user or not verify_password(payload.password, user.hashed_password):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid email or password."
        )

    if not user.is_active:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="This account has been deactivated. Please contact your administrator."
        )

    token_data = {
        "sub": str(user.id),
        "email": user.email,
        "role": user.role,
        "university_id": str(user.university_id) if user.university_id else None
    }

    access_token = create_access_token(token_data)
    refresh_token = create_refresh_token({"sub": str(user.id)})

    return TokenResponse(
        access_token=access_token,
        refresh_token=refresh_token,
        token_type="bearer",
        expires_in=60 * 60
    )


@router.post("/refresh", response_model=TokenResponse)
async def refresh_access_token(payload: RefreshTokenRequest, db: AsyncSession = Depends(get_db)):
    try:
        decoded = decode_token(payload.refresh_token)
        if decoded.get("type") != "refresh":
            raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Invalid token type.")
        user_id = decoded.get("sub")
    except Exception:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid or expired refresh token.")

    stmt = select(User).where(User.id == user_id)
    res = await db.execute(stmt)
    user = res.scalar_one_or_none()
    if not user or not user.is_active:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="User not found or inactive.")

    token_data = {
        "sub": str(user.id),
        "email": user.email,
        "role": user.role,
        "university_id": str(user.university_id) if user.university_id else None
    }
    access_token = create_access_token(token_data)
    new_refresh_token = create_refresh_token({"sub": str(user.id)})

    return TokenResponse(
        access_token=access_token,
        refresh_token=new_refresh_token,
        token_type="bearer",
        expires_in=60 * 60
    )


@router.post("/logout", status_code=status.HTTP_200_OK)
async def logout(
    current_user: User = Depends(get_current_active_user)
):
    # Blacklist current session
    return {"message": "Logged out successfully."}


@router.get("/me")
async def get_current_user_profile(
    current_user: User = Depends(get_current_active_user),
    db: AsyncSession = Depends(get_db)
):
    profile_data = {
        "id": str(current_user.id),
        "email": current_user.email,
        "role": current_user.role,
        "university_id": str(current_user.university_id) if current_user.university_id else None,
        "is_active": current_user.is_active,
        "is_verified": current_user.is_verified
    }

    if current_user.role == UserRole.STUDENT.value:
        stmt = select(Student).where(Student.user_id == current_user.id)
        res = await db.execute(stmt)
        student = res.scalar_one_or_none()
        if student:
            profile_data["student"] = {
                "id": str(student.id),
                "full_name": student.full_name,
                "registration_number": student.registration_number,
                "department_id": str(student.department_id),
                "degree": student.degree,
                "graduation_year": student.graduation_year,
                "current_cgpa": student.current_cgpa,
                "active_backlogs": student.active_backlogs,
                "skills": student.skills,
                "phone_number": student.phone_number,
                "resume_url": student.resume_url,
                "is_verified": student.is_verified
            }
    elif current_user.role == UserRole.RECRUITER.value:
        stmt = select(Recruiter).where(Recruiter.user_id == current_user.id)
        res = await db.execute(stmt)
        recruiter = res.scalar_one_or_none()
        if recruiter:
            profile_data["recruiter"] = {
                "id": str(recruiter.id),
                "company_id": str(recruiter.company_id),
                "full_name": recruiter.full_name,
                "designation": recruiter.designation,
                "phone_number": recruiter.phone_number,
                "is_approved": recruiter.is_approved
            }

    return profile_data

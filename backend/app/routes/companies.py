import uuid
from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from app.core.database import get_db
from app.core.dependencies import get_current_active_user, require_roles
from app.models.user import User, UserRole
from app.models.company import Company, CompanyVerificationStatus
from app.schemas.company import CompanyCreate, CompanyUpdate, CompanyResponse

router = APIRouter(prefix="/companies", tags=["Companies"])


@router.get("", response_model=List[CompanyResponse])
@router.get("/", response_model=List[CompanyResponse])
async def list_companies(
    status_filter: Optional[str] = None,
    current_user: User = Depends(get_current_active_user),
    db: AsyncSession = Depends(get_db)
):
    stmt = select(Company)
    # Students can only view verified companies
    if current_user.role == UserRole.STUDENT.value:
        stmt = stmt.where(Company.verification_status == CompanyVerificationStatus.VERIFIED.value)
    elif status_filter:
        stmt = stmt.where(Company.verification_status == status_filter.upper())

    res = await db.execute(stmt)
    companies = res.scalars().all()
    return companies


@router.post("/", response_model=CompanyResponse, status_code=status.HTTP_201_CREATED)
async def create_company(
    payload: CompanyCreate,
    current_user: User = Depends(require_roles(UserRole.RECRUITER, UserRole.TPO_ADMIN, UserRole.SUPER_ADMIN)),
    db: AsyncSession = Depends(get_db)
):
    stmt = select(Company).where(Company.name.ilike(payload.name.strip()))
    res = await db.execute(stmt)
    if res.scalar_one_or_none():
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Company name already registered.")

    init_status = CompanyVerificationStatus.VERIFIED.value if current_user.role in [
        UserRole.TPO_ADMIN.value, UserRole.SUPER_ADMIN.value
    ] else CompanyVerificationStatus.PENDING.value

    new_company = Company(
        id=str(uuid.uuid4()),
        name=payload.name.strip(),
        logo_url=payload.logo_url,
        website=payload.website,
        industry=payload.industry,
        description=payload.description,
        verification_status=init_status
    )
    db.add(new_company)
    await db.commit()
    await db.refresh(new_company)
    return new_company


@router.get("/{company_id}", response_model=CompanyResponse)
async def get_company(company_id: str, db: AsyncSession = Depends(get_db)):
    stmt = select(Company).where(Company.id == company_id)
    res = await db.execute(stmt)
    company = res.scalar_one_or_none()
    if not company:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Company not found.")
    return company


@router.put("/{company_id}/verification", response_model=CompanyResponse)
async def verify_company(
    company_id: str,
    payload: CompanyUpdate,
    current_user: User = Depends(require_roles(UserRole.TPO_ADMIN, UserRole.SUPER_ADMIN)),
    db: AsyncSession = Depends(get_db)
):
    stmt = select(Company).where(Company.id == company_id)
    res = await db.execute(stmt)
    company = res.scalar_one_or_none()
    if not company:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Company not found.")

    if payload.verification_status:
        company.verification_status = payload.verification_status.upper()
    if payload.description:
        company.description = payload.description

    await db.commit()
    await db.refresh(company)
    return company

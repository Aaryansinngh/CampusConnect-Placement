from typing import List
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from app.core.database import get_db
from app.core.dependencies import require_roles
from app.models.user import User, UserRole
from app.models.recruiter import Recruiter
from app.schemas.recruiter import RecruiterResponse, RecruiterApproval

router = APIRouter(prefix="/recruiters", tags=["Recruiters"])


@router.get("", response_model=List[RecruiterResponse])
@router.get("/", response_model=List[RecruiterResponse])
async def list_recruiters(
    current_user: User = Depends(require_roles(UserRole.TPO_ADMIN, UserRole.SUPER_ADMIN)),
    db: AsyncSession = Depends(get_db)
):
    stmt = select(Recruiter)
    res = await db.execute(stmt)
    return res.scalars().all()


@router.put("/{recruiter_id}/approval", response_model=RecruiterResponse)
async def approve_recruiter(
    recruiter_id: str,
    payload: RecruiterApproval,
    current_user: User = Depends(require_roles(UserRole.TPO_ADMIN, UserRole.SUPER_ADMIN)),
    db: AsyncSession = Depends(get_db)
):
    stmt = select(Recruiter).where(Recruiter.id == recruiter_id)
    res = await db.execute(stmt)
    recruiter = res.scalar_one_or_none()
    if not recruiter:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Recruiter not found.")

    recruiter.is_approved = payload.is_approved
    await db.commit()
    await db.refresh(recruiter)
    return recruiter

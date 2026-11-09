from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from sqlalchemy.orm import selectinload
from app.core.database import get_db
from app.core.dependencies import get_current_student
from app.models.student import Student
from app.models.placement_drive import PlacementDrive
from app.models.eligibility import EligibilityCriteria
from app.services.eligibility_service import EligibilityService
from app.schemas.eligibility import EligibilityCheckResult

router = APIRouter(prefix="/eligibility", tags=["Eligibility"])


@router.get("/check/{drive_id}", response_model=EligibilityCheckResult)
async def check_student_eligibility(
    drive_id: str,
    current_student: Student = Depends(get_current_student),
    db: AsyncSession = Depends(get_db)
):
    stmt = select(PlacementDrive).options(
        selectinload(PlacementDrive.eligibility_criteria)
    ).where(PlacementDrive.id == drive_id)
    res = await db.execute(stmt)
    drive = res.scalar_one_or_none()

    if not drive:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Placement drive not found.")

    if not drive.eligibility_criteria:
        # If no criteria specified, eligible by default
        return EligibilityCheckResult(
            is_eligible=True,
            reasons=[],
            evaluation_details={"note": "No eligibility criteria defined for this drive."}
        )

    result = EligibilityService.evaluate(current_student, drive.eligibility_criteria)
    return result

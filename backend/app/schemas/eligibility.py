from typing import Optional, List, Dict, Any
from pydantic import BaseModel, Field


class EligibilityCriteriaCreate(BaseModel):
    min_cgpa: float = Field(6.0, ge=0.0, le=10.0)
    max_backlogs: int = Field(0, ge=0)
    eligible_department_ids: List[str] = Field(default_factory=list)
    eligible_graduation_years: List[int] = Field(default_factory=list)
    required_skills: List[str] = Field(default_factory=list)
    min_attendance_percentage: Optional[float] = 75.0


class EligibilityCriteriaResponse(BaseModel):
    id: str
    drive_id: str
    min_cgpa: float
    max_backlogs: int
    eligible_department_ids: List[str]
    eligible_graduation_years: List[int]
    required_skills: List[str]
    min_attendance_percentage: Optional[float] = 75.0

    class Config:
        from_attributes = True


class EligibilityCheckResult(BaseModel):
    is_eligible: bool
    reasons: List[str]
    evaluation_details: Dict[str, Any]

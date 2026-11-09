from typing import Optional, List
from datetime import datetime
from pydantic import BaseModel, Field
from app.schemas.eligibility import EligibilityCriteriaCreate, EligibilityCriteriaResponse


class PlacementDriveCreate(BaseModel):
    company_id: str
    job_title: str = Field(..., min_length=2)
    job_description: str = Field(..., min_length=10)
    salary_package_ctc: str
    job_location: str
    employment_type: str = "FULL_TIME"
    vacancies: int = Field(1, ge=1)
    application_deadline: datetime
    eligibility: Optional[EligibilityCriteriaCreate] = None


class PlacementDriveUpdate(BaseModel):
    job_title: Optional[str] = None
    job_description: Optional[str] = None
    salary_package_ctc: Optional[str] = None
    job_location: Optional[str] = None
    employment_type: Optional[str] = None
    vacancies: Optional[int] = None
    status: Optional[str] = None
    application_deadline: Optional[datetime] = None


class DriveStatusUpdate(BaseModel):
    status: str


class PlacementDriveResponse(BaseModel):
    id: str
    university_id: str
    company_id: str
    created_by_recruiter_id: Optional[str] = None
    job_title: str
    job_description: str
    salary_package_ctc: str
    job_location: str
    employment_type: str
    vacancies: int
    status: str
    application_deadline: datetime
    eligibility_criteria: Optional[EligibilityCriteriaResponse] = None

    class Config:
        from_attributes = True

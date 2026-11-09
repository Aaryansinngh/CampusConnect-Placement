from typing import Optional, List
from pydantic import BaseModel, Field


class StudentProfileUpdate(BaseModel):
    phone_number: Optional[str] = None
    resume_url: Optional[str] = None
    skills: Optional[List[str]] = None


class StudentAcademicUpdate(BaseModel):
    """Restricted to TPO / Admin only to prevent student self-tampering of grades"""
    current_cgpa: Optional[float] = Field(None, ge=0.0, le=10.0)
    active_backlogs: Optional[int] = Field(None, ge=0)
    is_verified: Optional[bool] = None
    department_id: Optional[str] = None
    registration_number: Optional[str] = None


class StudentResponse(BaseModel):
    id: str
    user_id: str
    university_id: str
    department_id: str
    full_name: str
    registration_number: str
    degree: str
    graduation_year: int
    current_cgpa: float
    active_backlogs: int
    skills: List[str]
    phone_number: Optional[str] = None
    resume_url: Optional[str] = None
    is_verified: bool

    class Config:
        from_attributes = True

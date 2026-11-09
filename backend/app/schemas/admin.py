from typing import Optional, List
from pydantic import BaseModel, Field


class UniversityCreate(BaseModel):
    name: str = Field(..., min_length=2)
    code: str = Field(..., min_length=2)
    domain: Optional[str] = None
    logo_url: Optional[str] = None


class UniversityResponse(BaseModel):
    id: str
    name: str
    code: str
    domain: Optional[str] = None
    logo_url: Optional[str] = None

    class Config:
        from_attributes = True


class DepartmentCreate(BaseModel):
    code: str = Field(..., min_length=2)
    name: str = Field(..., min_length=2)


class DepartmentResponse(BaseModel):
    id: str
    university_id: str
    code: str
    name: str

    class Config:
        from_attributes = True


class AnnouncementCreate(BaseModel):
    title: str = Field(..., min_length=2)
    content: str = Field(..., min_length=5)
    target_departments: List[str] = Field(default_factory=list)
    target_batches: List[int] = Field(default_factory=list)


class AnnouncementResponse(BaseModel):
    id: str
    university_id: str
    title: str
    content: str
    target_departments: List[str]
    target_batches: List[int]

    class Config:
        from_attributes = True

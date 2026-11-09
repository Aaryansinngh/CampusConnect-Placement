from typing import Optional, List
from pydantic import BaseModel, EmailStr, Field


class TokenResponse(BaseModel):
    access_token: str
    refresh_token: str
    token_type: str = "bearer"
    expires_in: int


class RefreshTokenRequest(BaseModel):
    refresh_token: str


class LoginRequest(BaseModel):
    email: EmailStr
    password: str = Field(..., min_length=6)


class StudentRegisterRequest(BaseModel):
    email: EmailStr
    password: str = Field(..., min_length=6)
    full_name: str = Field(..., min_length=2)
    university_code: str = Field(..., min_length=2)
    department_code: str = Field(..., min_length=2)
    registration_number: str = Field(..., min_length=2)
    degree: str = "B.Tech"
    graduation_year: int
    current_cgpa: float = Field(..., ge=0.0, le=10.0)
    active_backlogs: int = Field(0, ge=0)
    skills: List[str] = Field(default_factory=list)
    phone_number: Optional[str] = None


class RecruiterRegisterRequest(BaseModel):
    email: EmailStr
    password: str = Field(..., min_length=6)
    full_name: str = Field(..., min_length=2)
    company_name: str = Field(..., min_length=2)
    company_website: Optional[str] = None
    industry: Optional[str] = None
    designation: Optional[str] = None
    phone_number: Optional[str] = None


class TPOCreateRequest(BaseModel):
    email: EmailStr
    password: str = Field(..., min_length=8)
    university_id: str


class UserResponse(BaseModel):
    id: str
    email: str
    role: str
    university_id: Optional[str] = None
    is_active: bool
    is_verified: bool

    class Config:
        from_attributes = True

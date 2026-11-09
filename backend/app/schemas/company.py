from typing import Optional
from pydantic import BaseModel, Field


class CompanyCreate(BaseModel):
    name: str = Field(..., min_length=2)
    logo_url: Optional[str] = None
    website: Optional[str] = None
    industry: Optional[str] = None
    description: Optional[str] = None


class CompanyUpdate(BaseModel):
    name: Optional[str] = None
    logo_url: Optional[str] = None
    website: Optional[str] = None
    industry: Optional[str] = None
    description: Optional[str] = None
    verification_status: Optional[str] = None


class CompanyResponse(BaseModel):
    id: str
    name: str
    logo_url: Optional[str] = None
    website: Optional[str] = None
    industry: Optional[str] = None
    description: Optional[str] = None
    verification_status: str

    class Config:
        from_attributes = True

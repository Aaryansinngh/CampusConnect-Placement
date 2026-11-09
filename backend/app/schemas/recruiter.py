from typing import Optional
from pydantic import BaseModel


class RecruiterResponse(BaseModel):
    id: str
    user_id: str
    company_id: str
    full_name: str
    designation: Optional[str] = None
    phone_number: Optional[str] = None
    is_approved: bool

    class Config:
        from_attributes = True


class RecruiterApproval(BaseModel):
    is_approved: bool

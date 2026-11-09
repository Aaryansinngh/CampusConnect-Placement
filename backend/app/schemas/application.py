from typing import Optional
from datetime import datetime
from pydantic import BaseModel


class ApplicationCreate(BaseModel):
    drive_id: str


class ApplicationStatusUpdate(BaseModel):
    status: str
    withdrawal_reason: Optional[str] = None


class ApplicationResponse(BaseModel):
    id: str
    drive_id: str
    student_id: str
    status: str
    applied_at: datetime
    withdrawal_reason: Optional[str] = None

    class Config:
        from_attributes = True

from typing import Optional, List
from datetime import datetime
from pydantic import BaseModel, Field


class RoundCreate(BaseModel):
    drive_id: str
    round_name: str = Field(..., min_length=2)
    round_type: str = Field("TECHNICAL_INTERVIEW")
    round_order: int = Field(1, ge=1)
    scheduled_date: Optional[datetime] = None
    venue: Optional[str] = None
    instructions: Optional[str] = None


class RoundResponse(BaseModel):
    id: str
    drive_id: str
    round_order: int
    round_name: str
    round_type: str
    scheduled_date: Optional[datetime] = None
    venue: Optional[str] = None
    instructions: Optional[str] = None

    class Config:
        from_attributes = True


class SlotCreate(BaseModel):
    round_id: str
    start_time: datetime
    end_time: datetime
    venue_or_link: str = Field(..., min_length=2)
    total_capacity: int = Field(1, ge=1)


class BulkSlotCreate(BaseModel):
    round_id: str
    start_datetime: datetime
    end_datetime: datetime
    slot_duration_minutes: int = Field(30, ge=10, le=180)
    venue_or_link: str = Field(..., min_length=2)
    capacity_per_slot: int = Field(1, ge=1, le=50)


class SlotResponse(BaseModel):
    id: str
    round_id: str
    recruiter_id: Optional[str] = None
    start_time: datetime
    end_time: datetime
    venue_or_link: str
    total_capacity: int
    booked_count: int
    is_cancelled: bool

    class Config:
        from_attributes = True


class SlotBookingRequest(BaseModel):
    slot_id: str
    application_id: str
    notes: Optional[str] = None


class BookingResponse(BaseModel):
    id: str
    slot_id: str
    application_id: str
    student_id: str
    status: str
    booked_at: datetime
    notes: Optional[str] = None

    class Config:
        from_attributes = True

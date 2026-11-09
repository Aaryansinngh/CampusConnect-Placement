import enum
from sqlalchemy import Column, String, Text, ForeignKey, DateTime, UniqueConstraint
from sqlalchemy.orm import relationship
from datetime import datetime, timezone
from app.models.base import TimeStampedModel
from app.utils.custom_types import GUID


class BookingStatus(str, enum.Enum):
    CONFIRMED = "CONFIRMED"
    RESCHEDULED = "RESCHEDULED"
    CANCELLED = "CANCELLED"


class InterviewBooking(TimeStampedModel):
    __tablename__ = "interview_bookings"

    slot_id = Column(GUID, ForeignKey("interview_slots.id", ondelete="CASCADE"), nullable=False, index=True)
    application_id = Column(GUID, ForeignKey("applications.id", ondelete="CASCADE"), nullable=False, index=True)
    student_id = Column(GUID, ForeignKey("students.id", ondelete="CASCADE"), nullable=False, index=True)

    status = Column(String(50), nullable=False, default=BookingStatus.CONFIRMED.value, index=True)
    booked_at = Column(DateTime, default=lambda: datetime.now(timezone.utc), nullable=False)
    notes = Column(Text, nullable=True)

    __table_args__ = (
        UniqueConstraint("slot_id", "student_id", name="uq_slot_student_booking"),
    )

    # Relationships
    slot = relationship("InterviewSlot", back_populates="bookings")
    application = relationship("Application", back_populates="interview_bookings")
    student = relationship("Student", back_populates="interview_bookings")

import enum
from sqlalchemy import Column, String, Text, ForeignKey, DateTime, UniqueConstraint
from sqlalchemy.orm import relationship
from datetime import datetime, timezone
from app.models.base import TimeStampedModel
from app.utils.custom_types import GUID


class ApplicationStatus(str, enum.Enum):
    APPLIED = "APPLIED"
    UNDER_REVIEW = "UNDER_REVIEW"
    SHORTLISTED = "SHORTLISTED"
    ASSESSMENT = "ASSESSMENT"
    TECHNICAL_INTERVIEW = "TECHNICAL_INTERVIEW"
    HR_INTERVIEW = "HR_INTERVIEW"
    SELECTED = "SELECTED"
    REJECTED = "REJECTED"
    WITHDRAWN = "WITHDRAWN"


class Application(TimeStampedModel):
    __tablename__ = "applications"

    drive_id = Column(GUID, ForeignKey("placement_drives.id", ondelete="CASCADE"), nullable=False, index=True)
    student_id = Column(GUID, ForeignKey("students.id", ondelete="CASCADE"), nullable=False, index=True)

    status = Column(String(50), nullable=False, default=ApplicationStatus.APPLIED.value, index=True)
    applied_at = Column(DateTime, default=lambda: datetime.now(timezone.utc), nullable=False)
    withdrawal_reason = Column(Text, nullable=True)

    __table_args__ = (
        UniqueConstraint("drive_id", "student_id", name="uq_drive_student_application"),
    )

    # Relationships
    drive = relationship("PlacementDrive", back_populates="applications")
    student = relationship("Student", back_populates="applications")
    round_candidates = relationship("RoundCandidate", back_populates="application", cascade="all, delete-orphan")
    interview_bookings = relationship("InterviewBooking", back_populates="application", cascade="all, delete-orphan")
    offer = relationship("Offer", back_populates="application", uselist=False, cascade="all, delete-orphan")

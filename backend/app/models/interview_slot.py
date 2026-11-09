from sqlalchemy import Column, String, Integer, Boolean, ForeignKey, DateTime
from sqlalchemy.orm import relationship
from app.models.base import TimeStampedModel
from app.utils.custom_types import GUID


class InterviewSlot(TimeStampedModel):
    __tablename__ = "interview_slots"

    round_id = Column(GUID, ForeignKey("recruitment_rounds.id", ondelete="CASCADE"), nullable=False, index=True)
    recruiter_id = Column(GUID, ForeignKey("recruiters.id", ondelete="SET NULL"), nullable=True, index=True)

    start_time = Column(DateTime, nullable=False, index=True)
    end_time = Column(DateTime, nullable=False)
    venue_or_link = Column(String(512), nullable=False)
    total_capacity = Column(Integer, nullable=False, default=1)
    booked_count = Column(Integer, nullable=False, default=0)
    is_cancelled = Column(Boolean, default=False, nullable=False)

    # Relationships
    round = relationship("RecruitmentRound", back_populates="interview_slots")
    recruiter = relationship("Recruiter", back_populates="interview_slots")
    bookings = relationship("InterviewBooking", back_populates="slot", cascade="all, delete-orphan")

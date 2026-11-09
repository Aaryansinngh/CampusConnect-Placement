from sqlalchemy import Column, String, Boolean, ForeignKey
from sqlalchemy.orm import relationship
from app.models.base import TimeStampedModel
from app.utils.custom_types import GUID


class Recruiter(TimeStampedModel):
    __tablename__ = "recruiters"

    user_id = Column(GUID, ForeignKey("users.id", ondelete="CASCADE"), unique=True, nullable=False, index=True)
    company_id = Column(GUID, ForeignKey("companies.id", ondelete="CASCADE"), nullable=False, index=True)
    full_name = Column(String(255), nullable=False)
    designation = Column(String(100), nullable=True)
    phone_number = Column(String(50), nullable=True)
    is_approved = Column(Boolean, default=False, nullable=False)

    # Relationships
    user = relationship("User", back_populates="recruiter_profile")
    company = relationship("Company", back_populates="recruiters")
    placement_drives = relationship("PlacementDrive", back_populates="created_by_recruiter")
    interview_slots = relationship("InterviewSlot", back_populates="recruiter")

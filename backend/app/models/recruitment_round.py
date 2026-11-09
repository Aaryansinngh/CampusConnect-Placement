import enum
from sqlalchemy import Column, String, Integer, Text, ForeignKey, DateTime
from sqlalchemy.orm import relationship
from app.models.base import TimeStampedModel
from app.utils.custom_types import GUID


class RoundType(str, enum.Enum):
    ONLINE_ASSESSMENT = "ONLINE_ASSESSMENT"
    TECHNICAL_INTERVIEW = "TECHNICAL_INTERVIEW"
    GROUP_DISCUSSION = "GROUP_DISCUSSION"
    HR_INTERVIEW = "HR_INTERVIEW"
    FINAL = "FINAL"


class RecruitmentRound(TimeStampedModel):
    __tablename__ = "recruitment_rounds"

    drive_id = Column(GUID, ForeignKey("placement_drives.id", ondelete="CASCADE"), nullable=False, index=True)
    round_order = Column(Integer, nullable=False, default=1)
    round_name = Column(String(255), nullable=False)
    round_type = Column(String(50), nullable=False, default=RoundType.ONLINE_ASSESSMENT.value)
    scheduled_date = Column(DateTime, nullable=True)
    venue = Column(String(255), nullable=True)
    instructions = Column(Text, nullable=True)

    # Relationships
    drive = relationship("PlacementDrive", back_populates="rounds")
    candidates = relationship("RoundCandidate", back_populates="round", cascade="all, delete-orphan")
    interview_slots = relationship("InterviewSlot", back_populates="round", cascade="all, delete-orphan")

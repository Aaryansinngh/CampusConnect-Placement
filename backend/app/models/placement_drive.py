import enum
from sqlalchemy import Column, String, Integer, Text, ForeignKey, DateTime
from sqlalchemy.orm import relationship
from app.models.base import TimeStampedModel
from app.utils.custom_types import GUID


class DriveStatus(str, enum.Enum):
    DRAFT = "DRAFT"
    PENDING_APPROVAL = "PENDING_APPROVAL"
    APPROVED = "APPROVED"
    REJECTED = "REJECTED"
    ACTIVE = "ACTIVE"
    COMPLETED = "COMPLETED"


class EmploymentType(str, enum.Enum):
    FULL_TIME = "FULL_TIME"
    INTERNSHIP = "INTERNSHIP"
    PPO = "PPO"


class PlacementDrive(TimeStampedModel):
    __tablename__ = "placement_drives"

    university_id = Column(GUID, ForeignKey("universities.id", ondelete="CASCADE"), nullable=False, index=True)
    company_id = Column(GUID, ForeignKey("companies.id", ondelete="CASCADE"), nullable=False, index=True)
    created_by_recruiter_id = Column(GUID, ForeignKey("recruiters.id", ondelete="SET NULL"), nullable=True, index=True)

    job_title = Column(String(255), nullable=False, index=True)
    job_description = Column(Text, nullable=False)
    salary_package_ctc = Column(String(100), nullable=False)
    job_location = Column(String(255), nullable=False)
    employment_type = Column(String(50), nullable=False, default=EmploymentType.FULL_TIME.value)
    vacancies = Column(Integer, nullable=False, default=1)
    status = Column(String(50), nullable=False, default=DriveStatus.DRAFT.value, index=True)
    application_deadline = Column(DateTime, nullable=False)

    # Relationships
    university = relationship("University", back_populates="placement_drives")
    company = relationship("Company", back_populates="placement_drives")
    created_by_recruiter = relationship("Recruiter", back_populates="placement_drives")
    eligibility_criteria = relationship("EligibilityCriteria", back_populates="drive", uselist=False, cascade="all, delete-orphan")
    applications = relationship("Application", back_populates="drive", cascade="all, delete-orphan")
    rounds = relationship("RecruitmentRound", back_populates="drive", cascade="all, delete-orphan", order_by="RecruitmentRound.round_order")

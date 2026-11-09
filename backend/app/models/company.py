import enum
from sqlalchemy import Column, String, Text
from sqlalchemy.orm import relationship
from app.models.base import TimeStampedModel


class CompanyVerificationStatus(str, enum.Enum):
    PENDING = "PENDING"
    VERIFIED = "VERIFIED"
    REJECTED = "REJECTED"


class Company(TimeStampedModel):
    __tablename__ = "companies"

    name = Column(String(255), unique=True, index=True, nullable=False)
    logo_url = Column(String(512), nullable=True)
    website = Column(String(255), nullable=True)
    industry = Column(String(100), nullable=True)
    description = Column(Text, nullable=True)
    verification_status = Column(
        String(50),
        default=CompanyVerificationStatus.PENDING.value,
        nullable=False,
        index=True
    )

    # Relationships
    recruiters = relationship("Recruiter", back_populates="company", cascade="all, delete-orphan")
    placement_drives = relationship("PlacementDrive", back_populates="company", cascade="all, delete-orphan")

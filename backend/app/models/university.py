from sqlalchemy import Column, String
from sqlalchemy.orm import relationship
from app.models.base import TimeStampedModel


class University(TimeStampedModel):
    __tablename__ = "universities"

    name = Column(String(255), nullable=False)
    code = Column(String(50), unique=True, index=True, nullable=False)
    domain = Column(String(255), nullable=True)
    logo_url = Column(String(512), nullable=True)

    # Relationships
    departments = relationship("Department", back_populates="university", cascade="all, delete-orphan")
    users = relationship("User", back_populates="university", cascade="all, delete-orphan")
    placement_drives = relationship("PlacementDrive", back_populates="university", cascade="all, delete-orphan")
    announcements = relationship("Announcement", back_populates="university", cascade="all, delete-orphan")
    audit_logs = relationship("AuditLog", back_populates="university", cascade="all, delete-orphan")

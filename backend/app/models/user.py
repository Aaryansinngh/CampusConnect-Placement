import enum
from sqlalchemy import Column, String, Boolean, ForeignKey, Enum as SQLEnum
from sqlalchemy.orm import relationship
from app.models.base import TimeStampedModel
from app.utils.custom_types import GUID


class UserRole(str, enum.Enum):
    STUDENT = "STUDENT"
    RECRUITER = "RECRUITER"
    TPO_ADMIN = "TPO_ADMIN"
    SUPER_ADMIN = "SUPER_ADMIN"


class User(TimeStampedModel):
    __tablename__ = "users"

    university_id = Column(GUID, ForeignKey("universities.id", ondelete="CASCADE"), nullable=True, index=True)
    email = Column(String(255), unique=True, index=True, nullable=False)
    hashed_password = Column(String(255), nullable=False)
    role = Column(String(50), nullable=False, default=UserRole.STUDENT.value)
    is_active = Column(Boolean, default=True, nullable=False)
    is_verified = Column(Boolean, default=False, nullable=False)

    # Relationships
    university = relationship("University", back_populates="users")
    student_profile = relationship("Student", back_populates="user", uselist=False, cascade="all, delete-orphan")
    recruiter_profile = relationship("Recruiter", back_populates="user", uselist=False, cascade="all, delete-orphan")
    notifications = relationship("Notification", back_populates="user", cascade="all, delete-orphan")
    device_tokens = relationship("DeviceToken", back_populates="user", cascade="all, delete-orphan")
    audit_logs = relationship("AuditLog", back_populates="user")

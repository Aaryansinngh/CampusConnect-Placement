from sqlalchemy import Column, String, Integer, Float, Boolean, ForeignKey, UniqueConstraint
from sqlalchemy.orm import relationship
from app.models.base import TimeStampedModel
from app.utils.custom_types import GUID, PortableJSON


class Student(TimeStampedModel):
    __tablename__ = "students"

    user_id = Column(GUID, ForeignKey("users.id", ondelete="CASCADE"), unique=True, nullable=False, index=True)
    university_id = Column(GUID, ForeignKey("universities.id", ondelete="CASCADE"), nullable=False, index=True)
    department_id = Column(GUID, ForeignKey("departments.id", ondelete="RESTRICT"), nullable=False, index=True)

    full_name = Column(String(255), nullable=False)
    registration_number = Column(String(100), nullable=False, index=True)
    degree = Column(String(100), nullable=False, default="B.Tech")
    graduation_year = Column(Integer, nullable=False, index=True)
    current_cgpa = Column(Float, nullable=False, default=0.0)
    active_backlogs = Column(Integer, nullable=False, default=0)
    skills = Column(PortableJSON, default=list, nullable=False)
    phone_number = Column(String(50), nullable=True)
    resume_url = Column(String(512), nullable=True)
    is_verified = Column(Boolean, default=False, nullable=False)

    __table_args__ = (
        UniqueConstraint("university_id", "registration_number", name="uq_university_student_reg"),
    )

    # Relationships
    user = relationship("User", back_populates="student_profile")
    department = relationship("Department", back_populates="students")
    applications = relationship("Application", back_populates="student", cascade="all, delete-orphan")
    resumes = relationship("Resume", back_populates="student", cascade="all, delete-orphan")
    offers = relationship("Offer", back_populates="student", cascade="all, delete-orphan")
    interview_bookings = relationship("InterviewBooking", back_populates="student", cascade="all, delete-orphan")

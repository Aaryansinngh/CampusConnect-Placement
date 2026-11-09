from sqlalchemy import Column, String, Integer, Boolean, ForeignKey, DateTime
from sqlalchemy.orm import relationship
from datetime import datetime, timezone
from app.models.base import TimeStampedModel
from app.utils.custom_types import GUID


class Resume(TimeStampedModel):
    __tablename__ = "resumes"

    student_id = Column(GUID, ForeignKey("students.id", ondelete="CASCADE"), nullable=False, index=True)
    file_url = Column(String(512), nullable=False)
    file_name = Column(String(255), nullable=False)
    file_size_bytes = Column(Integer, nullable=True)
    uploaded_at = Column(DateTime, default=lambda: datetime.now(timezone.utc), nullable=False)
    is_primary = Column(Boolean, default=True, nullable=False)

    # Relationships
    student = relationship("Student", back_populates="resumes")

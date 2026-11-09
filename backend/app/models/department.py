from sqlalchemy import Column, String, ForeignKey
from sqlalchemy.orm import relationship
from app.models.base import TimeStampedModel
from app.utils.custom_types import GUID


class Department(TimeStampedModel):
    __tablename__ = "departments"

    university_id = Column(GUID, ForeignKey("universities.id", ondelete="CASCADE"), nullable=False, index=True)
    code = Column(String(50), nullable=False)
    name = Column(String(255), nullable=False)

    # Relationships
    university = relationship("University", back_populates="departments")
    students = relationship("Student", back_populates="department")

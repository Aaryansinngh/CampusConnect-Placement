from sqlalchemy import Column, Float, Integer, ForeignKey
from sqlalchemy.orm import relationship
from app.models.base import TimeStampedModel
from app.utils.custom_types import GUID, PortableJSON


class EligibilityCriteria(TimeStampedModel):
    __tablename__ = "eligibility_criteria"

    drive_id = Column(GUID, ForeignKey("placement_drives.id", ondelete="CASCADE"), unique=True, nullable=False, index=True)

    min_cgpa = Column(Float, nullable=False, default=6.0)
    max_backlogs = Column(Integer, nullable=False, default=0)
    eligible_department_ids = Column(PortableJSON, default=list, nullable=False)
    eligible_graduation_years = Column(PortableJSON, default=list, nullable=False)
    required_skills = Column(PortableJSON, default=list, nullable=False)
    min_attendance_percentage = Column(Float, nullable=True, default=75.0)

    # Relationships
    drive = relationship("PlacementDrive", back_populates="eligibility_criteria")

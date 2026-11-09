from sqlalchemy import Column, String, Text, ForeignKey
from sqlalchemy.orm import relationship
from app.models.base import TimeStampedModel
from app.utils.custom_types import GUID, PortableJSON


class Announcement(TimeStampedModel):
    __tablename__ = "announcements"

    university_id = Column(GUID, ForeignKey("universities.id", ondelete="CASCADE"), nullable=False, index=True)
    created_by = Column(GUID, ForeignKey("users.id", ondelete="SET NULL"), nullable=True)

    title = Column(String(255), nullable=False)
    content = Column(Text, nullable=False)
    target_departments = Column(PortableJSON, default=list, nullable=False)
    target_batches = Column(PortableJSON, default=list, nullable=False)

    # Relationships
    university = relationship("University", back_populates="announcements")

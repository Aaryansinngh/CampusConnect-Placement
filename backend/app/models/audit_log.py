from sqlalchemy import Column, String, ForeignKey
from sqlalchemy.orm import relationship
from app.models.base import TimeStampedModel
from app.utils.custom_types import GUID, PortableJSON


class AuditLog(TimeStampedModel):
    __tablename__ = "audit_logs"

    university_id = Column(GUID, ForeignKey("universities.id", ondelete="CASCADE"), nullable=False, index=True)
    user_id = Column(GUID, ForeignKey("users.id", ondelete="SET NULL"), nullable=True, index=True)

    action = Column(String(100), nullable=False)
    resource_type = Column(String(100), nullable=False)
    resource_id = Column(String(100), nullable=True)
    details = Column(PortableJSON, default=dict, nullable=True)
    ip_address = Column(String(50), nullable=True)

    # Relationships
    university = relationship("University", back_populates="audit_logs")
    user = relationship("User", back_populates="audit_logs")

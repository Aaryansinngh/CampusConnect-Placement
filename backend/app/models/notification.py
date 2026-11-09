from sqlalchemy import Column, String, Text, Boolean, ForeignKey
from sqlalchemy.orm import relationship
from app.models.base import TimeStampedModel
from app.utils.custom_types import GUID, PortableJSON


class Notification(TimeStampedModel):
    __tablename__ = "notifications"

    user_id = Column(GUID, ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    title = Column(String(255), nullable=False)
    message = Column(Text, nullable=False)
    notification_type = Column(String(50), nullable=False, default="GENERAL")
    is_read = Column(Boolean, default=False, nullable=False, index=True)
    metadata_json = Column(PortableJSON, default=dict, nullable=True)

    # Relationships
    user = relationship("User", back_populates="notifications")

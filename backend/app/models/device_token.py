from sqlalchemy import Column, String, ForeignKey, UniqueConstraint
from sqlalchemy.orm import relationship
from app.models.base import TimeStampedModel
from app.utils.custom_types import GUID


class DeviceToken(TimeStampedModel):
    __tablename__ = "device_tokens"

    user_id = Column(GUID, ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    fcm_token = Column(String(512), nullable=False)
    platform = Column(String(50), nullable=False, default="ANDROID")

    __table_args__ = (
        UniqueConstraint("user_id", "fcm_token", name="uq_user_device_token"),
    )

    # Relationships
    user = relationship("User", back_populates="device_tokens")

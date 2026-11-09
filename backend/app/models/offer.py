import enum
from sqlalchemy import Column, String, ForeignKey, DateTime
from sqlalchemy.orm import relationship
from datetime import datetime, timezone
from app.models.base import TimeStampedModel
from app.utils.custom_types import GUID


class OfferStatus(str, enum.Enum):
    OFFERED = "OFFERED"
    ACCEPTED = "ACCEPTED"
    DECLINED = "DECLINED"


class Offer(TimeStampedModel):
    __tablename__ = "offers"

    application_id = Column(GUID, ForeignKey("applications.id", ondelete="CASCADE"), unique=True, nullable=False, index=True)
    student_id = Column(GUID, ForeignKey("students.id", ondelete="CASCADE"), nullable=False, index=True)

    package_offered = Column(String(100), nullable=False)
    offer_letter_url = Column(String(512), nullable=True)
    status = Column(String(50), nullable=False, default=OfferStatus.OFFERED.value, index=True)
    issued_at = Column(DateTime, default=lambda: datetime.now(timezone.utc), nullable=False)
    valid_until = Column(DateTime, nullable=True)

    # Relationships
    application = relationship("Application", back_populates="offer")
    student = relationship("Student", back_populates="offers")

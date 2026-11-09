import uuid
from datetime import datetime, timezone
from sqlalchemy import Column, DateTime
from app.utils.custom_types import GUID
from app.core.database import Base


class TimeStampedModel(Base):
    __abstract__ = True

    id = Column(GUID, primary_key=True, default=uuid.uuid4)
    created_at = Column(DateTime, default=lambda: datetime.now(timezone.utc), nullable=False)
    updated_at = Column(
        DateTime,
        default=lambda: datetime.now(timezone.utc),
        onupdate=lambda: datetime.now(timezone.utc),
        nullable=False
    )

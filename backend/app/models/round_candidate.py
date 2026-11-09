import enum
from sqlalchemy import Column, String, Text, ForeignKey, UniqueConstraint
from sqlalchemy.orm import relationship
from app.models.base import TimeStampedModel
from app.utils.custom_types import GUID


class CandidateRoundStatus(str, enum.Enum):
    PENDING = "PENDING"
    QUALIFIED = "QUALIFIED"
    DISQUALIFIED = "DISQUALIFIED"
    ABSENT = "ABSENT"


class RoundCandidate(TimeStampedModel):
    __tablename__ = "round_candidates"

    round_id = Column(GUID, ForeignKey("recruitment_rounds.id", ondelete="CASCADE"), nullable=False, index=True)
    application_id = Column(GUID, ForeignKey("applications.id", ondelete="CASCADE"), nullable=False, index=True)

    status = Column(String(50), nullable=False, default=CandidateRoundStatus.PENDING.value, index=True)
    score = Column(String(50), nullable=True)
    feedback = Column(Text, nullable=True)

    __table_args__ = (
        UniqueConstraint("round_id", "application_id", name="uq_round_candidate"),
    )

    # Relationships
    round = relationship("RecruitmentRound", back_populates="candidates")
    application = relationship("Application", back_populates="round_candidates")

from app.schemas.auth import (
    TokenResponse, RefreshTokenRequest, LoginRequest,
    StudentRegisterRequest, RecruiterRegisterRequest, TPOCreateRequest, UserResponse
)
from app.schemas.student import StudentProfileUpdate, StudentAcademicUpdate, StudentResponse
from app.schemas.company import CompanyCreate, CompanyUpdate, CompanyResponse
from app.schemas.recruiter import RecruiterResponse, RecruiterApproval
from app.schemas.placement_drive import (
    PlacementDriveCreate, PlacementDriveUpdate, DriveStatusUpdate, PlacementDriveResponse
)
from app.schemas.eligibility import (
    EligibilityCriteriaCreate, EligibilityCriteriaResponse, EligibilityCheckResult
)
from app.schemas.application import ApplicationCreate, ApplicationStatusUpdate, ApplicationResponse
from app.schemas.admin import (
    UniversityCreate, UniversityResponse, DepartmentCreate, DepartmentResponse,
    AnnouncementCreate, AnnouncementResponse
)
from app.schemas.interview import (
    SlotCreate, SlotResponse, SlotBookingRequest, BookingResponse
)

__all__ = [
    "TokenResponse", "RefreshTokenRequest", "LoginRequest",
    "StudentRegisterRequest", "RecruiterRegisterRequest", "TPOCreateRequest", "UserResponse",
    "StudentProfileUpdate", "StudentAcademicUpdate", "StudentResponse",
    "CompanyCreate", "CompanyUpdate", "CompanyResponse",
    "RecruiterResponse", "RecruiterApproval",
    "PlacementDriveCreate", "PlacementDriveUpdate", "DriveStatusUpdate", "PlacementDriveResponse",
    "EligibilityCriteriaCreate", "EligibilityCriteriaResponse", "EligibilityCheckResult",
    "ApplicationCreate", "ApplicationStatusUpdate", "ApplicationResponse",
    "UniversityCreate", "UniversityResponse", "DepartmentCreate", "DepartmentResponse",
    "AnnouncementCreate", "AnnouncementResponse",
    "SlotCreate", "SlotResponse", "SlotBookingRequest", "BookingResponse"
]

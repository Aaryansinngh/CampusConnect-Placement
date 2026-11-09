from app.routes.auth import router as auth_router
from app.routes.students import router as students_router
from app.routes.companies import router as companies_router
from app.routes.recruiters import router as recruiters_router
from app.routes.placement_drives import router as drives_router
from app.routes.eligibility import router as eligibility_router
from app.routes.applications import router as applications_router
from app.routes.admin import router as admin_router
from app.routes.interviews import router as interviews_router

__all__ = [
    "auth_router",
    "students_router",
    "companies_router",
    "recruiters_router",
    "drives_router",
    "eligibility_router",
    "applications_router",
    "admin_router",
    "interviews_router"
]

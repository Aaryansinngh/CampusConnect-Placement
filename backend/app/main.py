from contextlib import asynccontextmanager
from fastapi import FastAPI, Request, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from app.core.config import settings
from app.core.database import engine, Base
import app.models  # Ensure all SQLAlchemy models are registered
from app.routes import (
    auth_router,
    students_router,
    companies_router,
    recruiters_router,
    drives_router,
    eligibility_router,
    applications_router,
    admin_router,
    interviews_router
)


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Initialize tables on startup for local environments
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    yield
    await engine.dispose()


app = FastAPI(
    title=settings.PROJECT_NAME,
    description="CampusConnect Centralized University Placement & Recruitment Management API",
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc",
    openapi_url="/openapi.json",
    lifespan=lifespan
)

# CORS Middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.ALLOWED_ORIGINS if isinstance(settings.ALLOWED_ORIGINS, list) else ["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Global Exception Handling
@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    return JSONResponse(
        status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
        content={
            "error": "InternalServerError",
            "message": str(exc) if settings.DEBUG else "An unexpected error occurred. Please contact support.",
            "path": request.url.path
        }
    )


# Health Check
@app.get("/", tags=["Health"])
@app.get("/api/health", tags=["Health"])
async def health_check():
    return {
        "status": "healthy",
        "service": settings.PROJECT_NAME,
        "environment": settings.ENVIRONMENT,
        "version": "1.0.0"
    }


# Include Modular Routers
app.include_router(auth_router, prefix=settings.API_V1_STR)
app.include_router(students_router, prefix=settings.API_V1_STR)
app.include_router(companies_router, prefix=settings.API_V1_STR)
app.include_router(recruiters_router, prefix=settings.API_V1_STR)
app.include_router(drives_router, prefix=settings.API_V1_STR)
app.include_router(eligibility_router, prefix=settings.API_V1_STR)
app.include_router(applications_router, prefix=settings.API_V1_STR)
app.include_router(admin_router, prefix=settings.API_V1_STR)
app.include_router(interviews_router, prefix=settings.API_V1_STR)

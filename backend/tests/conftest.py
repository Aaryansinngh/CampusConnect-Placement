import pytest
import os
import sys
import uuid
import pytest_asyncio
from httpx import AsyncClient, ASGITransport
from sqlalchemy.ext.asyncio import create_async_engine, async_sessionmaker, AsyncSession

# Add backend directory to sys.path
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from app.core.database import Base, get_db
from app.core.security import hash_password, create_access_token
from app.models.university import University
from app.models.department import Department
from app.models.user import User, UserRole
from app.models.student import Student
from app.models.company import Company, CompanyVerificationStatus
from app.models.recruiter import Recruiter
from app.models.placement_drive import PlacementDrive, DriveStatus, EmploymentType
from app.models.eligibility import EligibilityCriteria
from app.main import app
from datetime import datetime, timedelta, timezone

TEST_DB_FILE = "./test_campusconnect.db"
TEST_DATABASE_URL = f"sqlite+aiosqlite:///{TEST_DB_FILE}"

test_engine = create_async_engine(TEST_DATABASE_URL, echo=False)
TestingSessionLocal = async_sessionmaker(
    bind=test_engine,
    class_=AsyncSession,
    expire_on_commit=False,
    autocommit=False,
    autoflush=False
)


@pytest_asyncio.fixture(scope="session", autouse=True)
async def prepare_database():
    if os.path.exists(TEST_DB_FILE):
        try:
            os.remove(TEST_DB_FILE)
        except Exception:
            pass

    async with test_engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)

    yield

    async with test_engine.begin() as conn:
        await conn.run_sync(Base.metadata.drop_all)
    await test_engine.dispose()
    if os.path.exists(TEST_DB_FILE):
        try:
            os.remove(TEST_DB_FILE)
        except Exception:
            pass


async def override_get_db():
    async with TestingSessionLocal() as session:
        try:
            yield session
        except Exception:
            await session.rollback()
            raise
        finally:
            await session.close()


app.dependency_overrides[get_db] = override_get_db


@pytest_asyncio.fixture
async def client():
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://testserver") as ac:
        yield ac


@pytest_asyncio.fixture
async def seeded_test_data():
    async with TestingSessionLocal() as session:
        # Check if already seeded
        from sqlalchemy import select
        res = await session.execute(select(University).where(University.code == "TEST_UNIV"))
        existing_u = res.scalar_one_or_none()
        if existing_u:
            return

        # 1. University
        univ = University(
            id=str(uuid.uuid4()),
            name="Test Technical University",
            code="TEST_UNIV",
            domain="test.edu"
        )
        session.add(univ)
        await session.flush()

        # 2. Departments
        cse = Department(id=str(uuid.uuid4()), university_id=univ.id, code="CSE", name="Computer Science")
        ece = Department(id=str(uuid.uuid4()), university_id=univ.id, code="ECE", name="Electronics")
        session.add_all([cse, ece])
        await session.flush()

        # 3. Super Admin & TPO Admin
        super_admin = User(
            id=str(uuid.uuid4()),
            university_id=univ.id,
            email="test_super@test.edu",
            hashed_password=hash_password("SuperSecure123!"),
            role=UserRole.SUPER_ADMIN.value,
            is_active=True,
            is_verified=True
        )
        tpo = User(
            id=str(uuid.uuid4()),
            university_id=univ.id,
            email="test_tpo@test.edu",
            hashed_password=hash_password("TpoSecure123!"),
            role=UserRole.TPO_ADMIN.value,
            is_active=True,
            is_verified=True
        )
        session.add_all([super_admin, tpo])
        await session.flush()

        # 4. Company & Recruiter
        comp = Company(
            id=str(uuid.uuid4()),
            name="Acme Tech Labs",
            verification_status=CompanyVerificationStatus.VERIFIED.value
        )
        session.add(comp)
        await session.flush()

        rec_user = User(
            id=str(uuid.uuid4()),
            email="recruiter@acme.com",
            hashed_password=hash_password("RecruiterPass123!"),
            role=UserRole.RECRUITER.value,
            is_active=True,
            is_verified=True
        )
        session.add(rec_user)
        await session.flush()

        recruiter = Recruiter(
            id=str(uuid.uuid4()),
            user_id=rec_user.id,
            company_id=comp.id,
            full_name="Alice Recruiter",
            is_approved=True
        )
        session.add(recruiter)
        await session.flush()

        # 5. Students
        # Eligible student (High CGPA, 0 backlogs, CSE)
        st1_user = User(
            id=str(uuid.uuid4()),
            university_id=univ.id,
            email="eligible.student@test.edu",
            hashed_password=hash_password("StudentPass123!"),
            role=UserRole.STUDENT.value,
            is_active=True,
            is_verified=True
        )
        session.add(st1_user)
        await session.flush()

        st1 = Student(
            id=str(uuid.uuid4()),
            user_id=st1_user.id,
            university_id=univ.id,
            department_id=cse.id,
            full_name="John HighCGPA",
            registration_number="REG-001",
            degree="B.Tech",
            graduation_year=2026,
            current_cgpa=9.2,
            active_backlogs=0,
            skills=["Python", "DSA", "FastAPI"],
            is_verified=True
        )

        # Ineligible student (Low CGPA, 2 backlogs, ECE)
        st2_user = User(
            id=str(uuid.uuid4()),
            university_id=univ.id,
            email="ineligible.student@test.edu",
            hashed_password=hash_password("StudentPass123!"),
            role=UserRole.STUDENT.value,
            is_active=True,
            is_verified=True
        )
        session.add(st2_user)
        await session.flush()

        st2 = Student(
            id=str(uuid.uuid4()),
            user_id=st2_user.id,
            university_id=univ.id,
            department_id=ece.id,
            full_name="Bob LowCGPA",
            registration_number="REG-002",
            degree="B.Tech",
            graduation_year=2026,
            current_cgpa=6.2,
            active_backlogs=2,
            skills=["HTML"],
            is_verified=True
        )
        session.add_all([st1, st2])
        await session.flush()

        # 6. Placement Drive with strict criteria
        drive = PlacementDrive(
            id=str(uuid.uuid4()),
            university_id=univ.id,
            company_id=comp.id,
            created_by_recruiter_id=recruiter.id,
            job_title="Senior Software Engineer",
            job_description="High-performance backend systems engineering.",
            salary_package_ctc="28.0 LPA",
            job_location="Bangalore",
            employment_type=EmploymentType.FULL_TIME.value,
            vacancies=5,
            status=DriveStatus.ACTIVE.value,
            application_deadline=datetime.now(timezone.utc) + timedelta(days=10)
        )
        session.add(drive)
        await session.flush()

        crit = EligibilityCriteria(
            id=str(uuid.uuid4()),
            drive_id=drive.id,
            min_cgpa=8.0,
            max_backlogs=0,
            eligible_department_ids=[cse.id],
            eligible_graduation_years=[2026],
            required_skills=["DSA", "Python"],
            min_attendance_percentage=75.0
        )
        session.add(crit)

        await session.commit()

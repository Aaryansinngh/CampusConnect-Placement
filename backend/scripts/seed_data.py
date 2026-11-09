import os
import sys
import uuid
from datetime import datetime, timedelta, timezone

# Add parent directory to sys.path
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from app.core.database import SyncSessionLocal, Base, sync_engine
from app.core.security import hash_password
from app.models.university import University
from app.models.department import Department
from app.models.user import User, UserRole
from app.models.company import Company, CompanyVerificationStatus
from app.models.recruiter import Recruiter
from app.models.student import Student
from app.models.placement_drive import PlacementDrive, DriveStatus, EmploymentType
from app.models.eligibility import EligibilityCriteria
from app.models.application import Application, ApplicationStatus
from app.models.announcement import Announcement


def seed_database():
    print("[Seed] Initializing CampusConnect database seeding...")

    # Ensure tables exist
    Base.metadata.create_all(bind=sync_engine)

    session = SyncSessionLocal()
    try:
        # Check if already seeded
        existing_u = session.query(University).filter(University.code == "AIT").first()
        if existing_u:
            print("[Seed] Database already seeded. Skipping.")
            return

        # 1. Create Demo University
        print("[Seed] Creating University...")
        university = University(
            id=str(uuid.uuid4()),
            name="Apex Institute of Technology",
            code="AIT",
            domain="apex.edu",
            logo_url="https://images.unsplash.com/photo-1562774053-701939374585?w=200"
        )
        session.add(university)
        session.flush()

        # 2. Create Academic Departments
        print("[Seed] Creating Departments...")
        dept_data = [
            ("CSE", "Computer Science & Engineering"),
            ("IT", "Information Technology"),
            ("ECE", "Electronics & Communication Engineering"),
            ("ME", "Mechanical Engineering")
        ]
        dept_map = {}
        for code, name in dept_data:
            dept = Department(
                id=str(uuid.uuid4()),
                university_id=university.id,
                code=code,
                name=name
            )
            session.add(dept)
            dept_map[code] = dept
        session.flush()

        # 3. Create Super Admin & TPO Admin
        print("[Seed] Creating Administrators...")
        super_admin = User(
            id=str(uuid.uuid4()),
            university_id=university.id,
            email="superadmin@campusconnect.edu",
            hashed_password=hash_password("AdminSecure2026!"),
            role=UserRole.SUPER_ADMIN.value,
            is_active=True,
            is_verified=True
        )
        tpo_admin = User(
            id=str(uuid.uuid4()),
            university_id=university.id,
            email="tpo@apex.edu",
            hashed_password=hash_password("TpoPassword123!"),
            role=UserRole.TPO_ADMIN.value,
            is_active=True,
            is_verified=True
        )
        session.add_all([super_admin, tpo_admin])
        session.flush()

        # 4. Create Companies & Recruiters
        print("[Seed] Creating Companies & Recruiters...")
        companies_info = [
            {
                "name": "Google",
                "logo": "https://images.unsplash.com/photo-1573804633927-bfcbcd909acd?w=150",
                "site": "https://careers.google.com",
                "ind": "Technology & AI",
                "rec_email": "google.recruiter@google.com",
                "rec_name": "Sundar Verma",
                "rec_desig": "Senior Technical Recruiter"
            },
            {
                "name": "Microsoft",
                "logo": "https://images.unsplash.com/photo-1583321500900-82807e458f3c?w=150",
                "site": "https://careers.microsoft.com",
                "ind": "Cloud Computing & Enterprise Software",
                "rec_email": "microsoft.recruiter@microsoft.com",
                "rec_name": "Ananya Sharma",
                "rec_desig": "University Talent Acquisition Lead"
            },
            {
                "name": "Amazon",
                "logo": "https://images.unsplash.com/photo-1523474255658-406167470201?w=150",
                "site": "https://amazon.jobs",
                "ind": "E-Commerce & Cloud Infrastructure",
                "rec_email": "amazon.recruiter@amazon.com",
                "rec_name": "Rohit Nair",
                "rec_desig": "Campus Hiring Specialist"
            }
        ]

        recruiter_map = {}
        company_map = {}
        for c in companies_info:
            comp = Company(
                id=str(uuid.uuid4()),
                name=c["name"],
                logo_url=c["logo"],
                website=c["site"],
                industry=c["ind"],
                description=f"{c['name']} campus recruitment division visiting university placement drives.",
                verification_status=CompanyVerificationStatus.VERIFIED.value
            )
            session.add(comp)
            session.flush()
            company_map[c["name"]] = comp

            rec_user = User(
                id=str(uuid.uuid4()),
                university_id=None,
                email=c["rec_email"],
                hashed_password=hash_password("Recruiter123!"),
                role=UserRole.RECRUITER.value,
                is_active=True,
                is_verified=True
            )
            session.add(rec_user)
            session.flush()

            recruiter = Recruiter(
                id=str(uuid.uuid4()),
                user_id=rec_user.id,
                company_id=comp.id,
                full_name=c["rec_name"],
                designation=c["rec_desig"],
                phone_number="+91 98765 43210",
                is_approved=True
            )
            session.add(recruiter)
            session.flush()
            recruiter_map[c["name"]] = recruiter

        # 5. Create 16 Students across different departments, CGPAs, and backlogs
        print("[Seed] Creating 16 Student Accounts...")
        student_profiles = [
            ("Alex Johnson", "alex.cs@apex.edu", "2026-CS-001", "CSE", 2026, 9.4, 0, ["Python", "DSA", "SQL", "React"]),
            ("Priya Nair", "priya.it@apex.edu", "2026-IT-002", "IT", 2026, 8.8, 0, ["Java", "Spring Boot", "Docker", "AWS"]),
            ("Rohan Mehta", "rohan.cs@apex.edu", "2026-CS-003", "CSE", 2026, 8.2, 0, ["C++", "DSA", "Machine Learning"]),
            ("Sneha Patel", "sneha.ece@apex.edu", "2026-EC-004", "ECE", 2026, 7.8, 1, ["Embedded C", "IoT", "Python", "Networks"]),
            ("Arjun Verma", "arjun.me@apex.edu", "2026-ME-005", "ME", 2026, 7.2, 0, ["AutoCAD", "MATLAB", "SolidWorks", "Python"]),
            ("Kavita Rao", "kavita.cs@apex.edu", "2026-CS-006", "CSE", 2026, 9.1, 0, ["Algorithms", "Go", "Kubernetes", "Linux"]),
            ("Devansh Gupta", "devansh.it@apex.edu", "2026-IT-007", "IT", 2026, 6.8, 2, ["JavaScript", "HTML", "CSS", "SQL"]),
            ("Meera Iyer", "meera.cs@apex.edu", "2026-CS-008", "CSE", 2026, 8.5, 0, ["Python", "TensorFlow", "FastAPI", "React"]),
            ("Vikram Singh", "vikram.ece@apex.edu", "2026-EC-009", "ECE", 2026, 7.5, 0, ["Verilog", "VLSI", "C", "Microcontrollers"]),
            ("Ananya Das", "ananya.it@apex.edu", "2026-IT-010", "IT", 2026, 8.9, 0, ["Java", "Cloud", "PostgreSQL", "React Native"]),
            ("Karan Malhotra", "karan.me@apex.edu", "2026-ME-011", "ME", 2026, 6.5, 1, ["Ansys", "Thermodynamics", "Operations", "Linux"]),
            ("Ritika Sen", "ritika.cs@apex.edu", "2026-CS-012", "CSE", 2026, 7.9, 0, ["Node.js", "MongoDB", "Express", "Python"]),
            ("Siddharth Roy", "sid.it@apex.edu", "2026-IT-013", "IT", 2026, 8.3, 0, ["Kotlin", "Android", "Jetpack Compose", "REST"]),
            ("Tanya Kapoor", "tanya.cs@apex.edu", "2026-CS-014", "CSE", 2026, 9.7, 0, ["System Design", "Distributed Systems", "C++", "DSA"]),
            ("Manish Tiwari", "manish.ece@apex.edu", "2026-EC-015", "ECE", 2026, 7.1, 1, ["Signal Processing", "Python", "Data Analysis"]),
            ("Pooja Hegde", "pooja.me@apex.edu", "2026-ME-016", "ME", 2026, 8.0, 0, ["Robotics", "ROS", "Python", "CAD"])
        ]

        created_students = []
        for name, email, reg_no, dept_code, grad_yr, cgpa, backlogs, skills in student_profiles:
            st_user = User(
                id=str(uuid.uuid4()),
                university_id=university.id,
                email=email,
                hashed_password=hash_password("Student123!"),
                role=UserRole.STUDENT.value,
                is_active=True,
                is_verified=True
            )
            session.add(st_user)
            session.flush()

            st = Student(
                id=str(uuid.uuid4()),
                user_id=st_user.id,
                university_id=university.id,
                department_id=dept_map[dept_code].id,
                full_name=name,
                registration_number=reg_no,
                degree="B.Tech",
                graduation_year=grad_yr,
                current_cgpa=cgpa,
                active_backlogs=backlogs,
                skills=skills,
                phone_number="+91 99887 76655",
                resume_url="https://res.cloudinary.com/demo/image/upload/sample_resume.pdf",
                is_verified=True
            )
            session.add(st)
            created_students.append(st)

        session.flush()

        # 6. Create Multiple Placement Drives with distinct eligibility criteria
        print("[Seed] Creating Placement Drives and Eligibility Criteria...")
        drives_data = [
            {
                "comp": "Google",
                "title": "Software Development Engineer (SDE-1)",
                "desc": "Build highly scalable distributed systems, microservices, and AI-driven cloud solutions for billions of users.",
                "ctc": "32.0 LPA",
                "loc": "Bangalore / Hyderabad",
                "vacancies": 8,
                "days_ahead": 14,
                "min_cgpa": 8.0,
                "max_backlogs": 0,
                "dept_codes": ["CSE", "IT"],
                "years": [2026],
                "skills": ["DSA", "Python", "C++"]
            },
            {
                "comp": "Microsoft",
                "title": "Cloud Solutions & DevOps Associate",
                "desc": "Design enterprise Azure solutions, automate cloud infrastructure, and partner with fortune 500 engineering teams.",
                "ctc": "24.5 LPA",
                "loc": "Hyderabad / Noida",
                "vacancies": 12,
                "days_ahead": 20,
                "min_cgpa": 7.5,
                "max_backlogs": 1,
                "dept_codes": ["CSE", "IT", "ECE"],
                "years": [2026],
                "skills": ["Cloud", "Docker", "Python"]
            },
            {
                "comp": "Amazon",
                "title": "Systems Specialist & Operations Engineer",
                "desc": "Lead fulfillment and automation infrastructure, optimize high-throughput operational systems, and debug distributed hardware.",
                "ctc": "18.0 LPA",
                "loc": "Chennai / Pune",
                "vacancies": 15,
                "days_ahead": 25,
                "min_cgpa": 6.5,
                "max_backlogs": 1,
                "dept_codes": ["CSE", "IT", "ECE", "ME"],
                "years": [2026],
                "skills": ["Linux", "Problem Solving"]
            }
        ]

        created_drives = []
        for d in drives_data:
            comp = company_map[d["comp"]]
            rec = recruiter_map[d["comp"]]
            deadline = datetime.now(timezone.utc) + timedelta(days=d["days_ahead"])

            drive = PlacementDrive(
                id=str(uuid.uuid4()),
                university_id=university.id,
                company_id=comp.id,
                created_by_recruiter_id=rec.id,
                job_title=d["title"],
                job_description=d["desc"],
                salary_package_ctc=d["ctc"],
                job_location=d["loc"],
                employment_type=EmploymentType.FULL_TIME.value,
                vacancies=d["vacancies"],
                status=DriveStatus.ACTIVE.value,
                application_deadline=deadline
            )
            session.add(drive)
            session.flush()

            # Map department codes to IDs
            eligible_dept_ids = [dept_map[code].id for code in d["dept_codes"] if code in dept_map]

            crit = EligibilityCriteria(
                id=str(uuid.uuid4()),
                drive_id=drive.id,
                min_cgpa=d["min_cgpa"],
                max_backlogs=d["max_backlogs"],
                eligible_department_ids=eligible_dept_ids,
                eligible_graduation_years=d["years"],
                required_skills=d["skills"],
                min_attendance_percentage=75.0
            )
            session.add(crit)
            created_drives.append(drive)

        session.flush()

        # 7. Create Sample Initial Applications
        print("[Seed] Creating Sample Applications...")
        # Alex Johnson applied to Google SDE
        app1 = Application(
            id=str(uuid.uuid4()),
            drive_id=created_drives[0].id,
            student_id=created_students[0].id,
            status=ApplicationStatus.SHORTLISTED.value
        )
        # Priya Nair applied to Microsoft Cloud
        app2 = Application(
            id=str(uuid.uuid4()),
            drive_id=created_drives[1].id,
            student_id=created_students[1].id,
            status=ApplicationStatus.APPLIED.value
        )
        session.add_all([app1, app2])

        # 8. Create Campus Announcement
        print("[Seed] Creating Campus Announcements...")
        announcement = Announcement(
            id=str(uuid.uuid4()),
            university_id=university.id,
            created_by=tpo_admin.id,
            title="Campus Placement Season 2026 Inauguration",
            content="Welcome graduating batch of 2026. Top tier recruitment drives from Google, Microsoft, and Amazon are now live on CampusConnect. Verify your resumes and ensure your profile information is up to date.",
            target_departments=[],
            target_batches=[2026]
        )
        session.add(announcement)

        session.commit()
        print("[Seed] SUCCESS: Database successfully seeded with demo university, administrators, recruiters, 16 students, and placement drives!")

    except Exception as e:
        session.rollback()
        print(f"[Seed] ERROR during seeding: {e}")
        raise e
    finally:
        session.close()


if __name__ == "__main__":
    seed_database()

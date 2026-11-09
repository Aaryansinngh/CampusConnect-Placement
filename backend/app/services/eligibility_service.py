from typing import List, Dict, Any
from app.models.student import Student
from app.models.eligibility import EligibilityCriteria
from app.schemas.eligibility import EligibilityCheckResult


class EligibilityService:
    @staticmethod
    def evaluate(student: Student, criteria: EligibilityCriteria) -> EligibilityCheckResult:
        """
        Evaluates a student's profile against placement drive criteria.
        Pure domain service: does not rely on frontend or client claims.
        """
        reasons: List[str] = []
        details: Dict[str, Any] = {}

        # 1. CGPA Check
        details["cgpa"] = {
            "student": student.current_cgpa,
            "required": criteria.min_cgpa,
            "passed": student.current_cgpa >= criteria.min_cgpa
        }
        if not details["cgpa"]["passed"]:
            reasons.append(
                f"Current CGPA ({student.current_cgpa:.2f}) is below the minimum required ({criteria.min_cgpa:.2f})."
            )

        # 2. Backlog Check
        details["backlogs"] = {
            "student": student.active_backlogs,
            "max_allowed": criteria.max_backlogs,
            "passed": student.active_backlogs <= criteria.max_backlogs
        }
        if not details["backlogs"]["passed"]:
            reasons.append(
                f"Active backlogs ({student.active_backlogs}) exceed the maximum limit of {criteria.max_backlogs}."
            )

        # 3. Department Check
        eligible_depts = [str(d) for d in (criteria.eligible_department_ids or [])]
        student_dept = str(student.department_id)
        if eligible_depts:
            dept_passed = student_dept in eligible_depts
            details["department"] = {
                "student": student_dept,
                "eligible": eligible_depts,
                "passed": dept_passed
            }
            if not dept_passed:
                reasons.append("Your academic department is not eligible for this placement drive.")
        else:
            details["department"] = {"passed": True, "note": "All departments eligible"}

        # 4. Graduation Year Check
        eligible_years = [int(y) for y in (criteria.eligible_graduation_years or [])]
        if eligible_years:
            year_passed = student.graduation_year in eligible_years
            details["graduation_year"] = {
                "student": student.graduation_year,
                "eligible": eligible_years,
                "passed": year_passed
            }
            if not year_passed:
                reasons.append(
                    f"Graduation batch {student.graduation_year} is not eligible (Eligible: {eligible_years})."
                )
        else:
            details["graduation_year"] = {"passed": True, "note": "All batches eligible"}

        # 5. Required Skills Check (Substring / case-insensitive)
        required_skills = criteria.required_skills or []
        student_skills_lower = [str(s).strip().lower() for s in (student.skills or [])]
        missing_skills = []
        for req in required_skills:
            req_str = str(req).strip().lower()
            if not any(req_str in s for s in student_skills_lower):
                missing_skills.append(req)

        details["skills"] = {
            "student": student.skills or [],
            "required": required_skills,
            "missing": missing_skills,
            "passed": len(missing_skills) == 0
        }
        if missing_skills:
            reasons.append(f"Missing required skills: {', '.join(missing_skills)}.")

        is_eligible = len(reasons) == 0

        return EligibilityCheckResult(
            is_eligible=is_eligible,
            reasons=reasons,
            evaluation_details=details
        )

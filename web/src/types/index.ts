export type UserRole = 'STUDENT' | 'RECRUITER' | 'TPO_ADMIN' | 'SUPER_ADMIN';

export interface User {
  id: string;
  email: string;
  role: UserRole;
  is_active: boolean;
  university_id?: string;
  student?: StudentProfile;
  recruiter?: RecruiterProfile;
}

export interface StudentProfile {
  id: string;
  user_id: string;
  university_id: string;
  department_id: string;
  full_name: string;
  registration_number: string;
  degree: string;
  graduation_year: number;
  current_cgpa: number;
  active_backlogs: number;
  skills: string[];
  phone_number?: string;
  resume_url?: string;
  is_verified: boolean;
}

export interface Company {
  id: string;
  name: string;
  website?: string;
  industry?: string;
  description?: string;
  logo_url?: string;
  is_verified: boolean;
}

export interface RecruiterProfile {
  id: string;
  user_id: string;
  company_id: string;
  full_name: string;
  designation?: string;
  phone_number?: string;
  company?: Company;
}

export interface EligibilityCriteria {
  id?: string;
  drive_id?: string;
  min_cgpa: number;
  max_backlogs: number;
  eligible_department_ids: string[];
  eligible_graduation_years: number[];
  required_skills: string[];
  min_attendance_percentage?: number;
}

export interface RecruitmentRound {
  id: string;
  drive_id: string;
  round_number: number;
  name: string;
  round_type: 'ONLINE_ASSESSMENT' | 'TECHNICAL_INTERVIEW' | 'MANAGERIAL' | 'HR_INTERVIEW' | 'GROUP_DISCUSSION';
  description?: string;
}

export interface PlacementDrive {
  id: string;
  university_id: string;
  company_id: string;
  job_title: string;
  job_description: string;
  salary_package_ctc: string;
  job_location: string;
  employment_type: string;
  vacancies: number;
  status: 'DRAFT' | 'PENDING_APPROVAL' | 'APPROVED' | 'ACTIVE' | 'COMPLETED' | 'CANCELLED';
  application_deadline: string;
  eligibility_criteria?: EligibilityCriteria;
  rounds?: RecruitmentRound[];
  company?: Company;
  applications_count?: number;
  created_at?: string;
}

export type ApplicationStatus = 
  | 'APPLIED' 
  | 'UNDER_REVIEW'
  | 'SHORTLISTED' 
  | 'ASSESSMENT'
  | 'TECHNICAL_INTERVIEW'
  | 'HR_INTERVIEW'
  | 'SELECTED' 
  | 'REJECTED' 
  | 'WITHDRAWN';

export interface Application {
  id: string;
  drive_id: string;
  student_id: string;
  status: ApplicationStatus;
  applied_at: string;
  withdrawal_reason?: string;
  student?: StudentProfile;
  drive?: PlacementDrive;
}

export interface Department {
  id: string;
  name: string;
  code: string;
}

import { apiClient } from './client';
import { 
  PlacementDrive, 
  Application, 
  StudentProfile, 
  Company, 
  Department,
  ApplicationStatus 
} from '../types';

export interface CreateDrivePayload {
  company_id: string;
  job_title: string;
  job_description: string;
  salary_package_ctc: string;
  job_location: string;
  employment_type: string;
  vacancies: number;
  application_deadline: string;
  eligibility?: {
    min_cgpa: number;
    max_backlogs: number;
    eligible_department_ids: string[];
    eligible_graduation_years: number[];
    required_skills: string[];
    min_attendance_percentage?: number;
  };
}

export const recruiterApi = {
  getDrives: async (statusFilter?: string): Promise<PlacementDrive[]> => {
    const params = statusFilter ? { status_filter: statusFilter } : {};
    const response = await apiClient.get<PlacementDrive[]>('/placement-drives', { params });
    return response.data;
  },

  getDriveById: async (driveId: string): Promise<PlacementDrive> => {
    const response = await apiClient.get<PlacementDrive>(`/placement-drives/${driveId}`);
    return response.data;
  },

  createDrive: async (payload: CreateDrivePayload): Promise<PlacementDrive> => {
    const response = await apiClient.post<PlacementDrive>('/placement-drives', payload);
    return response.data;
  },

  getDriveApplications: async (driveId: string, statusFilter?: string): Promise<Application[]> => {
    const params = statusFilter ? { status_filter: statusFilter } : {};
    const response = await apiClient.get<Application[]>(`/applications/drive/${driveId}`, { params });
    return response.data;
  },

  updateApplicationStatus: async (
    applicationId: string, 
    status: ApplicationStatus, 
    withdrawalReason?: string
  ): Promise<Application> => {
    const response = await apiClient.put<Application>(`/applications/${applicationId}/status`, {
      status,
      withdrawal_reason: withdrawalReason,
    });
    return response.data;
  },

  getStudentById: async (studentId: string): Promise<StudentProfile> => {
    const response = await apiClient.get<StudentProfile>(`/students/${studentId}`);
    return response.data;
  },

  getCompanies: async (): Promise<Company[]> => {
    const response = await apiClient.get<Company[]>('/companies');
    return response.data;
  },

  getCompanyById: async (companyId: string): Promise<Company> => {
    const response = await apiClient.get<Company>(`/companies/${companyId}`);
    return response.data;
  },

  getDepartments: async (): Promise<Department[]> => {
    const response = await apiClient.get<Department[]>('/admin/departments');
    return response.data;
  },
};

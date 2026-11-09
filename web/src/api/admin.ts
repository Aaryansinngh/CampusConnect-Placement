import { apiClient } from './client';
import { 
  Company, 
  PlacementDrive, 
  StudentProfile, 
  Department 
} from '../types';

export interface AdminStats {
  total_students: number;
  total_companies: number;
  active_drives: number;
  total_applications: number;
  students_placed: number;
  students_unplaced: number;
  placement_percentage: number;
  average_package_lpa: string;
  highest_package_lpa: string;
}

export interface RecruiterAccount {
  id: string;
  user_id: string;
  company_id: string;
  full_name: string;
  designation?: string;
  phone_number?: string;
  is_approved: boolean;
}

export interface AnnouncementItem {
  id: string;
  university_id: string;
  created_by: string;
  title: string;
  content: string;
  target_departments: string[];
  target_batches: number[];
  created_at?: string;
}

export interface StudentAcademicUpdatePayload {
  current_cgpa?: number;
  active_backlogs?: number;
  is_verified?: boolean;
  department_id?: string;
  registration_number?: string;
}

export const adminApi = {
  getStats: async (): Promise<AdminStats> => {
    const response = await apiClient.get<AdminStats>('/admin/stats');
    return response.data;
  },

  getStudents: async (params?: {
    department_id?: string;
    graduation_year?: number;
    is_verified?: boolean;
  }): Promise<StudentProfile[]> => {
    const response = await apiClient.get<StudentProfile[]>('/students', { params });
    return response.data;
  },

  updateStudentAcademic: async (
    studentId: string,
    payload: StudentAcademicUpdatePayload
  ): Promise<StudentProfile> => {
    const response = await apiClient.put<StudentProfile>(`/students/${studentId}/academic`, payload);
    return response.data;
  },

  getCompanies: async (statusFilter?: string): Promise<Company[]> => {
    const params = statusFilter ? { status_filter: statusFilter } : {};
    const response = await apiClient.get<Company[]>('/companies', { params });
    return response.data;
  },

  verifyCompany: async (
    companyId: string,
    verificationStatus: 'VERIFIED' | 'REJECTED' | 'PENDING',
    description?: string
  ): Promise<Company> => {
    const response = await apiClient.put<Company>(`/companies/${companyId}/verification`, {
      verification_status: verificationStatus,
      description,
    });
    return response.data;
  },

  getRecruiters: async (): Promise<RecruiterAccount[]> => {
    const response = await apiClient.get<RecruiterAccount[]>('/recruiters');
    return response.data;
  },

  approveRecruiter: async (recruiterId: string, isApproved: boolean): Promise<RecruiterAccount> => {
    const response = await apiClient.put<RecruiterAccount>(`/recruiters/${recruiterId}/approval`, {
      is_approved: isApproved,
    });
    return response.data;
  },

  getDrives: async (statusFilter?: string): Promise<PlacementDrive[]> => {
    const params = statusFilter ? { status_filter: statusFilter } : {};
    const response = await apiClient.get<PlacementDrive[]>('/placement-drives', { params });
    return response.data;
  },

  updateDriveStatus: async (driveId: string, status: string): Promise<PlacementDrive> => {
    const response = await apiClient.put<PlacementDrive>(`/placement-drives/${driveId}/status`, {
      status,
    });
    return response.data;
  },

  getAnnouncements: async (): Promise<AnnouncementItem[]> => {
    const response = await apiClient.get<AnnouncementItem[]>('/admin/announcements');
    return response.data;
  },

  createAnnouncement: async (payload: {
    title: string;
    content: string;
    target_departments: string[];
    target_batches: number[];
  }): Promise<AnnouncementItem> => {
    const response = await apiClient.post<AnnouncementItem>('/admin/announcements', payload);
    return response.data;
  },

  getDepartments: async (): Promise<Department[]> => {
    const response = await apiClient.get<Department[]>('/admin/departments');
    return response.data;
  },
};

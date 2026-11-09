import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from './context/AuthContext';
import { RecruiterLayout } from './components/layout/RecruiterLayout';
import { AdminLayout } from './components/layout/AdminLayout';

// Recruiter Pages
import { LoginPage as RecruiterLoginPage } from './pages/recruiter/LoginPage';
import { DashboardPage as RecruiterDashboardPage } from './pages/recruiter/DashboardPage';
import { DrivesListPage } from './pages/recruiter/DrivesListPage';
import { CreateDrivePage } from './pages/recruiter/CreateDrivePage';
import { DriveApplicantsPage } from './pages/recruiter/DriveApplicantsPage';
import { CompanyProfilePage } from './pages/recruiter/CompanyProfilePage';

// Admin / TPO Pages
import { AdminLoginPage } from './pages/admin/AdminLoginPage';
import { AdminDashboardPage } from './pages/admin/AdminDashboardPage';
import { AdminDrivesPage } from './pages/admin/AdminDrivesPage';
import { AdminCompaniesPage } from './pages/admin/AdminCompaniesPage';
import { AdminStudentsPage } from './pages/admin/AdminStudentsPage';
import { AdminAnnouncementsPage } from './pages/admin/AdminAnnouncementsPage';

const ProtectedRecruiterRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isAuthenticated, isLoading, role } = useAuth();

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-slate-50 text-slate-500 text-sm">
        Verifying corporate session...
      </div>
    );
  }

  if (!isAuthenticated || role !== 'RECRUITER') {
    return <Navigate to="/recruiter/login" replace />;
  }

  return <>{children}</>;
};

const ProtectedAdminRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isAuthenticated, isLoading, role } = useAuth();

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-slate-50 text-slate-500 text-sm">
        Verifying administrative credentials...
      </div>
    );
  }

  if (!isAuthenticated || (role !== 'TPO_ADMIN' && role !== 'SUPER_ADMIN')) {
    return <Navigate to="/admin/login" replace />;
  }

  return <>{children}</>;
};

export const App: React.FC = () => {
  return (
    <BrowserRouter>
      <Routes>
        {/* Recruiter Portal Routes */}
        <Route path="/recruiter/login" element={<RecruiterLoginPage />} />
        <Route
          path="/recruiter"
          element={
            <ProtectedRecruiterRoute>
              <RecruiterLayout />
            </ProtectedRecruiterRoute>
          }
        >
          <Route index element={<Navigate to="/recruiter/dashboard" replace />} />
          <Route path="dashboard" element={<RecruiterDashboardPage />} />
          <Route path="drives" element={<DrivesListPage />} />
          <Route path="drives/create" element={<CreateDrivePage />} />
          <Route path="drives/:driveId/applicants" element={<DriveApplicantsPage />} />
          <Route path="company" element={<CompanyProfilePage />} />
        </Route>

        {/* TPO / Admin Portal Routes */}
        <Route path="/admin/login" element={<AdminLoginPage />} />
        <Route
          path="/admin"
          element={
            <ProtectedAdminRoute>
              <AdminLayout />
            </ProtectedAdminRoute>
          }
        >
          <Route index element={<Navigate to="/admin/dashboard" replace />} />
          <Route path="dashboard" element={<AdminDashboardPage />} />
          <Route path="drives" element={<AdminDrivesPage />} />
          <Route path="companies" element={<AdminCompaniesPage />} />
          <Route path="students" element={<AdminStudentsPage />} />
          <Route path="announcements" element={<AdminAnnouncementsPage />} />
        </Route>

        {/* Root Fallback */}
        <Route path="/" element={<Navigate to="/recruiter/login" replace />} />
        <Route path="*" element={<Navigate to="/recruiter/login" replace />} />
      </Routes>
    </BrowserRouter>
  );
};

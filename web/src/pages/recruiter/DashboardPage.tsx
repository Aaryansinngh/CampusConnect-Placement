import React from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { recruiterApi } from '../../api/recruiter';
import { useAuth } from '../../context/AuthContext';
import { 
  Briefcase, 
  Users, 
  CheckCircle, 
  Clock, 
  Plus, 
  ArrowUpRight, 
  Building,
  GraduationCap
} from 'lucide-react';

export const DashboardPage: React.FC = () => {
  const { user } = useAuth();

  const { data: drives = [], isLoading: drivesLoading } = useQuery({
    queryKey: ['recruiter-drives'],
    queryFn: () => recruiterApi.getDrives(),
  });

  const activeDrives = drives.filter(d => d.status === 'ACTIVE' || d.status === 'APPROVED');
  const pendingDrives = drives.filter(d => d.status === 'PENDING_APPROVAL');

  return (
    <div className="space-y-8">
      {/* Welcome Banner */}
      <div className="bg-gradient-to-r from-slate-900 to-indigo-950 rounded-3xl p-8 text-white flex flex-col md:flex-row md:items-center justify-between gap-6 shadow-xl shadow-slate-900/10">
        <div>
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-primary-500/20 text-primary-300 text-xs font-bold uppercase tracking-wider mb-3">
            <Building className="h-3.5 w-3.5" />
            <span>Corporate Recruitment Console</span>
          </div>
          <h1 className="text-2xl md:text-3xl font-extrabold text-white">
            Welcome back, {user?.recruiter?.full_name || 'Recruiter'}
          </h1>
          <p className="text-slate-400 mt-1 text-sm max-w-xl">
            Manage your company's campus recruitment drives, review student eligibility assessments, and shortlist candidates for upcoming interview rounds.
          </p>
        </div>

        <Link
          to="/recruiter/drives/create"
          className="inline-flex items-center justify-center gap-2 px-5 py-3 rounded-xl bg-primary-600 hover:bg-primary-500 text-white font-semibold text-sm transition shadow-lg shadow-primary-600/30 shrink-0"
        >
          <Plus className="h-4 w-4" />
          <span>Post Placement Drive</span>
        </Link>
      </div>

      {/* KPI Stats Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Active Drives</span>
            <div className="p-2.5 rounded-xl bg-indigo-50 text-primary-600">
              <Briefcase className="h-5 w-5" />
            </div>
          </div>
          <div className="mt-4 flex items-baseline gap-2">
            <span className="text-3xl font-extrabold text-slate-900">{activeDrives.length}</span>
            <span className="text-xs font-medium text-emerald-600">● Live on Campus</span>
          </div>
        </div>

        <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Total Drives Posted</span>
            <div className="p-2.5 rounded-xl bg-purple-50 text-purple-600">
              <GraduationCap className="h-5 w-5" />
            </div>
          </div>
          <div className="mt-4 flex items-baseline gap-2">
            <span className="text-3xl font-extrabold text-slate-900">{drives.length}</span>
            <span className="text-xs font-medium text-slate-400">Class of 2026</span>
          </div>
        </div>

        <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Pending TPO Review</span>
            <div className="p-2.5 rounded-xl bg-amber-50 text-amber-600">
              <Clock className="h-5 w-5" />
            </div>
          </div>
          <div className="mt-4 flex items-baseline gap-2">
            <span className="text-3xl font-extrabold text-slate-900">{pendingDrives.length}</span>
            <span className="text-xs font-medium text-amber-600">Awaiting clearance</span>
          </div>
        </div>

        <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Partner Status</span>
            <div className="p-2.5 rounded-xl bg-emerald-50 text-emerald-600">
              <CheckCircle className="h-5 w-5" />
            </div>
          </div>
          <div className="mt-4 flex items-baseline gap-2">
            <span className="text-xl font-bold text-slate-900">Verified</span>
            <span className="text-xs font-medium text-emerald-600">Active Recruiter</span>
          </div>
        </div>
      </div>

      {/* Placement Drives List Section */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="p-6 border-b border-slate-100 flex items-center justify-between">
          <div>
            <h2 className="text-lg font-bold text-slate-900">Active Recruitment Drives</h2>
            <p className="text-xs text-slate-500 mt-0.5">Campus recruitment drives published to university students</p>
          </div>
          <Link
            to="/recruiter/drives"
            className="text-sm font-semibold text-primary-600 hover:text-primary-700 flex items-center gap-1"
          >
            <span>View All</span>
            <ArrowUpRight className="h-4 w-4" />
          </Link>
        </div>

        {drivesLoading ? (
          <div className="p-12 text-center text-slate-400 text-sm">Loading placement drives...</div>
        ) : drives.length === 0 ? (
          <div className="p-12 text-center">
            <Briefcase className="h-10 w-10 text-slate-300 mx-auto mb-3" />
            <h3 className="text-base font-semibold text-slate-700">No Placement Drives Yet</h3>
            <p className="text-sm text-slate-500 mt-1 max-w-sm mx-auto">
              Get started by creating your company's first placement drive with custom eligibility rules.
            </p>
            <Link
              to="/recruiter/drives/create"
              className="mt-4 inline-flex items-center gap-2 px-4 py-2 bg-primary-600 text-white rounded-xl text-sm font-semibold hover:bg-primary-700"
            >
              <Plus className="h-4 w-4" />
              <span>Create Drive</span>
            </Link>
          </div>
        ) : (
          <div className="divide-y divide-slate-100">
            {drives.slice(0, 5).map((drive) => (
              <div key={drive.id} className="p-6 flex flex-col md:flex-row md:items-center justify-between gap-4 hover:bg-slate-50/80 transition">
                <div className="space-y-1">
                  <div className="flex items-center gap-3">
                    <h3 className="text-base font-bold text-slate-900">{drive.job_title}</h3>
                    <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
                      {drive.status}
                    </span>
                    <span className="px-2 py-0.5 rounded-full text-xs font-semibold bg-indigo-50 text-primary-700">
                      {drive.employment_type.replace('_', ' ')}
                    </span>
                  </div>
                  <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-slate-500">
                    <span>💰 Package: <strong className="text-slate-800">{drive.salary_package_ctc}</strong></span>
                    <span>📍 {drive.job_location}</span>
                    <span>🎓 Min CGPA: <strong className="text-slate-800">{drive.eligibility_criteria?.min_cgpa ?? 6.0}</strong></span>
                    <span>👥 Vacancies: {drive.vacancies}</span>
                  </div>
                </div>

                <div className="flex items-center gap-3 shrink-0">
                  <Link
                    to={`/recruiter/drives/${drive.id}/applicants`}
                    className="px-4 py-2 rounded-xl bg-slate-100 hover:bg-primary-50 hover:text-primary-600 text-slate-700 text-sm font-semibold transition flex items-center gap-2"
                  >
                    <Users className="h-4 w-4" />
                    <span>Candidate ATS Pipeline</span>
                  </Link>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

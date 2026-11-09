import React from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { adminApi } from '../../api/admin';
import { 
  Users, 
  Briefcase, 
  Award, 
  Clock, 
  ArrowUpRight, 
  TrendingUp,
  FileCheck,
  AlertTriangle
} from 'lucide-react';

export const AdminDashboardPage: React.FC = () => {
  const { data: stats, isLoading: statsLoading } = useQuery({
    queryKey: ['admin-stats'],
    queryFn: () => adminApi.getStats(),
  });

  const { data: drives = [] } = useQuery({
    queryKey: ['admin-drives'],
    queryFn: () => adminApi.getDrives(),
  });

  const { data: companies = [] } = useQuery({
    queryKey: ['admin-companies'],
    queryFn: () => adminApi.getCompanies(),
  });

  const pendingDrives = drives.filter(d => d.status === 'PENDING_APPROVAL');
  const pendingCompanies = companies.filter(c => !c.is_verified);

  return (
    <div className="space-y-8">
      {/* Top Banner */}
      <div className="bg-gradient-to-r from-slate-950 via-slate-900 to-amber-950 rounded-3xl p-8 text-white shadow-xl shadow-slate-950/10">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-6">
          <div>
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-amber-500/20 text-amber-300 text-xs font-bold uppercase tracking-wider mb-3">
              <Award className="h-3.5 w-3.5" />
              <span>Campus Placement Leadership Console</span>
            </div>
            <h1 className="text-2xl md:text-3xl font-extrabold text-white">
              Apex Institute of Technology Placement Cell
            </h1>
            <p className="text-slate-400 mt-1 text-sm max-w-xl">
              Real-time monitoring of campus recruitment campaigns, academic eligibility thresholds, company approvals, and student placement rates for the Class of 2026.
            </p>
          </div>

          <div className="flex gap-3 shrink-0">
            <Link
              to="/admin/drives"
              className="inline-flex items-center gap-2 px-5 py-3 rounded-xl bg-amber-600 hover:bg-amber-500 text-white font-semibold text-sm transition shadow-lg shadow-amber-600/30"
            >
              <FileCheck className="h-4 w-4" />
              <span>Review Drives ({pendingDrives.length})</span>
            </Link>
          </div>
        </div>
      </div>

      {/* Pending Clearance Alerts */}
      {(pendingDrives.length > 0 || pendingCompanies.length > 0) && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {pendingDrives.length > 0 && (
            <div className="p-4 rounded-2xl bg-amber-50 border border-amber-200 flex items-center justify-between">
              <div className="flex items-center gap-3">
                <AlertTriangle className="h-5 w-5 text-amber-600" />
                <div>
                  <h4 className="text-sm font-bold text-amber-900">
                    {pendingDrives.length} Placement Drive{pendingDrives.length > 1 ? 's' : ''} Awaiting TPO Clearance
                  </h4>
                  <p className="text-xs text-amber-700 mt-0.5">Recruiters submitted new drives requiring verification.</p>
                </div>
              </div>
              <Link
                to="/admin/drives"
                className="px-3 py-1.5 bg-amber-600 hover:bg-amber-700 text-white text-xs font-bold rounded-lg transition"
              >
                Review
              </Link>
            </div>
          )}

          {pendingCompanies.length > 0 && (
            <div className="p-4 rounded-2xl bg-blue-50 border border-blue-200 flex items-center justify-between">
              <div className="flex items-center gap-3">
                <Clock className="h-5 w-5 text-blue-600" />
                <div>
                  <h4 className="text-sm font-bold text-blue-900">
                    {pendingCompanies.length} Corporate Partner{pendingCompanies.length > 1 ? 's' : ''} Pending Verification
                  </h4>
                  <p className="text-xs text-blue-700 mt-0.5">Authorize company profiles before drives can go live.</p>
                </div>
              </div>
              <Link
                to="/admin/companies"
                className="px-3 py-1.5 bg-blue-600 hover:bg-blue-700 text-white text-xs font-bold rounded-lg transition"
              >
                Verify
              </Link>
            </div>
          )}
        </div>
      )}

      {/* KPI Stats Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Placement Rate</span>
            <div className="p-2.5 rounded-xl bg-emerald-50 text-emerald-600">
              <TrendingUp className="h-5 w-5" />
            </div>
          </div>
          <div className="mt-4 flex items-baseline gap-2">
            <span className="text-3xl font-extrabold text-slate-900">
              {statsLoading ? '...' : `${stats?.placement_percentage ?? 0}%`}
            </span>
            <span className="text-xs font-medium text-emerald-600">
              {stats?.students_placed ?? 0} Placed
            </span>
          </div>
          <div className="w-full bg-slate-100 h-1.5 rounded-full mt-3 overflow-hidden">
            <div
              className="bg-emerald-500 h-full rounded-full transition-all duration-500"
              style={{ width: `${Math.min(100, stats?.placement_percentage ?? 0)}%` }}
            />
          </div>
        </div>

        <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Total Students</span>
            <div className="p-2.5 rounded-xl bg-blue-50 text-blue-600">
              <Users className="h-5 w-5" />
            </div>
          </div>
          <div className="mt-4 flex items-baseline gap-2">
            <span className="text-3xl font-extrabold text-slate-900">{stats?.total_students ?? 0}</span>
            <span className="text-xs font-medium text-slate-400">Enrolled Batch</span>
          </div>
          <p className="text-xs text-slate-500 mt-3">{stats?.students_unplaced ?? 0} Seeking Placement</p>
        </div>

        <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Active Placement Drives</span>
            <div className="p-2.5 rounded-xl bg-amber-50 text-amber-600">
              <Briefcase className="h-5 w-5" />
            </div>
          </div>
          <div className="mt-4 flex items-baseline gap-2">
            <span className="text-3xl font-extrabold text-slate-900">{stats?.active_drives ?? 0}</span>
            <span className="text-xs font-medium text-amber-600">{pendingDrives.length} Pending</span>
          </div>
          <p className="text-xs text-slate-500 mt-3">{stats?.total_applications ?? 0} Total Applications</p>
        </div>

        <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Compensation CTC</span>
            <div className="p-2.5 rounded-xl bg-purple-50 text-purple-600">
              <Award className="h-5 w-5" />
            </div>
          </div>
          <div className="mt-4">
            <div className="flex items-baseline justify-between">
              <span className="text-xs text-slate-400 font-semibold">Highest</span>
              <strong className="text-base text-slate-900">{stats?.highest_package_lpa ?? '42.0 LPA'}</strong>
            </div>
            <div className="flex items-baseline justify-between mt-1">
              <span className="text-xs text-slate-400 font-semibold">Average</span>
              <strong className="text-base text-slate-900">{stats?.average_package_lpa ?? '9.2 LPA'}</strong>
            </div>
          </div>
        </div>
      </div>

      {/* Drives Requiring TPO Review */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="p-6 border-b border-slate-100 flex items-center justify-between">
          <div>
            <h2 className="text-lg font-bold text-slate-900">Placement Drives Overview</h2>
            <p className="text-xs text-slate-500 mt-0.5">Manage drives posted by corporate partners</p>
          </div>
          <Link
            to="/admin/drives"
            className="text-sm font-semibold text-amber-600 hover:text-amber-700 flex items-center gap-1"
          >
            <span>Manage All Drives</span>
            <ArrowUpRight className="h-4 w-4" />
          </Link>
        </div>

        {drives.length === 0 ? (
          <div className="p-12 text-center text-slate-400 text-sm">No drives found.</div>
        ) : (
          <div className="divide-y divide-slate-100">
            {drives.slice(0, 5).map((drive) => (
              <div key={drive.id} className="p-6 flex flex-col md:flex-row md:items-center justify-between gap-4 hover:bg-slate-50/80 transition">
                <div className="space-y-1">
                  <div className="flex items-center gap-3">
                    <h3 className="text-base font-bold text-slate-900">{drive.job_title}</h3>
                    <span
                      className={`px-2.5 py-0.5 rounded-full text-xs font-bold ${
                        drive.status === 'APPROVED' || drive.status === 'ACTIVE'
                          ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                          : 'bg-amber-50 text-amber-700 border border-amber-200'
                      }`}
                    >
                      {drive.status}
                    </span>
                  </div>
                  <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-slate-500">
                    <span>💰 Package: <strong className="text-slate-800">{drive.salary_package_ctc}</strong></span>
                    <span>📍 {drive.job_location}</span>
                    <span>🎓 Min CGPA: <strong className="text-slate-800">{drive.eligibility_criteria?.min_cgpa ?? 6.0}</strong></span>
                    <span>👥 Vacancies: {drive.vacancies}</span>
                  </div>
                </div>

                <Link
                  to="/admin/drives"
                  className="px-4 py-2 rounded-xl bg-slate-100 hover:bg-amber-50 hover:text-amber-700 text-slate-700 text-xs font-bold transition shrink-0"
                >
                  Clearance Action →
                </Link>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

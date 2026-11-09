import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { recruiterApi } from '../../api/recruiter';
import { 
  Briefcase, 
  Plus, 
  Search, 
  Users, 
  Calendar, 
  MapPin, 
  DollarSign, 
  GraduationCap
} from 'lucide-react';

export const DrivesListPage: React.FC = () => {
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');

  const { data: drives = [], isLoading } = useQuery({
    queryKey: ['recruiter-drives'],
    queryFn: () => recruiterApi.getDrives(),
  });

  const filteredDrives = drives.filter((drive) => {
    const matchesSearch = 
      drive.job_title.toLowerCase().includes(search.toLowerCase()) ||
      drive.job_location.toLowerCase().includes(search.toLowerCase());
    
    if (statusFilter === 'ALL') return matchesSearch;
    return matchesSearch && drive.status.toUpperCase() === statusFilter;
  });

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900">Placement Drives</h1>
          <p className="text-sm text-slate-500 mt-0.5">Manage and track your company's campus recruitment drives</p>
        </div>

        <Link
          to="/recruiter/drives/create"
          className="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-primary-600 hover:bg-primary-700 text-white font-semibold text-sm transition shadow-md shadow-primary-600/20 shrink-0"
        >
          <Plus className="h-4 w-4" />
          <span>Post Placement Drive</span>
        </Link>
      </div>

      {/* Filters & Search */}
      <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm flex flex-col sm:flex-row gap-4 items-center justify-between">
        <div className="relative w-full sm:w-80">
          <Search className="h-4 w-4 text-slate-400 absolute left-3.5 top-3" />
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search drives by title, location..."
            className="w-full pl-10 pr-4 py-2 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent text-sm"
          />
        </div>

        <div className="flex items-center gap-1.5 w-full sm:w-auto overflow-x-auto pb-1 sm:pb-0">
          {['ALL', 'ACTIVE', 'APPROVED', 'PENDING_APPROVAL'].map((status) => (
            <button
              key={status}
              onClick={() => setStatusFilter(status)}
              className={`px-3.5 py-1.5 rounded-xl text-xs font-semibold whitespace-nowrap transition ${
                statusFilter === status
                  ? 'bg-primary-600 text-white shadow-sm'
                  : 'bg-slate-100 hover:bg-slate-200 text-slate-600'
              }`}
            >
              {status.replace('_', ' ')}
            </button>
          ))}
        </div>
      </div>

      {/* Drives Grid */}
      {isLoading ? (
        <div className="p-16 text-center text-slate-400 text-sm">Loading drives...</div>
      ) : filteredDrives.length === 0 ? (
        <div className="bg-white p-16 rounded-2xl border border-slate-200 text-center">
          <Briefcase className="h-10 w-10 text-slate-300 mx-auto mb-3" />
          <h3 className="text-base font-semibold text-slate-800">No matching drives found</h3>
          <p className="text-sm text-slate-500 mt-1 max-w-sm mx-auto">
            Try adjusting your search filters or create a new placement drive.
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {filteredDrives.map((drive) => (
            <div
              key={drive.id}
              className="bg-white rounded-2xl border border-slate-200 shadow-sm hover:shadow-md transition p-6 flex flex-col justify-between"
            >
              <div className="space-y-4">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <h3 className="text-lg font-bold text-slate-900 leading-snug">{drive.job_title}</h3>
                    <p className="text-xs text-primary-600 font-semibold uppercase mt-0.5 tracking-wider">
                      {drive.employment_type.replace('_', ' ')}
                    </p>
                  </div>
                  <span
                    className={`px-2.5 py-1 rounded-full text-xs font-bold shrink-0 ${
                      drive.status === 'ACTIVE' || drive.status === 'APPROVED'
                        ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                        : 'bg-amber-50 text-amber-700 border border-amber-200'
                    }`}
                  >
                    {drive.status}
                  </span>
                </div>

                <p className="text-sm text-slate-600 line-clamp-2 leading-relaxed">
                  {drive.job_description}
                </p>

                {/* Details Pills */}
                <div className="grid grid-cols-2 gap-2 text-xs">
                  <div className="flex items-center gap-2 p-2.5 rounded-xl bg-slate-50 text-slate-700">
                    <DollarSign className="h-4 w-4 text-emerald-600 shrink-0" />
                    <div>
                      <span className="text-[10px] uppercase text-slate-400 font-bold block">Salary Package</span>
                      <strong className="text-slate-900">{drive.salary_package_ctc}</strong>
                    </div>
                  </div>

                  <div className="flex items-center gap-2 p-2.5 rounded-xl bg-slate-50 text-slate-700">
                    <MapPin className="h-4 w-4 text-primary-600 shrink-0" />
                    <div>
                      <span className="text-[10px] uppercase text-slate-400 font-bold block">Location</span>
                      <strong className="text-slate-900 truncate block">{drive.job_location}</strong>
                    </div>
                  </div>

                  <div className="flex items-center gap-2 p-2.5 rounded-xl bg-slate-50 text-slate-700">
                    <GraduationCap className="h-4 w-4 text-indigo-600 shrink-0" />
                    <div>
                      <span className="text-[10px] uppercase text-slate-400 font-bold block">Min CGPA</span>
                      <strong className="text-slate-900">{drive.eligibility_criteria?.min_cgpa ?? 6.0}</strong>
                    </div>
                  </div>

                  <div className="flex items-center gap-2 p-2.5 rounded-xl bg-slate-50 text-slate-700">
                    <Calendar className="h-4 w-4 text-rose-500 shrink-0" />
                    <div>
                      <span className="text-[10px] uppercase text-slate-400 font-bold block">Deadline</span>
                      <strong className="text-slate-900">
                        {drive.application_deadline ? drive.application_deadline.slice(0, 10) : 'Open'}
                      </strong>
                    </div>
                  </div>
                </div>

                {/* Skills tags */}
                {drive.eligibility_criteria?.required_skills && drive.eligibility_criteria.required_skills.length > 0 && (
                  <div className="flex flex-wrap gap-1.5 pt-1">
                    {drive.eligibility_criteria.required_skills.slice(0, 4).map((skill, i) => (
                      <span key={i} className="px-2 py-0.5 rounded-md bg-indigo-50/70 text-primary-700 text-[11px] font-medium">
                        {skill}
                      </span>
                    ))}
                    {drive.eligibility_criteria.required_skills.length > 4 && (
                      <span className="text-[11px] text-slate-400 self-center">
                        +{drive.eligibility_criteria.required_skills.length - 4} more
                      </span>
                    )}
                  </div>
                )}
              </div>

              <div className="mt-6 pt-4 border-t border-slate-100 flex items-center justify-between">
                <span className="text-xs text-slate-500">
                  Vacancies: <strong className="text-slate-800">{drive.vacancies} open</strong>
                </span>

                <Link
                  to={`/recruiter/drives/${drive.id}/applicants`}
                  className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-primary-600 hover:bg-primary-700 text-white text-xs font-semibold shadow-sm transition"
                >
                  <Users className="h-3.5 w-3.5" />
                  <span>Applicant ATS Pipeline</span>
                </Link>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

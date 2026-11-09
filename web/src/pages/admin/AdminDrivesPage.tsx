import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { adminApi } from '../../api/admin';
import { 
  Briefcase, 
  Search, 
  Check, 
  X, 
  Calendar, 
  MapPin, 
  DollarSign, 
  GraduationCap
} from 'lucide-react';

export const AdminDrivesPage: React.FC = () => {
  const queryClient = useQueryClient();
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');

  const { data: drives = [], isLoading } = useQuery({
    queryKey: ['admin-drives'],
    queryFn: () => adminApi.getDrives(),
  });

  const updateStatusMutation = useMutation({
    mutationFn: ({ driveId, status }: { driveId: string; status: string }) =>
      adminApi.updateDriveStatus(driveId, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-drives'] });
      queryClient.invalidateQueries({ queryKey: ['admin-stats'] });
    },
  });

  const filteredDrives = drives.filter((drive) => {
    const matchesSearch = 
      drive.job_title.toLowerCase().includes(search.toLowerCase()) ||
      drive.job_location.toLowerCase().includes(search.toLowerCase());
    
    if (statusFilter === 'ALL') return matchesSearch;
    return matchesSearch && drive.status === statusFilter;
  });

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-extrabold text-slate-900">Placement Drive Clearances</h1>
        <p className="text-sm text-slate-500 mt-0.5">
          Review, approve, and authorize recruitment drives submitted by corporate partners
        </p>
      </div>

      {/* Filter and Search Bar */}
      <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm flex flex-col sm:flex-row gap-4 items-center justify-between">
        <div className="relative w-full sm:w-80">
          <Search className="h-4 w-4 text-slate-400 absolute left-3.5 top-3" />
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search drives by role, location..."
            className="w-full pl-10 pr-4 py-2 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-amber-500 text-sm"
          />
        </div>

        <div className="flex items-center gap-1.5 w-full sm:w-auto overflow-x-auto pb-1 sm:pb-0">
          {['ALL', 'PENDING_APPROVAL', 'APPROVED', 'ACTIVE', 'COMPLETED'].map((status) => (
            <button
              key={status}
              onClick={() => setStatusFilter(status)}
              className={`px-3.5 py-1.5 rounded-xl text-xs font-semibold whitespace-nowrap transition ${
                statusFilter === status
                  ? 'bg-amber-600 text-white shadow-sm'
                  : 'bg-slate-100 hover:bg-slate-200 text-slate-600'
              }`}
            >
              {status.replace('_', ' ')}
            </button>
          ))}
        </div>
      </div>

      {/* Drives List */}
      {isLoading ? (
        <div className="p-16 text-center text-slate-400 text-sm">Loading placement drives...</div>
      ) : filteredDrives.length === 0 ? (
        <div className="bg-white p-16 rounded-2xl border border-slate-200 text-center">
          <Briefcase className="h-10 w-10 text-slate-300 mx-auto mb-3" />
          <h3 className="text-base font-semibold text-slate-800">No matching drives found</h3>
          <p className="text-sm text-slate-500 mt-1 max-w-sm mx-auto">
            Try adjusting your search query or status filter.
          </p>
        </div>
      ) : (
        <div className="space-y-4">
          {filteredDrives.map((drive) => (
            <div
              key={drive.id}
              className="bg-white rounded-2xl border border-slate-200 shadow-sm p-6 flex flex-col lg:flex-row lg:items-center justify-between gap-6"
            >
              <div className="space-y-3 flex-1">
                <div className="flex items-center gap-3">
                  <h3 className="text-lg font-bold text-slate-900">{drive.job_title}</h3>
                  <span
                    className={`px-2.5 py-0.5 rounded-full text-xs font-bold ${
                      drive.status === 'APPROVED' || drive.status === 'ACTIVE'
                        ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                        : drive.status === 'PENDING_APPROVAL'
                        ? 'bg-amber-50 text-amber-700 border border-amber-200'
                        : 'bg-slate-100 text-slate-600 border border-slate-200'
                    }`}
                  >
                    {drive.status}
                  </span>
                  <span className="px-2 py-0.5 rounded-full text-xs font-semibold bg-indigo-50 text-primary-700">
                    {drive.employment_type.replace('_', ' ')}
                  </span>
                </div>

                <p className="text-sm text-slate-600 line-clamp-2 leading-relaxed">
                  {drive.job_description}
                </p>

                <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-xs pt-1">
                  <div className="flex items-center gap-2 p-2 rounded-xl bg-slate-50">
                    <DollarSign className="h-4 w-4 text-emerald-600 shrink-0" />
                    <div>
                      <span className="text-[10px] uppercase text-slate-400 font-bold block">Package</span>
                      <strong className="text-slate-900">{drive.salary_package_ctc}</strong>
                    </div>
                  </div>

                  <div className="flex items-center gap-2 p-2 rounded-xl bg-slate-50">
                    <GraduationCap className="h-4 w-4 text-indigo-600 shrink-0" />
                    <div>
                      <span className="text-[10px] uppercase text-slate-400 font-bold block">Min CGPA</span>
                      <strong className="text-slate-900">{drive.eligibility_criteria?.min_cgpa ?? 6.0}</strong>
                    </div>
                  </div>

                  <div className="flex items-center gap-2 p-2 rounded-xl bg-slate-50">
                    <MapPin className="h-4 w-4 text-primary-600 shrink-0" />
                    <div>
                      <span className="text-[10px] uppercase text-slate-400 font-bold block">Location</span>
                      <strong className="text-slate-900 truncate block">{drive.job_location}</strong>
                    </div>
                  </div>

                  <div className="flex items-center gap-2 p-2 rounded-xl bg-slate-50">
                    <Calendar className="h-4 w-4 text-rose-500 shrink-0" />
                    <div>
                      <span className="text-[10px] uppercase text-slate-400 font-bold block">Deadline</span>
                      <strong className="text-slate-900">{drive.application_deadline?.slice(0, 10) || 'Open'}</strong>
                    </div>
                  </div>
                </div>

                {drive.eligibility_criteria?.required_skills && drive.eligibility_criteria.required_skills.length > 0 && (
                  <div className="flex flex-wrap gap-1.5 pt-1">
                    {drive.eligibility_criteria.required_skills.map((skill, i) => (
                      <span key={i} className="px-2 py-0.5 rounded-md bg-slate-100 text-slate-700 text-[11px] font-medium">
                        {skill}
                      </span>
                    ))}
                  </div>
                )}
              </div>

              {/* TPO Action Buttons */}
              <div className="flex lg:flex-col gap-2 shrink-0 border-t lg:border-t-0 lg:border-l border-slate-100 pt-4 lg:pt-0 lg:pl-6 justify-end">
                {drive.status === 'PENDING_APPROVAL' ? (
                  <>
                    <button
                      type="button"
                      disabled={updateStatusMutation.isPending}
                      onClick={() => updateStatusMutation.mutate({ driveId: drive.id, status: 'APPROVED' })}
                      className="inline-flex items-center justify-center gap-1.5 px-4 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs shadow-sm transition disabled:opacity-50"
                    >
                      <Check className="h-4 w-4" />
                      <span>Authorize Drive</span>
                    </button>
                    <button
                      type="button"
                      disabled={updateStatusMutation.isPending}
                      onClick={() => updateStatusMutation.mutate({ driveId: drive.id, status: 'CANCELLED' })}
                      className="inline-flex items-center justify-center gap-1.5 px-4 py-2 rounded-xl border border-rose-200 hover:bg-rose-50 text-rose-700 font-semibold text-xs transition disabled:opacity-50"
                    >
                      <X className="h-4 w-4" />
                      <span>Reject</span>
                    </button>
                  </>
                ) : drive.status === 'APPROVED' ? (
                  <>
                    <button
                      type="button"
                      disabled={updateStatusMutation.isPending}
                      onClick={() => updateStatusMutation.mutate({ driveId: drive.id, status: 'ACTIVE' })}
                      className="inline-flex items-center justify-center gap-1.5 px-4 py-2.5 rounded-xl bg-primary-600 hover:bg-primary-700 text-white font-bold text-xs shadow-sm transition"
                    >
                      <span>Set Active / Live</span>
                    </button>
                    <button
                      type="button"
                      disabled={updateStatusMutation.isPending}
                      onClick={() => updateStatusMutation.mutate({ driveId: drive.id, status: 'COMPLETED' })}
                      className="inline-flex items-center justify-center gap-1.5 px-4 py-2 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 font-semibold text-xs transition"
                    >
                      <span>Mark Completed</span>
                    </button>
                  </>
                ) : (
                  <span className="text-xs text-slate-400 font-semibold italic text-center py-2">
                    Drive Authorized
                  </span>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

import React, { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { recruiterApi } from '../../api/recruiter';
import { Application, ApplicationStatus, StudentProfile } from '../../types';
import { 
  ArrowLeft, 
  Search, 
  Download, 
  FileText, 
  ExternalLink,
  X,
  GraduationCap
} from 'lucide-react';

export const DriveApplicantsPage: React.FC = () => {
  const { driveId = '' } = useParams<{ driveId: string }>();
  const queryClient = useQueryClient();

  // Search & Filter State
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [cgpaFilter, setCgpaFilter] = useState<number>(0);
  const [backlogsFilter, setBacklogsFilter] = useState<string>('ALL');

  // Modal State
  const [selectedStudent, setSelectedStudent] = useState<StudentProfile | null>(null);

  // Fetch Drive Details
  const { data: drive } = useQuery({
    queryKey: ['drive', driveId],
    queryFn: () => recruiterApi.getDriveById(driveId),
    enabled: !!driveId,
  });

  // Fetch Applications for this drive
  const { data: applications = [], isLoading } = useQuery({
    queryKey: ['drive-applications', driveId],
    queryFn: () => recruiterApi.getDriveApplications(driveId),
    enabled: !!driveId,
  });

  // Fetch Departments
  const { data: departments = [] } = useQuery({
    queryKey: ['departments'],
    queryFn: () => recruiterApi.getDepartments(),
  });

  // Fetch student profiles for applicants (or enrich them)
  const { data: enrichedApplications = [] } = useQuery({
    queryKey: ['enriched-applications', applications],
    queryFn: async () => {
      const enriched: (Application & { studentProfile?: StudentProfile })[] = [];
      for (const app of applications) {
        try {
          const studentProfile = await recruiterApi.getStudentById(app.student_id);
          enriched.push({ ...app, studentProfile });
        } catch {
          enriched.push(app);
        }
      }
      return enriched;
    },
    enabled: applications.length > 0,
  });

  // Status Mutation
  const updateStatusMutation = useMutation({
    mutationFn: ({ appId, newStatus }: { appId: string; newStatus: ApplicationStatus }) =>
      recruiterApi.updateApplicationStatus(appId, newStatus),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['drive-applications', driveId] });
    },
  });

  const getDeptName = (deptId?: string) => {
    if (!deptId) return 'Engineering';
    const dept = departments.find(d => d.id === deptId);
    return dept ? dept.code : 'Engg';
  };

  // Filter logic
  const filteredList = enrichedApplications.filter((item) => {
    const student = item.studentProfile;
    const matchesSearch = 
      !search ||
      student?.full_name.toLowerCase().includes(search.toLowerCase()) ||
      student?.registration_number.toLowerCase().includes(search.toLowerCase()) ||
      student?.skills.some(s => s.toLowerCase().includes(search.toLowerCase()));

    const matchesStatus = statusFilter === 'ALL' || item.status === statusFilter;
    const matchesCgpa = !cgpaFilter || (student && student.current_cgpa >= cgpaFilter);
    const matchesBacklogs = 
      backlogsFilter === 'ALL' || 
      (backlogsFilter === 'ZERO' && student?.active_backlogs === 0) ||
      (backlogsFilter === 'MAX1' && (student?.active_backlogs ?? 0) <= 1);

    return matchesSearch && matchesStatus && matchesCgpa && matchesBacklogs;
  });

  const exportCSV = () => {
    const headers = ['Applicant ID', 'Full Name', 'Reg Number', 'Department', 'CGPA', 'Backlogs', 'Status', 'Phone', 'Applied Date'];
    const rows = filteredList.map(item => [
      item.id,
      item.studentProfile?.full_name || 'N/A',
      item.studentProfile?.registration_number || 'N/A',
      getDeptName(item.studentProfile?.department_id),
      item.studentProfile?.current_cgpa ?? 'N/A',
      item.studentProfile?.active_backlogs ?? 'N/A',
      item.status,
      item.studentProfile?.phone_number || 'N/A',
      item.applied_at?.slice(0, 10) || 'N/A'
    ]);

    const csvContent = 'data:text/csv;charset=utf-8,' + [headers, ...rows].map(e => e.join(',')).join('\n');
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement('a');
    link.setAttribute('href', encodedUri);
    link.setAttribute('download', `applicants_${drive?.job_title.replace(/\s+/g, '_')}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const getStatusBadge = (status: ApplicationStatus) => {
    switch (status) {
      case 'UNDER_REVIEW':
        return 'bg-blue-50 text-blue-700 border-blue-200';
      case 'SHORTLISTED':
        return 'bg-amber-50 text-amber-700 border-amber-200';
      case 'ASSESSMENT':
      case 'TECHNICAL_INTERVIEW':
      case 'HR_INTERVIEW':
        return 'bg-purple-50 text-purple-700 border-purple-200';
      case 'SELECTED':
        return 'bg-emerald-50 text-emerald-700 border-emerald-200';
      case 'REJECTED':
        return 'bg-rose-50 text-rose-700 border-rose-200';
      case 'WITHDRAWN':
        return 'bg-slate-100 text-slate-500 border-slate-200';
      default:
        return 'bg-indigo-50 text-primary-700 border-indigo-200';
    }
  };

  return (
    <div className="space-y-6">
      {/* Top Navigation & Drive Header */}
      <div>
        <Link
          to="/recruiter/drives"
          className="inline-flex items-center gap-1.5 text-xs font-semibold text-slate-500 hover:text-slate-800 transition mb-3"
        >
          <ArrowLeft className="h-4 w-4" />
          <span>Back to Drives</span>
        </Link>

        <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-3">
              <h1 className="text-2xl font-extrabold text-slate-900">{drive?.job_title || 'Placement Drive'}</h1>
              <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-primary-50 text-primary-700 border border-primary-200">
                {drive?.employment_type?.replace('_', ' ') || 'FULL TIME'}
              </span>
            </div>
            <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-slate-500 mt-2">
              <span>💰 CTC: <strong className="text-slate-800">{drive?.salary_package_ctc}</strong></span>
              <span>📍 {drive?.job_location}</span>
              <span>🎓 Required Min CGPA: <strong className="text-slate-800">{drive?.eligibility_criteria?.min_cgpa ?? 6.0}</strong></span>
              <span>👥 Headcount: {drive?.vacancies}</span>
            </div>
          </div>

          <button
            type="button"
            onClick={exportCSV}
            disabled={filteredList.length === 0}
            className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl border border-slate-200 hover:bg-slate-50 text-slate-700 font-semibold text-xs transition shrink-0 disabled:opacity-50"
          >
            <Download className="h-4 w-4" />
            <span>Export CSV</span>
          </button>
        </div>
      </div>

      {/* Filters Bar */}
      <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm space-y-4">
        <div className="flex flex-col sm:flex-row gap-3 items-center justify-between">
          <div className="relative w-full sm:w-80">
            <Search className="h-4 w-4 text-slate-400 absolute left-3.5 top-3" />
            <input
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Search by name, reg number, skill..."
              className="w-full pl-10 pr-4 py-2 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-primary-500 text-sm"
            />
          </div>

          <div className="flex flex-wrap items-center gap-2 w-full sm:w-auto">
            {/* CGPA Filter */}
            <select
              value={cgpaFilter}
              onChange={(e) => setCgpaFilter(parseFloat(e.target.value))}
              className="px-3 py-2 rounded-xl border border-slate-200 text-xs font-semibold text-slate-700 bg-white"
            >
              <option value={0}>All CGPA</option>
              <option value={9.0}>CGPA ≥ 9.0</option>
              <option value={8.5}>CGPA ≥ 8.5</option>
              <option value={8.0}>CGPA ≥ 8.0</option>
              <option value={7.5}>CGPA ≥ 7.5</option>
            </select>

            {/* Backlogs Filter */}
            <select
              value={backlogsFilter}
              onChange={(e) => setBacklogsFilter(e.target.value)}
              className="px-3 py-2 rounded-xl border border-slate-200 text-xs font-semibold text-slate-700 bg-white"
            >
              <option value="ALL">All Backlog Status</option>
              <option value="ZERO">Zero Backlogs Only</option>
              <option value="MAX1">Max 1 Backlog</option>
            </select>
          </div>
        </div>

        {/* Status Pill Tabs */}
        <div className="flex items-center gap-1.5 overflow-x-auto pb-1 pt-2 border-t border-slate-100">
          {[
            { id: 'ALL', label: 'All Candidates' },
            { id: 'APPLIED', label: 'Applied' },
            { id: 'SHORTLISTED', label: 'Shortlisted' },
            { id: 'INTERVIEW_SCHEDULED', label: 'Interviews' },
            { id: 'SELECTED', label: 'Selected / Offered' },
            { id: 'REJECTED', label: 'Rejected' },
          ].map((tab) => (
            <button
              key={tab.id}
              onClick={() => setStatusFilter(tab.id)}
              className={`px-3.5 py-1.5 rounded-xl text-xs font-semibold whitespace-nowrap transition ${
                statusFilter === tab.id
                  ? 'bg-primary-600 text-white shadow-sm'
                  : 'bg-slate-100 hover:bg-slate-200 text-slate-600'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>
      </div>

      {/* Applicants ATS Table */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
        {isLoading ? (
          <div className="p-16 text-center text-slate-400 text-sm">Loading applicants...</div>
        ) : filteredList.length === 0 ? (
          <div className="p-16 text-center">
            <GraduationCap className="h-10 w-10 text-slate-300 mx-auto mb-3" />
            <h3 className="text-base font-semibold text-slate-800">No applicants match this criteria</h3>
            <p className="text-sm text-slate-500 mt-1">
              {applications.length === 0
                ? 'No students have applied to this drive yet.'
                : 'Try clearing your search query or adjusting your CGPA/status filters.'}
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-slate-50/80 border-b border-slate-200 text-[11px] font-bold uppercase tracking-wider text-slate-500">
                  <th className="py-3.5 px-6">Student Applicant</th>
                  <th className="py-3.5 px-4">Department &amp; Batch</th>
                  <th className="py-3.5 px-4">Academic CGPA</th>
                  <th className="py-3.5 px-4">Backlogs</th>
                  <th className="py-3.5 px-4">Skills</th>
                  <th className="py-3.5 px-4">Pipeline Status</th>
                  <th className="py-3.5 px-6 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-sm">
                {filteredList.map((app) => {
                  const student = app.studentProfile;
                  return (
                    <tr key={app.id} className="hover:bg-slate-50/80 transition">
                      <td className="py-4 px-6">
                        <div className="font-bold text-slate-900">
                          {student?.full_name || 'Loading Student...'}
                        </div>
                        <div className="text-xs text-slate-400 font-medium">
                          {student?.registration_number || app.student_id.slice(0, 8)}
                        </div>
                      </td>

                      <td className="py-4 px-4 text-xs">
                        <div className="font-semibold text-slate-800">
                          {getDeptName(student?.department_id)}
                        </div>
                        <div className="text-slate-400">Class of {student?.graduation_year || 2026}</div>
                      </td>

                      <td className="py-4 px-4">
                        <span className="inline-flex items-center px-2.5 py-1 rounded-lg bg-emerald-50 text-emerald-800 font-extrabold text-xs border border-emerald-200">
                          {student ? student.current_cgpa.toFixed(2) : '-'}
                        </span>
                      </td>

                      <td className="py-4 px-4 text-xs font-semibold">
                        {student ? (
                          student.active_backlogs === 0 ? (
                            <span className="text-emerald-600">0</span>
                          ) : (
                            <span className="text-rose-600">{student.active_backlogs}</span>
                          )
                        ) : '-'}
                      </td>

                      <td className="py-4 px-4">
                        <div className="flex flex-wrap gap-1 max-w-xs">
                          {student?.skills.slice(0, 3).map((skill, i) => (
                            <span key={i} className="px-1.5 py-0.5 rounded bg-slate-100 text-slate-700 text-[10px] font-medium">
                              {skill}
                            </span>
                          ))}
                          {(student?.skills.length ?? 0) > 3 && (
                            <span className="text-[10px] text-slate-400 self-center">
                              +{(student?.skills.length ?? 0) - 3}
                            </span>
                          )}
                        </div>
                      </td>

                      <td className="py-4 px-4">
                        <select
                          value={app.status}
                          onChange={(e) =>
                            updateStatusMutation.mutate({
                              appId: app.id,
                              newStatus: e.target.value as ApplicationStatus,
                            })
                          }
                          className={`text-xs font-bold px-2.5 py-1 rounded-full border bg-white focus:outline-none focus:ring-2 focus:ring-primary-500 ${getStatusBadge(
                            app.status
                          )}`}
                        >
                          <option value="APPLIED">Applied</option>
                          <option value="UNDER_REVIEW">Under Review</option>
                          <option value="SHORTLISTED">Shortlisted</option>
                          <option value="ASSESSMENT">Online Assessment</option>
                          <option value="TECHNICAL_INTERVIEW">Technical Interview</option>
                          <option value="HR_INTERVIEW">HR Interview</option>
                          <option value="SELECTED">Selected / Offer</option>
                          <option value="REJECTED">Rejected</option>
                        </select>
                      </td>

                      <td className="py-4 px-6 text-right">
                        <button
                          type="button"
                          onClick={() => setSelectedStudent(student || null)}
                          className="px-3 py-1.5 rounded-lg border border-slate-200 hover:border-primary-400 hover:bg-primary-50 text-xs font-semibold text-slate-700 transition"
                        >
                          View Profile
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Student Profile Modal */}
      {selectedStudent && (
        <div className="fixed inset-0 z-50 bg-slate-900/50 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-lg w-full p-6 shadow-2xl space-y-6">
            <div className="flex items-start justify-between border-b border-slate-100 pb-4">
              <div>
                <h3 className="text-xl font-bold text-slate-900">{selectedStudent.full_name}</h3>
                <p className="text-xs text-primary-600 font-semibold">
                  {selectedStudent.degree} • Reg No: {selectedStudent.registration_number}
                </p>
              </div>
              <button
                type="button"
                onClick={() => setSelectedStudent(null)}
                className="p-1 rounded-lg text-slate-400 hover:text-slate-600 hover:bg-slate-100"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            <div className="grid grid-cols-2 gap-3 text-xs">
              <div className="p-3 rounded-xl bg-slate-50 border border-slate-100">
                <span className="text-slate-400 uppercase font-bold text-[10px] block">Verified CGPA</span>
                <strong className="text-base text-slate-900">{selectedStudent.current_cgpa.toFixed(2)}</strong>
              </div>
              <div className="p-3 rounded-xl bg-slate-50 border border-slate-100">
                <span className="text-slate-400 uppercase font-bold text-[10px] block">Active Backlogs</span>
                <strong className="text-base text-slate-900">{selectedStudent.active_backlogs}</strong>
              </div>
              <div className="p-3 rounded-xl bg-slate-50 border border-slate-100">
                <span className="text-slate-400 uppercase font-bold text-[10px] block">Phone Number</span>
                <strong className="text-slate-900">{selectedStudent.phone_number || 'Not provided'}</strong>
              </div>
              <div className="p-3 rounded-xl bg-slate-50 border border-slate-100">
                <span className="text-slate-400 uppercase font-bold text-[10px] block">Graduation Batch</span>
                <strong className="text-slate-900">Class of {selectedStudent.graduation_year}</strong>
              </div>
            </div>

            {selectedStudent.skills.length > 0 && (
              <div>
                <h4 className="text-xs font-bold uppercase tracking-wider text-slate-500 mb-2">Technical Skills</h4>
                <div className="flex flex-wrap gap-1.5">
                  {selectedStudent.skills.map((skill) => (
                    <span key={skill} className="px-2.5 py-1 rounded-lg bg-primary-50 text-primary-700 text-xs font-semibold">
                      {skill}
                    </span>
                  ))}
                </div>
              </div>
            )}

            {selectedStudent.resume_url && (
              <div className="p-4 rounded-xl bg-slate-50 border border-slate-200 flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <FileText className="h-5 w-5 text-primary-600" />
                  <span className="text-xs font-semibold text-slate-800">Student Resume (PDF)</span>
                </div>
                <a
                  href={selectedStudent.resume_url}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-flex items-center gap-1 text-xs font-bold text-primary-600 hover:text-primary-700"
                >
                  <span>Open Resume</span>
                  <ExternalLink className="h-3.5 w-3.5" />
                </a>
              </div>
            )}

            <div className="pt-2 flex justify-end">
              <button
                type="button"
                onClick={() => setSelectedStudent(null)}
                className="px-5 py-2.5 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 font-semibold text-xs"
              >
                Close Profile
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { adminApi, StudentAcademicUpdatePayload } from '../../api/admin';
import { StudentProfile } from '../../types';
import { 
  Search, 
  Download, 
  CheckCircle, 
  X, 
  Edit3, 
  GraduationCap
} from 'lucide-react';

export const AdminStudentsPage: React.FC = () => {
  const queryClient = useQueryClient();

  const [search, setSearch] = useState('');
  const [deptFilter, setDeptFilter] = useState<string>('ALL');
  const [verifiedFilter, setVerifiedFilter] = useState<string>('ALL');
  const [batchFilter, setBatchFilter] = useState<string>('ALL');

  // Edit Modal State
  const [editingStudent, setEditingStudent] = useState<StudentProfile | null>(null);
  const [editCgpa, setEditCgpa] = useState<number>(0);
  const [editBacklogs, setEditBacklogs] = useState<number>(0);
  const [editVerified, setEditVerified] = useState<boolean>(false);

  const { data: students = [], isLoading } = useQuery({
    queryKey: ['admin-students'],
    queryFn: () => adminApi.getStudents(),
  });

  const { data: departments = [] } = useQuery({
    queryKey: ['admin-departments'],
    queryFn: () => adminApi.getDepartments(),
  });

  const updateAcademicMutation = useMutation({
    mutationFn: ({
      studentId,
      payload,
    }: {
      studentId: string;
      payload: StudentAcademicUpdatePayload;
    }) => adminApi.updateStudentAcademic(studentId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-students'] });
      queryClient.invalidateQueries({ queryKey: ['admin-stats'] });
      setEditingStudent(null);
    },
  });

  const openEditModal = (student: StudentProfile) => {
    setEditingStudent(student);
    setEditCgpa(student.current_cgpa);
    setEditBacklogs(student.active_backlogs);
    setEditVerified(student.is_verified);
  };

  const handleSaveAcademic = (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingStudent) return;

    updateAcademicMutation.mutate({
      studentId: editingStudent.id,
      payload: {
        current_cgpa: editCgpa,
        active_backlogs: editBacklogs,
        is_verified: editVerified,
      },
    });
  };

  const getDeptCode = (deptId: string) => {
    const dept = departments.find(d => d.id === deptId);
    return dept ? dept.code : 'Engg';
  };

  const filteredStudents = students.filter((student) => {
    const matchesSearch =
      student.full_name.toLowerCase().includes(search.toLowerCase()) ||
      student.registration_number.toLowerCase().includes(search.toLowerCase());

    const matchesDept = deptFilter === 'ALL' || student.department_id === deptFilter;
    const matchesBatch = batchFilter === 'ALL' || student.graduation_year.toString() === batchFilter;
    const matchesVerified =
      verifiedFilter === 'ALL' ||
      (verifiedFilter === 'VERIFIED' && student.is_verified) ||
      (verifiedFilter === 'UNVERIFIED' && !student.is_verified);

    return matchesSearch && matchesDept && matchesBatch && matchesVerified;
  });

  const exportCSV = () => {
    const headers = ['Registration Number', 'Full Name', 'Department', 'Degree', 'Batch', 'CGPA', 'Backlogs', 'Verified', 'Phone', 'Skills'];
    const rows = filteredStudents.map(s => [
      s.registration_number,
      s.full_name,
      getDeptCode(s.department_id),
      s.degree,
      s.graduation_year,
      s.current_cgpa,
      s.active_backlogs,
      s.is_verified ? 'YES' : 'NO',
      s.phone_number || 'N/A',
      `"${s.skills.join(', ')}"`
    ]);

    const csvContent = 'data:text/csv;charset=utf-8,' + [headers, ...rows].map(e => e.join(',')).join('\n');
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement('a');
    link.setAttribute('href', encodedUri);
    link.setAttribute('download', `student_registry_apex_${new Date().toISOString().slice(0, 10)}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900">University Student Registry</h1>
          <p className="text-sm text-slate-500 mt-0.5">
            Official academic records, CGPA verification, and placement readiness desk
          </p>
        </div>

        <button
          type="button"
          onClick={exportCSV}
          disabled={filteredStudents.length === 0}
          className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl border border-slate-200 hover:bg-slate-50 text-slate-700 font-semibold text-xs transition shrink-0 disabled:opacity-50"
        >
          <Download className="h-4 w-4" />
          <span>Export Registry (CSV)</span>
        </button>
      </div>

      {/* Filter and Search Bar */}
      <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm space-y-3">
        <div className="flex flex-col sm:flex-row gap-3 items-center justify-between">
          <div className="relative w-full sm:w-80">
            <Search className="h-4 w-4 text-slate-400 absolute left-3.5 top-3" />
            <input
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Search by name, reg number..."
              className="w-full pl-10 pr-4 py-2 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-amber-500 text-sm"
            />
          </div>

          <div className="flex flex-wrap items-center gap-2 w-full sm:w-auto">
            {/* Department Filter */}
            <select
              value={deptFilter}
              onChange={(e) => setDeptFilter(e.target.value)}
              className="px-3 py-2 rounded-xl border border-slate-200 text-xs font-semibold text-slate-700 bg-white"
            >
              <option value="ALL">All Departments</option>
              {departments.map((d) => (
                <option key={d.id} value={d.id}>
                  {d.name}
                </option>
              ))}
            </select>

            {/* Batch Filter */}
            <select
              value={batchFilter}
              onChange={(e) => setBatchFilter(e.target.value)}
              className="px-3 py-2 rounded-xl border border-slate-200 text-xs font-semibold text-slate-700 bg-white"
            >
              <option value="ALL">All Batches</option>
              <option value="2025">Class of 2025</option>
              <option value="2026">Class of 2026</option>
              <option value="2027">Class of 2027</option>
            </select>

            {/* Verified Filter */}
            <select
              value={verifiedFilter}
              onChange={(e) => setVerifiedFilter(e.target.value)}
              className="px-3 py-2 rounded-xl border border-slate-200 text-xs font-semibold text-slate-700 bg-white"
            >
              <option value="ALL">All Verification Status</option>
              <option value="VERIFIED">Verified Records Only</option>
              <option value="UNVERIFIED">Pending Verification</option>
            </select>
          </div>
        </div>
      </div>

      {/* Students Table */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
        {isLoading ? (
          <div className="p-16 text-center text-slate-400 text-sm">Loading student registry...</div>
        ) : filteredStudents.length === 0 ? (
          <div className="p-16 text-center">
            <GraduationCap className="h-10 w-10 text-slate-300 mx-auto mb-3" />
            <h3 className="text-base font-semibold text-slate-800">No students found</h3>
            <p className="text-sm text-slate-500 mt-1">Try clearing your search query or filters.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-slate-50/80 border-b border-slate-200 text-[11px] font-bold uppercase tracking-wider text-slate-500">
                  <th className="py-3.5 px-6">Reg Number</th>
                  <th className="py-3.5 px-4">Student Name</th>
                  <th className="py-3.5 px-4">Branch</th>
                  <th className="py-3.5 px-4">Verified CGPA</th>
                  <th className="py-3.5 px-4">Backlogs</th>
                  <th className="py-3.5 px-4">Status</th>
                  <th className="py-3.5 px-6 text-right">Academic Control</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-sm">
                {filteredStudents.map((student) => (
                  <tr key={student.id} className="hover:bg-slate-50/80 transition">
                    <td className="py-4 px-6 font-bold text-slate-900">{student.registration_number}</td>
                    <td className="py-4 px-4 font-semibold text-slate-800">{student.full_name}</td>
                    <td className="py-4 px-4 text-xs font-semibold text-slate-600">
                      {getDeptCode(student.department_id)} ({student.graduation_year})
                    </td>
                    <td className="py-4 px-4">
                      <span className="inline-flex items-center px-2.5 py-1 rounded-lg bg-emerald-50 text-emerald-800 font-extrabold text-xs border border-emerald-200">
                        {student.current_cgpa.toFixed(2)}
                      </span>
                    </td>
                    <td className="py-4 px-4 text-xs font-semibold">
                      {student.active_backlogs === 0 ? (
                        <span className="text-emerald-600 font-bold">0</span>
                      ) : (
                        <span className="text-rose-600 font-bold">{student.active_backlogs}</span>
                      )}
                    </td>
                    <td className="py-4 px-4">
                      <span
                        className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-bold ${
                          student.is_verified
                            ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                            : 'bg-amber-50 text-amber-700 border border-amber-200'
                        }`}
                      >
                        {student.is_verified && <CheckCircle className="h-3 w-3" />}
                        <span>{student.is_verified ? 'Verified' : 'Pending'}</span>
                      </span>
                    </td>
                    <td className="py-4 px-6 text-right">
                      <button
                        type="button"
                        onClick={() => openEditModal(student)}
                        className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-slate-200 hover:border-amber-400 hover:bg-amber-50 text-xs font-semibold text-slate-700 transition"
                      >
                        <Edit3 className="h-3.5 w-3.5 text-amber-600" />
                        <span>Edit Academic</span>
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Edit Academic Modal */}
      {editingStudent && (
        <div className="fixed inset-0 z-50 bg-slate-900/50 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 shadow-2xl space-y-6">
            <div className="flex items-start justify-between border-b border-slate-100 pb-3">
              <div>
                <h3 className="text-lg font-bold text-slate-900">Modify Academic Record</h3>
                <p className="text-xs text-amber-600 font-semibold">
                  {editingStudent.full_name} ({editingStudent.registration_number})
                </p>
              </div>
              <button
                type="button"
                onClick={() => setEditingStudent(null)}
                className="p-1 rounded-lg text-slate-400 hover:text-slate-600 hover:bg-slate-100"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            <form onSubmit={handleSaveAcademic} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold uppercase tracking-wider text-slate-700 mb-1.5">
                  Verified Cumulative CGPA (out of 10.0)
                </label>
                <input
                  type="number"
                  step="0.01"
                  min="0"
                  max="10"
                  required
                  value={editCgpa}
                  onChange={(e) => setEditCgpa(parseFloat(e.target.value) || 0)}
                  className="w-full px-4 py-2 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-amber-500 text-sm font-bold"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold uppercase tracking-wider text-slate-700 mb-1.5">
                  Active Backlogs Count
                </label>
                <input
                  type="number"
                  min="0"
                  max="20"
                  required
                  value={editBacklogs}
                  onChange={(e) => setEditBacklogs(parseInt(e.target.value) || 0)}
                  className="w-full px-4 py-2 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-amber-500 text-sm font-bold"
                />
              </div>

              <div className="pt-2">
                <label className="flex items-center gap-3 p-3 rounded-xl border border-slate-200 bg-slate-50 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={editVerified}
                    onChange={(e) => setEditVerified(e.target.checked)}
                    className="h-4 w-4 rounded text-amber-600 focus:ring-amber-500"
                  />
                  <div className="text-xs">
                    <span className="font-bold text-slate-900 block">Mark as Verified Student</span>
                    <span className="text-slate-500">Student satisfies all university academic integrity guidelines.</span>
                  </div>
                </label>
              </div>

              <div className="pt-4 border-t border-slate-100 flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setEditingStudent(null)}
                  className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-600 hover:bg-slate-100"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={updateAcademicMutation.isPending}
                  className="px-5 py-2 rounded-xl bg-amber-600 hover:bg-amber-700 text-white text-xs font-bold transition shadow-sm disabled:opacity-50"
                >
                  {updateAcademicMutation.isPending ? 'Saving...' : 'Save Academic Record'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

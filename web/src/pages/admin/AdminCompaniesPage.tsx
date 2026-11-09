import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { adminApi } from '../../api/admin';
import { 
  Building2, 
  Users, 
  CheckCircle, 
  XCircle, 
  Globe, 
  Search
} from 'lucide-react';

export const AdminCompaniesPage: React.FC = () => {
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState<'COMPANIES' | 'RECRUITERS'>('COMPANIES');
  const [search, setSearch] = useState('');

  const { data: companies = [], isLoading: companiesLoading } = useQuery({
    queryKey: ['admin-companies'],
    queryFn: () => adminApi.getCompanies(),
  });

  const { data: recruiters = [], isLoading: recruitersLoading } = useQuery({
    queryKey: ['admin-recruiters'],
    queryFn: () => adminApi.getRecruiters(),
  });

  const verifyCompanyMutation = useMutation({
    mutationFn: ({ companyId, status }: { companyId: string; status: 'VERIFIED' | 'REJECTED' | 'PENDING' }) =>
      adminApi.verifyCompany(companyId, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-companies'] });
      queryClient.invalidateQueries({ queryKey: ['admin-stats'] });
    },
  });

  const approveRecruiterMutation = useMutation({
    mutationFn: ({ recruiterId, isApproved }: { recruiterId: string; isApproved: boolean }) =>
      adminApi.approveRecruiter(recruiterId, isApproved),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-recruiters'] });
    },
  });

  const filteredCompanies = companies.filter(c =>
    c.name.toLowerCase().includes(search.toLowerCase()) ||
    (c.industry && c.industry.toLowerCase().includes(search.toLowerCase()))
  );

  const filteredRecruiters = recruiters.filter(r =>
    r.full_name.toLowerCase().includes(search.toLowerCase()) ||
    (r.designation && r.designation.toLowerCase().includes(search.toLowerCase()))
  );

  const getCompanyName = (companyId: string) => {
    const c = companies.find(comp => comp.id === companyId);
    return c ? c.name : 'Corporate Partner';
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-extrabold text-slate-900">Corporate Partnerships &amp; Recruiters</h1>
        <p className="text-sm text-slate-500 mt-0.5">
          Verify corporate partners and approve recruiter accounts authorized to conduct campus recruitment
        </p>
      </div>

      {/* Tabs & Search */}
      <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm flex flex-col sm:flex-row gap-4 items-center justify-between">
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => setActiveTab('COMPANIES')}
            className={`inline-flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-bold transition ${
              activeTab === 'COMPANIES'
                ? 'bg-amber-600 text-white shadow-sm'
                : 'bg-slate-100 hover:bg-slate-200 text-slate-700'
            }`}
          >
            <Building2 className="h-4 w-4" />
            <span>Companies ({companies.length})</span>
          </button>

          <button
            type="button"
            onClick={() => setActiveTab('RECRUITERS')}
            className={`inline-flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-bold transition ${
              activeTab === 'RECRUITERS'
                ? 'bg-amber-600 text-white shadow-sm'
                : 'bg-slate-100 hover:bg-slate-200 text-slate-700'
            }`}
          >
            <Users className="h-4 w-4" />
            <span>Recruiters ({recruiters.length})</span>
          </button>
        </div>

        <div className="relative w-full sm:w-72">
          <Search className="h-4 w-4 text-slate-400 absolute left-3.5 top-3" />
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder={activeTab === 'COMPANIES' ? 'Search company...' : 'Search recruiter...'}
            className="w-full pl-10 pr-4 py-2 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-amber-500 text-sm"
          />
        </div>
      </div>

      {/* Content */}
      {activeTab === 'COMPANIES' ? (
        companiesLoading ? (
          <div className="p-16 text-center text-slate-400 text-sm">Loading companies...</div>
        ) : filteredCompanies.length === 0 ? (
          <div className="bg-white p-16 rounded-2xl border border-slate-200 text-center text-slate-400 text-sm">
            No companies found.
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {filteredCompanies.map((company) => (
              <div
                key={company.id}
                className="bg-white rounded-2xl border border-slate-200 shadow-sm p-6 flex flex-col justify-between space-y-4"
              >
                <div>
                  <div className="flex items-start justify-between gap-3">
                    <div className="flex items-center gap-3">
                      <div className="h-12 w-12 rounded-xl bg-gradient-to-tr from-amber-600 to-indigo-600 text-white font-extrabold flex items-center justify-center text-lg shrink-0">
                        {company.name.charAt(0)}
                      </div>
                      <div>
                        <h3 className="text-base font-bold text-slate-900">{company.name}</h3>
                        <p className="text-xs text-slate-500">{company.industry || 'Technology Partner'}</p>
                      </div>
                    </div>

                    <span
                      className={`px-2.5 py-0.5 rounded-full text-xs font-bold shrink-0 ${
                        company.is_verified
                          ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                          : 'bg-amber-50 text-amber-700 border border-amber-200'
                      }`}
                    >
                      {company.is_verified ? 'Verified' : 'Pending Verification'}
                    </span>
                  </div>

                  <p className="text-xs text-slate-600 mt-3 line-clamp-2 leading-relaxed">
                    {company.description || 'Corporate recruitment partner at university campus.'}
                  </p>

                  {company.website && (
                    <a
                      href={company.website}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="inline-flex items-center gap-1.5 text-xs text-amber-600 hover:text-amber-700 font-semibold mt-3"
                    >
                      <Globe className="h-3.5 w-3.5" />
                      <span>{company.website}</span>
                    </a>
                  )}
                </div>

                <div className="pt-4 border-t border-slate-100 flex items-center justify-end gap-2">
                  {!company.is_verified ? (
                    <button
                      type="button"
                      disabled={verifyCompanyMutation.isPending}
                      onClick={() =>
                        verifyCompanyMutation.mutate({ companyId: company.id, status: 'VERIFIED' })
                      }
                      className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold transition disabled:opacity-50"
                    >
                      <CheckCircle className="h-4 w-4" />
                      <span>Verify Partner</span>
                    </button>
                  ) : (
                    <button
                      type="button"
                      disabled={verifyCompanyMutation.isPending}
                      onClick={() =>
                        verifyCompanyMutation.mutate({ companyId: company.id, status: 'PENDING' })
                      }
                      className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl border border-slate-200 hover:bg-slate-50 text-slate-600 text-xs font-semibold transition disabled:opacity-50"
                    >
                      <span>Revoke Verification</span>
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        )
      ) : recruitersLoading ? (
        <div className="p-16 text-center text-slate-400 text-sm">Loading recruiters...</div>
      ) : filteredRecruiters.length === 0 ? (
        <div className="bg-white p-16 rounded-2xl border border-slate-200 text-center text-slate-400 text-sm">
          No recruiters found.
        </div>
      ) : (
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-slate-50/80 border-b border-slate-200 text-[11px] font-bold uppercase tracking-wider text-slate-500">
                <th className="py-3.5 px-6">Recruiter Name</th>
                <th className="py-3.5 px-4">Company Partner</th>
                <th className="py-3.5 px-4">Designation</th>
                <th className="py-3.5 px-4">Contact Phone</th>
                <th className="py-3.5 px-4">Status</th>
                <th className="py-3.5 px-6 text-right">Approval Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 text-sm">
              {filteredRecruiters.map((recruiter) => (
                <tr key={recruiter.id} className="hover:bg-slate-50/80 transition">
                  <td className="py-4 px-6 font-bold text-slate-900">{recruiter.full_name}</td>
                  <td className="py-4 px-4 font-semibold text-slate-700">
                    {getCompanyName(recruiter.company_id)}
                  </td>
                  <td className="py-4 px-4 text-xs text-slate-600">
                    {recruiter.designation || 'Talent Acquisition'}
                  </td>
                  <td className="py-4 px-4 text-xs text-slate-600">
                    {recruiter.phone_number || 'N/A'}
                  </td>
                  <td className="py-4 px-4">
                    <span
                      className={`px-2.5 py-0.5 rounded-full text-xs font-bold ${
                        recruiter.is_approved
                          ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                          : 'bg-amber-50 text-amber-700 border border-amber-200'
                      }`}
                    >
                      {recruiter.is_approved ? 'Approved' : 'Pending Approval'}
                    </span>
                  </td>
                  <td className="py-4 px-6 text-right">
                    <button
                      type="button"
                      disabled={approveRecruiterMutation.isPending}
                      onClick={() =>
                        approveRecruiterMutation.mutate({
                          recruiterId: recruiter.id,
                          isApproved: !recruiter.is_approved,
                        })
                      }
                      className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-bold transition ${
                        recruiter.is_approved
                          ? 'border border-rose-200 text-rose-700 hover:bg-rose-50'
                          : 'bg-emerald-600 text-white hover:bg-emerald-700'
                      }`}
                    >
                      {recruiter.is_approved ? (
                        <>
                          <XCircle className="h-3.5 w-3.5" />
                          <span>Revoke</span>
                        </>
                      ) : (
                        <>
                          <CheckCircle className="h-3.5 w-3.5" />
                          <span>Approve Access</span>
                        </>
                      )}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};

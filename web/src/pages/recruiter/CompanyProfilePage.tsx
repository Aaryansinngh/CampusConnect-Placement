import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { useAuth } from '../../context/AuthContext';
import { recruiterApi } from '../../api/recruiter';
import { Globe, Mail, Phone, ShieldCheck, CheckCircle2 } from 'lucide-react';

export const CompanyProfilePage: React.FC = () => {
  const { user } = useAuth();
  const companyId = user?.recruiter?.company_id;

  const { data: company, isLoading } = useQuery({
    queryKey: ['company-profile', companyId],
    queryFn: () => recruiterApi.getCompanyById(companyId!),
    enabled: !!companyId,
  });

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      <div>
        <h1 className="text-2xl font-extrabold text-slate-900">Company &amp; Recruiter Profile</h1>
        <p className="text-sm text-slate-500 mt-0.5">
          Corporate partnership profile verified by the University Training &amp; Placement Cell
        </p>
      </div>

      {isLoading ? (
        <div className="p-12 text-center text-slate-400 text-sm">Loading profile...</div>
      ) : (
        <div className="space-y-6">
          {/* Company Card */}
          <div className="bg-white rounded-2xl border border-slate-200 p-8 shadow-sm space-y-6">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-100 pb-6">
              <div className="flex items-center gap-4">
                <div className="h-16 w-16 rounded-2xl bg-gradient-to-tr from-primary-600 to-indigo-600 flex items-center justify-center text-white text-2xl font-black shadow-md shadow-primary-500/20">
                  {company?.name.charAt(0) || 'C'}
                </div>
                <div>
                  <div className="flex items-center gap-2">
                    <h2 className="text-2xl font-bold text-slate-900">{company?.name || 'Recruitment Partner'}</h2>
                    <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">
                      <CheckCircle2 className="h-3 w-3" />
                      <span>Verified Partner</span>
                    </span>
                  </div>
                  <p className="text-xs text-slate-500 mt-0.5">{company?.industry || 'Technology & Software'}</p>
                </div>
              </div>

              {company?.website && (
                <a
                  href={company.website}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl bg-slate-50 hover:bg-slate-100 text-slate-700 text-xs font-semibold border border-slate-200 transition"
                >
                  <Globe className="h-4 w-4 text-primary-600" />
                  <span>{company.website.replace('https://', '').replace('http://', '')}</span>
                </a>
              )}
            </div>

            <div>
              <h3 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-2">Company Overview</h3>
              <p className="text-sm text-slate-600 leading-relaxed">
                {company?.description || 'Leading technology and recruitment enterprise actively participating in university campus placement drives.'}
              </p>
            </div>
          </div>

          {/* Recruiter Representative Card */}
          <div className="bg-white rounded-2xl border border-slate-200 p-8 shadow-sm space-y-6">
            <h3 className="text-lg font-bold text-slate-900 border-b border-slate-100 pb-3">
              Official Campus Recruiter Representative
            </h3>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
              <div className="p-4 rounded-xl bg-slate-50 border border-slate-100">
                <span className="text-slate-400 uppercase font-bold text-[10px] block">Full Name</span>
                <strong className="text-sm text-slate-900 mt-0.5 block">{user?.recruiter?.full_name || 'Recruiter'}</strong>
              </div>

              <div className="p-4 rounded-xl bg-slate-50 border border-slate-100">
                <span className="text-slate-400 uppercase font-bold text-[10px] block">Designation</span>
                <strong className="text-sm text-slate-900 mt-0.5 block">{user?.recruiter?.designation || 'Campus Lead'}</strong>
              </div>

              <div className="p-4 rounded-xl bg-slate-50 border border-slate-100 flex items-center gap-3">
                <Mail className="h-5 w-5 text-primary-600 shrink-0" />
                <div>
                  <span className="text-slate-400 uppercase font-bold text-[10px] block">Work Email</span>
                  <strong className="text-sm text-slate-900">{user?.email}</strong>
                </div>
              </div>

              <div className="p-4 rounded-xl bg-slate-50 border border-slate-100 flex items-center gap-3">
                <Phone className="h-5 w-5 text-emerald-600 shrink-0" />
                <div>
                  <span className="text-slate-400 uppercase font-bold text-[10px] block">Contact Phone</span>
                  <strong className="text-sm text-slate-900">{user?.recruiter?.phone_number || '+91 9876543210'}</strong>
                </div>
              </div>
            </div>

            <div className="p-4 rounded-xl bg-emerald-50/70 border border-emerald-200 flex items-center gap-3 text-xs text-emerald-800">
              <ShieldCheck className="h-5 w-5 text-emerald-600 shrink-0" />
              <span>
                Your corporate account has full authorization from the University TPO to publish placement drives and conduct selection rounds.
              </span>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

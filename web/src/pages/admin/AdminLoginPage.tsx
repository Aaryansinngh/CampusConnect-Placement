import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { ShieldCheck, Lock, Mail, ArrowRight, AlertCircle, CheckCircle2 } from 'lucide-react';

export const AdminLoginPage: React.FC = () => {
  const navigate = useNavigate();
  const { login } = useAuth();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);

    try {
      await login(email, password);
      navigate('/admin/dashboard');
    } catch (err: any) {
      setError(err.response?.data?.detail || 'Authentication failed. Please verify administrative credentials.');
    } finally {
      setLoading(false);
    }
  };

  const setDemoAccount = (demoEmail: string, demoPass: string) => {
    setEmail(demoEmail);
    setPassword(demoPass);
    setError(null);
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-950 via-slate-900 to-amber-950 flex flex-col justify-center items-center p-6">
      <div className="w-full max-w-md">
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center h-14 w-14 rounded-2xl bg-amber-600 text-white shadow-lg shadow-amber-600/30 mb-4">
            <ShieldCheck className="h-8 w-8" />
          </div>
          <h1 className="text-2xl font-extrabold text-white">CampusConnect TPO Portal</h1>
          <p className="text-sm text-slate-400 mt-1">
            Training &amp; Placement Office Management &amp; Analytics Console
          </p>
        </div>

        <div className="bg-white rounded-3xl shadow-2xl p-8 border border-slate-100">
          <h2 className="text-xl font-bold text-slate-900 mb-6">University Officer Sign In</h2>

          {error && (
            <div className="mb-6 p-4 rounded-xl bg-rose-50 border border-rose-200 flex items-start gap-3 text-rose-700 text-sm">
              <AlertCircle className="h-5 w-5 shrink-0 mt-0.5" />
              <span>{error}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-700 mb-1.5">
                University Email Address
              </label>
              <div className="relative">
                <Mail className="h-5 w-5 text-slate-400 absolute left-3.5 top-3" />
                <input
                  type="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="tpo@university.edu"
                  className="w-full pl-11 pr-4 py-2.5 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-amber-500 text-sm"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-700 mb-1.5">
                Password
              </label>
              <div className="relative">
                <Lock className="h-5 w-5 text-slate-400 absolute left-3.5 top-3" />
                <input
                  type="password"
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••••••"
                  className="w-full pl-11 pr-4 py-2.5 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-amber-500 text-sm"
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full mt-2 py-3 px-4 bg-amber-600 hover:bg-amber-700 text-white font-semibold rounded-xl transition duration-150 flex items-center justify-center gap-2 shadow-lg shadow-amber-600/25 disabled:opacity-70"
            >
              <span>{loading ? 'Authenticating...' : 'Sign In as TPO Admin'}</span>
              <ArrowRight className="h-4 w-4" />
            </button>
          </form>

          {/* Quick Demo Credentials */}
          <div className="mt-8 pt-6 border-t border-slate-100">
            <p className="text-xs font-semibold uppercase tracking-wider text-slate-500 mb-3 text-center">
              Quick Demo Administrative Accounts
            </p>
            <div className="grid grid-cols-2 gap-2">
              <button
                type="button"
                onClick={() => setDemoAccount('tpo@apex.edu', 'TpoPassword123!')}
                className="py-2.5 px-3 text-xs font-semibold rounded-xl border border-slate-200 hover:border-amber-400 hover:bg-amber-50 text-slate-700 transition text-center"
              >
                TPO Admin (Apex)
              </button>
              <button
                type="button"
                onClick={() => setDemoAccount('superadmin@campusconnect.edu', 'AdminSecure2026!')}
                className="py-2.5 px-3 text-xs font-semibold rounded-xl border border-slate-200 hover:border-amber-400 hover:bg-amber-50 text-slate-700 transition text-center"
              >
                Super Admin
              </button>
            </div>
          </div>
        </div>

        <div className="flex items-center justify-center gap-2 mt-6 text-xs text-slate-400">
          <CheckCircle2 className="h-4 w-4 text-emerald-400" />
          <span>Restricted to Authorized University Officers Only</span>
        </div>
      </div>
    </div>
  );
};

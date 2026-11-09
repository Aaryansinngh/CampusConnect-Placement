import React from 'react';
import { useAuth } from '../../context/AuthContext';
import { ShieldCheck, LogOut, UserCircle } from 'lucide-react';

export const AdminNavbar: React.FC = () => {
  const { user, logout } = useAuth();

  return (
    <header className="h-16 bg-white border-b border-slate-200 sticky top-0 z-30 flex items-center justify-between px-6">
      <div className="flex items-center gap-3">
        <div className="h-9 w-9 rounded-xl bg-gradient-to-tr from-amber-600 to-orange-500 flex items-center justify-center text-white font-bold shadow-md shadow-amber-500/20">
          <ShieldCheck className="h-5 w-5" />
        </div>
        <div>
          <h1 className="text-base font-bold text-slate-900 leading-tight">CampusConnect</h1>
          <p className="text-xs text-amber-600 font-semibold tracking-wide uppercase">TPO Placement Cell</p>
        </div>
      </div>

      <div className="flex items-center gap-4">
        <div className="flex items-center gap-3 pl-4 border-l border-slate-200">
          <div className="h-9 w-9 rounded-full bg-amber-50 border border-amber-200 flex items-center justify-center text-amber-700">
            <UserCircle className="h-5 w-5" />
          </div>
          <div className="text-right hidden sm:block">
            <div className="text-sm font-semibold text-slate-800">
              {user?.email?.split('@')[0]}
            </div>
            <div className="text-xs text-amber-600 font-medium">
              {user?.role === 'SUPER_ADMIN' ? 'University Super Admin' : 'Training & Placement Officer'}
            </div>
          </div>
        </div>

        <button
          onClick={logout}
          title="Logout"
          className="p-2 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition-colors"
        >
          <LogOut className="h-5 w-5" />
        </button>
      </div>
    </header>
  );
};

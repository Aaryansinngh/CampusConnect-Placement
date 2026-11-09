import React from 'react';
import { NavLink } from 'react-router-dom';
import { 
  LayoutDashboard, 
  Briefcase, 
  Building2, 
  Users, 
  Bell,
  GraduationCap
} from 'lucide-react';

export const AdminSidebar: React.FC = () => {
  const navItems = [
    { label: 'Executive Analytics', to: '/admin/dashboard', icon: LayoutDashboard },
    { label: 'Drive Clearances', to: '/admin/drives', icon: Briefcase },
    { label: 'Corporate Partners', to: '/admin/companies', icon: Building2 },
    { label: 'Student Registry', to: '/admin/students', icon: Users },
    { label: 'Announcements', to: '/admin/announcements', icon: Bell },
  ];

  return (
    <aside className="w-64 bg-slate-900 text-slate-300 min-h-[calc(100vh-4rem)] flex flex-col justify-between p-4">
      <div className="space-y-1">
        <div className="px-3 py-2 text-xs font-semibold uppercase tracking-wider text-slate-400">
          Placement Administration
        </div>
        {navItems.map((item) => {
          const Icon = item.icon;
          return (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                `flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-sm font-medium transition-all ${
                  isActive
                    ? 'bg-amber-600 text-white shadow-md shadow-amber-900/40'
                    : 'text-slate-300 hover:bg-slate-800 hover:text-white'
                }`
              }
            >
              <Icon className="h-4 w-4" />
              <span>{item.label}</span>
            </NavLink>
          );
        })}
      </div>

      <div className="p-4 bg-slate-800/60 border border-slate-700/50 rounded-2xl">
        <div className="flex items-center gap-2 text-amber-400 text-xs font-bold uppercase tracking-wider">
          <GraduationCap className="h-4 w-4" />
          <span>University Portal</span>
        </div>
        <p className="mt-1 text-sm font-semibold text-white">Apex Institute of Tech</p>
        <p className="text-xs text-slate-400">TPO Official Clearance Desk</p>
      </div>
    </aside>
  );
};

import React from 'react';
import { Outlet } from 'react-router-dom';
import { AdminNavbar } from './AdminNavbar';
import { AdminSidebar } from './AdminSidebar';

export const AdminLayout: React.FC = () => {
  return (
    <div className="min-h-screen bg-slate-50 flex flex-col">
      <AdminNavbar />
      <div className="flex flex-1">
        <AdminSidebar />
        <main className="flex-1 p-8 overflow-y-auto max-w-7xl">
          <Outlet />
        </main>
      </div>
    </div>
  );
};

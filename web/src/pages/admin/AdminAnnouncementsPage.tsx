import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { adminApi } from '../../api/admin';
import { 
  Bell, 
  Send, 
  AlertCircle, 
  CheckCircle2, 
  Megaphone, 
  Calendar,
  Users
} from 'lucide-react';

export const AdminAnnouncementsPage: React.FC = () => {
  const queryClient = useQueryClient();

  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [targetBatches, setTargetBatches] = useState<number[]>([2026]);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const { data: announcements = [], isLoading } = useQuery({
    queryKey: ['admin-announcements'],
    queryFn: () => adminApi.getAnnouncements(),
  });

  const { data: departments = [] } = useQuery({
    queryKey: ['admin-departments'],
    queryFn: () => adminApi.getDepartments(),
  });

  const createMutation = useMutation({
    mutationFn: adminApi.createAnnouncement,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-announcements'] });
      setTitle('');
      setContent('');
      setSuccessMsg('Announcement successfully published to university students!');
      setErrorMsg(null);
      setTimeout(() => setSuccessMsg(null), 5000);
    },
    onError: (err: any) => {
      setErrorMsg(err.response?.data?.detail || 'Failed to broadcast announcement.');
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim() || !content.trim()) return;

    createMutation.mutate({
      title: title.trim(),
      content: content.trim(),
      target_departments: departments.map(d => d.id),
      target_batches: targetBatches,
    });
  };

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-extrabold text-slate-900">University Placement Circulars</h1>
        <p className="text-sm text-slate-500 mt-0.5">
          Broadcast official placement cell notifications, interview schedules, and guidelines to enrolled students
        </p>
      </div>

      {successMsg && (
        <div className="p-4 rounded-xl bg-emerald-50 border border-emerald-200 flex items-center gap-3 text-emerald-800 text-sm">
          <CheckCircle2 className="h-5 w-5 shrink-0" />
          <span>{successMsg}</span>
        </div>
      )}

      {errorMsg && (
        <div className="p-4 rounded-xl bg-rose-50 border border-rose-200 flex items-center gap-3 text-rose-800 text-sm">
          <AlertCircle className="h-5 w-5 shrink-0" />
          <span>{errorMsg}</span>
        </div>
      )}

      {/* Broadcast Form Card */}
      <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm space-y-4">
        <div className="flex items-center gap-2 border-b border-slate-100 pb-3">
          <Megaphone className="h-5 w-5 text-amber-600" />
          <h2 className="text-base font-bold text-slate-900">Broadcast Placement Announcement</h2>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-700 mb-1.5">
              Announcement Headline *
            </label>
            <input
              type="text"
              required
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="e.g. Mandatory Pre-Placement Talk for Google SDE Drive"
              className="w-full px-4 py-2.5 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-amber-500 text-sm font-semibold"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-700 mb-1.5">
              Announcement Message Body *
            </label>
            <textarea
              rows={3}
              required
              value={content}
              onChange={(e) => setContent(e.target.value)}
              placeholder="Detail date, reporting time, dress code, required documents, or webinar link..."
              className="w-full px-4 py-2.5 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-amber-500 text-sm"
            />
          </div>

          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pt-2">
            <div className="flex items-center gap-2">
              <span className="text-xs font-semibold text-slate-500">Target Batches:</span>
              {[2025, 2026, 2027].map((year) => {
                const isSel = targetBatches.includes(year);
                return (
                  <button
                    key={year}
                    type="button"
                    onClick={() => {
                      if (isSel) setTargetBatches(targetBatches.filter(y => y !== year));
                      else setTargetBatches([...targetBatches, year]);
                    }}
                    className={`px-3 py-1 rounded-lg text-xs font-bold transition ${
                      isSel
                        ? 'bg-amber-600 text-white'
                        : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                    }`}
                  >
                    Class of {year}
                  </button>
                );
              })}
            </div>

            <button
              type="submit"
              disabled={createMutation.isPending || !title.trim() || !content.trim()}
              className="inline-flex items-center justify-center gap-2 px-6 py-2.5 bg-amber-600 hover:bg-amber-700 text-white font-bold rounded-xl text-xs transition shadow-md shadow-amber-600/20 disabled:opacity-50"
            >
              <Send className="h-4 w-4" />
              <span>{createMutation.isPending ? 'Publishing...' : 'Broadcast to Students'}</span>
            </button>
          </div>
        </form>
      </div>

      {/* Announcements History List */}
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-base font-bold text-slate-900">Active Notice Board</h2>
          <span className="text-xs text-slate-500 font-semibold">{announcements.length} Published</span>
        </div>

        {isLoading ? (
          <div className="p-12 text-center text-slate-400 text-sm">Loading announcements...</div>
        ) : announcements.length === 0 ? (
          <div className="bg-white p-12 rounded-2xl border border-slate-200 text-center text-slate-400 text-sm">
            <Bell className="h-8 w-8 text-slate-300 mx-auto mb-2" />
            <span>No placement announcements have been posted yet.</span>
          </div>
        ) : (
          <div className="space-y-3">
            {announcements.map((item) => (
              <div
                key={item.id}
                className="bg-white rounded-2xl border border-slate-200 shadow-sm p-6 space-y-2 hover:shadow-md transition"
              >
                <div className="flex items-start justify-between gap-4">
                  <h3 className="text-base font-bold text-slate-900">{item.title}</h3>
                  <div className="flex items-center gap-1.5 shrink-0 text-xs text-slate-400 font-medium">
                    <Calendar className="h-3.5 w-3.5" />
                    <span>Official Circular</span>
                  </div>
                </div>

                <p className="text-sm text-slate-600 whitespace-pre-line leading-relaxed">
                  {item.content}
                </p>

                <div className="flex items-center gap-2 pt-2 border-t border-slate-100 text-xs text-slate-400">
                  <Users className="h-3.5 w-3.5 text-amber-600" />
                  <span>
                    Targeted to:{' '}
                    <strong className="text-slate-700">
                      {item.target_batches.length > 0
                        ? item.target_batches.map(b => `Class of ${b}`).join(', ')
                        : 'All Enrolled Students'}
                    </strong>
                  </span>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

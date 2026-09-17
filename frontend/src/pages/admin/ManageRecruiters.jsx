import { useEffect, useState } from 'react';
import DashboardLayout from '../../components/DashboardLayout';
import LoadingState from '../../components/LoadingState';
import EmptyState from '../../components/EmptyState';
import { getAllRecruiters } from '../../api/adminApi';

export default function ManageRecruiters() {
  const [recruiters, setRecruiters] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getAllRecruiters().then((res) => setRecruiters(res.data)).finally(() => setLoading(false));
  }, []);

  return (
    <DashboardLayout>
      <h1 className="font-display text-2xl font-semibold text-ink">Recruiters</h1>
      <div className="mt-1 w-12 border-t border-ink/30" />

      <div className="mt-8">
        {loading ? (
          <LoadingState />
        ) : recruiters.length === 0 ? (
          <EmptyState title="No recruiters yet" />
        ) : (
          <div className="bg-white border border-ink/10 rounded-lg overflow-x-auto">
            <table className="w-full text-sm min-w-[600px]">
              <thead className="bg-paper border-b border-ink/10">
                <tr>
                  <th className="text-left px-5 py-3 text-xs font-medium text-slate uppercase tracking-wide">Name</th>
                  <th className="text-left px-5 py-3 text-xs font-medium text-slate uppercase tracking-wide">Email</th>
                  <th className="text-left px-5 py-3 text-xs font-medium text-slate uppercase tracking-wide">Company</th>
                  <th className="text-left px-5 py-3 text-xs font-medium text-slate uppercase tracking-wide">Designation</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-ink/10">
                {recruiters.map((r) => (
                  <tr key={r.id}>
                    <td className="px-5 py-3 text-ink font-medium">{r.fullName || '—'}</td>
                    <td className="px-5 py-3 text-slate font-mono text-xs">{r.email}</td>
                    <td className="px-5 py-3 text-slate">{r.companyName || '—'}</td>
                    <td className="px-5 py-3 text-slate">{r.designation || '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </DashboardLayout>
  );
}
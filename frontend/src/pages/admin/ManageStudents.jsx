import { useEffect, useState } from 'react';
import DashboardLayout from '../../components/DashboardLayout';
import LoadingState from '../../components/LoadingState';
import EmptyState from '../../components/EmptyState';
import { getAllStudents } from '../../api/adminApi';

export default function ManageStudents() {
  const [students, setStudents] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getAllStudents().then((res) => setStudents(res.data)).finally(() => setLoading(false));
  }, []);

  return (
    <DashboardLayout>
      <h1 className="font-display text-2xl font-semibold text-ink">Students</h1>
      <div className="mt-1 w-12 border-t border-ink/30" />

      <div className="mt-8">
        {loading ? (
          <LoadingState />
        ) : students.length === 0 ? (
          <EmptyState title="No students yet" />
        ) : (
          <div className="bg-white border border-ink/10 rounded-lg overflow-hidden">
            <table className="w-full text-sm">
              <thead className="bg-paper border-b border-ink/10">
                <tr>
                  <th className="text-left px-5 py-3 text-xs font-medium text-slate uppercase tracking-wide">Name</th>
                  <th className="text-left px-5 py-3 text-xs font-medium text-slate uppercase tracking-wide">Email</th>
                  <th className="text-left px-5 py-3 text-xs font-medium text-slate uppercase tracking-wide">College</th>
                  <th className="text-left px-5 py-3 text-xs font-medium text-slate uppercase tracking-wide">Branch</th>
                  <th className="text-left px-5 py-3 text-xs font-medium text-slate uppercase tracking-wide">Grad Year</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-ink/10">
                {students.map((s) => (
                  <tr key={s.id}>
                    <td className="px-5 py-3 text-ink font-medium">{s.fullName || '—'}</td>
                    <td className="px-5 py-3 text-slate font-mono text-xs">{s.email}</td>
                    <td className="px-5 py-3 text-slate">{s.college || '—'}</td>
                    <td className="px-5 py-3 text-slate">{s.branch || '—'}</td>
                    <td className="px-5 py-3 text-slate">{s.graduationYear || '—'}</td>
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
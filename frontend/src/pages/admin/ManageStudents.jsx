import { useEffect, useState } from 'react';
import DashboardLayout from '../../components/DashboardLayout';
import LoadingState from '../../components/LoadingState';
import EmptyState from '../../components/EmptyState';
import Pagination from '../../components/Pagination';
import { getAllStudents } from '../../api/adminApi';

export default function ManageStudents() {
  const [students, setStudents] = useState([]);
  const [pageInfo, setPageInfo] = useState({ page: 0, totalPages: 0, totalElements: 0 });
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getAllStudents(page)
      .then((res) => {
        const { content, ...info } = res.data;
        setStudents(content);
        setPageInfo(info);
      })
      .finally(() => setLoading(false));
  }, [page]);

  const handlePageChange = (next) => {
    setLoading(true);
    setPage(next);
  };

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
          <div className="bg-white border border-ink/10 rounded-lg overflow-x-auto">
            <table className="w-full text-sm min-w-[600px]">
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
        <Pagination {...pageInfo} onPageChange={handlePageChange} disabled={loading} />
      </div>
    </DashboardLayout>
  );
}
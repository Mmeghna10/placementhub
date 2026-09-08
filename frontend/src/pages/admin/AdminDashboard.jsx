import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import DashboardLayout from '../../components/DashboardLayout';
import LoadingState from '../../components/LoadingState';
import { getDashboard } from '../../api/adminApi';

export default function AdminDashboard() {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getDashboard().then((res) => setStats(res.data)).finally(() => setLoading(false));
  }, []);

  if (loading) return <DashboardLayout><LoadingState /></DashboardLayout>;

  const cards = [
    { label: 'Students', value: stats.totalStudents },
    { label: 'Recruiters', value: stats.totalRecruiters },
    { label: 'Companies', value: stats.totalCompanies },
    { label: 'Total Jobs', value: stats.totalJobs },
    { label: 'Pending Jobs', value: stats.pendingJobs, highlight: true },
    { label: 'Applications', value: stats.totalApplications },
  ];

  return (
    <DashboardLayout>
      <h1 className="font-display text-2xl font-semibold text-ink">Admin Dashboard</h1>
      <div className="mt-1 w-12 border-t border-ink/30" />

      {stats.pendingJobs > 0 && (
        <div className="mt-6 bg-brass/10 border border-brass/30 rounded-lg px-5 py-4 flex items-center justify-between">
          <p className="text-sm text-ink">
            {stats.pendingJobs} job{stats.pendingJobs > 1 ? 's' : ''} waiting for approval.
          </p>
          <Link to="/admin/jobs/pending" className="text-sm font-medium text-teal hover:underline whitespace-nowrap ml-4">
            Review now →
          </Link>
        </div>
      )}

      <div className="mt-8 grid grid-cols-2 md:grid-cols-3 gap-4">
        {cards.map((c) => (
          <div
            key={c.label}
            className={`border rounded-lg p-5 ${c.highlight ? 'bg-brass/10 border-brass/30' : 'bg-white border-ink/10'}`}
          >
            <p className="text-xs font-mono uppercase tracking-wide text-slate">{c.label}</p>
            <p className="font-display text-3xl font-semibold text-ink mt-2">{c.value}</p>
          </div>
        ))}
      </div>
    </DashboardLayout>
  );
}
import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import DashboardLayout from '../../components/DashboardLayout';
import LoadingState from '../../components/LoadingState';
import StatCard from '../../components/StatCard';
import { BarChart3, Briefcase, Building2, FileText, GraduationCap, Hourglass, UserRoundSearch } from 'lucide-react';
import { getDashboard } from '../../api/adminApi';

export default function AdminDashboard() {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getDashboard().then((res) => setStats(res.data)).finally(() => setLoading(false));
  }, []);

  if (loading) return <DashboardLayout><LoadingState /></DashboardLayout>;

  const cards = [
    { label: 'Students', value: stats.totalStudents, icon: GraduationCap, tone: 'teal' },
    { label: 'Recruiters', value: stats.totalRecruiters, icon: UserRoundSearch, tone: 'indigo' },
    { label: 'Companies', value: stats.totalCompanies, icon: Building2, tone: 'brass' },
    { label: 'Total Jobs', value: stats.totalJobs, icon: Briefcase, tone: 'slate' },
    { label: 'Pending Jobs', value: stats.pendingJobs, icon: Hourglass, tone: 'amber', highlight: stats.pendingJobs > 0 },
    { label: 'Applications', value: stats.totalApplications, icon: FileText, tone: 'emerald' },
  ];

  return (
    <DashboardLayout>
      <div className="flex flex-col sm:flex-row sm:items-end sm:justify-between gap-4">
        <div>
          <h1 className="font-display text-[1.75rem] font-semibold text-ink tracking-tight">Admin Dashboard</h1>
          <div className="mt-2 h-1 w-10 rounded-full bg-brass/70" />
          <p className="mt-3 text-sm text-slate">Placement activity across the campus.</p>
        </div>
        <Link
          to="/admin/analytics"
          className="inline-flex items-center gap-2 self-start sm:self-auto px-4 py-2.5 border border-ink/15 bg-white text-ink rounded-lg text-sm font-medium hover:border-ink/40 transition"
        >
          <BarChart3 size={16} /> View analytics
        </Link>
      </div>

      {stats.pendingJobs > 0 && (
        <div className="mt-6 bg-amber-50/70 border border-amber-200 rounded-xl px-5 py-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div className="flex items-center gap-3">
            <Hourglass size={18} className="text-amber-600 shrink-0" />
            <p className="text-sm text-ink">
              {stats.pendingJobs} job{stats.pendingJobs > 1 ? 's' : ''} waiting for approval.
            </p>
          </div>
          <Link to="/admin/jobs/pending" className="text-sm font-medium text-teal hover:underline whitespace-nowrap">
            Review now →
          </Link>
        </div>
      )}

      <div className="mt-8 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        {cards.map((c) => (
          <StatCard key={c.label} {...c} />
        ))}
      </div>
    </DashboardLayout>
  );
}

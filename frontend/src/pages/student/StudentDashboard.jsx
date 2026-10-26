import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import DashboardLayout from '../../components/DashboardLayout';
import LoadingState from '../../components/LoadingState';
import StatusBadge from '../../components/StatusBadge';
import StatCard from '../../components/StatCard';
import CompanyAvatar from '../../components/CompanyAvatar';
import EmptyState from '../../components/EmptyState';
import { Briefcase, CalendarDays, ChevronRight, FileText, Gift, Search, TrendingUp, Trophy, UserRoundPen } from 'lucide-react';
import { formatDateTime } from '../../utils/jobFormat';
import { tierLabel } from '../../utils/tiers';
import { getStudentProfile, getMyApplications } from '../../api/studentApi';

export default function StudentDashboard() {
  const [profile, setProfile] = useState(null);
  const [applications, setApplications] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([getStudentProfile(), getMyApplications()])
      .then(([profileRes, appsRes]) => {
        setProfile(profileRes.data);
        setApplications(appsRes.data);
      })
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <DashboardLayout><LoadingState /></DashboardLayout>;

  const recent = applications.slice(0, 5);

  const firstName = profile?.fullName ? profile.fullName.split(' ')[0] : '';
  const inProgress = applications.filter((a) => ['SHORTLISTED', 'INTERVIEW', 'OFFERED', 'ACCEPTED'].includes(a.status)).length;
  const interviews = applications.filter((a) => a.status === 'INTERVIEW').length;
  const offers = applications.filter((a) => ['OFFERED', 'ACCEPTED'].includes(a.status)).length;
  const pendingOffer = applications.find((a) => a.status === 'OFFERED');
  const placedWith = applications.find((a) => a.status === 'ACCEPTED');

  return (
    <DashboardLayout>
      <div className="flex flex-col sm:flex-row sm:items-end sm:justify-between gap-4">
        <div>
          <h1 className="font-display text-[1.75rem] font-semibold text-ink tracking-tight">
            Welcome back{firstName ? `, ${firstName}` : ''}
          </h1>
          <div className="mt-2 h-1 w-10 rounded-full bg-brass/70" />
          <p className="mt-3 text-sm text-slate">Here is where your placement season stands today.</p>
        </div>
        <Link
          to="/student/jobs"
          className="inline-flex items-center gap-2 self-start sm:self-auto px-4 py-2.5 bg-ink text-paper rounded-lg text-sm font-medium hover:bg-ink/90 transition shadow-sm"
        >
          <Search size={16} /> Browse jobs
        </Link>
      </div>

      {pendingOffer && (
        <Link
          to={`/student/applications/${pendingOffer.id}`}
          className="mt-6 group flex flex-col sm:flex-row sm:items-center justify-between gap-3 rounded-xl border border-emerald-200 bg-gradient-to-r from-emerald-50 to-white px-5 py-4 hover:border-emerald-300 transition"
        >
          <div className="flex items-center gap-3">
            <Gift size={20} className="text-emerald-600 shrink-0" />
            <p className="text-sm text-ink">
              <span className="font-semibold">{pendingOffer.companyName}</span> offered you {pendingOffer.jobTitle}.
              {pendingOffer.offerExpiresAt && <> Decide by {formatDateTime(pendingOffer.offerExpiresAt)}.</>}
            </p>
          </div>
          <span className="text-sm font-semibold text-emerald-700 whitespace-nowrap group-hover:underline">Review offer →</span>
        </Link>
      )}

      {placedWith && (
        <p className="mt-6 flex items-center gap-3 rounded-xl border border-emerald-200 bg-emerald-50 px-5 py-4 text-sm text-emerald-800">
          <Trophy size={18} className="shrink-0" />
          <span>
            Placed at <span className="font-semibold">{placedWith.companyName}</span> as {placedWith.jobTitle}
            {placedWith.jobTier && <> ({tierLabel(placedWith.jobTier)})</>}. Congratulations!
            {placedWith.jobTier !== 'SUPER_DREAM' && <> Higher-tier jobs may still be open to you, depending on the placement rules.</>}
          </span>
        </p>
      )}

      {!profile?.profileCompleted && (
        <div className="mt-6 bg-amber-50/70 border border-amber-200 rounded-xl px-5 py-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div className="flex items-start gap-3">
            <UserRoundPen size={18} className="text-amber-600 mt-0.5 shrink-0" />
            <p className="text-sm text-ink">
              Your profile is incomplete. Recruiters won't see you in matches until it's filled out.
            </p>
          </div>
          <Link to="/student/profile" className="text-sm font-medium text-teal hover:underline whitespace-nowrap">
            Complete profile →
          </Link>
        </div>
      )}

      <div className="mt-8 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard label="Applications" value={applications.length} icon={FileText} tone="teal" />
        <StatCard label="Shortlisted" value={inProgress} icon={TrendingUp} tone="brass" hint="Shortlisted or further" />
        <StatCard label="Interviews" value={interviews} icon={CalendarDays} tone="indigo" />
        <StatCard label="Offers" value={offers} icon={Trophy} tone="emerald" />
      </div>

      <div className="mt-10">
        <div className="flex items-center justify-between mb-4">
          <h2 className="font-display text-lg font-semibold text-ink">Recent Applications</h2>
          <Link to="/student/applications" className="text-sm font-medium text-teal hover:underline">View all →</Link>
        </div>

        {recent.length === 0 ? (
          <EmptyState
            icon={Briefcase}
            title="No applications yet"
            description="Find a role you're eligible for and apply in one click."
            action={<Link to="/student/jobs" className="text-sm font-medium text-teal hover:underline">Browse open roles →</Link>}
          />
        ) : (
          <div className="bg-white border border-ink/[0.08] rounded-xl shadow-card divide-y divide-ink/[0.06] overflow-hidden">
            {recent.map((app) => (
              <Link
                key={app.id}
                to={`/student/applications/${app.id}`}
                className="group flex items-center gap-4 px-5 py-4 hover:bg-paper/60 transition"
              >
                <CompanyAvatar name={app.companyName} />
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-semibold text-ink truncate group-hover:text-teal transition">{app.jobTitle}</p>
                  <p className="text-xs text-slate mt-0.5">{app.companyName}</p>
                </div>
                <StatusBadge status={app.status} />
                <ChevronRight size={16} className="text-slate/40 group-hover:text-teal transition hidden sm:block" />
              </Link>
            ))}
          </div>
        )}
      </div>
    </DashboardLayout>
  );
}

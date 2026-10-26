import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import DashboardLayout from '../../components/DashboardLayout';
import LoadingState from '../../components/LoadingState';
import StatusBadge from '../../components/StatusBadge';
import StatCard from '../../components/StatCard';
import EmptyState from '../../components/EmptyState';
import { Briefcase, Building2, ChevronRight, Gift, Hourglass, MapPin, Plus, Radio } from 'lucide-react';
import { getRecruiterProfile, getMyJobs } from '../../api/recruiterApi';

export default function RecruiterDashboard() {
  const [profile, setProfile] = useState(null);
  const [jobs, setJobs] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([getRecruiterProfile(), getMyJobs()])
      .then(([profileRes, jobsRes]) => {
        setProfile(profileRes.data);
        setJobs(jobsRes.data);
      })
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <DashboardLayout><LoadingState /></DashboardLayout>;

  const recent = jobs.slice(0, 5);

  const firstName = profile?.fullName ? profile.fullName.split(' ')[0] : '';

  return (
    <DashboardLayout>
      <div className="flex flex-col sm:flex-row sm:items-end sm:justify-between gap-4">
        <div>
          <h1 className="font-display text-[1.75rem] font-semibold text-ink tracking-tight">
            Welcome back{firstName ? `, ${firstName}` : ''}
          </h1>
          <div className="mt-2 h-1 w-10 rounded-full bg-brass/70" />
          <p className="mt-3 text-sm text-slate">
            {profile?.companyName ? `Hiring for ${profile.companyName}.` : 'Your hiring overview.'}
          </p>
        </div>
        <div className="flex gap-3 self-start sm:self-auto">
        <Link
          to="/recruiter/offer-desk"
          className="inline-flex items-center gap-2 px-4 py-2.5 border border-ink/15 bg-white text-ink rounded-lg text-sm font-medium hover:border-ink/40 transition"
        >
          <Gift size={16} /> Offer Desk
        </Link>
        <Link
          to="/recruiter/jobs/new"
          className="inline-flex items-center gap-2 px-4 py-2.5 bg-ink text-paper rounded-lg text-sm font-medium hover:bg-ink/90 transition shadow-sm"
        >
          <Plus size={16} /> Post a job
        </Link>
        </div>
      </div>

      {!profile?.companyName && (
        <div className="mt-6 bg-amber-50/70 border border-amber-200 rounded-xl px-5 py-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div className="flex items-start gap-3">
            <Building2 size={18} className="text-amber-600 mt-0.5 shrink-0" />
            <p className="text-sm text-ink">Set up your company profile before posting jobs.</p>
          </div>
          <Link to="/recruiter/profile" className="text-sm font-medium text-teal hover:underline whitespace-nowrap">
            Complete profile →
          </Link>
        </div>
      )}

      <div className="mt-8 grid grid-cols-1 md:grid-cols-3 gap-4">
        <StatCard label="Jobs Posted" value={jobs.length} icon={Briefcase} tone="teal" />
        <StatCard label="Pending Approval" value={jobs.filter((j) => j.status === 'PENDING').length} icon={Hourglass} tone="amber" hint="Reviewed by the placement cell" />
        <StatCard label="Live Roles" value={jobs.filter((j) => j.status === 'APPROVED').length} icon={Radio} tone="emerald" hint="Open to student applications" />
      </div>

      <div className="mt-10">
        <div className="flex items-center justify-between mb-4">
          <h2 className="font-display text-lg font-semibold text-ink">Recent Postings</h2>
          <Link to="/recruiter/jobs" className="text-sm font-medium text-teal hover:underline">View all →</Link>
        </div>

        {recent.length === 0 ? (
          <EmptyState
            icon={Briefcase}
            title="No jobs posted yet"
            description="Post your first role. It goes live once the placement cell approves it."
            action={<Link to="/recruiter/jobs/new" className="text-sm font-medium text-teal hover:underline">Post your first role →</Link>}
          />
        ) : (
          <div className="bg-white border border-ink/[0.08] rounded-xl shadow-card divide-y divide-ink/[0.06] overflow-hidden">
            {recent.map((job) => (
              <Link
                key={job.id}
                to={`/recruiter/jobs/${job.id}/applicants`}
                className="group flex items-center gap-4 px-5 py-4 hover:bg-paper/60 transition"
              >
                <div className="h-10 w-10 rounded-lg bg-teal/10 text-teal flex items-center justify-center shrink-0">
                  <Briefcase size={18} strokeWidth={1.9} />
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-semibold text-ink truncate group-hover:text-teal transition">{job.title}</p>
                  <p className="text-xs text-slate mt-0.5 flex items-center gap-1"><MapPin size={12} /> {job.location || 'Location not set'}</p>
                </div>
                <StatusBadge status={job.status} />
                <ChevronRight size={16} className="text-slate/40 group-hover:text-teal transition hidden sm:block" />
              </Link>
            ))}
          </div>
        )}
      </div>
    </DashboardLayout>
  );
}

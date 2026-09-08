import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import DashboardLayout from '../../components/DashboardLayout';
import LoadingState from '../../components/LoadingState';
import StatusBadge from '../../components/StatusBadge';
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

  return (
    <DashboardLayout>
      <h1 className="font-display text-2xl font-semibold text-ink">
        Welcome back{profile?.fullName ? `, ${profile.fullName.split(' ')[0]}` : ''}
      </h1>
      <div className="mt-1 w-12 border-t border-ink/30" />

      {!profile?.companyName && (
        <div className="mt-6 bg-brass/10 border border-brass/30 rounded-lg px-5 py-4 flex items-center justify-between">
          <p className="text-sm text-ink">
            Set up your company profile before posting jobs.
          </p>
          <Link to="/recruiter/profile" className="text-sm font-medium text-teal hover:underline whitespace-nowrap ml-4">
            Complete profile →
          </Link>
        </div>
      )}

      <div className="mt-8 grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="bg-white border border-ink/10 rounded-lg p-5">
          <p className="text-xs font-mono uppercase tracking-wide text-slate">Jobs Posted</p>
          <p className="font-display text-3xl font-semibold text-ink mt-2">{jobs.length}</p>
        </div>
        <div className="bg-white border border-ink/10 rounded-lg p-5">
          <p className="text-xs font-mono uppercase tracking-wide text-slate">Pending Approval</p>
          <p className="font-display text-3xl font-semibold text-ink mt-2">
            {jobs.filter((j) => j.status === 'PENDING').length}
          </p>
        </div>
        <div className="bg-white border border-ink/10 rounded-lg p-5">
          <p className="text-xs font-mono uppercase tracking-wide text-slate">Live Roles</p>
          <p className="font-display text-3xl font-semibold text-ink mt-2">
            {jobs.filter((j) => j.status === 'APPROVED').length}
          </p>
        </div>
      </div>

      <div className="mt-10">
        <div className="flex items-center justify-between mb-4">
          <h2 className="font-display text-lg font-semibold text-ink">Recent Postings</h2>
          <Link to="/recruiter/jobs" className="text-sm text-teal hover:underline">View all →</Link>
        </div>

        {recent.length === 0 ? (
          <p className="text-sm text-slate">
            No jobs posted yet. <Link to="/recruiter/jobs/new" className="text-teal hover:underline">Post your first role</Link>.
          </p>
        ) : (
          <div className="bg-white border border-ink/10 rounded-lg divide-y divide-ink/10">
            {recent.map((job) => (
              <Link
                key={job.id}
                to={`/recruiter/jobs/${job.id}/applicants`}
                className="flex items-center justify-between px-5 py-4 hover:bg-paper transition"
              >
                <div>
                  <p className="text-sm font-medium text-ink">{job.title}</p>
                  <p className="text-xs text-slate mt-0.5">{job.location}</p>
                </div>
                <StatusBadge status={job.status} />
              </Link>
            ))}
          </div>
        )}
      </div>
    </DashboardLayout>
  );
}
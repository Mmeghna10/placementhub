import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import DashboardLayout from '../../components/DashboardLayout';
import LoadingState from '../../components/LoadingState';
import StatusBadge from '../../components/StatusBadge';
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

  return (
    <DashboardLayout>
      <h1 className="font-display text-2xl font-semibold text-ink">
        Welcome back{profile?.fullName ? `, ${profile.fullName.split(' ')[0]}` : ''}
      </h1>
      <div className="mt-1 w-12 border-t border-ink/30" />

      {!profile?.profileCompleted && (
        <div className="mt-6 bg-brass/10 border border-brass/30 rounded-lg px-5 py-4 flex items-center justify-between">
          <p className="text-sm text-ink">
            Your profile is incomplete — recruiters won't see you in matches until it's filled out.
          </p>
          <Link to="/student/profile" className="text-sm font-medium text-teal hover:underline whitespace-nowrap ml-4">
            Complete profile →
          </Link>
        </div>
      )}

      <div className="mt-8 grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="bg-white border border-ink/10 rounded-lg p-5">
          <p className="text-xs font-mono uppercase tracking-wide text-slate">Applications</p>
          <p className="font-display text-3xl font-semibold text-ink mt-2">{applications.length}</p>
        </div>
        <div className="bg-white border border-ink/10 rounded-lg p-5">
          <p className="text-xs font-mono uppercase tracking-wide text-slate">Shortlisted</p>
          <p className="font-display text-3xl font-semibold text-ink mt-2">
            {applications.filter((a) => ['SHORTLISTED', 'INTERVIEW', 'OFFERED'].includes(a.status)).length}
          </p>
        </div>
        <div className="bg-white border border-ink/10 rounded-lg p-5">
          <p className="text-xs font-mono uppercase tracking-wide text-slate">Offers</p>
          <p className="font-display text-3xl font-semibold text-ink mt-2">
            {applications.filter((a) => a.status === 'OFFERED').length}
          </p>
        </div>
      </div>

      <div className="mt-10">
        <div className="flex items-center justify-between mb-4">
          <h2 className="font-display text-lg font-semibold text-ink">Recent Applications</h2>
          <Link to="/student/applications" className="text-sm text-teal hover:underline">View all →</Link>
        </div>

        {recent.length === 0 ? (
          <p className="text-sm text-slate">
            No applications yet. <Link to="/student/jobs" className="text-teal hover:underline">Browse open roles</Link>.
          </p>
        ) : (
          <div className="bg-white border border-ink/10 rounded-lg divide-y divide-ink/10">
            {recent.map((app) => (
              <Link
                key={app.id}
                to={`/student/applications/${app.id}`}
                className="flex items-center justify-between px-5 py-4 hover:bg-paper transition"
              >
                <div>
                  <p className="text-sm font-medium text-ink">{app.jobTitle}</p>
                  <p className="text-xs text-slate mt-0.5">{app.companyName}</p>
                </div>
                <StatusBadge status={app.status} />
              </Link>
            ))}
          </div>
        )}
      </div>
    </DashboardLayout>
  );
}
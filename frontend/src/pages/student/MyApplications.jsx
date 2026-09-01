import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import DashboardLayout from '../../components/DashboardLayout';
import LoadingState from '../../components/LoadingState';
import EmptyState from '../../components/EmptyState';
import StatusBadge from '../../components/StatusBadge';
import { getMyApplications } from '../../api/studentApi';

export default function MyApplications() {
  const [applications, setApplications] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getMyApplications().then((res) => setApplications(res.data)).finally(() => setLoading(false));
  }, []);

  return (
    <DashboardLayout>
      <h1 className="font-display text-2xl font-semibold text-ink">My Applications</h1>
      <div className="mt-1 w-12 border-t border-ink/30" />

      <div className="mt-8">
        {loading ? (
          <LoadingState />
        ) : applications.length === 0 ? (
          <EmptyState title="No applications yet" description="Once you apply to a role, it'll show up here." />
        ) : (
          <div className="bg-white border border-ink/10 rounded-lg divide-y divide-ink/10">
            {applications.map((app) => (
              <Link
                key={app.id}
                to={`/student/applications/${app.id}`}
                className="flex items-center justify-between px-5 py-4 hover:bg-paper transition"
              >
                <div>
                  <p className="text-sm font-medium text-ink">{app.jobTitle}</p>
                  <p className="text-xs text-slate mt-0.5">{app.companyName}</p>
                  <p className="text-xs font-mono text-slate/70 mt-1">
                    Applied {new Date(app.appliedAt).toLocaleDateString()}
                  </p>
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
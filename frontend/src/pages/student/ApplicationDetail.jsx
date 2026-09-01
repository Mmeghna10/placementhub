import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import DashboardLayout from '../../components/DashboardLayout';
import LoadingState from '../../components/LoadingState';
import StatusBadge from '../../components/StatusBadge';
import { getApplicationDetails } from '../../api/studentApi';

export default function ApplicationDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [application, setApplication] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getApplicationDetails(id).then((res) => setApplication(res.data)).finally(() => setLoading(false));
  }, [id]);

  if (loading) return <DashboardLayout><LoadingState /></DashboardLayout>;
  if (!application) return <DashboardLayout><p className="text-slate">Application not found.</p></DashboardLayout>;

  return (
    <DashboardLayout>
      <button onClick={() => navigate('/student/applications')} className="text-sm text-teal hover:underline mb-6">
        ← Back to applications
      </button>

      <div className="flex items-start justify-between mb-8">
        <div>
          <h1 className="font-display text-2xl font-semibold text-ink">{application.jobTitle}</h1>
          <p className="text-sm text-slate mt-1">{application.companyName}</p>
        </div>
        <StatusBadge status={application.status} />
      </div>

      <div className="bg-white border border-ink/10 rounded-lg p-8 max-w-xl">
        <h2 className="text-xs font-medium text-slate uppercase tracking-wide mb-6">Timeline</h2>

        <div className="space-y-0">
          {application.timeline.map((entry, i) => (
            <div key={i} className="flex gap-4">
              <div className="flex flex-col items-center">
                <div className="w-2.5 h-2.5 rounded-full bg-teal mt-1.5 shrink-0" />
                {i < application.timeline.length - 1 && (
                  <div className="w-px flex-1 bg-ink/10 my-1" />
                )}
              </div>
              <div className={`pb-6 ${i === application.timeline.length - 1 ? '' : ''}`}>
                <p className="text-sm font-medium text-ink font-mono uppercase tracking-wide">
                  {entry.status}
                </p>
                {entry.remarks && (
                  <p className="text-sm text-slate mt-1">{entry.remarks}</p>
                )}
                <p className="text-xs font-mono text-slate/60 mt-1">
                  {new Date(entry.changedAt).toLocaleString()}
                </p>
              </div>
            </div>
          ))}
        </div>
      </div>
    </DashboardLayout>
  );
}
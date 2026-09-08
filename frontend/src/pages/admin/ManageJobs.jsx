import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import DashboardLayout from '../../components/DashboardLayout';
import LoadingState from '../../components/LoadingState';
import EmptyState from '../../components/EmptyState';
import StatusBadge from '../../components/StatusBadge';
import { getAllJobs, getPendingJobs, approveJob } from '../../api/adminApi';

export default function ManageJobs() {
  const [searchParams] = useSearchParams();
  const pendingOnly = searchParams.get('pending') === 'true';

  const [jobs, setJobs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [updatingId, setUpdatingId] = useState(null);

  const fetchJobs = () => {
    setLoading(true);
    const call = pendingOnly ? getPendingJobs() : getAllJobs();
    call.then((res) => setJobs(res.data)).finally(() => setLoading(false));
  };

  useEffect(() => {
    fetchJobs();
  }, [pendingOnly]);

  const handleDecision = async (jobId, status) => {
    setUpdatingId(jobId);
    try {
      await approveJob(jobId, status);
      fetchJobs();
    } finally {
      setUpdatingId(null);
    }
  };

  return (
    <DashboardLayout>
      <h1 className="font-display text-2xl font-semibold text-ink">
        {pendingOnly ? 'Pending Approvals' : 'All Jobs'}
      </h1>
      <div className="mt-1 w-12 border-t border-ink/30" />

      <div className="mt-8">
        {loading ? (
          <LoadingState />
        ) : jobs.length === 0 ? (
          <EmptyState title={pendingOnly ? 'Nothing pending' : 'No jobs yet'} />
        ) : (
          <div className="bg-white border border-ink/10 rounded-lg divide-y divide-ink/10">
            {jobs.map((job) => (
              <div key={job.id} className="flex items-center justify-between px-5 py-4">
                <div>
                  <p className="text-sm font-medium text-ink">{job.title}</p>
                  <p className="text-xs text-slate mt-0.5">{job.companyName} · {job.location}</p>
                </div>

                <div className="flex items-center gap-3">
                  <StatusBadge status={job.status} />
                  {job.status === 'PENDING' && (
                    <>
                      <button
                        onClick={() => handleDecision(job.id, 'APPROVED')}
                        disabled={updatingId === job.id}
                        className="px-3 py-1.5 bg-teal text-paper rounded-md text-xs font-medium hover:bg-teal/90 transition disabled:opacity-50"
                      >
                        Approve
                      </button>
                      <button
                        onClick={() => handleDecision(job.id, 'REJECTED')}
                        disabled={updatingId === job.id}
                        className="px-3 py-1.5 bg-brick text-paper rounded-md text-xs font-medium hover:bg-brick/90 transition disabled:opacity-50"
                      >
                        Reject
                      </button>
                    </>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </DashboardLayout>
  );
}
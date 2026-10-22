import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import DashboardLayout from '../../components/DashboardLayout';
import LoadingState from '../../components/LoadingState';
import EmptyState from '../../components/EmptyState';
import StatusBadge from '../../components/StatusBadge';
import Pagination from '../../components/Pagination';
import { getAllJobs, getPendingJobs, approveJob } from '../../api/adminApi';

const EMPTY_PAGE = { page: 0, totalPages: 0, totalElements: 0 };

export default function ManageJobs() {
  const [searchParams] = useSearchParams();
  const pendingOnly = searchParams.get('pending') === 'true';

  const [jobs, setJobs] = useState([]);
  const [pageInfo, setPageInfo] = useState(EMPTY_PAGE);
  const [page, setPage] = useState(0);
  const [reloadKey, setReloadKey] = useState(0);
  const [loading, setLoading] = useState(true);
  const [updatingId, setUpdatingId] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    // The pending endpoint returns a plain list; the all-jobs endpoint returns a page.
    const call = pendingOnly ? getPendingJobs() : getAllJobs(page);
    call
      .then((res) => {
        if (pendingOnly) {
          setJobs(res.data);
          setPageInfo(EMPTY_PAGE);
        } else {
          const { content, ...info } = res.data;
          setJobs(content);
          setPageInfo(info);
        }
      })
      .finally(() => setLoading(false));
  }, [pendingOnly, page, reloadKey]);

  const handlePageChange = (next) => {
    setLoading(true);
    setPage(next);
  };

  const handleDecision = async (jobId, status) => {
    setUpdatingId(jobId);
    setError('');
    try {
      await approveJob(jobId, status);
      setReloadKey((k) => k + 1);
    } catch (err) {
      setError(err.response?.data?.error || 'Could not update this job');
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
        {error && <p className="text-sm text-brick font-medium mb-4">{error}</p>}
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
        {!pendingOnly && <Pagination {...pageInfo} onPageChange={handlePageChange} disabled={loading} />}
      </div>
    </DashboardLayout>
  );
}
import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import DashboardLayout from '../../components/DashboardLayout';
import LoadingState from '../../components/LoadingState';
import EmptyState from '../../components/EmptyState';
import { getApprovedJobs, getRecommendedJobs } from '../../api/studentApi';

export default function BrowseJobs() {
  const [jobs, setJobs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showRecommended, setShowRecommended] = useState(false);

  const fetchJobs = (recommended) => {
    setLoading(true);
    const call = recommended ? getRecommendedJobs() : getApprovedJobs();
    call.then((res) => setJobs(res.data)).finally(() => setLoading(false));
  };

  useEffect(() => {
    fetchJobs(showRecommended);
  }, [showRecommended]);

  return (
    <DashboardLayout>
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="font-display text-2xl font-semibold text-ink">
            {showRecommended ? 'Recommended For You' : 'Open Roles'}
          </h1>
          <div className="mt-1 w-12 border-t border-ink/30" />
        </div>

        <div className="flex bg-white border border-ink/10 rounded-md p-1 self-start">
          <button
            onClick={() => setShowRecommended(false)}
            className={`px-3.5 py-1.5 rounded text-sm font-medium transition ${!showRecommended ? 'bg-ink text-paper' : 'text-slate'}`}
          >
            All
          </button>
          <button
            onClick={() => setShowRecommended(true)}
            className={`px-3.5 py-1.5 rounded text-sm font-medium transition ${showRecommended ? 'bg-ink text-paper' : 'text-slate'}`}
          >
            Recommended
          </button>
        </div>
      </div>

      <div className="mt-8">
        {loading ? (
          <LoadingState />
        ) : jobs.length === 0 ? (
          <EmptyState
            title="No roles here yet"
            description={showRecommended ? "We'll match roles once your profile is complete." : 'Check back soon for new postings.'}
          />
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {jobs.map((job) => (
              <Link
                key={job.id}
                to={`/student/jobs/${job.id}`}
                className="bg-white border border-ink/10 rounded-lg p-5 hover:border-teal/40 transition"
              >
                <p className="text-xs font-mono uppercase tracking-wide text-brass">{job.companyName}</p>
                <h3 className="font-display text-lg font-semibold text-ink mt-1">{job.title}</h3>
                <p className="text-sm text-slate mt-2 line-clamp-2">{job.description}</p>
                <div className="flex flex-wrap items-center gap-x-3 gap-y-1 mt-4 text-xs text-slate">
                  <span>{job.location}</span>
                  <span>·</span>
                  <span>{job.jobType?.replace('_', ' ')}</span>
                  {job.salaryMin && (
                    <>
                      <span>·</span>
                      <span>₹{job.salaryMin.toLocaleString()}–{job.salaryMax?.toLocaleString()}</span>
                    </>
                  )}
                </div>
              </Link>
            ))}
          </div>
        )}
      </div>
    </DashboardLayout>
  );
}
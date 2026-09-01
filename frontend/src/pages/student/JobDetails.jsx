import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import DashboardLayout from '../../components/DashboardLayout';
import LoadingState from '../../components/LoadingState';
import { getApprovedJobs, applyToJob } from '../../api/studentApi';

export default function JobDetails() {
  const { jobId } = useParams();
  const navigate = useNavigate();
  const [job, setJob] = useState(null);
  const [loading, setLoading] = useState(true);
  const [applying, setApplying] = useState(false);
  const [error, setError] = useState('');
  const [applied, setApplied] = useState(false);

  useEffect(() => {
    getApprovedJobs()
      .then((res) => {
        const found = res.data.find((j) => String(j.id) === jobId);
        setJob(found || null);
      })
      .finally(() => setLoading(false));
  }, [jobId]);

  const handleApply = async () => {
    setApplying(true);
    setError('');
    try {
      await applyToJob(jobId);
      setApplied(true);
    } catch (err) {
      setError(err.response?.data?.error || 'Could not apply to this job');
    } finally {
      setApplying(false);
    }
  };

  if (loading) return <DashboardLayout><LoadingState /></DashboardLayout>;
  if (!job) return <DashboardLayout><p className="text-slate">Job not found.</p></DashboardLayout>;

  return (
    <DashboardLayout>
      <button onClick={() => navigate(-1)} className="text-sm text-teal hover:underline mb-6">
        ← Back
      </button>

      <div className="bg-white border border-ink/10 rounded-lg p-8 max-w-2xl">
        <p className="text-xs font-mono uppercase tracking-wide text-brass">{job.companyName}</p>
        <h1 className="font-display text-2xl font-semibold text-ink mt-1">{job.title}</h1>

        <div className="flex flex-wrap gap-3 mt-4 text-sm text-slate">
          <span>{job.location}</span>
          <span>·</span>
          <span>{job.jobType?.replace('_', ' ')}</span>
          {job.salaryMin && (
            <>
              <span>·</span>
              <span>₹{job.salaryMin.toLocaleString()}–{job.salaryMax?.toLocaleString()}</span>
            </>
          )}
          {job.applicationDeadline && (
            <>
              <span>·</span>
              <span>Apply by {job.applicationDeadline}</span>
            </>
          )}
        </div>

        <div className="mt-6 pt-6 border-t border-ink/10">
          <h2 className="text-xs font-medium text-slate uppercase tracking-wide mb-2">Description</h2>
          <p className="text-sm text-ink leading-relaxed whitespace-pre-line">{job.description}</p>
        </div>

        {job.requiredSkills && (
          <div className="mt-6">
            <h2 className="text-xs font-medium text-slate uppercase tracking-wide mb-2">Required Skills</h2>
            <p className="text-sm text-ink">{job.requiredSkills}</p>
          </div>
        )}

        {job.eligibleBranches && (
          <div className="mt-6">
            <h2 className="text-xs font-medium text-slate uppercase tracking-wide mb-2">Eligible Branches</h2>
            <p className="text-sm text-ink">{job.eligibleBranches}</p>
          </div>
        )}

        {job.minCgpa && (
          <div className="mt-6">
            <h2 className="text-xs font-medium text-slate uppercase tracking-wide mb-2">Minimum CGPA</h2>
            <p className="text-sm text-ink">{job.minCgpa}</p>
          </div>
        )}

        <div className="mt-8 pt-6 border-t border-ink/10">
          {applied ? (
            <p className="text-sm text-teal font-medium">Application submitted.</p>
          ) : (
            <button
              onClick={handleApply}
              disabled={applying}
              className="px-6 py-2.5 bg-ink text-paper rounded-md font-medium hover:bg-ink/90 transition disabled:opacity-50"
            >
              {applying ? 'Applying…' : 'Apply Now'}
            </button>
          )}
          {error && <p className="text-sm text-brick font-medium mt-2">{error}</p>}
        </div>
      </div>
    </DashboardLayout>
  );
}
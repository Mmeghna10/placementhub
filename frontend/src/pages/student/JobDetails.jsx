import { useEffect, useRef, useState } from 'react';
import { Link, useParams, useNavigate } from 'react-router-dom';
import DashboardLayout from '../../components/DashboardLayout';
import CompanyAvatar from '../../components/CompanyAvatar';
import JobMeta from '../../components/JobMeta';
import { ArrowLeft } from 'lucide-react';
import LoadingState from '../../components/LoadingState';
import { getJob, getMyApplications, applyToJob } from '../../api/studentApi';

export default function JobDetails() {
  const { jobId } = useParams();
  const navigate = useNavigate();
  const [job, setJob] = useState(null);
  const [loading, setLoading] = useState(true);
  const [applying, setApplying] = useState(false);
  const [error, setError] = useState('');
  // The student's application to this job, if any (shown instead of the Apply button).
  const [application, setApplication] = useState(null);
  // `disabled` only takes effect after React re-renders, so a fast double-click could
  // still send two requests. A ref blocks the second click immediately.
  const submitting = useRef(false);

  useEffect(() => {
    // A missing or hidden job is a 404; treat it as "not found" rather than an error.
    Promise.all([getJob(jobId).catch(() => null), getMyApplications()])
      .then(([jobRes, appsRes]) => {
        setJob(jobRes?.data || null);
        setApplication(appsRes.data.find((a) => String(a.jobId) === jobId) || null);
      })
      .finally(() => setLoading(false));
  }, [jobId]);

  const handleApply = async () => {
    if (submitting.current) return;
    submitting.current = true;
    setApplying(true);
    setError('');
    try {
      const res = await applyToJob(jobId);
      setApplication(res.data);
    } catch (err) {
      if (err.response?.data?.code === 'APPLICATION_ALREADY_EXISTS') {
        // Not a failure from the student's point of view: show their existing application.
        const appsRes = await getMyApplications().catch(() => null);
        setApplication(appsRes?.data.find((a) => String(a.jobId) === jobId) || { id: null });
      } else {
        setError(err.response?.data?.error || 'Could not apply to this job');
      }
    } finally {
      setApplying(false);
      submitting.current = false;
    }
  };

  if (loading) return <DashboardLayout><LoadingState /></DashboardLayout>;
  if (!job) return <DashboardLayout><p className="text-slate">Job not found.</p></DashboardLayout>;

  return (
    <DashboardLayout>
      <button
        onClick={() => navigate(-1)}
        className="inline-flex items-center gap-1.5 text-sm font-medium text-slate hover:text-teal mb-6 transition"
      >
        <ArrowLeft size={15} /> Back
      </button>

      <div className="bg-white border border-ink/[0.08] rounded-2xl shadow-card p-6 sm:p-8 max-w-3xl">
        <div className="flex items-start gap-4">
          <CompanyAvatar name={job.companyName} size="lg" />
          <div className="min-w-0">
            <p className="text-sm font-medium text-teal">{job.companyName}</p>
            <h1 className="font-display text-[1.75rem] font-semibold text-ink tracking-tight leading-tight mt-0.5">{job.title}</h1>
          </div>
        </div>

        <div className="mt-5">
          <JobMeta job={job} />
        </div>

        <div className="mt-6 pt-6 border-t border-ink/10">
          <h2 className="text-xs font-semibold text-slate uppercase tracking-wider mb-2.5">Description</h2>
          <p className="text-sm text-ink leading-relaxed whitespace-pre-line">{job.description}</p>
        </div>

        {job.requiredSkills && (
          <div className="mt-6">
            <h2 className="text-xs font-semibold text-slate uppercase tracking-wider mb-2.5">Required Skills</h2>
            <div className="flex flex-wrap gap-2">
              {job.requiredSkills.split(',').map((skill) => skill.trim()).filter(Boolean).map((skill) => (
                <span key={skill} className="px-2.5 py-1 rounded-full bg-teal/10 text-teal text-xs font-medium">{skill}</span>
              ))}
            </div>
          </div>
        )}

        {job.eligibleBranches && (
          <div className="mt-6">
            <h2 className="text-xs font-semibold text-slate uppercase tracking-wider mb-2.5">Eligible Branches</h2>
            <p className="text-sm text-ink">{job.eligibleBranches}</p>
          </div>
        )}

        {job.minCgpa && (
          <div className="mt-6">
            <h2 className="text-xs font-semibold text-slate uppercase tracking-wider mb-2.5">Minimum CGPA</h2>
            <p className="text-sm text-ink">{job.minCgpa}</p>
          </div>
        )}

        <div className="mt-8 pt-6 border-t border-ink/10">
          {application ? (
            <p className="text-sm text-teal font-medium bg-teal/5 border border-teal/15 rounded-lg px-4 py-3">
              You have applied to this job.{' '}
              {application.id && (
                <Link to={`/student/applications/${application.id}`} className="underline">
                  View your application →
                </Link>
              )}
            </p>
          ) : job.acceptingApplications === false ? (
            <p className="text-sm text-slate font-medium">This job is no longer accepting applications.</p>
          ) : (
            <button
              onClick={handleApply}
              disabled={applying}
              className="px-7 py-3 bg-ink text-paper rounded-lg font-semibold hover:bg-ink/90 transition disabled:opacity-50 shadow-sm"
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
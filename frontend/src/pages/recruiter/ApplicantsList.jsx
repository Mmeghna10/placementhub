import { useEffect, useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { ArrowLeft, CalendarClock } from 'lucide-react';
import CompanyAvatar from '../../components/CompanyAvatar';
import { formatDateTime } from '../../utils/jobFormat';
import DashboardLayout from '../../components/DashboardLayout';
import LoadingState from '../../components/LoadingState';
import EmptyState from '../../components/EmptyState';
import StatusBadge from '../../components/StatusBadge';
import { getApplicantsForJob, getMyJobs, updateApplicationStatus } from '../../api/recruiterApi';
import SeatMeter from '../../components/SeatMeter';

function ApplicantDetails({ applicant }) {
  if (!applicant) return null;
  const facts = [
    applicant.college,
    applicant.degree && applicant.branch ? `${applicant.degree} · ${applicant.branch}` : applicant.branch,
    applicant.graduationYear && `Class of ${applicant.graduationYear}`,
    applicant.cgpa != null && `CGPA ${applicant.cgpa}`,
  ].filter(Boolean);
  const links = [
    ['Resume', applicant.resumeUrl],
    ['LinkedIn', applicant.linkedinUrl],
    ['GitHub', applicant.githubUrl],
  ].filter(([, url]) => url);

  return (
    <div className="flex items-start gap-4">
      <CompanyAvatar name={applicant.fullName || 'Unnamed student'} />
      <div className="min-w-0">
        <p className="text-sm font-semibold text-ink">{applicant.fullName || 'Unnamed student'}</p>
        <p className="text-xs text-slate mt-0.5">{applicant.email}</p>
        {facts.length > 0 && <p className="text-xs text-ink/75 mt-1.5">{facts.join(' · ')}</p>}
        {applicant.skills && <p className="text-xs text-slate mt-1">Skills: {applicant.skills}</p>}
        {links.length > 0 && (
          <div className="flex flex-wrap gap-2 mt-2.5">
            {links.map(([label, url]) => (
              <a
                key={label}
                href={url}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-teal/10 text-xs font-medium text-teal hover:bg-teal/15 transition"
              >
                {label}
              </a>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

export default function ApplicantsList() {
  const { jobId } = useParams();
  const navigate = useNavigate();
  const [applicants, setApplicants] = useState([]);
  const [loading, setLoading] = useState(true);
  const [reloadKey, setReloadKey] = useState(0);
  const [updatingId, setUpdatingId] = useState(null);
  const [message, setMessage] = useState('');
  const [job, setJob] = useState(null);

  useEffect(() => {
    getApplicantsForJob(jobId)
      .then((res) => setApplicants(res.data))
      .finally(() => setLoading(false));
    // Title and openings for the header; seat counts change with every offer.
    getMyJobs()
      .then((res) => setJob(res.data.find((j) => String(j.id) === jobId) ?? null))
      .catch(() => setJob(null));
  }, [jobId, reloadKey]);

  const handleStatusChange = async (app, newStatus) => {
    if (!newStatus) return;
    setUpdatingId(app.id);
    setMessage('');
    try {
      // Sending the version we displayed lets the server refuse the change if someone
      // else updated this application in the meantime.
      await updateApplicationStatus(app.id, { status: newStatus, remarks: '', version: app.version });
    } catch (err) {
      setMessage(err.response?.data?.error || 'Could not update the application status');
    } finally {
      setUpdatingId(null);
      // Always reload: after a 409 this shows the status someone else just set.
      setReloadKey((k) => k + 1);
    }
  };

  return (
    <DashboardLayout>
      <button
        onClick={() => navigate('/recruiter/jobs')}
        className="inline-flex items-center gap-1.5 text-sm font-medium text-slate hover:text-teal mb-6 transition"
      >
        <ArrowLeft size={15} /> Back to jobs
      </button>

      <div className="flex flex-col sm:flex-row sm:items-end sm:justify-between gap-4">
        <div>
          <h1 className="font-display text-[1.75rem] font-semibold text-ink tracking-tight">Applicants</h1>
          <div className="mt-2 h-1 w-10 rounded-full bg-brass/70" />
          {job && (
            <div className="mt-3 flex flex-wrap items-center gap-x-5 gap-y-2">
              <p className="text-sm text-slate">{job.title}</p>
              <SeatMeter job={job} />
            </div>
          )}
        </div>
        <Link
          to={`/recruiter/jobs/${jobId}/slots`}
          className="flex items-center gap-1.5 px-4 py-2 border border-ink/15 rounded-lg text-sm font-medium text-ink hover:bg-ink/5 transition self-start"
        >
          <CalendarClock size={16} />
          Interview slots
        </Link>
      </div>

      <div className="mt-8">
        {message && <p className="text-sm text-brick font-medium mb-4">{message}</p>}
        {loading ? (
          <LoadingState />
        ) : applicants.length === 0 ? (
          <EmptyState title="No applicants yet" description="Check back once students start applying." />
        ) : (
          <div className="bg-white border border-ink/[0.08] rounded-xl shadow-card divide-y divide-ink/[0.06]">
            {applicants.map((app) => (
              <div key={app.id} data-testid="applicant" className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-4 px-5 py-5">
                <div>
                  <ApplicantDetails applicant={app.applicant} />
                  <p className="text-xs text-slate/80 mt-3 sm:pl-[3.75rem]">
                    Applied {formatDateTime(app.appliedAt)}
                    {app.interview && ` · Interview ${formatDateTime(app.interview.startsAt)}`}
                    {app.status === 'OFFERED' && app.offerExpiresAt && (
                      <span className="text-amber-700 font-medium">{` · Offer open until ${formatDateTime(app.offerExpiresAt)}`}</span>
                    )}
                  </p>
                </div>

                <div className="flex items-center gap-3 shrink-0">
                  <StatusBadge status={app.status} />
                  {app.allowedNextStatuses?.length > 0 ? (
                    <select
                      value=""
                      disabled={updatingId === app.id}
                      onChange={(e) => handleStatusChange(app, e.target.value)}
                      className="text-sm border border-ink/15 rounded-lg px-3 py-1.5 bg-paper focus:outline-none focus:ring-2 focus:ring-teal/40"
                    >
                      <option value="">Move to…</option>
                      {app.allowedNextStatuses.map((s) => (
                        <option key={s} value={s}>{s}</option>
                      ))}
                    </select>
                  ) : (
                    <span className="text-xs text-slate">Final</span>
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

import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import DashboardLayout from '../../components/DashboardLayout';
import LoadingState from '../../components/LoadingState';
import EmptyState from '../../components/EmptyState';
import StatusBadge from '../../components/StatusBadge';
import { getMyJobs, deleteJob, closeJob } from '../../api/recruiterApi';
import { Plus, Trash2, Pencil, Users, CalendarClock, Lock } from 'lucide-react';

export default function MyJobs() {
  const [jobs, setJobs] = useState([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    getMyJobs().then((res) => setJobs(res.data)).finally(() => setLoading(false));
  }, [reloadKey]);

  const fetchJobs = () => {
    setLoading(true);
    setReloadKey((k) => k + 1);
  };

  const handleDelete = async (id, e) => {
    e.stopPropagation();
    e.preventDefault();
    if (!confirm('Delete this job posting? This cannot be undone.')) return;
    try {
      await deleteJob(id);
      fetchJobs();
    } catch (err) {
      alert(err.response?.data?.error || 'Could not delete this job');
    }
  };

  const handleClose = async (id, e) => {
    e.stopPropagation();
    e.preventDefault();
    if (!confirm('Stop accepting new applications for this job? Existing applicants are kept.')) return;
    try {
      await closeJob(id);
      fetchJobs();
    } catch (err) {
      alert(err.response?.data?.error || 'Could not close this job');
    }
  };

  return (
    <DashboardLayout>
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="font-display text-[1.75rem] font-semibold text-ink tracking-tight">My Jobs</h1>
          <div className="mt-2 h-1 w-10 rounded-full bg-brass/70" />
        </div>
        <Link
          to="/recruiter/jobs/new"
          className="flex items-center gap-1.5 px-4 py-2 bg-ink text-paper rounded-lg text-sm font-medium hover:bg-ink/90 transition self-start"
        >
          <Plus size={16} />
          Post a Job
        </Link>
      </div>

      <div className="mt-8">
        {loading ? (
          <LoadingState />
        ) : jobs.length === 0 ? (
          <EmptyState title="No jobs posted yet" description="Post your first role to start receiving applications." />
        ) : (
          <div className="bg-white border border-ink/[0.08] rounded-xl shadow-card divide-y divide-ink/10">
            {jobs.map((job) => (
              <div
                key={job.id}
                onClick={() => navigate(`/recruiter/jobs/${job.id}/applicants`)}
                className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 px-5 py-4 hover:bg-paper transition cursor-pointer"
              >
                <div>
                  <p className="text-sm font-medium text-ink">{job.title}</p>
                  <p className="text-xs text-slate mt-0.5">{job.location} · {job.jobType?.replace('_', ' ')}</p>
                </div>

                <div className="flex items-center gap-3">
                  <StatusBadge status={job.status} />
                  <Link
                    to={`/recruiter/jobs/${job.id}/applicants`}
                    onClick={(e) => e.stopPropagation()}
                    className="p-1.5 text-slate hover:text-teal transition"
                    title="View applicants"
                  >
                    <Users size={16} />
                  </Link>
                  <Link
                    to={`/recruiter/jobs/${job.id}/slots`}
                    onClick={(e) => e.stopPropagation()}
                    className="p-1.5 text-slate hover:text-teal transition"
                    title="Interview slots"
                  >
                    <CalendarClock size={16} />
                  </Link>
                  {job.status === 'APPROVED' && (
                    <button
                      onClick={(e) => handleClose(job.id, e)}
                      className="p-1.5 text-slate hover:text-ink transition"
                      title="Close applications"
                    >
                      <Lock size={16} />
                    </button>
                  )}
                  <Link
                    to={`/recruiter/jobs/${job.id}/edit`}
                    onClick={(e) => e.stopPropagation()}
                    className="p-1.5 text-slate hover:text-teal transition"
                    title="Edit"
                  >
                    <Pencil size={16} />
                  </Link>
                  <button
                    onClick={(e) => handleDelete(job.id, e)}
                    className="p-1.5 text-slate hover:text-brick transition"
                    title="Delete"
                  >
                    <Trash2 size={16} />
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </DashboardLayout>
  );
}
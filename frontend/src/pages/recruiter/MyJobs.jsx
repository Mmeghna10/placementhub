import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import DashboardLayout from '../../components/DashboardLayout';
import LoadingState from '../../components/LoadingState';
import EmptyState from '../../components/EmptyState';
import StatusBadge from '../../components/StatusBadge';
import { getMyJobs, deleteJob } from '../../api/recruiterApi';
import { Plus, Trash2, Pencil, Users } from 'lucide-react';

export default function MyJobs() {
  const [jobs, setJobs] = useState([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  const fetchJobs = () => {
    setLoading(true);
    getMyJobs().then((res) => setJobs(res.data)).finally(() => setLoading(false));
  };

  useEffect(() => {
    fetchJobs();
  }, []);

  const handleDelete = async (id, e) => {
    e.stopPropagation();
    e.preventDefault();
    if (!confirm('Delete this job posting? This cannot be undone.')) return;
    await deleteJob(id);
    fetchJobs();
  };

  return (
    <DashboardLayout>
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="font-display text-2xl font-semibold text-ink">My Jobs</h1>
          <div className="mt-1 w-12 border-t border-ink/30" />
        </div>
        <Link
          to="/recruiter/jobs/new"
          className="flex items-center gap-1.5 px-4 py-2 bg-ink text-paper rounded-md text-sm font-medium hover:bg-ink/90 transition self-start"
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
          <div className="bg-white border border-ink/10 rounded-lg divide-y divide-ink/10">
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
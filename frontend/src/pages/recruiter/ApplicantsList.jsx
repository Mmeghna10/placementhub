import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import DashboardLayout from '../../components/DashboardLayout';
import LoadingState from '../../components/LoadingState';
import EmptyState from '../../components/EmptyState';
import StatusBadge from '../../components/StatusBadge';
import { getApplicantsForJob, updateApplicationStatus } from '../../api/recruiterApi';

const statusOptions = ['APPLIED', 'SHORTLISTED', 'INTERVIEW', 'OFFERED', 'REJECTED'];

export default function ApplicantsList() {
  const { jobId } = useParams();
  const navigate = useNavigate();
  const [applicants, setApplicants] = useState([]);
  const [loading, setLoading] = useState(true);
  const [updatingId, setUpdatingId] = useState(null);

  const fetchApplicants = () => {
    setLoading(true);
    getApplicantsForJob(jobId).then((res) => setApplicants(res.data)).finally(() => setLoading(false));
  };

  useEffect(() => {
    fetchApplicants();
  }, [jobId]);

  const handleStatusChange = async (applicationId, newStatus) => {
    setUpdatingId(applicationId);
    try {
      await updateApplicationStatus(applicationId, { status: newStatus, remarks: '' });
      fetchApplicants();
    } finally {
      setUpdatingId(null);
    }
  };

  return (
    <DashboardLayout>
      <button onClick={() => navigate('/recruiter/jobs')} className="text-sm text-teal hover:underline mb-6">
        ← Back to jobs
      </button>

      <h1 className="font-display text-2xl font-semibold text-ink">Applicants</h1>
      <div className="mt-1 w-12 border-t border-ink/30" />

      <div className="mt-8">
        {loading ? (
          <LoadingState />
        ) : applicants.length === 0 ? (
          <EmptyState title="No applicants yet" description="Check back once students start applying." />
        ) : (
          <div className="bg-white border border-ink/10 rounded-lg divide-y divide-ink/10">
            {applicants.map((app) => (
              <div key={app.id} className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 px-5 py-4">
                <div>
                  <p className="text-sm font-medium text-ink">Application #{app.id}</p>
                  <p className="text-xs font-mono text-slate/70 mt-1">
                    Applied {new Date(app.appliedAt).toLocaleDateString()}
                  </p>
                </div>

                <div className="flex items-center gap-3">
                  <StatusBadge status={app.status} />
                  <select
                    value={app.status}
                    disabled={updatingId === app.id}
                    onChange={(e) => handleStatusChange(app.id, e.target.value)}
                    className="text-sm border border-ink/15 rounded-md px-3 py-1.5 bg-paper focus:outline-none focus:ring-2 focus:ring-teal/40"
                  >
                    {statusOptions.map((s) => (
                      <option key={s} value={s}>{s}</option>
                    ))}
                  </select>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </DashboardLayout>
  );
}
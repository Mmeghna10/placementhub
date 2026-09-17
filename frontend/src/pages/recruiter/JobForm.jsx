import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import DashboardLayout from '../../components/DashboardLayout';
import LoadingState from '../../components/LoadingState';
import { createJob, updateJob, getMyJobs } from '../../api/recruiterApi';

const emptyForm = {
  title: '',
  description: '',
  jobType: 'FULL_TIME',
  location: '',
  salaryMin: '',
  salaryMax: '',
  requiredSkills: '',
  minCgpa: '',
  eligibleBranches: '',
  applicationDeadline: '',
};

export default function JobForm() {
  const { jobId } = useParams();
  const isEdit = Boolean(jobId);
  const navigate = useNavigate();

  const [form, setForm] = useState(emptyForm);
  const [loading, setLoading] = useState(isEdit);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!isEdit) return;
    getMyJobs().then((res) => {
      const job = res.data.find((j) => String(j.id) === jobId);
      if (job) setForm({ ...emptyForm, ...job });
    }).finally(() => setLoading(false));
  }, [jobId, isEdit]);

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError('');
    try {
      if (isEdit) {
        await updateJob(jobId, form);
      } else {
        await createJob(form);
      }
      navigate('/recruiter/jobs');
    } catch (err) {
      setError(err.response?.data?.error || 'Could not save job');
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <DashboardLayout><LoadingState /></DashboardLayout>;

  return (
    <DashboardLayout>
      <h1 className="font-display text-2xl font-semibold text-ink">
        {isEdit ? 'Edit Job' : 'Post a New Job'}
      </h1>
      <div className="mt-1 w-12 border-t border-ink/30" />

      <form onSubmit={handleSubmit} className="mt-8 max-w-2xl bg-white border border-ink/10 rounded-lg p-6 space-y-5">
        <div>
          <label className="block text-xs font-medium text-slate uppercase tracking-wide mb-1.5">Title</label>
          <input
            type="text" name="title" required value={form.title} onChange={handleChange}
            className="w-full px-3.5 py-2.5 bg-paper border border-ink/15 rounded-md text-ink focus:outline-none focus:ring-2 focus:ring-teal/40 focus:border-teal transition"
          />
        </div>

        <div>
          <label className="block text-xs font-medium text-slate uppercase tracking-wide mb-1.5">Description</label>
          <textarea
            name="description" rows={4} value={form.description} onChange={handleChange}
            className="w-full px-3.5 py-2.5 bg-paper border border-ink/15 rounded-md text-ink focus:outline-none focus:ring-2 focus:ring-teal/40 focus:border-teal transition"
          />
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
          <div>
            <label className="block text-xs font-medium text-slate uppercase tracking-wide mb-1.5">Job Type</label>
            <select
              name="jobType" value={form.jobType} onChange={handleChange}
              className="w-full px-3.5 py-2.5 bg-paper border border-ink/15 rounded-md text-ink focus:outline-none focus:ring-2 focus:ring-teal/40 focus:border-teal transition"
            >
              <option value="FULL_TIME">Full Time</option>
              <option value="INTERNSHIP">Internship</option>
            </select>
          </div>
          <div>
            <label className="block text-xs font-medium text-slate uppercase tracking-wide mb-1.5">Location</label>
            <input
              type="text" name="location" value={form.location} onChange={handleChange}
              className="w-full px-3.5 py-2.5 bg-paper border border-ink/15 rounded-md text-ink focus:outline-none focus:ring-2 focus:ring-teal/40 focus:border-teal transition"
            />
          </div>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
          <div>
            <label className="block text-xs font-medium text-slate uppercase tracking-wide mb-1.5">Min Salary</label>
            <input
              type="number" name="salaryMin" value={form.salaryMin} onChange={handleChange}
              className="w-full px-3.5 py-2.5 bg-paper border border-ink/15 rounded-md text-ink focus:outline-none focus:ring-2 focus:ring-teal/40 focus:border-teal transition"
            />
          </div>
          <div>
            <label className="block text-xs font-medium text-slate uppercase tracking-wide mb-1.5">Max Salary</label>
            <input
              type="number" name="salaryMax" value={form.salaryMax} onChange={handleChange}
              className="w-full px-3.5 py-2.5 bg-paper border border-ink/15 rounded-md text-ink focus:outline-none focus:ring-2 focus:ring-teal/40 focus:border-teal transition"
            />
          </div>
        </div>

        <div>
          <label className="block text-xs font-medium text-slate uppercase tracking-wide mb-1.5">Required Skills</label>
          <input
            type="text" name="requiredSkills" value={form.requiredSkills} onChange={handleChange}
            placeholder="Java, Spring Boot, React"
            className="w-full px-3.5 py-2.5 bg-paper border border-ink/15 rounded-md text-ink focus:outline-none focus:ring-2 focus:ring-teal/40 focus:border-teal transition"
          />
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
          <div>
            <label className="block text-xs font-medium text-slate uppercase tracking-wide mb-1.5">Minimum CGPA</label>
            <input
              type="number" step="0.1" name="minCgpa" value={form.minCgpa} onChange={handleChange}
              className="w-full px-3.5 py-2.5 bg-paper border border-ink/15 rounded-md text-ink focus:outline-none focus:ring-2 focus:ring-teal/40 focus:border-teal transition"
            />
          </div>
          <div>
            <label className="block text-xs font-medium text-slate uppercase tracking-wide mb-1.5">Application Deadline</label>
            <input
              type="date" name="applicationDeadline" value={form.applicationDeadline} onChange={handleChange}
              className="w-full px-3.5 py-2.5 bg-paper border border-ink/15 rounded-md text-ink focus:outline-none focus:ring-2 focus:ring-teal/40 focus:border-teal transition"
            />
          </div>
        </div>

        <div>
          <label className="block text-xs font-medium text-slate uppercase tracking-wide mb-1.5">Eligible Branches</label>
          <input
            type="text" name="eligibleBranches" value={form.eligibleBranches} onChange={handleChange}
            placeholder="Computer Science, Information Technology"
            className="w-full px-3.5 py-2.5 bg-paper border border-ink/15 rounded-md text-ink focus:outline-none focus:ring-2 focus:ring-teal/40 focus:border-teal transition"
          />
        </div>

        {error && <p className="text-sm text-brick font-medium">{error}</p>}

        <button
          type="submit"
          disabled={saving}
          className="px-5 py-2.5 bg-ink text-paper rounded-md font-medium hover:bg-ink/90 transition disabled:opacity-50"
        >
          {saving ? 'Saving…' : isEdit ? 'Save Changes' : 'Post Job'}
        </button>
      </form>
    </DashboardLayout>
  );
}
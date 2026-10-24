import { useEffect, useState } from 'react';
import DashboardLayout from '../../components/DashboardLayout';
import LoadingState from '../../components/LoadingState';
import { Pencil } from 'lucide-react';
import { getStudentProfile, updateStudentProfile } from '../../api/studentApi';

const fields = [
  { name: 'fullName', label: 'Full Name', type: 'text' },
  { name: 'phone', label: 'Phone', type: 'text' },
  { name: 'college', label: 'College', type: 'text' },
  { name: 'degree', label: 'Degree', type: 'text' },
  { name: 'branch', label: 'Branch', type: 'text' },
  { name: 'graduationYear', label: 'Graduation Year', type: 'number' },
  { name: 'cgpa', label: 'CGPA', type: 'number', step: '0.01' },
  { name: 'skills', label: 'Skills', type: 'text' },
  { name: 'resumeUrl', label: 'Resume URL', type: 'text' },
  { name: 'linkedinUrl', label: 'LinkedIn URL', type: 'text' },
  { name: 'githubUrl', label: 'GitHub URL', type: 'text' },
];

export default function StudentProfile() {
  const [profile, setProfile] = useState(null);
  const [form, setForm] = useState(null);
  const [editing, setEditing] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  const load = () => {
    getStudentProfile().then((res) => {
      setProfile(res.data);
      setForm(res.data);
    }).finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, []);

  const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError('');
    try {
      const res = await updateStudentProfile(form);
      setProfile(res.data);
      setForm(res.data);
      setEditing(false);
    } catch (err) {
      setError(err.response?.data?.error || 'Could not save changes');
    } finally {
      setSaving(false);
    }
  };

  const handleCancel = () => {
    setForm(profile);
    setEditing(false);
  };

  if (loading || !profile) return <DashboardLayout><LoadingState /></DashboardLayout>;

  return (
    <DashboardLayout>
      <div className="flex items-center justify-between">
        <div>
          <h1 className="font-display text-[1.75rem] font-semibold text-ink tracking-tight">My Profile</h1>
          <div className="mt-2 h-1 w-10 rounded-full bg-brass/70" />
        </div>
        {!editing && (
          <button
            onClick={() => setEditing(true)}
            className="flex items-center gap-1.5 px-4 py-2 bg-ink text-paper rounded-lg text-sm font-medium hover:bg-ink/90 transition"
          >
            <Pencil size={15} />
            Edit
          </button>
        )}
      </div>

      {!editing ? (
        <div className="mt-8 max-w-2xl bg-white border border-ink/[0.08] rounded-xl shadow-card p-6">
          <dl className="grid grid-cols-1 sm:grid-cols-2 gap-x-6 gap-y-5">
            <div>
              <dt className="text-xs font-medium text-slate uppercase tracking-wide">Email</dt>
              <dd className="text-sm text-ink mt-1 font-mono">{profile.email}</dd>
            </div>
            {fields.map((f) => (
              <div key={f.name}>
                <dt className="text-xs font-medium text-slate uppercase tracking-wide">{f.label}</dt>
                <dd className="text-sm text-ink mt-1">{profile[f.name] || '—'}</dd>
              </div>
            ))}
          </dl>
        </div>
      ) : (
        <form onSubmit={handleSubmit} className="mt-8 max-w-2xl bg-white border border-ink/[0.08] rounded-xl shadow-card p-6 space-y-5">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
            {fields.map((f) => (
              <div key={f.name} className={f.name === 'skills' || f.name.includes('Url') ? 'sm:col-span-2' : ''}>
                <label className="block text-xs font-medium text-slate uppercase tracking-wide mb-1.5">
                  {f.label}
                </label>
                <input
                  type={f.type}
                  step={f.step}
                  name={f.name}
                  value={form[f.name] ?? ''}
                  onChange={handleChange}
                  className="w-full px-3.5 py-2.5 bg-paper border border-ink/15 rounded-lg text-ink focus:outline-none focus:ring-2 focus:ring-teal/40 focus:border-teal transition"
                />
              </div>
            ))}
          </div>

          <div className="flex items-center gap-3 pt-2">
            {error && <p className="text-sm text-brick font-medium">{error}</p>}
            <button
              type="submit"
              disabled={saving}
              className="px-5 py-2.5 bg-ink text-paper rounded-lg font-medium hover:bg-ink/90 transition disabled:opacity-50"
            >
              {saving ? 'Saving…' : 'Save Changes'}
            </button>
            <button
              type="button"
              onClick={handleCancel}
              className="px-5 py-2.5 border border-ink/15 text-ink rounded-lg font-medium hover:bg-paper transition"
            >
              Cancel
            </button>
          </div>
        </form>
      )}
    </DashboardLayout>
  );
}
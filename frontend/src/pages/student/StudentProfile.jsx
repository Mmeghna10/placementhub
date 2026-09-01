import { useEffect, useState } from 'react';
import DashboardLayout from '../../components/DashboardLayout';
import LoadingState from '../../components/LoadingState';
import { getStudentProfile, updateStudentProfile } from '../../api/studentApi';

const fields = [
  { name: 'fullName', label: 'Full Name', type: 'text' },
  { name: 'phone', label: 'Phone', type: 'text' },
  { name: 'college', label: 'College', type: 'text' },
  { name: 'degree', label: 'Degree', type: 'text' },
  { name: 'branch', label: 'Branch', type: 'text' },
  { name: 'graduationYear', label: 'Graduation Year', type: 'number' },
  { name: 'cgpa', label: 'CGPA', type: 'number', step: '0.01' },
  { name: 'skills', label: 'Skills (comma separated)', type: 'text' },
  { name: 'resumeUrl', label: 'Resume URL', type: 'text' },
  { name: 'linkedinUrl', label: 'LinkedIn URL', type: 'text' },
  { name: 'githubUrl', label: 'GitHub URL', type: 'text' },
];

export default function StudentProfile() {
  const [form, setForm] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    getStudentProfile().then((res) => setForm(res.data)).finally(() => setLoading(false));
  }, []);

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
    setSaved(false);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      const res = await updateStudentProfile(form);
      setForm(res.data);
      setSaved(true);
    } finally {
      setSaving(false);
    }
  };

  if (loading || !form) return <DashboardLayout><LoadingState /></DashboardLayout>;

  return (
    <DashboardLayout>
      <h1 className="font-display text-2xl font-semibold text-ink">My Profile</h1>
      <div className="mt-1 w-12 border-t border-ink/30" />

      <form onSubmit={handleSubmit} className="mt-8 max-w-2xl bg-white border border-ink/10 rounded-lg p-6 space-y-5">
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
                className="w-full px-3.5 py-2.5 bg-paper border border-ink/15 rounded-md text-ink focus:outline-none focus:ring-2 focus:ring-teal/40 focus:border-teal transition"
              />
            </div>
          ))}
        </div>

        <div className="flex items-center gap-4 pt-2">
          <button
            type="submit"
            disabled={saving}
            className="px-5 py-2.5 bg-ink text-paper rounded-md font-medium hover:bg-ink/90 transition disabled:opacity-50"
          >
            {saving ? 'Saving…' : 'Save Changes'}
          </button>
          {saved && <span className="text-sm text-teal font-medium">Saved.</span>}
        </div>
      </form>
    </DashboardLayout>
  );
}
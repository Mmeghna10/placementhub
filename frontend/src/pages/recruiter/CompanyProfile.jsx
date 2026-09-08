import { useEffect, useState } from 'react';
import DashboardLayout from '../../components/DashboardLayout';
import LoadingState from '../../components/LoadingState';
import { getRecruiterProfile, updateRecruiterProfile } from '../../api/recruiterApi';

const recruiterFields = [
  { name: 'fullName', label: 'Your Full Name' },
  { name: 'phone', label: 'Phone' },
  { name: 'designation', label: 'Designation' },
];

const companyFields = [
  { name: 'companyName', label: 'Company Name' },
  { name: 'companyIndustry', label: 'Industry' },
  { name: 'companyLocation', label: 'Location' },
  { name: 'companyWebsite', label: 'Website' },
  { name: 'companyDescription', label: 'Description', full: true },
];

export default function CompanyProfile() {
  const [form, setForm] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    getRecruiterProfile().then((res) => setForm(res.data)).finally(() => setLoading(false));
  }, []);

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
    setSaved(false);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      const res = await updateRecruiterProfile(form);
      setForm(res.data);
      setSaved(true);
    } finally {
      setSaving(false);
    }
  };

  if (loading || !form) return <DashboardLayout><LoadingState /></DashboardLayout>;

  return (
    <DashboardLayout>
      <h1 className="font-display text-2xl font-semibold text-ink">Company Profile</h1>
      <div className="mt-1 w-12 border-t border-ink/30" />

      <form onSubmit={handleSubmit} className="mt-8 max-w-2xl space-y-8">
        <div className="bg-white border border-ink/10 rounded-lg p-6">
          <h2 className="text-xs font-medium text-slate uppercase tracking-wide mb-5">Recruiter Details</h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
            {recruiterFields.map((f) => (
              <div key={f.name}>
                <label className="block text-xs font-medium text-slate uppercase tracking-wide mb-1.5">
                  {f.label}
                </label>
                <input
                  type="text"
                  name={f.name}
                  value={form[f.name] ?? ''}
                  onChange={handleChange}
                  className="w-full px-3.5 py-2.5 bg-paper border border-ink/15 rounded-md text-ink focus:outline-none focus:ring-2 focus:ring-teal/40 focus:border-teal transition"
                />
              </div>
            ))}
          </div>
        </div>

        <div className="bg-white border border-ink/10 rounded-lg p-6">
          <h2 className="text-xs font-medium text-slate uppercase tracking-wide mb-5">Company Details</h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
            {companyFields.map((f) => (
              <div key={f.name} className={f.full ? 'sm:col-span-2' : ''}>
                <label className="block text-xs font-medium text-slate uppercase tracking-wide mb-1.5">
                  {f.label}
                </label>
                {f.full ? (
                  <textarea
                    name={f.name}
                    value={form[f.name] ?? ''}
                    onChange={handleChange}
                    rows={3}
                    className="w-full px-3.5 py-2.5 bg-paper border border-ink/15 rounded-md text-ink focus:outline-none focus:ring-2 focus:ring-teal/40 focus:border-teal transition"
                  />
                ) : (
                  <input
                    type="text"
                    name={f.name}
                    value={form[f.name] ?? ''}
                    onChange={handleChange}
                    className="w-full px-3.5 py-2.5 bg-paper border border-ink/15 rounded-md text-ink focus:outline-none focus:ring-2 focus:ring-teal/40 focus:border-teal transition"
                  />
                )}
              </div>
            ))}
          </div>
        </div>

        <div className="flex items-center gap-4">
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
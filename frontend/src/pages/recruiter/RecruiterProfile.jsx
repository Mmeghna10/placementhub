import { useEffect, useState } from 'react';
import DashboardLayout from '../../components/DashboardLayout';
import LoadingState from '../../components/LoadingState';
import { Pencil } from 'lucide-react';
import { getRecruiterProfile, updateRecruiterProfile } from '../../api/recruiterApi';

const personalFields = [
  { name: 'fullName', label: 'Full Name' },
  { name: 'phone', label: 'Phone' },
  { name: 'designation', label: 'Designation' },
];

export default function RecruiterProfile() {
  const [profile, setProfile] = useState(null);
  const [form, setForm] = useState(null);
  const [editing, setEditing] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  const load = () => {
    getRecruiterProfile().then((res) => {
      setProfile(res.data);
      setForm(res.data);
    }).finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, []);

  const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      const res = await updateRecruiterProfile(form);
      setProfile(res.data);
      setForm(res.data);
      setEditing(false);
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
          <h1 className="font-display text-2xl font-semibold text-ink">My Profile</h1>
          <div className="mt-1 w-12 border-t border-ink/30" />
        </div>
        {!editing && (
          <button
            onClick={() => setEditing(true)}
            className="flex items-center gap-1.5 px-4 py-2 bg-ink text-paper rounded-md text-sm font-medium hover:bg-ink/90 transition"
          >
            <Pencil size={15} />
            Edit
          </button>
        )}
      </div>

      {!editing ? (
        <div className="mt-8 max-w-xl bg-white border border-ink/10 rounded-lg p-6">
          <dl className="grid grid-cols-1 sm:grid-cols-2 gap-x-6 gap-y-5">
            <div>
              <dt className="text-xs font-medium text-slate uppercase tracking-wide">Email</dt>
              <dd className="text-sm text-ink mt-1 font-mono">{profile.email}</dd>
            </div>
            {personalFields.map((f) => (
              <div key={f.name}>
                <dt className="text-xs font-medium text-slate uppercase tracking-wide">{f.label}</dt>
                <dd className="text-sm text-ink mt-1">{profile[f.name] || '—'}</dd>
              </div>
            ))}
          </dl>
        </div>
      ) : (
        <form onSubmit={handleSubmit} className="mt-8 max-w-xl bg-white border border-ink/10 rounded-lg p-6 space-y-5">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
            {personalFields.map((f) => (
              <div key={f.name}>
                <label className="block text-xs font-medium text-slate uppercase tracking-wide mb-1.5">
                  {f.label}
                </label>
                <input
                  type="text" name={f.name} value={form[f.name] ?? ''} onChange={handleChange}
                  className="w-full px-3.5 py-2.5 bg-paper border border-ink/15 rounded-md text-ink focus:outline-none focus:ring-2 focus:ring-teal/40 focus:border-teal transition"
                />
              </div>
            ))}
          </div>
          <div className="flex items-center gap-3 pt-2">
            <button type="submit" disabled={saving} className="px-5 py-2.5 bg-ink text-paper rounded-md font-medium hover:bg-ink/90 transition disabled:opacity-50">
              {saving ? 'Saving…' : 'Save Changes'}
            </button>
            <button type="button" onClick={handleCancel} className="px-5 py-2.5 border border-ink/15 text-ink rounded-md font-medium hover:bg-paper transition">
              Cancel
            </button>
          </div>
        </form>
      )}
    </DashboardLayout>
  );
}
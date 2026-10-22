import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Trash2 } from 'lucide-react';
import DashboardLayout from '../../components/DashboardLayout';
import LoadingState from '../../components/LoadingState';
import EmptyState from '../../components/EmptyState';
import StatusBadge from '../../components/StatusBadge';
import { getInterviewSlots, createInterviewSlot, deleteInterviewSlot } from '../../api/recruiterApi';

const inputClass =
  'w-full px-3.5 py-2.5 bg-paper border border-ink/15 rounded-md text-ink focus:outline-none focus:ring-2 focus:ring-teal/40 focus:border-teal transition';
const labelClass = 'block text-xs font-medium text-slate uppercase tracking-wide mb-1.5';

const emptyForm = { startsAt: '', durationMinutes: 30, capacity: 1, location: '' };

export default function InterviewSlots() {
  const { jobId } = useParams();
  const navigate = useNavigate();
  const [slots, setSlots] = useState([]);
  const [loading, setLoading] = useState(true);
  const [reloadKey, setReloadKey] = useState(0);
  const [form, setForm] = useState(emptyForm);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    getInterviewSlots(jobId)
      .then((res) => setSlots(res.data))
      .catch((err) => setError(err.response?.data?.error || 'Could not load interview slots'))
      .finally(() => setLoading(false));
  }, [jobId, reloadKey]);

  const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const handleCreate = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError('');
    try {
      await createInterviewSlot(jobId, {
        // datetime-local gives "YYYY-MM-DDTHH:mm" in local time, which the API expects.
        startsAt: form.startsAt,
        durationMinutes: Number(form.durationMinutes),
        capacity: Number(form.capacity),
        location: form.location,
      });
      setForm(emptyForm);
      setReloadKey((k) => k + 1);
    } catch (err) {
      setError(err.response?.data?.error || 'Could not create the slot');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (slotId) => {
    if (!confirm('Delete this interview slot?')) return;
    setError('');
    try {
      await deleteInterviewSlot(jobId, slotId);
      setReloadKey((k) => k + 1);
    } catch (err) {
      setError(err.response?.data?.error || 'Could not delete the slot');
    }
  };

  return (
    <DashboardLayout>
      <button onClick={() => navigate(`/recruiter/jobs/${jobId}/applicants`)} className="text-sm text-teal hover:underline mb-6">
        ← Back to applicants
      </button>

      <h1 className="font-display text-2xl font-semibold text-ink">Interview Slots</h1>
      <div className="mt-1 w-12 border-t border-ink/30" />
      <p className="mt-4 text-sm text-slate max-w-2xl">
        Students whose application you move to <span className="font-mono">INTERVIEW</span> can book one seat in any
        upcoming slot. A slot never accepts more students than its capacity.
      </p>

      <form onSubmit={handleCreate} className="mt-8 max-w-2xl bg-white border border-ink/10 rounded-lg p-6 space-y-5">
        <h2 className="text-xs font-medium text-slate uppercase tracking-wide">Add a slot</h2>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
          <div>
            <label className={labelClass}>Starts at</label>
            <input type="datetime-local" name="startsAt" required value={form.startsAt} onChange={handleChange} className={inputClass} />
          </div>
          <div>
            <label className={labelClass}>Location / meeting link</label>
            <input type="text" name="location" value={form.location} onChange={handleChange} className={inputClass} />
          </div>
          <div>
            <label className={labelClass}>Duration (minutes)</label>
            <input type="number" name="durationMinutes" min={5} max={480} required value={form.durationMinutes} onChange={handleChange} className={inputClass} />
          </div>
          <div>
            <label className={labelClass}>Capacity (students)</label>
            <input type="number" name="capacity" min={1} max={100} required value={form.capacity} onChange={handleChange} className={inputClass} />
          </div>
        </div>
        {error && <p className="text-sm text-brick font-medium">{error}</p>}
        <button type="submit" disabled={saving} className="px-5 py-2.5 bg-ink text-paper rounded-md font-medium hover:bg-ink/90 transition disabled:opacity-50">
          {saving ? 'Adding…' : 'Add Slot'}
        </button>
      </form>

      <div className="mt-8 max-w-2xl">
        {loading ? (
          <LoadingState />
        ) : slots.length === 0 ? (
          <EmptyState title="No interview slots yet" description="Add a slot above so shortlisted students can book." />
        ) : (
          <div className="bg-white border border-ink/10 rounded-lg divide-y divide-ink/10">
            {slots.map((slot) => (
              <div key={slot.id} className="px-5 py-4">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <p className="text-sm font-medium text-ink">{new Date(slot.startsAt).toLocaleString()}</p>
                    <p className="text-xs text-slate mt-0.5">
                      {slot.durationMinutes} min{slot.location && ` · ${slot.location}`}
                    </p>
                    <p className="text-xs font-mono text-slate/80 mt-1">
                      {slot.bookedCount} / {slot.capacity} booked · {slot.seatsLeft} left
                    </p>
                  </div>
                  {slot.bookedCount === 0 && (
                    <button onClick={() => handleDelete(slot.id)} className="p-1.5 text-slate hover:text-brick transition" title="Delete slot">
                      <Trash2 size={16} />
                    </button>
                  )}
                </div>
                {slot.bookings?.length > 0 && (
                  <ul className="mt-3 space-y-1.5">
                    {slot.bookings.map((b) => (
                      <li key={b.applicationId} className="flex items-center gap-2 text-sm text-ink">
                        {b.fullName || `Application #${b.applicationId}`}
                        <StatusBadge status={b.applicationStatus} />
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </DashboardLayout>
  );
}

import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import DashboardLayout from '../../components/DashboardLayout';
import LoadingState from '../../components/LoadingState';
import StatusBadge from '../../components/StatusBadge';
import {
  getApplicationDetails,
  getInterviewSlots,
  bookInterviewSlot,
  cancelInterviewBooking,
} from '../../api/studentApi';

function InterviewSection({ application, onChanged }) {
  const [slots, setSlots] = useState([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    // Slots are only offered while the application is in INTERVIEW and nothing is booked.
    if (application.interview || application.status !== 'INTERVIEW') return;
    getInterviewSlots(application.id)
      .then((res) => setSlots(res.data))
      .catch((err) => setError(err.response?.data?.error || 'Could not load interview slots'))
      .finally(() => setLoading(false));
  }, [application.id, application.status, application.interview]);

  const run = async (action) => {
    setBusy(true);
    setError('');
    try {
      await action();
    } catch (err) {
      // e.g. 409 SLOT_FULL when someone took the last seat a moment earlier.
      setError(err.response?.data?.error || 'Something went wrong. Please try again.');
    } finally {
      setBusy(false);
      onChanged();
    }
  };

  const booking = application.interview;

  return (
    <div className="bg-white border border-ink/10 rounded-lg p-8 max-w-xl mb-6">
      <h2 className="text-xs font-medium text-slate uppercase tracking-wide mb-4">Interview</h2>

      {booking ? (
        <div>
          <p className="text-sm text-ink">
            Booked for <span className="font-medium">{new Date(booking.startsAt).toLocaleString()}</span>
          </p>
          <p className="text-xs text-slate mt-1">
            {booking.durationMinutes} min{booking.location && ` · ${booking.location}`}
          </p>
          <button
            onClick={() => confirm('Cancel this interview booking?') && run(() => cancelInterviewBooking(application.id))}
            disabled={busy}
            className="mt-4 px-4 py-2 border border-brick/40 text-brick rounded-md text-sm font-medium hover:bg-brick/5 transition disabled:opacity-50"
          >
            Cancel booking
          </button>
        </div>
      ) : loading ? (
        <LoadingState />
      ) : slots.length === 0 ? (
        <p className="text-sm text-slate">No interview slots are available yet. Check back soon.</p>
      ) : (
        <div className="space-y-3">
          <p className="text-sm text-slate">Pick one slot:</p>
          {slots.map((slot) => (
            <div key={slot.id} className="flex items-center justify-between gap-3 border border-ink/10 rounded-md px-4 py-3">
              <div>
                <p className="text-sm font-medium text-ink">{new Date(slot.startsAt).toLocaleString()}</p>
                <p className="text-xs text-slate mt-0.5">
                  {slot.durationMinutes} min{slot.location && ` · ${slot.location}`} · {slot.seatsLeft} seat{slot.seatsLeft === 1 ? '' : 's'} left
                </p>
              </div>
              <button
                onClick={() => run(() => bookInterviewSlot(application.id, slot.id))}
                disabled={busy || slot.seatsLeft === 0}
                className="px-4 py-1.5 bg-ink text-paper rounded-md text-sm font-medium hover:bg-ink/90 transition disabled:opacity-40"
              >
                {slot.seatsLeft === 0 ? 'Full' : 'Book'}
              </button>
            </div>
          ))}
        </div>
      )}

      {error && <p className="text-sm text-brick font-medium mt-4">{error}</p>}
    </div>
  );
}

export default function ApplicationDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [application, setApplication] = useState(null);
  const [loading, setLoading] = useState(true);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    getApplicationDetails(id).then((res) => setApplication(res.data)).finally(() => setLoading(false));
  }, [id, reloadKey]);

  if (loading) return <DashboardLayout><LoadingState /></DashboardLayout>;
  if (!application) return <DashboardLayout><p className="text-slate">Application not found.</p></DashboardLayout>;

  return (
    <DashboardLayout>
      <button onClick={() => navigate('/student/applications')} className="text-sm text-teal hover:underline mb-6">
        ← Back to applications
      </button>

      <div className="flex items-start justify-between mb-8">
        <div>
          <h1 className="font-display text-2xl font-semibold text-ink">{application.jobTitle}</h1>
          <p className="text-sm text-slate mt-1">{application.companyName}</p>
        </div>
        <StatusBadge status={application.status} />
      </div>

      {(application.status === 'INTERVIEW' || application.interview) && (
        <InterviewSection application={application} onChanged={() => setReloadKey((k) => k + 1)} />
      )}

      <div className="bg-white border border-ink/10 rounded-lg p-8 max-w-xl">
        <h2 className="text-xs font-medium text-slate uppercase tracking-wide mb-6">Timeline</h2>

        <div className="space-y-0">
          {application.timeline.map((entry, i) => (
            <div key={i} className="flex gap-4">
              <div className="flex flex-col items-center">
                <div className="w-2.5 h-2.5 rounded-full bg-teal mt-1.5 shrink-0" />
                {i < application.timeline.length - 1 && (
                  <div className="w-px flex-1 bg-ink/10 my-1" />
                )}
              </div>
              <div className="pb-6">
                <p className="text-sm font-medium text-ink font-mono uppercase tracking-wide">
                  {entry.status}
                </p>
                {entry.remarks && (
                  <p className="text-sm text-slate mt-1">{entry.remarks}</p>
                )}
                <p className="text-xs font-mono text-slate/60 mt-1">
                  {new Date(entry.changedAt).toLocaleString()}
                </p>
              </div>
            </div>
          ))}
        </div>
      </div>
    </DashboardLayout>
  );
}

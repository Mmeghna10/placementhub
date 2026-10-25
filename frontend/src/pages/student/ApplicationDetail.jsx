import { useEffect, useRef, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import DashboardLayout from '../../components/DashboardLayout';
import LoadingState from '../../components/LoadingState';
import StatusBadge from '../../components/StatusBadge';
import CompanyAvatar from '../../components/CompanyAvatar';
import OfferCard from '../../components/OfferCard';
import { ArrowLeft, CalendarDays, History } from 'lucide-react';
import { calendarParts, formatDateTime } from '../../utils/jobFormat';
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

  // `disabled={busy}` only applies after a re-render, so a fast double-click on "Book"
  // would send two requests and the second would report the student's own seat as
  // "slot full". The ref blocks the second click immediately.
  const inFlight = useRef(false);

  const run = async (action) => {
    if (inFlight.current) return;
    inFlight.current = true;
    setBusy(true);
    setError('');
    try {
      await action();
    } catch (err) {
      // e.g. 409 SLOT_FULL when someone else took the last seat a moment earlier.
      setError(err.response?.data?.error || 'Something went wrong. Please try again.');
    } finally {
      setBusy(false);
      inFlight.current = false;
      onChanged();
    }
  };

  const booking = application.interview;

  return (
    <div className="bg-white border border-ink/[0.08] rounded-xl shadow-card p-6 sm:p-7">
      <h2 className="flex items-center gap-2 text-xs font-semibold text-slate uppercase tracking-wider mb-4">
        <CalendarDays size={15} className="text-teal" /> Interview
      </h2>

      {booking ? (
        <div>
          <p className="text-sm text-ink bg-emerald-50 border border-emerald-200 rounded-lg px-3 py-2">
            Booked for <span className="font-semibold">{formatDateTime(booking.startsAt)}</span>
          </p>
          <p className="text-xs text-slate mt-2">
            {booking.durationMinutes} min{booking.location && ` · ${booking.location}`}
          </p>
          <button
            onClick={() => confirm('Cancel this interview booking?') && run(() => cancelInterviewBooking(application.id))}
            disabled={busy}
            className="mt-4 px-4 py-2 border border-brick/40 text-brick rounded-lg text-sm font-medium hover:bg-brick/5 transition disabled:opacity-50"
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
            <div key={slot.id} className="flex items-center gap-4 border border-ink/10 rounded-lg px-4 py-3 hover:border-teal/30 transition">
              <div className="h-12 w-12 shrink-0 rounded-lg bg-teal/10 text-teal flex flex-col items-center justify-center leading-none" aria-hidden="true">
                <span className="text-lg font-semibold">{calendarParts(slot.startsAt).day}</span>
                <span className="text-[10px] uppercase tracking-wide mt-0.5">{calendarParts(slot.startsAt).month}</span>
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-ink">{formatDateTime(slot.startsAt)}</p>
                <p className="text-xs text-slate mt-0.5">
                  {slot.durationMinutes} min{slot.location && ` · ${slot.location}`} · {slot.seatsLeft} seat{slot.seatsLeft === 1 ? '' : 's'} left
                </p>
              </div>
              <button
                onClick={() => run(() => bookInterviewSlot(application.id, slot.id))}
                disabled={busy || slot.seatsLeft === 0}
                className="px-4 py-1.5 bg-ink text-paper rounded-lg text-sm font-medium hover:bg-ink/90 transition disabled:opacity-40"
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
      <button
        onClick={() => navigate('/student/applications')}
        className="inline-flex items-center gap-1.5 text-sm font-medium text-slate hover:text-teal mb-6 transition"
      >
        <ArrowLeft size={15} /> Back to applications
      </button>

      <div className="flex flex-col items-start sm:flex-row sm:items-center justify-between gap-4 mb-8">
        <div className="flex items-center gap-4">
          <CompanyAvatar name={application.companyName} size="lg" />
          <div>
            <h1 className="font-display text-[1.75rem] font-semibold text-ink tracking-tight leading-tight">{application.jobTitle}</h1>
            <p className="text-sm text-slate mt-0.5">{application.companyName}</p>
          </div>
        </div>
        <StatusBadge status={application.status} />
      </div>

      <OfferCard application={application} onChanged={() => setReloadKey((k) => k + 1)} />

      <div className="grid gap-6 lg:grid-cols-2 items-start">

      {(application.status === 'INTERVIEW' || application.interview) && (
        <InterviewSection application={application} onChanged={() => setReloadKey((k) => k + 1)} />
      )}

      <div className="bg-white border border-ink/[0.08] rounded-xl shadow-card p-6 sm:p-7 lg:order-first">
        <h2 className="flex items-center gap-2 text-xs font-semibold text-slate uppercase tracking-wider mb-6">
          <History size={15} className="text-teal" /> Timeline
        </h2>

        <div className="space-y-0">
          {application.timeline.map((entry, i) => (
            <div key={i} className="flex gap-4">
              <div className="flex flex-col items-center">
                <div className="w-3 h-3 rounded-full bg-teal ring-4 ring-teal/10 mt-1.5 shrink-0" />
                {i < application.timeline.length - 1 && (
                  <div className="w-px flex-1 bg-ink/10 my-1" />
                )}
              </div>
              <div className="pb-6">
                <StatusBadge status={entry.status} />
                {entry.remarks && (
                  <p className="text-sm text-ink/80 mt-2">{entry.remarks}</p>
                )}
                <p className="text-xs text-slate/70 mt-1">
                  {formatDateTime(entry.changedAt)}
                </p>
              </div>
            </div>
          ))}
        </div>
      </div>
      </div>
    </DashboardLayout>
  );
}

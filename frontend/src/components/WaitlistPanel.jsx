import { useRef, useState } from 'react';
import { ArrowDown, ArrowUp, ListOrdered, Sparkles } from 'lucide-react';
import { fillFromWaitlist, reorderWaitlist } from '../api/recruiterApi';

/**
 * A job's ranked waitlist. When an offer is declined or expires, the server offers the
 * opening to the first free candidate here automatically; "Fill open openings" does the
 * same on demand (e.g. after raising the openings).
 */
export default function WaitlistPanel({ jobId, job, applicants, onChanged }) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const inFlight = useRef(false);

  const waitlist = applicants
    .filter((a) => a.status === 'WAITLISTED')
    .sort((a, b) => (a.waitlistRank ?? 0) - (b.waitlistRank ?? 0) || a.id - b.id);
  if (waitlist.length === 0) return null;

  const free = job ? job.openings - job.offersPending - job.offersAccepted : 0;

  const run = async (action) => {
    if (inFlight.current) return;
    inFlight.current = true;
    setBusy(true);
    setError('');
    try {
      await action();
    } catch (err) {
      // e.g. 409 WAITLIST_CHANGED when someone was promoted while the page was open.
      setError(err.response?.data?.error || 'Could not update the waitlist');
    } finally {
      setBusy(false);
      inFlight.current = false;
      onChanged();
    }
  };

  const move = (index, delta) => {
    const ids = waitlist.map((a) => a.id);
    [ids[index], ids[index + delta]] = [ids[index + delta], ids[index]];
    run(() => reorderWaitlist(jobId, ids));
  };

  return (
    <section aria-label="Waitlist" className="mt-8 bg-white border border-ink/[0.08] rounded-xl shadow-card p-5 sm:p-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h2 className="flex items-center gap-2 font-display text-lg font-semibold text-ink">
            <ListOrdered size={18} className="text-teal" /> Waitlist
          </h2>
          <p className="text-sm text-slate mt-1">
            If an offer is declined or expires, the next free candidate here gets it automatically.
          </p>
        </div>
        <button
          onClick={() => run(() => fillFromWaitlist(jobId))}
          disabled={busy || free <= 0}
          title={free <= 0 ? 'No free openings' : undefined}
          className="inline-flex items-center gap-2 self-start px-4 py-2 bg-ink text-paper rounded-lg text-sm font-medium hover:bg-ink/90 transition disabled:opacity-40"
        >
          <Sparkles size={15} />
          Fill {free > 0 ? free : 'no'} open opening{free === 1 ? '' : 's'}
        </button>
      </div>

      <ol className="mt-4 divide-y divide-ink/[0.06]">
        {waitlist.map((app, index) => (
          <li key={app.id} className="flex items-center gap-3 py-2.5">
            <span className="h-7 w-7 shrink-0 rounded-full bg-teal/10 text-teal text-xs font-semibold flex items-center justify-center">
              {index + 1}
            </span>
            <span className="flex-1 text-sm text-ink">{app.applicant?.fullName || 'Unnamed student'}</span>
            <button
              onClick={() => move(index, -1)}
              disabled={busy || index === 0}
              aria-label={`Move ${app.applicant?.fullName} up`}
              className="p-1.5 rounded-md text-slate hover:text-teal hover:bg-teal/5 disabled:opacity-30"
            >
              <ArrowUp size={16} />
            </button>
            <button
              onClick={() => move(index, 1)}
              disabled={busy || index === waitlist.length - 1}
              aria-label={`Move ${app.applicant?.fullName} down`}
              className="p-1.5 rounded-md text-slate hover:text-teal hover:bg-teal/5 disabled:opacity-30"
            >
              <ArrowDown size={16} />
            </button>
          </li>
        ))}
      </ol>
      {error && <p role="alert" className="mt-3 text-sm text-brick font-medium">{error}</p>}
    </section>
  );
}

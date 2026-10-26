import { useEffect, useRef, useState } from 'react';
import { CheckCircle2, Clock, Gift, ListOrdered, XCircle } from 'lucide-react';
import { acceptOffer, declineOffer } from '../api/studentApi';
import { formatDateTime, timeLeft } from '../utils/jobFormat';

/** Re-renders every `ms` so the countdown stays current while the page is open. */
function useNow(ms) {
  const [now, setNow] = useState(() => new Date());
  useEffect(() => {
    const id = setInterval(() => setNow(new Date()), ms);
    return () => clearInterval(id);
  }, [ms]);
  return now;
}

/**
 * The student's offer: accept or decline before the deadline, or the outcome once decided.
 * Renders nothing for applications that never reached an offer.
 */
export default function OfferCard({ application, onChanged }) {
  const now = useNow(30_000);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  // Same double-click guard as booking: `disabled` only applies after a re-render.
  const inFlight = useRef(false);

  const run = async (action) => {
    if (inFlight.current) return;
    inFlight.current = true;
    setBusy(true);
    setError('');
    try {
      await action();
    } catch (err) {
      // e.g. 422 OFFER_EXPIRED if the deadline passed while the page was open.
      setError(err.response?.data?.error || 'Something went wrong. Please try again.');
    } finally {
      setBusy(false);
      inFlight.current = false;
      onChanged();
    }
  };

  const { status, companyName, offerExpiresAt } = application;

  if (status === 'OFFERED') {
    const left = timeLeft(offerExpiresAt, now);
    return (
      <section
        aria-label="Your offer"
        className="mb-6 rounded-2xl border border-emerald-200 bg-gradient-to-br from-emerald-50 to-white p-6 sm:p-7 shadow-card"
      >
        <div className="flex flex-col sm:flex-row sm:items-center gap-5 justify-between">
          <div className="flex items-start gap-4">
            <div className="h-12 w-12 shrink-0 rounded-xl bg-emerald-600 text-white flex items-center justify-center">
              <Gift size={22} />
            </div>
            <div>
              <h2 className="font-display text-xl font-semibold text-ink">You have an offer from {companyName}!</h2>
              <p className="mt-1 text-sm text-slate flex flex-wrap items-center gap-x-2">
                <Clock size={14} className="text-amber-600" />
                Decide by <span className="font-medium text-ink">{formatDateTime(offerExpiresAt)}</span>
                {left && <span className="text-amber-700 font-medium">· {left}</span>}
              </p>
              <p className="mt-2 text-xs text-slate">
                Accepting places you with this company. If you don't reply in time, the offer expires and the opening goes to someone else.
              </p>
            </div>
          </div>
          <div className="flex gap-3 shrink-0">
            <button
              onClick={() => run(() => acceptOffer(application.id))}
              disabled={busy || !left}
              className="px-5 py-2.5 bg-emerald-600 text-white rounded-lg text-sm font-semibold hover:bg-emerald-700 transition disabled:opacity-50 shadow-sm"
            >
              Accept offer
            </button>
            <button
              onClick={() => confirm(`Decline the offer from ${companyName}? This cannot be undone.`) && run(() => declineOffer(application.id))}
              disabled={busy}
              className="px-5 py-2.5 border border-ink/15 bg-white text-ink rounded-lg text-sm font-medium hover:border-brick/40 hover:text-brick transition disabled:opacity-50"
            >
              Decline
            </button>
          </div>
        </div>
        {error && <p role="alert" className="mt-4 text-sm text-brick font-medium">{error}</p>}
      </section>
    );
  }

  if (status === 'WAITLISTED') {
    return (
      <p className="mb-6 flex items-start gap-3 rounded-xl border border-sky-200 bg-sky-50 px-5 py-4 text-sm text-sky-900">
        <ListOrdered size={18} className="shrink-0 mt-0.5" />
        <span>
          {application.waitlistPosition ? (
            <>You're <span className="font-semibold">#{application.waitlistPosition}</span> on the waitlist at {companyName}.</>
          ) : (
            <>You're on the waitlist at {companyName}.</>
          )}{' '}
          If an offer is declined or expires, the opening goes to the next person automatically, and you'll see the offer here.
        </span>
      </p>
    );
  }

  const outcomes = {
    ACCEPTED: {
      icon: CheckCircle2,
      style: 'border-emerald-200 bg-emerald-50 text-emerald-800',
      text: `You accepted the offer from ${companyName}. Congratulations, you're placed!`,
    },
    DECLINED: {
      icon: XCircle,
      style: 'border-ink/10 bg-white text-slate',
      text: 'You declined this offer.',
    },
    EXPIRED: {
      icon: Clock,
      style: 'border-amber-200 bg-amber-50 text-amber-800',
      text: 'This offer expired without a reply, and the opening was released.',
    },
  };
  const outcome = outcomes[status];
  if (!outcome) return null;
  const Icon = outcome.icon;
  return (
    <p className={`mb-6 flex items-center gap-3 rounded-xl border px-5 py-4 text-sm font-medium ${outcome.style}`}>
      <Icon size={18} className="shrink-0" /> {outcome.text}
    </p>
  );
}

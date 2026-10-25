/** Openings for a job: filled (accepted), offered (waiting for a reply) and free. */
export default function SeatMeter({ job, compact = false }) {
  const openings = job.openings ?? 1;
  const accepted = job.offersAccepted ?? 0;
  const pending = job.offersPending ?? 0;
  const pct = (n) => `${Math.min(100, (n / openings) * 100)}%`;

  return (
    <div className={compact ? 'w-40' : 'w-full max-w-xs'} title={`${accepted} accepted, ${pending} pending, ${openings} openings`}>
      <div className="flex justify-between text-xs">
        <span className="font-medium text-ink">
          {accepted} of {openings} filled
        </span>
        {pending > 0 && <span className="text-amber-700">{pending} pending</span>}
      </div>
      <div className="mt-1 h-1.5 rounded-full bg-ink/10 overflow-hidden flex" aria-hidden="true">
        <div className="bg-emerald-500" style={{ width: pct(accepted) }} />
        <div className="bg-amber-400" style={{ width: pct(pending) }} />
      </div>
    </div>
  );
}

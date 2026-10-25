const statusStyles = {
  APPLIED: { pill: 'bg-teal/10 text-teal ring-teal/20', dot: 'bg-teal' },
  SHORTLISTED: { pill: 'bg-brass/15 text-[#7d6238] ring-brass/30', dot: 'bg-brass' },
  INTERVIEW: { pill: 'bg-indigo-50 text-indigo-700 ring-indigo-200', dot: 'bg-indigo-500' },
  OFFERED: { pill: 'bg-emerald-50 text-emerald-700 ring-emerald-200', dot: 'bg-emerald-500' },
  REJECTED: { pill: 'bg-brick/10 text-brick ring-brick/20', dot: 'bg-brick' },
  ACCEPTED: { pill: 'bg-emerald-600 text-white ring-emerald-600', dot: 'bg-white' },
  DECLINED: { pill: 'bg-ink/5 text-slate ring-ink/10', dot: 'bg-slate' },
  EXPIRED: { pill: 'bg-amber-50 text-amber-800 ring-amber-200', dot: 'bg-amber-500' },
  PENDING: { pill: 'bg-amber-50 text-amber-700 ring-amber-200', dot: 'bg-amber-500' },
  APPROVED: { pill: 'bg-emerald-50 text-emerald-700 ring-emerald-200', dot: 'bg-emerald-500' },
  CLOSED: { pill: 'bg-ink/5 text-slate ring-ink/10', dot: 'bg-slate' },
};

const fallback = { pill: 'bg-slate/10 text-slate ring-slate/20', dot: 'bg-slate' };

export default function StatusBadge({ status }) {
  const style = statusStyles[status] || fallback;
  return (
    <span
      className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium ring-1 ring-inset whitespace-nowrap ${style.pill}`}
    >
      <span className={`h-1.5 w-1.5 rounded-full ${style.dot}`} aria-hidden="true" />
      {/* The text stays the API value (e.g. SHORTLISTED); CSS shows it as "Shortlisted". */}
      <span className="lowercase first-letter:uppercase">{status}</span>
    </span>
  );
}

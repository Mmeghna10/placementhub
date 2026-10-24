const tones = {
  teal: 'bg-teal/10 text-teal',
  brass: 'bg-brass/15 text-[#7d6238]',
  indigo: 'bg-indigo-50 text-indigo-600',
  emerald: 'bg-emerald-50 text-emerald-600',
  amber: 'bg-amber-50 text-amber-600',
  slate: 'bg-ink/5 text-slate',
};

/** A dashboard number with a label, an icon and an optional hint underneath. */
export default function StatCard({ label, value, icon: Icon, tone = 'teal', hint, highlight = false }) {
  return (
    <div
      className={`rounded-xl p-5 border shadow-card transition hover:shadow-card-hover ${
        highlight ? 'bg-amber-50/60 border-amber-200' : 'bg-white border-ink/[0.08]'
      }`}
    >
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-xs font-medium uppercase tracking-wider text-slate">{label}</p>
          <p className="font-display text-3xl font-semibold text-ink mt-2 tabular-nums">{value}</p>
        </div>
        {Icon && (
          <div className={`h-10 w-10 shrink-0 rounded-lg flex items-center justify-center ${tones[tone]}`}>
            <Icon size={19} strokeWidth={1.9} />
          </div>
        )}
      </div>
      {hint && <p className="text-xs text-slate mt-3">{hint}</p>}
    </div>
  );
}

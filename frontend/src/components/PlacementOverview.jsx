import { useEffect, useState } from 'react';
import { Building2, Clock, Sparkles, TrendingUp } from 'lucide-react';
import { getPlacementOverview } from '../api/adminApi';
import TierBadge from './TierBadge';

const percent = (rate) => (rate == null ? '–' : `${Math.round(rate * 100)}%`);

const outcomeLabels = [
  ['OFFERED', 'Pending', 'bg-amber-400'],
  ['ACCEPTED', 'Accepted', 'bg-emerald-500'],
  ['DECLINED', 'Declined', 'bg-slate'],
  ['EXPIRED', 'Expired', 'bg-amber-600'],
  ['RELEASED', 'Released (upgraded)', 'bg-violet-500'],
  ['WITHDRAWN', 'Withdrawn by rules', 'bg-ink/30'],
];

/** The placement cell's view of the season (Offer Desk phase 4). */
export default function PlacementOverview() {
  const [data, setData] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    getPlacementOverview()
      .then((res) => setData(res.data))
      .catch((err) => setError(err.response?.data?.error || 'Could not load the placement overview'));
  }, []);

  if (error) return <p role="alert" className="mt-8 text-sm text-brick font-medium">{error}</p>;
  if (!data) return null;

  const maxTier = Math.max(1, ...Object.values(data.placedByTier));
  const totalOffers = Object.values(data.offers).reduce((a, b) => a + b, 0);

  return (
    <section aria-label="Placement overview" className="mt-10">
      <h2 className="font-display text-lg font-semibold text-ink mb-4">Placement season</h2>
      <div className="grid gap-4 lg:grid-cols-3">
        <div className="rounded-xl border border-ink/[0.08] bg-ink text-paper p-6 shadow-card">
          <p className="text-xs font-medium uppercase tracking-wider text-paper/60">Placement rate</p>
          <p className="mt-2 font-display text-5xl font-semibold">{percent(data.placementRate)}</p>
          <p className="mt-2 text-sm text-paper/70">
            {data.placedStudents} of {data.students} students placed
          </p>
          <div className="mt-4 h-2 rounded-full bg-paper/15 overflow-hidden" aria-hidden="true">
            <div className="h-full bg-brass" style={{ width: `${Math.round((data.placementRate ?? 0) * 100)}%` }} />
          </div>
          <dl className="mt-5 grid grid-cols-2 gap-3 text-sm">
            <div>
              <dt className="text-paper/55 flex items-center gap-1"><TrendingUp size={13} /> Openings filled</dt>
              <dd className="font-semibold">{data.filled} / {data.openings}</dd>
            </div>
            <div>
              <dt className="text-paper/55 flex items-center gap-1"><Clock size={13} /> Avg. time to decide</dt>
              <dd className="font-semibold">{data.avgHoursToDecide == null ? '–' : `${data.avgHoursToDecide} h`}</dd>
            </div>
            <div>
              <dt className="text-paper/55">Acceptance rate</dt>
              <dd className="font-semibold">{percent(data.acceptanceRate)}</dd>
            </div>
            <div>
              <dt className="text-paper/55 flex items-center gap-1"><Sparkles size={13} /> Auto-refills</dt>
              <dd className="font-semibold">{data.autoRefills}</dd>
            </div>
          </dl>
        </div>

        <div className="rounded-xl border border-ink/[0.08] bg-white p-6 shadow-card">
          <p className="text-xs font-semibold uppercase tracking-wider text-slate">Placed by tier</p>
          <ul className="mt-4 space-y-4">
            {Object.entries(data.placedByTier).map(([tier, count]) => (
              <li key={tier}>
                <div className="flex items-center justify-between text-sm">
                  <TierBadge tier={tier} />
                  <span className="font-semibold text-ink tabular-nums">{count}</span>
                </div>
                <div className="mt-1.5 h-2 rounded-full bg-ink/5 overflow-hidden" aria-hidden="true">
                  <div className="h-full bg-teal" style={{ width: `${(count / maxTier) * 100}%` }} />
                </div>
              </li>
            ))}
          </ul>

          <p className="mt-6 text-xs font-semibold uppercase tracking-wider text-slate flex items-center gap-1">
            <Building2 size={13} /> Top hiring companies
          </p>
          {data.topCompanies.length === 0 ? (
            <p className="mt-2 text-sm text-slate">No accepted offers yet.</p>
          ) : (
            <ol className="mt-2 space-y-1.5 text-sm">
              {data.topCompanies.map((c) => (
                <li key={c.company} className="flex justify-between">
                  <span className="text-ink">{c.company}</span>
                  <span className="font-semibold tabular-nums">{c.hires}</span>
                </li>
              ))}
            </ol>
          )}
        </div>

        <div className="rounded-xl border border-ink/[0.08] bg-white p-6 shadow-card">
          <p className="text-xs font-semibold uppercase tracking-wider text-slate">Offer outcomes</p>
          {totalOffers > 0 && (
            <div className="mt-4 flex h-3 rounded-full overflow-hidden" aria-hidden="true">
              {outcomeLabels.map(([status, , color]) =>
                data.offers[status] > 0 ? (
                  <div key={status} className={color} style={{ width: `${(data.offers[status] / totalOffers) * 100}%` }} />
                ) : null,
              )}
            </div>
          )}
          <ul className="mt-4 space-y-2 text-sm">
            {outcomeLabels.map(([status, label, color]) => (
              <li key={status} className="flex items-center justify-between">
                <span className="flex items-center gap-2 text-ink">
                  <span className={`h-2.5 w-2.5 rounded-full ${color}`} aria-hidden="true" /> {label}
                </span>
                <span className="font-semibold tabular-nums">{data.offers[status] ?? 0}</span>
              </li>
            ))}
          </ul>
        </div>
      </div>
    </section>
  );
}

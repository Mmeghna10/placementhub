import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Activity, CheckCircle2, Clock, Gift, ListOrdered, Percent, Sparkles, Users } from 'lucide-react';
import DashboardLayout from '../../components/DashboardLayout';
import LoadingState from '../../components/LoadingState';
import EmptyState from '../../components/EmptyState';
import StatCard from '../../components/StatCard';
import StatusBadge from '../../components/StatusBadge';
import SeatMeter from '../../components/SeatMeter';
import TierBadge from '../../components/TierBadge';
import { getOfferDesk } from '../../api/recruiterApi';
import { formatDateTime, timeLeft } from '../../utils/jobFormat';

const percent = (rate) => (rate == null ? '–' : `${Math.round(rate * 100)}%`);

/** Offer Desk: every opening, pending offer and waitlist across the recruiter's jobs. */
export default function OfferDesk() {
  const [desk, setDesk] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    getOfferDesk()
      .then((res) => setDesk(res.data))
      .catch((err) => setError(err.response?.data?.error || 'Could not load the Offer Desk'));
  }, []);

  if (error) return <DashboardLayout><p role="alert" className="text-sm text-brick font-medium">{error}</p></DashboardLayout>;
  if (!desk) return <DashboardLayout><LoadingState /></DashboardLayout>;

  const { totals, jobs, recentEvents } = desk;
  const now = new Date();

  return (
    <DashboardLayout>
      <h1 className="font-display text-[1.75rem] font-semibold text-ink tracking-tight">Offer Desk</h1>
      <div className="mt-2 h-1 w-10 rounded-full bg-brass/70" />
      <p className="mt-3 text-sm text-slate max-w-2xl">
        Every opening, offer and waitlist in one place. Declined or expired offers refill from your waitlist automatically.
      </p>

      <div className="mt-8 grid grid-cols-2 lg:grid-cols-5 gap-4">
        <StatCard label="Filled" value={`${totals.filled} / ${totals.openings}`} icon={CheckCircle2} tone="emerald" hint="Accepted of all openings" />
        <StatCard label="Offers pending" value={totals.pendingOffers} icon={Gift} tone="amber" />
        <StatCard label="Waitlisted" value={totals.waitlisted} icon={ListOrdered} tone="indigo" />
        <StatCard label="Acceptance rate" value={percent(totals.acceptanceRate)} icon={Percent} tone="teal" hint="Accepted of decided offers" />
        <StatCard label="Auto-refills" value={totals.autoRefills} icon={Sparkles} tone="brass" hint="Offers made from a waitlist" />
      </div>

      <div className="mt-10 grid gap-6 lg:grid-cols-[1.6fr_1fr] items-start">
        <section aria-label="Jobs">
          <h2 className="font-display text-lg font-semibold text-ink mb-4">Your jobs</h2>
          {jobs.length === 0 ? (
            <EmptyState icon={Users} title="No jobs yet" description="Post a job to start making offers." />
          ) : (
            <div className="space-y-4">
              {jobs.map((job) => (
                <article key={job.jobId} data-testid="desk-job" className="bg-white border border-ink/[0.08] rounded-xl shadow-card p-5">
                  <div className="flex flex-wrap items-start justify-between gap-3">
                    <div>
                      <Link to={`/recruiter/jobs/${job.jobId}/applicants`} className="font-semibold text-ink hover:text-teal">
                        {job.title}
                      </Link>
                      <div className="mt-1 flex items-center gap-2">
                        <TierBadge tier={job.tier} />
                        <StatusBadge status={job.status} />
                      </div>
                    </div>
                    <SeatMeter job={{ openings: job.openings, offersAccepted: job.filled, offersPending: job.pendingOffers }} compact />
                  </div>

                  {job.offers.length > 0 && (
                    <ul className="mt-4 divide-y divide-ink/[0.06] rounded-lg bg-paper/60">
                      {job.offers.map((offer) => (
                        <li key={offer.applicationId} className="flex items-center justify-between gap-3 px-3 py-2 text-sm">
                          <span className="text-ink">{offer.studentName}</span>
                          <span className="flex items-center gap-1 text-xs text-amber-700">
                            <Clock size={13} /> {timeLeft(offer.expiresAt, now) ?? 'expiring now'}
                          </span>
                        </li>
                      ))}
                    </ul>
                  )}
                  {job.waitlisted > 0 && (
                    <p className="mt-3 text-xs text-slate">
                      {job.waitlisted} on the waitlist, ready if an offer is declined or expires.
                    </p>
                  )}
                </article>
              ))}
            </div>
          )}
        </section>

        <section aria-label="Recent activity" className="bg-white border border-ink/[0.08] rounded-xl shadow-card p-5">
          <h2 className="flex items-center gap-2 font-display text-lg font-semibold text-ink mb-4">
            <Activity size={18} className="text-teal" /> Recent activity
          </h2>
          {recentEvents.length === 0 ? (
            <p className="text-sm text-slate">No offer activity yet.</p>
          ) : (
            <ol className="space-y-3">
              {recentEvents.map((event, i) => (
                <li key={`${event.applicationId}-${event.status}-${i}`} className="text-sm">
                  <div className="flex items-center gap-2">
                    <StatusBadge status={event.status} />
                    <span className="font-medium text-ink truncate">{event.studentName}</span>
                  </div>
                  <p className="mt-1 text-xs text-slate">
                    {event.jobTitle} · {formatDateTime(event.at)}
                    {event.remarks && <> · {event.remarks}</>}
                  </p>
                </li>
              ))}
            </ol>
          )}
        </section>
      </div>
    </DashboardLayout>
  );
}

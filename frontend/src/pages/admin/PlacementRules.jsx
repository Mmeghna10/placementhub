import { useEffect, useState } from 'react';
import { Scale } from 'lucide-react';
import DashboardLayout from '../../components/DashboardLayout';
import LoadingState from '../../components/LoadingState';
import TierBadge from '../../components/TierBadge';
import { getPlacementPolicy, updatePlacementPolicy } from '../../api/adminApi';

/** The campus placement rules: whether placed students may upgrade, and how far. */
export default function PlacementRules() {
  const [policy, setPolicy] = useState(null);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    getPlacementPolicy()
      .then((res) => setPolicy(res.data))
      .catch((err) => setError(err.response?.data?.error || 'Could not load the placement rules'));
  }, []);

  const save = async (e) => {
    e.preventDefault();
    setSaving(true);
    setMessage('');
    setError('');
    try {
      const res = await updatePlacementPolicy({ ...policy, maxAcceptedOffers: Number(policy.maxAcceptedOffers) });
      setPolicy(res.data);
      setMessage('Placement rules saved. They apply to every new offer and application.');
    } catch (err) {
      setError(err.response?.data?.error || 'Could not save the placement rules');
    } finally {
      setSaving(false);
    }
  };

  if (!policy && !error) return <DashboardLayout><LoadingState /></DashboardLayout>;

  return (
    <DashboardLayout>
      <h1 className="font-display text-[1.75rem] font-semibold text-ink tracking-tight">Placement rules</h1>
      <div className="mt-2 h-1 w-10 rounded-full bg-brass/70" />
      <p className="mt-3 text-sm text-slate max-w-2xl">
        Every approved job has a tier: <TierBadge tier="REGULAR" /> <TierBadge tier="DREAM" /> <TierBadge tier="SUPER_DREAM" />.
        These rules decide what a student can still do once they have accepted an offer.
      </p>

      {policy && (
        <form onSubmit={save} className="mt-8 max-w-2xl bg-white border border-ink/[0.08] rounded-xl shadow-card p-6 space-y-6">
          <label className="flex items-start gap-3 cursor-pointer">
            <input
              type="checkbox"
              checked={policy.allowUpgrades}
              onChange={(e) => setPolicy({ ...policy, allowUpgrades: e.target.checked })}
              className="mt-1 h-4 w-4 accent-teal"
            />
            <span>
              <span className="block text-sm font-semibold text-ink">Allow upgrades to a higher tier</span>
              <span className="block text-sm text-slate mt-0.5">
                A placed student may still apply to, and accept offers from, higher-tier jobs only. Accepting one releases the
                earlier placement, and that company's opening goes to its waitlist automatically. When off: one student, one job.
              </span>
            </span>
          </label>

          <div className={policy.allowUpgrades ? '' : 'opacity-50'}>
            <label htmlFor="max-accepted" className="block text-sm font-semibold text-ink">
              Maximum offers a student may accept
            </label>
            <input
              id="max-accepted"
              type="number"
              min="1"
              max="5"
              required
              disabled={!policy.allowUpgrades}
              value={policy.maxAcceptedOffers}
              onChange={(e) => setPolicy({ ...policy, maxAcceptedOffers: e.target.value })}
              className="mt-1.5 w-28 px-3.5 py-2 bg-paper border border-ink/15 rounded-lg text-ink focus:outline-none focus:ring-2 focus:ring-teal/40"
            />
            <p className="text-xs text-slate mt-1">Including the ones given up for an upgrade. 2 means one placement and one upgrade.</p>
          </div>

          <div className="flex items-center gap-4">
            <button
              type="submit"
              disabled={saving}
              className="inline-flex items-center gap-2 px-5 py-2.5 bg-ink text-paper rounded-lg text-sm font-medium hover:bg-ink/90 transition disabled:opacity-50"
            >
              <Scale size={16} /> {saving ? 'Saving…' : 'Save rules'}
            </button>
            {message && <p role="status" className="text-sm text-emerald-700">{message}</p>}
          </div>
        </form>
      )}
      {error && <p role="alert" className="mt-4 text-sm text-brick font-medium">{error}</p>}
    </DashboardLayout>
  );
}

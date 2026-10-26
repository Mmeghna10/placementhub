import { Crown, Gem, Star } from 'lucide-react';
import { tierLabel } from '../utils/tiers';

const styles = {
  REGULAR: { icon: Star, className: 'bg-ink/5 text-slate ring-ink/10' },
  DREAM: { icon: Gem, className: 'bg-violet-50 text-violet-700 ring-violet-200' },
  SUPER_DREAM: { icon: Crown, className: 'bg-amber-50 text-amber-800 ring-amber-300' },
};

/** A job's placement tier, set by the placement cell. */
export default function TierBadge({ tier }) {
  if (!tier) return null;
  const { icon: Icon, className } = styles[tier] ?? styles.REGULAR;
  return (
    <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-xs font-medium ring-1 ring-inset ${className}`}>
      <Icon size={12} aria-hidden="true" />
      {tierLabel(tier)}
    </span>
  );
}

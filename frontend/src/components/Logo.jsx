import { GraduationCap } from 'lucide-react';

/** Brand mark + wordmark. `tone="light"` for dark backgrounds. */
export default function Logo({ tone = 'dark', className = '' }) {
  const light = tone === 'light';
  return (
    <span className={`inline-flex items-center gap-2.5 ${className}`}>
      <span
        className={`h-8 w-8 rounded-lg flex items-center justify-center ${
          light ? 'bg-brass text-ink' : 'bg-ink text-brass'
        }`}
        aria-hidden="true"
      >
        <GraduationCap size={18} strokeWidth={2} />
      </span>
      <span className={`font-display text-lg font-semibold tracking-tight ${light ? 'text-paper' : 'text-ink'}`}>
        PlacementHub
      </span>
    </span>
  );
}

// A stable colour per name, so the same company always gets the same tile.
const palettes = [
  'bg-teal/10 text-teal',
  'bg-brass/15 text-[#7d6238]',
  'bg-indigo-50 text-indigo-600',
  'bg-emerald-50 text-emerald-700',
  'bg-rose-50 text-rose-600',
  'bg-sky-50 text-sky-700',
];

function initials(name = '') {
  const words = name.trim().split(/\s+/).filter(Boolean);
  if (words.length === 0) return '?';
  return (words[0][0] + (words[1]?.[0] ?? '')).toUpperCase();
}

function paletteFor(name = '') {
  let hash = 0;
  for (const ch of name) hash = (hash * 31 + ch.charCodeAt(0)) >>> 0;
  return palettes[hash % palettes.length];
}

export default function CompanyAvatar({ name, size = 'md' }) {
  const dimensions = size === 'lg' ? 'h-14 w-14 text-lg rounded-xl' : size === 'sm' ? 'h-8 w-8 text-xs rounded-md' : 'h-11 w-11 text-sm rounded-lg';
  return (
    <div
      className={`${dimensions} ${paletteFor(name)} shrink-0 flex items-center justify-center font-semibold tracking-wide`}
      aria-hidden="true"
    >
      {initials(name)}
    </div>
  );
}

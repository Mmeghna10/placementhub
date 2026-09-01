export default function EmptyState({ title, description }) {
  return (
    <div className="py-16 text-center border border-dashed border-ink/15 rounded-lg">
      <p className="font-display text-lg text-ink font-semibold">{title}</p>
      {description && <p className="text-sm text-slate mt-1">{description}</p>}
    </div>
  );
}
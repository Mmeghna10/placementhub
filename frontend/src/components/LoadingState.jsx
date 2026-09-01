export default function LoadingState({ label = 'Loading…' }) {
  return (
    <div className="py-16 text-center text-slate text-sm font-mono">
      {label}
    </div>
  );
}
export default function LoadingState({ label = 'Loading…' }) {
  return (
    // role="status": screen readers announce the loading message.
    <div role="status" className="py-16 text-center text-slate text-sm font-mono">
      {label}
    </div>
  );
}

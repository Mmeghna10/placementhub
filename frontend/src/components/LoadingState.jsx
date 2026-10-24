export default function LoadingState({ label = 'Loading…' }) {
  return (
    // role="status": screen readers announce the loading message.
    <div role="status" className="py-16 flex flex-col items-center gap-3 text-slate text-sm">
      <span
        className="h-7 w-7 rounded-full border-2 border-teal/20 border-t-teal animate-spin"
        aria-hidden="true"
      />
      {label}
    </div>
  );
}

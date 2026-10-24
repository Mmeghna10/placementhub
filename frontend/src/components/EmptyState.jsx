import { Inbox } from 'lucide-react';

export default function EmptyState({ title, description, icon: Icon = Inbox, action }) {
  return (
    <div className="py-14 px-6 text-center bg-white/60 border border-dashed border-ink/15 rounded-xl">
      <div className="mx-auto mb-4 h-12 w-12 rounded-full bg-teal/10 text-teal flex items-center justify-center">
        <Icon size={22} strokeWidth={1.75} />
      </div>
      <p className="font-display text-lg text-ink font-semibold">{title}</p>
      {description && <p className="text-sm text-slate mt-1 max-w-sm mx-auto">{description}</p>}
      {action && <div className="mt-5">{action}</div>}
    </div>
  );
}

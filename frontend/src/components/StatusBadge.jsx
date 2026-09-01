const statusStyles = {
  APPLIED: 'bg-teal/10 text-teal',
  SHORTLISTED: 'bg-brass/15 text-brass',
  INTERVIEW: 'bg-brass/15 text-brass',
  OFFERED: 'bg-green-100 text-green-700',
  REJECTED: 'bg-brick/10 text-brick',
  PENDING: 'bg-slate/10 text-slate',
  APPROVED: 'bg-teal/10 text-teal',
};

export default function StatusBadge({ status }) {
  const style = statusStyles[status] || 'bg-slate/10 text-slate';
  return (
    <span className={`inline-block px-2.5 py-1 rounded-full text-xs font-medium font-mono uppercase tracking-wide ${style}`}>
      {status}
    </span>
  );
}
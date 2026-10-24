import { BriefcaseBusiness, CalendarClock, IndianRupee, MapPin } from 'lucide-react';
import { deadlineHint, formatDate, formatJobType, formatSalary } from '../utils/jobFormat';

function Chip({ icon: Icon, children, className = 'bg-paper text-slate' }) {
  return (
    <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-xs font-medium ${className}`}>
      {Icon && <Icon size={13} strokeWidth={2} aria-hidden="true" />}
      {children}
    </span>
  );
}

/** Location, type, salary and deadline as chips, plus a "closes in N days" hint. */
export default function JobMeta({ job, showDeadlineHint = true }) {
  const salary = formatSalary(job.salaryMin, job.salaryMax);
  const hint = showDeadlineHint && job.acceptingApplications !== false ? deadlineHint(job.applicationDeadline) : null;
  return (
    <div className="flex flex-wrap items-center gap-2">
      {job.location && <Chip icon={MapPin}>{job.location}</Chip>}
      {job.jobType && <Chip icon={BriefcaseBusiness}>{formatJobType(job.jobType)}</Chip>}
      {salary && <Chip icon={IndianRupee}>{salary.replace('₹', '')}</Chip>}
      {job.applicationDeadline && <Chip icon={CalendarClock}>Apply by {formatDate(job.applicationDeadline)}</Chip>}
      {hint && (
        <Chip className={hint.urgent ? 'bg-amber-50 text-amber-700' : 'bg-teal/10 text-teal'}>{hint.text}</Chip>
      )}
    </div>
  );
}

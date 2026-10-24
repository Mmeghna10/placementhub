// Display helpers shared by the job list and job detail pages.

const jobTypeLabels = { FULL_TIME: 'Full time', INTERNSHIP: 'Internship', PART_TIME: 'Part time', CONTRACT: 'Contract' };

export function formatJobType(jobType) {
  if (!jobType) return null;
  return jobTypeLabels[jobType] || jobType.replace('_', ' ').toLowerCase().replace(/^\w/, (c) => c.toUpperCase());
}

/** "2026-12-31" -> Date at local midnight (not UTC, so the day never shifts). */
function parseDate(isoDate) {
  const [y, m, d] = isoDate.split('-').map(Number);
  return new Date(y, m - 1, d);
}

export function formatDate(isoDate) {
  if (!isoDate) return null;
  return parseDate(isoDate).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });
}

/** Whole days from today until the deadline (0 = today, negative = passed). */
export function daysUntil(isoDate, today = new Date()) {
  const start = new Date(today.getFullYear(), today.getMonth(), today.getDate());
  return Math.round((parseDate(isoDate) - start) / 86_400_000);
}

export function deadlineHint(isoDate) {
  if (!isoDate) return null;
  const days = daysUntil(isoDate);
  if (days < 0) return { text: 'Closed', urgent: false };
  if (days === 0) return { text: 'Closes today', urgent: true };
  if (days === 1) return { text: 'Closes tomorrow', urgent: true };
  return { text: `Closes in ${days} days`, urgent: days <= 7 };
}

function compactRupees(amount) {
  if (amount >= 100_000) {
    const lakhs = amount / 100_000;
    return `${Number.isInteger(lakhs) ? lakhs : lakhs.toFixed(1)}`;
  }
  return `${Math.round(amount / 1000)}k`;
}

/** Yearly packages as "₹12–16 LPA", internship stipends as "₹40k–50k / month". */
export function formatSalary(min, max) {
  if (!min && !max) return null;
  const low = min ?? max;
  const high = max ?? min;
  if (high >= 100_000) {
    return low === high ? `₹${compactRupees(low)} LPA` : `₹${compactRupees(low)}–${compactRupees(high)} LPA`;
  }
  return low === high ? `₹${compactRupees(low)} / month` : `₹${compactRupees(low)}–${compactRupees(high)} / month`;
}

/** "Tue, 27 Oct, 10:00 am" for timestamps such as interview slots and timeline entries. */
export function formatDateTime(isoDateTime) {
  if (!isoDateTime) return null;
  return new Date(isoDateTime).toLocaleString('en-IN', {
    weekday: 'short', day: 'numeric', month: 'short', hour: 'numeric', minute: '2-digit',
  });
}

/** Day number and short month for calendar tiles, e.g. { day: '27', month: 'Oct' }. */
export function calendarParts(isoDateTime) {
  const d = new Date(isoDateTime);
  return { day: String(d.getDate()), month: d.toLocaleString('en-IN', { month: 'short' }) };
}

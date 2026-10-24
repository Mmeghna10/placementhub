import { describe, expect, it } from 'vitest';
import { daysUntil, deadlineHint, formatDate, formatJobType, formatSalary } from './jobFormat';

describe('job display helpers', () => {
  it('shows yearly packages in lakhs and stipends per month', () => {
    expect(formatSalary(1200000, 1600000)).toBe('₹12–16 LPA');
    expect(formatSalary(650000, 900000)).toBe('₹6.5–9 LPA');
    expect(formatSalary(40000, 50000)).toBe('₹40k–50k / month');
    expect(formatSalary(1000000, 1000000)).toBe('₹10 LPA');
    expect(formatSalary(null, null)).toBeNull();
  });

  it('formats dates without shifting the day across time zones', () => {
    expect(formatDate('2026-12-31')).toBe('31 Dec 2026');
    expect(formatDate(null)).toBeNull();
  });

  it('counts whole days to the deadline and flags the last week', () => {
    const today = new Date(2026, 9, 24, 23, 30);
    expect(daysUntil('2026-10-24', today)).toBe(0);
    expect(daysUntil('2026-10-31', today)).toBe(7);
    expect(daysUntil('2026-10-20', today)).toBeLessThan(0);
    expect(deadlineHint(null)).toBeNull();
  });

  it('labels job types', () => {
    expect(formatJobType('FULL_TIME')).toBe('Full time');
    expect(formatJobType('INTERNSHIP')).toBe('Internship');
    expect(formatJobType(undefined)).toBeNull();
  });
});

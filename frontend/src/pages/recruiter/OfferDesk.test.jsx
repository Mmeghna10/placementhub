import { screen, within } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { RECRUITER, apiError, renderPage } from '../../test/render';
import { getOfferDesk } from '../../api/recruiterApi';
import OfferDesk from './OfferDesk';

vi.mock('../../api/recruiterApi');

const inHours = (h) => new Date(Date.now() + h * 3_600_000 + 60_000).toISOString();

const desk = {
  totals: { openings: 3, filled: 1, pendingOffers: 1, waitlisted: 2, acceptanceRate: 0.5, autoRefills: 1 },
  jobs: [
    {
      jobId: 7, title: 'Backend Engineer', tier: 'DREAM', status: 'APPROVED', openings: 2, filled: 1, pendingOffers: 1, waitlisted: 2,
      offers: [{ applicationId: 31, studentName: 'Pia Pending', expiresAt: inHours(5) }],
    },
    { jobId: 8, title: 'Data Analyst', tier: 'REGULAR', status: 'APPROVED', openings: 1, filled: 0, pendingOffers: 0, waitlisted: 0, offers: [] },
  ],
  recentEvents: [
    { applicationId: 40, jobTitle: 'Data Analyst', studentName: 'Rhea Refill', status: 'OFFERED',
      remarks: 'Offered automatically from the waitlist when an opening became free', at: '2026-10-26T10:00:00' },
  ],
};

const open = () => renderPage(<OfferDesk />, { route: '/recruiter/offer-desk', user: RECRUITER });

describe('Offer Desk page', () => {
  it('shows the totals, each job with its pending offers, and recent activity', async () => {
    getOfferDesk.mockResolvedValue({ data: desk });
    open();

    expect(await screen.findByText('1 / 3')).toBeInTheDocument();
    expect(screen.getByText('50%')).toBeInTheDocument();

    const jobs = screen.getAllByTestId('desk-job');
    expect(jobs).toHaveLength(2);
    expect(within(jobs[0]).getByRole('link', { name: 'Backend Engineer' })).toHaveAttribute('href', '/recruiter/jobs/7/applicants');
    expect(within(jobs[0]).getByText('Pia Pending')).toBeInTheDocument();
    expect(within(jobs[0]).getByText(/5 h .*left/)).toBeInTheDocument();
    expect(within(jobs[0]).getByText(/2 on the waitlist/)).toBeInTheDocument();

    const activity = screen.getByRole('region', { name: 'Recent activity' });
    expect(within(activity).getByText('Rhea Refill')).toBeInTheDocument();
    expect(within(activity).getByText(/automatically from the waitlist/)).toBeInTheDocument();
  });

  it('shows "–" for the acceptance rate before any offer is decided', async () => {
    getOfferDesk.mockResolvedValue({ data: { ...desk, totals: { ...desk.totals, acceptanceRate: null } } });
    open();

    expect(await screen.findByText('–')).toBeInTheDocument();
  });

  it('shows the error when the desk cannot load', async () => {
    getOfferDesk.mockRejectedValue(apiError(500, { error: 'Something went wrong. Please try again.' }));
    open();

    expect(await screen.findByRole('alert')).toHaveTextContent('Something went wrong');
  });
});

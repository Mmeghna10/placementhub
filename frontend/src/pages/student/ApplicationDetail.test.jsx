import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { STUDENT, apiError, deferred, renderPage } from '../../test/render';
import {
  acceptOffer, bookInterviewSlot, cancelInterviewBooking, declineOffer, getApplicationDetails, getInterviewSlots,
} from '../../api/studentApi';
import ApplicationDetail from './ApplicationDetail';

vi.mock('../../api/studentApi');

const application = (extra = {}) => ({
  id: 3, jobId: 7, jobTitle: 'Java Developer', companyName: 'Acme', status: 'INTERVIEW', version: 2,
  timeline: [
    { status: 'APPLIED', remarks: 'Application submitted', changedAt: '2026-10-20T10:00:00' },
    { status: 'INTERVIEW', remarks: null, changedAt: '2026-10-21T10:00:00' },
  ],
  ...extra,
});
const slot = (id, seatsLeft, startsAt = '2026-11-02T10:00:00') => ({
  id, startsAt, durationMinutes: 30, location: 'Room 4', capacity: 2, bookedCount: 2 - seatsLeft, seatsLeft,
});

const open = () => renderPage(<ApplicationDetail />, {
  route: '/student/applications/3', path: '/student/applications/:id', user: STUDENT,
});

describe('Application page', () => {
  beforeEach(() => vi.spyOn(window, 'confirm').mockReturnValue(true));
  afterEach(() => window.confirm.mockRestore());

  it('shows the status timeline', async () => {
    getApplicationDetails.mockResolvedValue({ data: application({ status: 'APPLIED', timeline: application().timeline.slice(0, 1) }) });
    open();

    expect(await screen.findByText('Application submitted')).toBeInTheDocument();
    // No interview section before the INTERVIEW stage, and no slot request.
    expect(screen.queryByText(/pick one slot/i)).not.toBeInTheDocument();
    expect(getInterviewSlots).not.toHaveBeenCalled();
  });

  it('lists open slots in the INTERVIEW stage, with full ones disabled', async () => {
    getApplicationDetails.mockResolvedValue({ data: application() });
    getInterviewSlots.mockResolvedValue({ data: [slot(1, 0), slot(2, 1, '2026-11-02T11:00:00')] });
    open();

    expect(await screen.findByText(/pick one slot/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Full' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Book' })).toBeEnabled();
    expect(screen.getByText(/1 seat left/)).toBeInTheDocument();
  });

  it('books a slot once even when Book is double-clicked (regression)', async () => {
    const pending = deferred();
    getApplicationDetails
      .mockResolvedValueOnce({ data: application() })
      .mockResolvedValue({ data: application({ interview: { slotId: 2, startsAt: '2026-11-02T11:00:00', durationMinutes: 30, location: 'Room 4' } }) });
    getInterviewSlots.mockResolvedValue({ data: [slot(2, 1)] });
    bookInterviewSlot.mockReturnValue(pending.promise);
    open();
    const book = await screen.findByRole('button', { name: 'Book' });

    book.click();
    book.click();
    pending.resolve({ data: {} });

    expect(await screen.findByText(/booked for/i)).toBeInTheDocument();
    expect(bookInterviewSlot).toHaveBeenCalledTimes(1);
    expect(bookInterviewSlot).toHaveBeenCalledWith(3, 2);
    // The student's own seat must not be reported as "slot full".
    expect(screen.queryByText(/already full/i)).not.toBeInTheDocument();
  });

  it('explains when someone else took the last seat first', async () => {
    getApplicationDetails.mockResolvedValue({ data: application() });
    getInterviewSlots.mockResolvedValue({ data: [slot(2, 1)] });
    bookInterviewSlot.mockRejectedValue(apiError(409, {
      code: 'SLOT_FULL', error: 'This interview slot is already full. Please pick another one.',
    }));
    open();

    await userEvent.setup().click(await screen.findByRole('button', { name: 'Book' }));

    expect(await screen.findByText(/already full\. please pick another one/i)).toBeInTheDocument();
  });

  it('cancels a booking after confirmation', async () => {
    getApplicationDetails
      .mockResolvedValueOnce({ data: application({ interview: { slotId: 2, startsAt: '2026-11-02T11:00:00', durationMinutes: 30, location: 'Room 4' } }) })
      .mockResolvedValue({ data: application() });
    getInterviewSlots.mockResolvedValue({ data: [slot(2, 1)] });
    cancelInterviewBooking.mockResolvedValue({ data: {} });
    open();

    await userEvent.setup().click(await screen.findByRole('button', { name: /cancel booking/i }));

    expect(window.confirm).toHaveBeenCalled();
    expect(cancelInterviewBooking).toHaveBeenCalledWith(3);
    expect(await screen.findByRole('button', { name: 'Book' })).toBeInTheDocument();
  });

  it('does nothing if the student changes their mind about cancelling', async () => {
    window.confirm.mockReturnValue(false);
    getApplicationDetails.mockResolvedValue({ data: application({ interview: { slotId: 2, startsAt: '2026-11-02T11:00:00', durationMinutes: 30 } }) });
    open();

    await userEvent.setup().click(await screen.findByRole('button', { name: /cancel booking/i }));

    await waitFor(() => expect(window.confirm).toHaveBeenCalled());
    expect(cancelInterviewBooking).not.toHaveBeenCalled();
  });

  it('tells the student when no slots are published yet', async () => {
    getApplicationDetails.mockResolvedValue({ data: application() });
    getInterviewSlots.mockResolvedValue({ data: [] });
    open();

    expect(await screen.findByText(/no interview slots are available yet/i)).toBeInTheDocument();
  });

  describe('offers', () => {
    const inTwoDays = () => new Date(Date.now() + 2 * 86_400_000 + 90 * 60_000).toISOString();
    const offered = (extra = {}) => application({ status: 'OFFERED', offerExpiresAt: inTwoDays(), ...extra });

    it('shows the offer with its deadline and accepts it with one request on a double-click', async () => {
      const pending = deferred();
      getApplicationDetails
        .mockResolvedValueOnce({ data: offered() })
        .mockResolvedValue({ data: application({ status: 'ACCEPTED' }) });
      acceptOffer.mockReturnValue(pending.promise);
      open();

      expect(await screen.findByText('You have an offer from Acme!')).toBeInTheDocument();
      expect(screen.getByText(/2 days 1 h left/)).toBeInTheDocument();

      // Two clicks before React re-renders (what a fast double-click does).
      const accept = screen.getByRole('button', { name: 'Accept offer' });
      accept.click();
      accept.click();
      pending.resolve({ data: {} });

      expect(await screen.findByText(/you're placed/i)).toBeInTheDocument();
      expect(acceptOffer).toHaveBeenCalledTimes(1);
      expect(acceptOffer).toHaveBeenCalledWith(3);
    });

    it('asks before declining and does nothing if the student cancels', async () => {
      getApplicationDetails.mockResolvedValue({ data: offered() });
      window.confirm.mockReturnValue(false);
      open();

      await userEvent.setup().click(await screen.findByRole('button', { name: 'Decline' }));

      expect(window.confirm).toHaveBeenCalledWith(expect.stringMatching(/decline the offer from acme/i));
      expect(declineOffer).not.toHaveBeenCalled();
    });

    it('declines after confirming', async () => {
      getApplicationDetails
        .mockResolvedValueOnce({ data: offered() })
        .mockResolvedValue({ data: application({ status: 'DECLINED' }) });
      declineOffer.mockResolvedValue({ data: {} });
      open();

      await userEvent.setup().click(await screen.findByRole('button', { name: 'Decline' }));

      expect(declineOffer).toHaveBeenCalledWith(3);
      expect(await screen.findByText('You declined this offer.')).toBeInTheDocument();
    });

    it("shows the server's reason when the offer expired while the page was open", async () => {
      getApplicationDetails.mockResolvedValue({ data: offered() });
      acceptOffer.mockRejectedValue(apiError(422, { code: 'OFFER_EXPIRED', error: 'This offer expired on 2026-10-25T12:00.' }));
      open();

      await userEvent.setup().click(await screen.findByRole('button', { name: 'Accept offer' }));

      expect(await screen.findByRole('alert')).toHaveTextContent('This offer expired on 2026-10-25T12:00.');
    });

    it('explains an expired offer', async () => {
      getApplicationDetails.mockResolvedValue({ data: application({ status: 'EXPIRED' }) });
      open();

      expect(await screen.findByText(/expired without a reply/i)).toBeInTheDocument();
      expect(screen.queryByRole('button', { name: 'Accept offer' })).not.toBeInTheDocument();
    });
  });

  it('shows the waitlist position', async () => {
    getApplicationDetails.mockResolvedValue({ data: application({ status: 'WAITLISTED', waitlistPosition: 2 }) });
    open();

    expect(await screen.findByText('#2')).toBeInTheDocument();
    expect(screen.getByText(/on the waitlist at/i)).toHaveTextContent(/next person automatically/i);
  });

  it('explains an application withdrawn by the placement rules', async () => {
    getApplicationDetails.mockResolvedValue({ data: application({ status: 'WITHDRAWN' }) });
    open();

    expect(await screen.findByText(/withdrawn automatically/i)).toHaveTextContent(/placement rules/i);
  });
});

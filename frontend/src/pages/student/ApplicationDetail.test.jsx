import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { STUDENT, apiError, deferred, renderPage } from '../../test/render';
import {
  bookInterviewSlot, cancelInterviewBooking, getApplicationDetails, getInterviewSlots,
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
});

import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { RECRUITER, apiError, deferred, renderPage } from '../../test/render';
import { createInterviewSlot, deleteInterviewSlot, getInterviewSlots } from '../../api/recruiterApi';
import InterviewSlots from './InterviewSlots';

vi.mock('../../api/recruiterApi');

const slot = (id, booked, bookings = []) => ({
  id, startsAt: '2026-11-02T10:00:00', durationMinutes: 30, location: 'Room 4', capacity: 2,
  bookedCount: booked, seatsLeft: 2 - booked, bookings,
});

const open = () => renderPage(<InterviewSlots />, {
  route: '/recruiter/jobs/7/slots', path: '/recruiter/jobs/:jobId/slots', user: RECRUITER,
});

async function fillForm(user) {
  const startsAt = document.querySelector('input[name="startsAt"]');
  await user.type(startsAt, '2026-11-02T10:00');
  await user.clear(document.querySelector('input[name="capacity"]'));
  await user.type(document.querySelector('input[name="capacity"]'), '3');
  await user.type(document.querySelector('input[name="location"]'), 'Room 4');
}

describe('Interview slots page', () => {
  beforeEach(() => vi.spyOn(window, 'confirm').mockReturnValue(true));
  afterEach(() => window.confirm.mockRestore());

  it('lists slots with seats and who booked them; only empty slots can be deleted', async () => {
    getInterviewSlots.mockResolvedValue({ data: [
      slot(1, 1, [{ applicationId: 9, fullName: 'Sam Student', applicationStatus: 'INTERVIEW' }]),
      slot(2, 0),
    ] });
    open();

    expect(await screen.findByText('Sam Student')).toBeInTheDocument();
    expect(screen.getByText(/1 \/ 2 booked · 1 left/)).toBeInTheDocument();
    expect(screen.getAllByTitle('Delete slot')).toHaveLength(1);
  });

  it('creates a slot with the entered values', async () => {
    getInterviewSlots.mockResolvedValue({ data: [] });
    createInterviewSlot.mockResolvedValue({ data: slot(3, 0) });
    open();
    await screen.findByText('No interview slots yet');
    const user = userEvent.setup();

    await fillForm(user);
    await user.click(screen.getByRole('button', { name: 'Add Slot' }));

    await waitFor(() => expect(createInterviewSlot).toHaveBeenCalledWith('7', {
      startsAt: '2026-11-02T10:00', durationMinutes: 30, capacity: 3, location: 'Room 4',
    }));
    await waitFor(() => expect(getInterviewSlots).toHaveBeenCalledTimes(2));
  });

  it('creates only one slot when Add Slot is double-clicked (regression)', async () => {
    const pending = deferred();
    getInterviewSlots.mockResolvedValue({ data: [] });
    createInterviewSlot.mockReturnValue(pending.promise);
    open();
    await screen.findByText('No interview slots yet');
    await fillForm(userEvent.setup());

    const add = screen.getByRole('button', { name: 'Add Slot' });
    add.click();
    add.click();
    pending.resolve({ data: slot(3, 0) });

    await waitFor(() => expect(getInterviewSlots).toHaveBeenCalledTimes(2));
    expect(createInterviewSlot).toHaveBeenCalledTimes(1);
  });

  it('shows why a slot was refused', async () => {
    getInterviewSlots.mockResolvedValue({ data: [] });
    createInterviewSlot.mockRejectedValue(apiError(422, { code: 'SLOT_IN_PAST', error: 'The interview slot must start in the future' }));
    open();
    await screen.findByText('No interview slots yet');
    const user = userEvent.setup();

    await fillForm(user);
    await user.click(screen.getByRole('button', { name: 'Add Slot' }));

    expect(await screen.findByText('The interview slot must start in the future')).toBeInTheDocument();
  });

  it('deletes an empty slot after confirmation', async () => {
    getInterviewSlots.mockResolvedValue({ data: [slot(2, 0)] });
    deleteInterviewSlot.mockResolvedValue({});
    open();

    await userEvent.setup().click(await screen.findByTitle('Delete slot'));

    expect(deleteInterviewSlot).toHaveBeenCalledWith('7', 2);
  });
});

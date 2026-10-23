import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { RECRUITER, apiError, renderPage } from '../../test/render';
import { closeJob, deleteJob, getMyJobs } from '../../api/recruiterApi';
import MyJobs from './MyJobs';

vi.mock('../../api/recruiterApi');

const job = (id, title, status) => ({ id, title, status, location: 'Pune', jobType: 'FULL_TIME' });
const open = () => renderPage(<MyJobs />, { route: '/recruiter/jobs', user: RECRUITER });

describe('My jobs page', () => {
  beforeEach(() => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    vi.spyOn(window, 'alert').mockImplementation(() => {});
  });
  afterEach(() => {
    window.confirm.mockRestore();
    window.alert.mockRestore();
  });

  it('offers "close" only for approved jobs', async () => {
    getMyJobs.mockResolvedValue({ data: [job(1, 'Open role', 'APPROVED'), job(2, 'Waiting role', 'PENDING'), job(3, 'Old role', 'CLOSED')] });
    open();

    await screen.findByText('Open role');
    expect(screen.getAllByTitle('Close applications')).toHaveLength(1);
  });

  it('closes a job after confirmation and reloads the list', async () => {
    getMyJobs
      .mockResolvedValueOnce({ data: [job(1, 'Open role', 'APPROVED')] })
      .mockResolvedValue({ data: [job(1, 'Open role', 'CLOSED')] });
    closeJob.mockResolvedValue({ data: {} });
    open();

    await userEvent.setup().click(await screen.findByTitle('Close applications'));

    expect(closeJob).toHaveBeenCalledWith(1);
    expect(await screen.findByText('CLOSED')).toBeInTheDocument();
  });

  it('shows why a job with applicants cannot be deleted', async () => {
    getMyJobs.mockResolvedValue({ data: [job(1, 'Busy role', 'APPROVED')] });
    deleteJob.mockRejectedValue(apiError(409, { code: 'JOB_HAS_APPLICATIONS', error: 'This job already has applications and cannot be deleted' }));
    open();

    await userEvent.setup().click(await screen.findByTitle('Delete'));

    await waitFor(() => expect(window.alert).toHaveBeenCalledWith('This job already has applications and cannot be deleted'));
  });

  it('shows an empty state with no jobs', async () => {
    getMyJobs.mockResolvedValue({ data: [] });
    open();
    expect(await screen.findByText('No jobs posted yet')).toBeInTheDocument();
  });
});

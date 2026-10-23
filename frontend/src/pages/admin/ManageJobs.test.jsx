import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { ADMIN, apiError, renderPage } from '../../test/render';
import { approveJob, getAllJobs, getPendingJobs } from '../../api/adminApi';
import ManageJobs from './ManageJobs';

vi.mock('../../api/adminApi');

const job = (id, title, status) => ({ id, title, status, companyName: 'Acme', location: 'Pune' });

describe('Admin jobs page', () => {
  it('lists pending jobs and approves one', async () => {
    getPendingJobs
      .mockResolvedValueOnce({ data: [job(1, 'Java Developer', 'PENDING')] })
      .mockResolvedValue({ data: [] });
    approveJob.mockResolvedValue({ data: {} });
    renderPage(<ManageJobs />, { route: '/admin/jobs?pending=true', path: '/admin/jobs', user: ADMIN });

    await userEvent.setup().click(await screen.findByRole('button', { name: 'Approve' }));

    expect(approveJob).toHaveBeenCalledWith(1, 'APPROVED');
    expect(await screen.findByText('Nothing pending')).toBeInTheDocument();
  });

  it('rejects a job', async () => {
    getPendingJobs.mockResolvedValue({ data: [job(1, 'Java Developer', 'PENDING')] });
    approveJob.mockResolvedValue({ data: {} });
    renderPage(<ManageJobs />, { route: '/admin/jobs?pending=true', path: '/admin/jobs', user: ADMIN });

    await userEvent.setup().click(await screen.findByRole('button', { name: 'Reject' }));

    expect(approveJob).toHaveBeenCalledWith(1, 'REJECTED');
  });

  it('shows the server message when a decision is refused', async () => {
    getPendingJobs.mockResolvedValue({ data: [job(1, 'Java Developer', 'PENDING')] });
    approveJob.mockRejectedValue(apiError(422, { code: 'JOB_NOT_PENDING', error: 'Only pending jobs can be approved or rejected (this job is APPROVED)' }));
    renderPage(<ManageJobs />, { route: '/admin/jobs?pending=true', path: '/admin/jobs', user: ADMIN });

    await userEvent.setup().click(await screen.findByRole('button', { name: 'Approve' }));

    expect(await screen.findByText(/only pending jobs can be approved/i)).toBeInTheDocument();
  });

  it('pages through all jobs, with buttons only on pending ones', async () => {
    getAllJobs
      .mockResolvedValueOnce({ data: { content: [job(1, 'Live role', 'APPROVED'), job(2, 'New role', 'PENDING')], page: 0, size: 20, totalElements: 25, totalPages: 2 } })
      .mockResolvedValue({ data: { content: [job(3, 'Old role', 'REJECTED')], page: 1, size: 20, totalElements: 25, totalPages: 2 } });
    renderPage(<ManageJobs />, { route: '/admin/jobs', path: '/admin/jobs', user: ADMIN });

    expect(await screen.findByText('Page 1 of 2 · 25 total')).toBeInTheDocument();
    expect(screen.getAllByRole('button', { name: 'Approve' })).toHaveLength(1);

    await userEvent.setup().click(screen.getByRole('button', { name: 'Next' }));

    expect(await screen.findByText('Old role')).toBeInTheDocument();
    await waitFor(() => expect(getAllJobs).toHaveBeenLastCalledWith(1));
  });
});

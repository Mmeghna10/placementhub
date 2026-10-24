import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { STUDENT, apiError, deferred, renderPage } from '../../test/render';
import { applyToJob, getJob, getMyApplications } from '../../api/studentApi';
import JobDetails from './JobDetails';

vi.mock('../../api/studentApi');

const JOB = {
  id: 7, title: 'Java Developer', companyName: 'Acme', description: 'Build APIs.', location: 'Pune', jobType: 'FULL_TIME',
  minCgpa: 7.5, eligibleBranches: 'CSE, IT', applicationDeadline: '2026-12-31', acceptingApplications: true,
};

function open({ job = JOB, applications = [] } = {}) {
  getJob.mockResolvedValue({ data: job });
  getMyApplications.mockResolvedValue({ data: applications });
  renderPage(<JobDetails />, { route: '/student/jobs/7', path: '/student/jobs/:jobId', user: STUDENT });
}

const applyButton = () => screen.findByRole('button', { name: /apply now/i });

describe('Job details page', () => {
  it('shows the job and its requirements', async () => {
    open();
    expect(await screen.findByRole('heading', { name: 'Java Developer' })).toBeInTheDocument();
    expect(screen.getByText('CSE, IT')).toBeInTheDocument();
    expect(screen.getByText('Apply by 31 Dec 2026')).toBeInTheDocument();
    expect(getJob).toHaveBeenCalledWith('7');
  });

  it('applies and then links to the new application', async () => {
    applyToJob.mockResolvedValue({ data: { id: 55, jobId: 7, status: 'APPLIED' } });
    open();

    await userEvent.setup().click(await applyButton());

    expect(await screen.findByText(/you have applied to this job/i)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /view your application/i })).toHaveAttribute('href', '/student/applications/55');
    expect(screen.queryByRole('button', { name: /apply now/i })).not.toBeInTheDocument();
  });

  it('sends only one request when Apply is double-clicked (regression)', async () => {
    const pending = deferred();
    applyToJob.mockReturnValue(pending.promise);
    open();
    const button = await applyButton();

    // Two fast clicks before React re-renders: the second must be ignored.
    button.click();
    button.click();
    pending.resolve({ data: { id: 55, jobId: 7 } });

    expect(await screen.findByText(/you have applied/i)).toBeInTheDocument();
    expect(applyToJob).toHaveBeenCalledTimes(1);
    expect(screen.queryByText(/already applied/i)).not.toBeInTheDocument();
  });

  it('treats "already applied" (409) as applied, not as an error', async () => {
    applyToJob.mockRejectedValue(apiError(409, { code: 'APPLICATION_ALREADY_EXISTS', error: 'You have already applied to this job' }));
    open();
    getMyApplications.mockResolvedValue({ data: [{ id: 55, jobId: 7 }] });

    await userEvent.setup().click(await applyButton());

    expect(await screen.findByText(/you have applied to this job/i)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /view your application/i })).toHaveAttribute('href', '/student/applications/55');
  });

  it('shows why the student is not eligible', async () => {
    applyToJob.mockRejectedValue(apiError(422, { code: 'NOT_ELIGIBLE', error: 'This job requires a minimum CGPA of 7.50.' }));
    open();

    await userEvent.setup().click(await applyButton());

    expect(await screen.findByText('This job requires a minimum CGPA of 7.50.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /apply now/i })).toBeEnabled();
  });

  it('shows "already applied" straight away when the student applied before', async () => {
    open({ applications: [{ id: 55, jobId: 7 }] });

    expect(await screen.findByText(/you have applied to this job/i)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /apply now/i })).not.toBeInTheDocument();
  });

  it('does not offer Apply on a job that stopped accepting applications', async () => {
    open({ job: { ...JOB, acceptingApplications: false } });

    expect(await screen.findByText(/no longer accepting applications/i)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /apply now/i })).not.toBeInTheDocument();
  });

  it('says "Job not found" for a missing or hidden job', async () => {
    getJob.mockRejectedValue(apiError(404, { error: 'Job not found' }));
    getMyApplications.mockResolvedValue({ data: [] });
    renderPage(<JobDetails />, { route: '/student/jobs/999', path: '/student/jobs/:jobId', user: STUDENT });

    expect(await screen.findByText('Job not found.')).toBeInTheDocument();
  });
});

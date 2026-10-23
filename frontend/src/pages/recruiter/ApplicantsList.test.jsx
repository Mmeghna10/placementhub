import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { RECRUITER, apiError, renderPage } from '../../test/render';
import { getApplicantsForJob, updateApplicationStatus } from '../../api/recruiterApi';
import ApplicantsList from './ApplicantsList';

vi.mock('../../api/recruiterApi');

const applicant = (id, status, next, extra = {}) => ({
  id, status, version: 4, allowedNextStatuses: next, appliedAt: '2026-10-20T10:00:00',
  applicant: {
    fullName: `Student ${id}`, email: `s${id}@college.test`, college: 'COEP', degree: 'B.Tech', branch: 'CSE',
    graduationYear: 2027, cgpa: 8.2, skills: 'Java, SQL', resumeUrl: 'https://example.com/cv.pdf',
  },
  ...extra,
});

const open = () => renderPage(<ApplicantsList />, {
  route: '/recruiter/jobs/7/applicants', path: '/recruiter/jobs/:jobId/applicants', user: RECRUITER,
});

describe('Applicants page', () => {
  it("shows each applicant's profile and links", async () => {
    getApplicantsForJob.mockResolvedValue({ data: [applicant(1, 'APPLIED', ['SHORTLISTED', 'REJECTED'])] });
    open();

    expect(await screen.findByText('Student 1')).toBeInTheDocument();
    expect(screen.getByText(/COEP · B\.Tech · CSE · Class of 2027 · CGPA 8\.2/)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Resume' })).toHaveAttribute('href', 'https://example.com/cv.pdf');
    expect(getApplicantsForJob).toHaveBeenCalledWith('7');
  });

  it('offers only the next allowed statuses, and none for final ones', async () => {
    getApplicantsForJob.mockResolvedValue({ data: [
      applicant(1, 'APPLIED', ['SHORTLISTED', 'REJECTED']),
      applicant(2, 'OFFERED', []),
    ] });
    open();

    const menu = await screen.findByRole('combobox');
    const options = within(menu).getAllByRole('option').map((o) => o.value).filter(Boolean);
    expect(options).toEqual(['SHORTLISTED', 'REJECTED']);
    expect(screen.getAllByRole('combobox')).toHaveLength(1); // OFFERED has no menu
    expect(screen.getByText('Final')).toBeInTheDocument();
  });

  it('sends the version it displayed with a status change, then reloads', async () => {
    getApplicantsForJob.mockResolvedValue({ data: [applicant(1, 'APPLIED', ['SHORTLISTED', 'REJECTED'])] });
    updateApplicationStatus.mockResolvedValue({ data: {} });
    open();

    await userEvent.setup().selectOptions(await screen.findByRole('combobox'), 'SHORTLISTED');

    await waitFor(() => expect(updateApplicationStatus).toHaveBeenCalledWith(1, { status: 'SHORTLISTED', remarks: '', version: 4 }));
    await waitFor(() => expect(getApplicantsForJob).toHaveBeenCalledTimes(2));
  });

  it('explains a conflicting update and shows the latest state', async () => {
    getApplicantsForJob
      .mockResolvedValueOnce({ data: [applicant(1, 'APPLIED', ['SHORTLISTED', 'REJECTED'])] })
      .mockResolvedValue({ data: [applicant(1, 'REJECTED', [], { version: 5 })] });
    updateApplicationStatus.mockRejectedValue(apiError(409, {
      code: 'APPLICATION_MODIFIED', error: 'This application was updated by someone else. Refresh and try again.',
    }));
    open();

    await userEvent.setup().selectOptions(await screen.findByRole('combobox'), 'SHORTLISTED');

    expect(await screen.findByText(/updated by someone else/i)).toBeInTheDocument();
    // Reloaded: now shows what the other person set.
    expect(await screen.findByText('Final')).toBeInTheDocument();
  });

  it('says so when nobody has applied yet', async () => {
    getApplicantsForJob.mockResolvedValue({ data: [] });
    open();
    expect(await screen.findByText('No applicants yet')).toBeInTheDocument();
  });
});

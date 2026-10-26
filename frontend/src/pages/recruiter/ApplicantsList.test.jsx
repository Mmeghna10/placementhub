import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { RECRUITER, apiError, renderPage } from '../../test/render';
import { fillFromWaitlist, getApplicantsForJob, getMyJobs, reorderWaitlist, updateApplicationStatus } from '../../api/recruiterApi';
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
  beforeEach(() => {
    getMyJobs.mockResolvedValue({ data: [{ id: 7, title: 'Java Developer', openings: 3, offersPending: 1, offersAccepted: 1 }] });
  });

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

  describe('waitlist', () => {
    const waiting = (id, rank) => applicant(id, 'WAITLISTED', ['OFFERED', 'REJECTED'], { waitlistRank: rank });

    it('lists the waitlist in rank order and moves a candidate up', async () => {
      getApplicantsForJob.mockResolvedValue({ data: [waiting(1, 2), waiting(2, 1), waiting(3, 3)] });
      reorderWaitlist.mockResolvedValue({ data: [] });
      open();

      const panel = await screen.findByRole('region', { name: 'Waitlist' });
      expect(within(panel).getAllByRole('listitem').map((li) => li.textContent)).toEqual([
        expect.stringContaining('Student 2'), expect.stringContaining('Student 1'), expect.stringContaining('Student 3'),
      ]);

      await userEvent.setup().click(within(panel).getByRole('button', { name: 'Move Student 3 up' }));

      expect(reorderWaitlist).toHaveBeenCalledWith('7', [2, 3, 1]);
    });

    it('fills the free openings from the waitlist', async () => {
      // 3 openings, 1 pending, 1 accepted (see beforeEach): 1 free.
      getApplicantsForJob.mockResolvedValue({ data: [waiting(1, 1)] });
      fillFromWaitlist.mockResolvedValue({ data: [] });
      open();

      await userEvent.setup().click(await screen.findByRole('button', { name: 'Fill 1 open opening' }));

      expect(fillFromWaitlist).toHaveBeenCalledWith('7');
    });

    it('cannot fill when every opening is taken', async () => {
      getMyJobs.mockResolvedValue({ data: [{ id: 7, title: 'Java Developer', openings: 2, offersPending: 1, offersAccepted: 1 }] });
      getApplicantsForJob.mockResolvedValue({ data: [waiting(1, 1)] });
      open();

      expect(await screen.findByRole('button', { name: 'Fill no open openings' })).toBeDisabled();
    });

    it('asks to refresh when the waitlist changed meanwhile', async () => {
      getApplicantsForJob.mockResolvedValue({ data: [waiting(1, 1), waiting(2, 2)] });
      reorderWaitlist.mockRejectedValue(apiError(409, { code: 'WAITLIST_CHANGED', error: 'The waitlist changed in the meantime. Refresh and try again.' }));
      open();

      await userEvent.setup().click(await screen.findByRole('button', { name: 'Move Student 2 up' }));

      expect(await screen.findByRole('alert')).toHaveTextContent(/waitlist changed/i);
    });
  });
});

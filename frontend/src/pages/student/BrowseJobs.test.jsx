import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { STUDENT, apiError, deferred, renderPage } from '../../test/render';
import { searchJobs } from '../../api/studentApi';
import BrowseJobs from './BrowseJobs';

vi.mock('../../api/studentApi');

const job = (id, title, extra = {}) => ({
  id, title, companyName: 'Acme', description: `${title} role`, location: 'Pune', jobType: 'FULL_TIME',
  salaryMin: 30000, salaryMax: 50000, applicationDeadline: '2026-12-31', ...extra,
});
const page = (content, info = {}) => ({
  data: { content, page: 0, size: 10, totalElements: content.length, totalPages: 1, ...info },
});

const open = () => renderPage(<BrowseJobs />, { route: '/student/jobs', user: STUDENT });

describe('Browse jobs page', () => {
  it('shows a loading state, then the jobs as links to their pages', async () => {
    const pending = deferred();
    searchJobs.mockReturnValue(pending.promise);
    open();

    expect(screen.getByRole('status')).toHaveTextContent('Loading');
    pending.resolve(page([job(1, 'Java Developer'), job(2, 'Data Intern', { jobType: 'INTERNSHIP' })]));

    const link = await screen.findByRole('link', { name: /java developer/i });
    expect(link).toHaveAttribute('href', '/student/jobs/1');
    expect(screen.getByText('Data Intern')).toBeInTheDocument();
    expect(screen.getByText('2 roles')).toBeInTheDocument();
    expect(screen.getAllByText('Apply by 2026-12-31')).toHaveLength(2);
  });

  it('asks the server for page 0, newest first, 10 per page', async () => {
    searchJobs.mockResolvedValue(page([]));
    open();
    await waitFor(() => expect(searchJobs).toHaveBeenCalled());

    expect(searchJobs).toHaveBeenCalledWith(expect.objectContaining({
      q: '', jobType: '', location: '', sort: 'newest', eligibleOnly: false, page: 0, size: 10,
    }));
  });

  it('sends the typed search, filters and sort when Search is clicked', async () => {
    searchJobs.mockResolvedValue(page([job(1, 'Java Developer')]));
    open();
    await screen.findByText('Java Developer');
    const user = userEvent.setup();

    await user.type(screen.getByRole('textbox', { name: /search jobs/i }), 'java');
    await user.selectOptions(screen.getByRole('combobox', { name: /job type/i }), 'INTERNSHIP');
    await user.type(screen.getByRole('textbox', { name: /location/i }), 'Pune');
    await user.selectOptions(screen.getByRole('combobox', { name: /sort by/i }), 'salary');
    await user.click(screen.getByRole('button', { name: 'Search' }));

    await waitFor(() => expect(searchJobs).toHaveBeenLastCalledWith(expect.objectContaining({
      q: 'java', jobType: 'INTERNSHIP', location: 'Pune', sort: 'salary', page: 0,
    })));
  });

  it('says when nothing matches the search and offers to clear it', async () => {
    searchJobs.mockResolvedValueOnce(page([job(1, 'Java Developer')])).mockResolvedValue(page([]));
    open();
    await screen.findByText('Java Developer');
    const user = userEvent.setup();

    await user.type(screen.getByRole('textbox', { name: /search jobs/i }), 'astronaut');
    await user.click(screen.getByRole('button', { name: 'Search' }));

    expect(await screen.findByText('No roles match your search')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Clear' }));
    await waitFor(() => expect(searchJobs).toHaveBeenLastCalledWith(expect.objectContaining({ q: '', page: 0 })));
  });

  it('shows "Recommended" by asking only for jobs the student is eligible for', async () => {
    searchJobs.mockResolvedValue(page([]));
    open();
    await waitFor(() => expect(searchJobs).toHaveBeenCalledTimes(1));

    await userEvent.setup().click(screen.getByRole('button', { name: 'Recommended' }));

    expect(await screen.findByRole('heading', { name: 'Recommended For You' })).toBeInTheDocument();
    expect(searchJobs).toHaveBeenLastCalledWith(expect.objectContaining({ eligibleOnly: true, page: 0 }));
    expect(screen.getByText(/once your profile \(CGPA, branch\) is complete/i)).toBeInTheDocument();
  });

  it('pages through results with Next and Previous', async () => {
    const tenJobs = Array.from({ length: 10 }, (_, i) => job(i + 1, `Role ${i + 1}`));
    searchJobs
      .mockResolvedValueOnce(page(tenJobs, { totalElements: 12, totalPages: 2 }))
      .mockResolvedValueOnce(page([job(11, 'Role 11'), job(12, 'Role 12')], { page: 1, totalElements: 12, totalPages: 2 }));
    open();
    expect(await screen.findByText('Page 1 of 2 · 12 total')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Previous' })).toBeDisabled();

    await userEvent.setup().click(screen.getByRole('button', { name: 'Next' }));

    expect(await screen.findByText('Role 12')).toBeInTheDocument();
    expect(searchJobs).toHaveBeenLastCalledWith(expect.objectContaining({ page: 1 }));
    expect(screen.getByRole('button', { name: 'Next' })).toBeDisabled();
  });

  it('shows the server error instead of an empty page', async () => {
    searchJobs.mockRejectedValue(apiError(500, { error: 'Something went wrong. Please try again.' }));
    open();

    expect(await screen.findByText('Something went wrong. Please try again.')).toBeInTheDocument();
  });
});

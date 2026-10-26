import { test, expect } from '@playwright/test';
import {
  adminCredentials, api, createRecruiter, loginInBrowser, loginViaApi, postJob, registerViaApi,
} from './helpers.js';

/**
 * Offer Desk phase 2 in the browser: the recruiter builds and reorders a waitlist, the
 * offered student declines, and the opening goes to the first person on the waitlist
 * automatically.
 */
test('a declined offer goes to the next person on the waitlist automatically', async ({ page, request }) => {
  const runId = Date.now().toString(36);
  const recruiter = await createRecruiter(request, `Waitlist Co ${runId}`);
  const job = await postJob(request, recruiter, { title: `Waitlist Role ${runId}`, openings: 1 });
  const adminToken = await loginViaApi(request, adminCredentials());
  await api(request, 'PUT', `/api/admin/jobs/${job.id}/approve`, { token: adminToken, data: { status: 'APPROVED' }, expectStatus: 200 });

  // Three candidates apply and reach the interview stage.
  const names = ['Ana Offered', 'Ben Waiting', 'Cara Waiting'];
  const students = [];
  for (const name of names) {
    const student = await registerViaApi(request, 'STUDENT', name);
    await api(request, 'POST', `/api/students/jobs/${job.id}/apply`, { token: student.token, expectStatus: 201 });
    students.push(student);
  }
  const move = async (name, status) => {
    const { body } = await api(request, 'GET', `/api/recruiter/jobs/${job.id}/applications`, { token: recruiter.token, expectStatus: 200 });
    const app = body.find((a) => a.applicant.fullName === name);
    await api(request, 'PUT', `/api/recruiter/applications/${app.id}/status`, {
      token: recruiter.token, data: { status, version: app.version }, expectStatus: 200,
    });
    return app.id;
  };
  for (const name of names) {
    await move(name, 'SHORTLISTED');
    await move(name, 'INTERVIEW');
  }
  const anaApplication = await move('Ana Offered', 'OFFERED');

  // Recruiter, in the browser: waitlist Ben and Cara, then move Cara to the front.
  await loginInBrowser(page, recruiter);
  await page.goto(`/recruiter/jobs/${job.id}/applicants`);
  for (const name of ['Ben Waiting', 'Cara Waiting']) {
    const row = page.getByTestId('applicant').filter({ hasText: name });
    await row.getByRole('combobox').selectOption('WAITLISTED');
    // The status was saved: the dropdown now offers a waitlisted candidate's moves.
    await expect(row.getByRole('combobox').locator('option')).toHaveText(['Move to…', 'OFFERED', 'REJECTED']);
  }
  const waitlist = page.getByRole('region', { name: 'Waitlist' });
  await expect(waitlist.getByRole('listitem')).toHaveText([/Ben Waiting/, /Cara Waiting/]);
  await waitlist.getByRole('button', { name: 'Move Cara Waiting up' }).click();
  await expect(waitlist.getByRole('listitem')).toHaveText([/Cara Waiting/, /Ben Waiting/]);
  await expect(waitlist.getByRole('button', { name: 'Fill no open openings' })).toBeDisabled();

  // Ana declines in the browser.
  await page.evaluate(() => localStorage.clear());
  page.once('dialog', (dialog) => dialog.accept());
  await loginInBrowser(page, students[0]);
  await page.goto(`/student/applications/${anaApplication}`);
  await page.getByRole('button', { name: 'Decline' }).click();
  await expect(page.getByText('You declined this offer.')).toBeVisible();

  // Cara, first on the waitlist, now has the offer; Ben is still waiting, now #1.
  await page.evaluate(() => localStorage.clear());
  await loginInBrowser(page, students[2]);
  await page.getByRole('link', { name: /review offer/i }).click();
  await expect(page.getByText(/you have an offer from/i)).toBeVisible();

  const { body: bens } = await api(request, 'GET', '/api/students/applications', { token: students[1].token, expectStatus: 200 });
  const ben = await api(request, 'GET', `/api/students/applications/${bens[0].id}`, { token: students[1].token, expectStatus: 200 });
  expect(ben.body.status).toBe('WAITLISTED');
  expect(ben.body.waitlistPosition).toBe(1);
});

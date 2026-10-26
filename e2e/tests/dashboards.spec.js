import { test, expect } from '@playwright/test';
import {
  adminCredentials, api, createRecruiter, loginInBrowser, loginViaApi, postJob, registerViaApi,
} from './helpers.js';

/** Offer Desk phase 4: the recruiter's Offer Desk and the placement cell's overview. */
test('the Offer Desk and the placement overview show live offer data', async ({ page, request }) => {
  const runId = Date.now().toString(36);
  const recruiter = await createRecruiter(request, `Desk Co ${runId}`);
  const job = await postJob(request, recruiter, { title: `Desk Role ${runId}`, openings: 2 });
  const adminToken = await loginViaApi(request, adminCredentials());
  await api(request, 'PUT', `/api/admin/jobs/${job.id}/approve`, { token: adminToken, data: { status: 'APPROVED', tier: 'DREAM' }, expectStatus: 200 });

  const student = await registerViaApi(request, 'STUDENT', `Olu Offered ${runId}`);
  await api(request, 'POST', `/api/students/jobs/${job.id}/apply`, { token: student.token, expectStatus: 201 });
  for (const status of ['SHORTLISTED', 'INTERVIEW', 'OFFERED']) {
    const { body } = await api(request, 'GET', `/api/recruiter/jobs/${job.id}/applications`, { token: recruiter.token, expectStatus: 200 });
    await api(request, 'PUT', `/api/recruiter/applications/${body[0].id}/status`, {
      token: recruiter.token, data: { status, version: body[0].version }, expectStatus: 200,
    });
  }

  // Recruiter: the Offer Desk lists the pending offer with its countdown.
  await loginInBrowser(page, recruiter);
  await page.getByRole('link', { name: 'Offer Desk' }).first().click();
  await expect(page).toHaveURL(/\/recruiter\/offer-desk$/);
  const deskJob = page.getByTestId('desk-job').filter({ hasText: job.title });
  await expect(deskJob).toContainText(`Olu Offered ${runId}`);
  await expect(deskJob).toContainText(/left/);
  await expect(deskJob).toContainText('Dream');
  await expect(page.getByRole('region', { name: 'Recent activity' })).toContainText(`Olu Offered ${runId}`);

  // Placement cell: the overview on the dashboard counts the pending offer.
  await page.evaluate(() => localStorage.clear());
  await loginInBrowser(page, adminCredentials());
  const overview = page.getByRole('region', { name: 'Placement overview' });
  await expect(overview).toContainText('Placement rate');
  await expect(overview.getByText('Pending').locator('xpath=ancestor::li')).not.toHaveText(/\b0$/);
});

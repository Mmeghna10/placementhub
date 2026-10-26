import { test, expect } from '@playwright/test';
import {
  adminCredentials, api, createRecruiter, loginInBrowser, loginViaApi, postJob, registerViaApi,
} from './helpers.js';

/**
 * Offer Desk phase 3 in the browser: the placement cell approves a job as Dream, a student
 * placed at a Regular company upgrades to it, the Regular placement is released, and that
 * company's opening goes to its waitlist automatically.
 */
test('upgrading to a Dream offer releases the Regular placement and refills it from the waitlist', async ({ page, request }) => {
  const runId = Date.now().toString(36);
  const admin = adminCredentials();
  const adminToken = await loginViaApi(request, admin);

  const regularCo = await createRecruiter(request, `Regular Co ${runId}`);
  const dreamCo = await createRecruiter(request, `Dream Co ${runId}`);
  const regularJob = await postJob(request, regularCo, { title: `Regular Role ${runId}`, openings: 1 });
  const dreamJob = await postJob(request, dreamCo, { title: `Dream Role ${runId}`, openings: 1 });
  await api(request, 'PUT', `/api/admin/jobs/${regularJob.id}/approve`, {
    token: adminToken, data: { status: 'APPROVED', tier: 'REGULAR' }, expectStatus: 200,
  });

  // The placement cell approves the second job as Dream, in the browser.
  await loginInBrowser(page, admin);
  await page.goto('/admin/jobs?pending=true');
  const row = page.getByTestId('job-row').filter({ hasText: dreamJob.title });
  await row.getByRole('combobox', { name: `Tier for ${dreamJob.title}` }).selectOption('DREAM');
  await row.getByRole('button', { name: 'Approve' }).click();
  await expect(row).toHaveCount(0);

  // Asha is placed at the Regular company; Ravi waits on its waitlist.
  const asha = await registerViaApi(request, 'STUDENT', 'Asha Upgrader');
  const ravi = await registerViaApi(request, 'STUDENT', 'Ravi Waiting');
  for (const [student, job] of [[asha, regularJob], [ravi, regularJob], [asha, dreamJob]]) {
    await api(request, 'POST', `/api/students/jobs/${job.id}/apply`, { token: student.token, expectStatus: 201 });
  }
  const move = async (recruiter, job, name, status) => {
    const { body } = await api(request, 'GET', `/api/recruiter/jobs/${job.id}/applications`, { token: recruiter.token, expectStatus: 200 });
    const app = body.find((a) => a.applicant.fullName === name);
    await api(request, 'PUT', `/api/recruiter/applications/${app.id}/status`, {
      token: recruiter.token, data: { status, version: app.version }, expectStatus: 200,
    });
    return app.id;
  };
  for (const status of ['SHORTLISTED', 'INTERVIEW']) {
    await move(regularCo, regularJob, 'Asha Upgrader', status);
    await move(regularCo, regularJob, 'Ravi Waiting', status);
    await move(dreamCo, dreamJob, 'Asha Upgrader', status);
  }
  const regularApplication = await move(regularCo, regularJob, 'Asha Upgrader', 'OFFERED');
  await api(request, 'POST', `/api/students/applications/${regularApplication}/offer/accept`, { token: asha.token, expectStatus: 200 });
  await move(regularCo, regularJob, 'Ravi Waiting', 'WAITLISTED');
  const dreamApplication = await move(dreamCo, dreamJob, 'Asha Upgrader', 'OFFERED');

  // Asha accepts the Dream offer in the browser.
  await page.evaluate(() => localStorage.clear());
  await loginInBrowser(page, asha);
  await page.goto(`/student/applications/${dreamApplication}`);
  await expect(page.getByText(/releases it/i)).toBeVisible();
  await page.getByRole('button', { name: 'Accept offer' }).click();
  await expect(page.getByText(/you're placed/i)).toBeVisible();
  await page.goto('/student/dashboard');
  await expect(page.getByText(/\(Dream\)/)).toBeVisible();

  // The Regular placement was released, and its opening went to Ravi automatically.
  await page.goto(`/student/applications/${regularApplication}`);
  await expect(page.getByText(/gave up this placement/i)).toBeVisible();
  const { body: ravis } = await api(request, 'GET', '/api/students/applications', { token: ravi.token, expectStatus: 200 });
  expect(ravis.find((a) => a.jobId === regularJob.id).status).toBe('OFFERED');
});

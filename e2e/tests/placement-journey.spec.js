import { test, expect } from '@playwright/test';
import {
  PASSWORD, adminCredentials, api, createRecruiter, loginInBrowser, loginViaApi, postJob, uniqueEmail,
} from './helpers.js';

/**
 * The main placement journey, end to end through the browser, in the order it happens:
 * a student signs up, the admin approves a job, the student finds and applies to it,
 * a duplicate is refused, and the recruiter reviews the application.
 *
 * Test data that is not the subject of a step (the recruiter, the jobs) is created
 * through the API to keep each step focused on the behaviour it checks.
 */
test.describe.serial('Placement journey', () => {
  const runId = Date.now().toString(36);
  const student = { email: uniqueEmail('student'), password: PASSWORD, name: 'Asha Student' };
  const eligibleTitle = `Backend Engineer ${runId}`;
  const ineligibleTitle = `Quant Analyst ${runId}`;
  let recruiter;
  let eligibleJob;
  let ineligibleJob;
  let applicationId;

  test.beforeAll(async ({ request }) => {
    recruiter = await createRecruiter(request, `Acme ${runId}`);
    // The student will have CGPA 8.2 in Computer Science.
    eligibleJob = await postJob(request, recruiter, {
      title: eligibleTitle, minCgpa: 7.0, eligibleBranches: 'Computer Science, Information Technology',
    });
    ineligibleJob = await postJob(request, recruiter, {
      title: ineligibleTitle, minCgpa: 9.5, eligibleBranches: 'Computer Science',
    });
    expect(eligibleJob.status).toBe('PENDING');
  });

  test('student registers, logs in and completes the profile', async ({ page }) => {
    await page.goto('/register');
    await page.getByRole('button', { name: 'Student', exact: true }).click();
    await page.getByLabel('Full Name').fill(student.name);
    await page.getByLabel('Email').fill(student.email);
    await page.getByLabel('Password').fill(student.password);
    await page.getByRole('button', { name: 'Create Account' }).click();
    await expect(page).toHaveURL(/\/student\/dashboard$/);

    // Log out by clearing the session, then log in through the form.
    await page.evaluate(() => localStorage.clear());
    await loginInBrowser(page, student);
    await expect(page).toHaveURL(/\/student\/dashboard$/);

    // Complete the profile so eligibility can be checked.
    await page.goto('/student/profile');
    await page.getByRole('button', { name: 'Edit' }).click();
    await page.locator('input[name="college"]').fill('National Institute of Technology');
    await page.locator('input[name="branch"]').fill('Computer Science');
    await page.locator('input[name="cgpa"]').fill('8.2');
    await page.locator('input[name="skills"]').fill('Java, Spring Boot, SQL');
    await page.getByRole('button', { name: 'Save Changes' }).click();
    await expect(page.getByText('Computer Science', { exact: true })).toBeVisible();
    await expect(page.getByText('8.2', { exact: true })).toBeVisible();
  });

  test('admin approves the pending jobs', async ({ page, request }) => {
    await loginInBrowser(page, adminCredentials());
    await expect(page).toHaveURL(/\/admin\/dashboard$/);

    await page.goto('/admin/jobs?pending=true');
    await expect(page.getByRole('heading', { name: 'Pending Approvals' })).toBeVisible();
    for (const title of [eligibleTitle, ineligibleTitle]) {
      const row = page.getByTestId('job-row').filter({ hasText: title });
      await row.getByRole('button', { name: 'Approve' }).click();
      await expect(row).toHaveCount(0);
    }

    // The decision is stored: both jobs are now APPROVED.
    const adminToken = await loginViaApi(request, adminCredentials());
    const { body } = await api(request, 'GET', '/api/admin/jobs?size=100', { token: adminToken, expectStatus: 200 });
    const approved = body.content.filter((j) => [eligibleJob.id, ineligibleJob.id].includes(j.id));
    expect(approved.map((j) => j.status)).toEqual(['APPROVED', 'APPROVED']);
  });

  test('student sees the jobs they are eligible for', async ({ page }) => {
    await loginInBrowser(page, student);
    await page.goto('/student/jobs');
    await page.getByLabel('Search jobs').fill(runId);
    await page.getByLabel('Search jobs').press('Enter');

    // "All" lists both open jobs.
    await expect(page.getByText(eligibleTitle)).toBeVisible();
    await expect(page.getByText(ineligibleTitle)).toBeVisible();

    // "Recommended" keeps only the job whose CGPA and branch rules the student meets.
    await page.getByRole('button', { name: 'Recommended' }).click();
    await expect(page.getByRole('heading', { name: 'Recommended For You' })).toBeVisible();
    await expect(page.getByText(eligibleTitle)).toBeVisible();
    await expect(page.getByText(ineligibleTitle)).toHaveCount(0);
  });

  test('student applies for a job', async ({ page }) => {
    await loginInBrowser(page, student);
    await expect(page).toHaveURL(/\/student\/dashboard$/);
    await page.goto(`/student/jobs/${eligibleJob.id}`);
    await expect(page.getByRole('heading', { name: eligibleTitle })).toBeVisible();

    const apply = page.getByRole('button', { name: 'Apply Now' });
    // A double-click must still send exactly one application.
    const applyRequests = [];
    page.on('request', (r) => r.method() === 'POST' && r.url().includes('/apply') && applyRequests.push(r));
    await apply.dblclick();

    await expect(page.getByText('You have applied to this job.')).toBeVisible();
    expect(applyRequests).toHaveLength(1);

    await page.getByRole('link', { name: /View your application/ }).click();
    await expect(page).toHaveURL(/\/student\/applications\/\d+$/);
    applicationId = Number(page.url().split('/').pop());
    await expect(page.getByText('APPLIED').first()).toBeVisible();
  });

  test('a duplicate application is rejected', async ({ page, request }) => {
    const token = await loginViaApi(request, student);

    // Directly against the API, bypassing the disabled button: the database refuses it.
    const duplicate = await api(request, 'POST', `/api/students/jobs/${eligibleJob.id}/apply`, { token });
    expect(duplicate.status).toBe(409);
    expect(duplicate.body.code).toBe('APPLICATION_ALREADY_EXISTS');

    // Still exactly one application for this job.
    const { body: mine } = await api(request, 'GET', '/api/students/applications', { token, expectStatus: 200 });
    expect(mine.filter((a) => a.jobId === eligibleJob.id)).toHaveLength(1);

    // The job page shows "applied" instead of offering the button again.
    await loginInBrowser(page, student);
    await expect(page).toHaveURL(/\/student\/dashboard$/);
    await page.goto(`/student/jobs/${eligibleJob.id}`);
    await expect(page.getByText('You have applied to this job.')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Apply Now' })).toHaveCount(0);

    // A job the student is not eligible for is refused by the server too.
    const notEligible = await api(request, 'POST', `/api/students/jobs/${ineligibleJob.id}/apply`, { token });
    expect(notEligible.status).toBe(422);
    expect(notEligible.body.code).toBe('NOT_ELIGIBLE');
  });

  test('recruiter reviews the application for their own job', async ({ page, request }) => {
    await loginInBrowser(page, recruiter);
    await expect(page).toHaveURL(/\/recruiter\/dashboard$/);
    await page.goto(`/recruiter/jobs/${eligibleJob.id}/applicants`);

    const applicant = page.getByTestId('applicant').filter({ hasText: student.name });
    await expect(applicant).toContainText('National Institute of Technology');
    await expect(applicant).toContainText('APPLIED');

    // Only the allowed next steps are offered, then the move is saved.
    const move = applicant.getByRole('combobox');
    await expect(move.locator('option')).toHaveText(['Move to…', 'SHORTLISTED', 'REJECTED']);
    await move.selectOption('SHORTLISTED');
    await expect(applicant).toContainText('SHORTLISTED');

    // The student sees the new status and the timeline entry.
    const token = await loginViaApi(request, student);
    const { body } = await api(request, 'GET', `/api/students/applications/${applicationId}`, { token, expectStatus: 200 });
    expect(body.status).toBe('SHORTLISTED');
    expect(body.timeline.map((t) => t.status)).toEqual(['APPLIED', 'SHORTLISTED']);
  });
});

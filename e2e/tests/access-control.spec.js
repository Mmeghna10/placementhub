import { test, expect } from '@playwright/test';
import {
  adminCredentials, api, createRecruiter, loginInBrowser, loginViaApi, postJob, registerViaApi,
} from './helpers.js';

/**
 * Invalid and unauthorized requests are rejected, both in the browser and when the
 * API is called directly (which is what an attacker would do).
 */
test.describe('Invalid and unauthorized requests are rejected', () => {
  const runId = Date.now().toString(36);
  let owner;        // student who owns the application
  let otherStudent; // student trying to read it
  let recruiter;    // recruiter who owns the job
  let otherRecruiter;
  let job;
  let applicationId;

  test.beforeAll(async ({ request }) => {
    recruiter = await createRecruiter(request, `Owner Co ${runId}`);
    otherRecruiter = await createRecruiter(request, `Rival Co ${runId}`);
    job = await postJob(request, recruiter, { title: `Security Test Role ${runId}` });

    const adminToken = await loginViaApi(request, adminCredentials());
    await api(request, 'PUT', `/api/admin/jobs/${job.id}/approve`, {
      token: adminToken, data: { status: 'APPROVED' }, expectStatus: 200,
    });

    owner = await registerViaApi(request, 'STUDENT', 'Owner Student');
    otherStudent = await registerViaApi(request, 'STUDENT', 'Other Student');
    const { body } = await api(request, 'POST', `/api/students/jobs/${job.id}/apply`, {
      token: owner.token, expectStatus: 201,
    });
    applicationId = body.id;
  });

  test('requests without a valid token get 401', async ({ request }) => {
    const noToken = await api(request, 'GET', '/api/students/applications');
    expect(noToken.status).toBe(401);
    expect(noToken.body.code).toBe('UNAUTHORIZED');

    const forged = await api(request, 'GET', '/api/students/applications', { token: 'not-a-real-token' });
    expect(forged.status).toBe(401);
    expect(forged.body.code).toBe('INVALID_TOKEN');
  });

  test('a wrong password is refused and shown on the login form', async ({ page }) => {
    await loginInBrowser(page, { email: owner.email, password: 'definitely-wrong' }, { expectSuccess: false });
    await expect(page.getByText('Invalid email or password')).toBeVisible();
    await expect(page).toHaveURL(/\/login$/);
  });

  test('a student cannot use admin or recruiter APIs (403)', async ({ request }) => {
    for (const path of ['/api/admin/dashboard', '/api/recruiter/jobs']) {
      const response = await api(request, 'GET', path, { token: owner.token });
      expect(response.status, path).toBe(403);
    }
    const approve = await api(request, 'PUT', `/api/admin/jobs/${job.id}/approve`, {
      token: owner.token, data: { status: 'REJECTED' },
    });
    expect(approve.status).toBe(403);
  });

  test("a student cannot see another student's application (404)", async ({ request }) => {
    const own = await api(request, 'GET', `/api/students/applications/${applicationId}`, { token: owner.token });
    expect(own.status).toBe(200);

    // Same answer as for an id that does not exist, so ids cannot be probed.
    const other = await api(request, 'GET', `/api/students/applications/${applicationId}`, { token: otherStudent.token });
    expect(other.status).toBe(404);
    const missing = await api(request, 'GET', '/api/students/applications/999999999', { token: otherStudent.token });
    expect(missing.status).toBe(404);
    expect(other.body.error).toBe(missing.body.error);

    const { body: list } = await api(request, 'GET', '/api/students/applications', {
      token: otherStudent.token, expectStatus: 200,
    });
    expect(list).toEqual([]);
  });

  test("a recruiter cannot see or change another company's applications (404)", async ({ request }) => {
    const applicants = await api(request, 'GET', `/api/recruiter/jobs/${job.id}/applications`, {
      token: otherRecruiter.token,
    });
    expect(applicants.status).toBe(404);

    const update = await api(request, 'PUT', `/api/recruiter/applications/${applicationId}/status`, {
      token: otherRecruiter.token, data: { status: 'REJECTED', version: 0 },
    });
    expect(update.status).toBe(404);

    // The real owner still sees it untouched.
    const { body } = await api(request, 'GET', `/api/recruiter/jobs/${job.id}/applications`, {
      token: recruiter.token, expectStatus: 200,
    });
    expect(body.map((a) => a.status)).toEqual(['APPLIED']);
  });

  test('invalid input is rejected with field errors (400)', async ({ request }) => {
    const badRegister = await api(request, 'POST', '/api/auth/register', {
      data: { email: 'not-an-email', password: '1', role: 'STUDENT', fullName: '' },
    });
    expect(badRegister.status).toBe(400);
    expect(badRegister.body.code).toBe('VALIDATION_FAILED');
    expect(Object.keys(badRegister.body.fieldErrors)).toEqual(
      expect.arrayContaining(['email', 'password', 'fullName']),
    );

    const badCgpa = await api(request, 'PUT', '/api/students/profile', {
      token: owner.token, data: { fullName: 'Owner Student', cgpa: 42 },
    });
    expect(badCgpa.status).toBe(400);
    expect(badCgpa.body.fieldErrors.cgpa).toBeTruthy();
  });

  test('nobody can sign up as an admin', async ({ request }) => {
    const response = await api(request, 'POST', '/api/auth/register', {
      data: { email: `wannabe-${runId}@e2e.test`, password: 'e2e-Password-123', role: 'ADMIN', fullName: 'Wannabe' },
    });
    expect(response.status).toBeGreaterThanOrEqual(400);
    expect(response.status).toBeLessThan(500);
    expect((await api(request, 'POST', '/api/auth/login', {
      data: { email: `wannabe-${runId}@e2e.test`, password: 'e2e-Password-123' },
    })).status).toBe(401);
  });

  test('the browser keeps a student out of admin and recruiter pages', async ({ page }) => {
    await loginInBrowser(page, owner);
    await expect(page).toHaveURL(/\/student\/dashboard$/);

    for (const path of ['/admin/dashboard', '/recruiter/jobs']) {
      await page.goto(path);
      await expect(page, path).toHaveURL(/\/login$/);
    }
  });

  test('a logged-out visitor is sent to the login page', async ({ page }) => {
    await page.goto('/student/applications');
    await expect(page).toHaveURL(/\/login$/);
  });
});

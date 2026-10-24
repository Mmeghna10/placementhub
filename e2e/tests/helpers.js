import { expect } from '@playwright/test';

export const PASSWORD = 'e2e-Password-123';

// Unique per run, so the tests also work against a stack that already holds data.
const run = Date.now().toString(36);
let counter = 0;
export const uniqueEmail = (who) => `${who}-${run}-${++counter}@e2e.test`;

export function adminCredentials() {
  const email = process.env.E2E_ADMIN_EMAIL;
  const password = process.env.E2E_ADMIN_PASSWORD;
  if (!email || !password) {
    throw new Error('Set E2E_ADMIN_EMAIL and E2E_ADMIN_PASSWORD (run-e2e.ps1 does this for you)');
  }
  return { email, password };
}

const auth = (token) => ({ Authorization: `Bearer ${token}` });

/** Calls the API (through the same nginx as the browser) and checks the status code. */
export async function api(request, method, path, { token, data, expectStatus } = {}) {
  const response = await request.fetch(path, {
    method,
    headers: token ? auth(token) : {},
    data,
  });
  if (expectStatus !== undefined) {
    expect(response.status(), `${method} ${path}: ${await response.text()}`).toBe(expectStatus);
  }
  const text = await response.text();
  return { status: response.status(), body: text ? JSON.parse(text) : null };
}

export async function registerViaApi(request, role, fullName) {
  const email = uniqueEmail(role.toLowerCase());
  const { body } = await api(request, 'POST', '/api/auth/register', {
    data: { email, password: PASSWORD, role, fullName },
    expectStatus: 201,
  });
  return { email, password: PASSWORD, token: body.token };
}

export async function loginViaApi(request, { email, password }) {
  const { body } = await api(request, 'POST', '/api/auth/login', {
    data: { email, password },
    expectStatus: 200,
  });
  return body.token;
}

/** A recruiter with a company profile, ready to post jobs. */
export async function createRecruiter(request, companyName) {
  const recruiter = await registerViaApi(request, 'RECRUITER', `Rita ${companyName}`);
  await api(request, 'PUT', '/api/recruiter/profile', {
    token: recruiter.token,
    data: { fullName: `Rita ${companyName}`, designation: 'Hiring Manager', companyName },
    expectStatus: 200,
  });
  return recruiter;
}

export function inDays(days) {
  const date = new Date();
  date.setDate(date.getDate() + days);
  return date.toISOString().slice(0, 10);
}

export async function postJob(request, recruiter, job) {
  const { body } = await api(request, 'POST', '/api/recruiter/jobs', {
    token: recruiter.token,
    data: {
      description: 'End-to-end test job',
      jobType: 'FULL_TIME',
      location: 'Bengaluru',
      salaryMin: 600000,
      salaryMax: 900000,
      requiredSkills: 'Java, SQL',
      applicationDeadline: inDays(30),
      ...job,
    },
    expectStatus: 201,
  });
  return body;
}

/** Logs in through the form and, unless told otherwise, waits until the dashboard has loaded. */
export async function loginInBrowser(page, { email, password }, { expectSuccess = true } = {}) {
  await page.goto('/login');
  await page.getByLabel('Email').fill(email);
  await page.getByLabel('Password').fill(password);
  await page.getByRole('button', { name: 'Sign In' }).click();
  if (expectSuccess) {
    await expect(page).toHaveURL(/\/(student|recruiter|admin)\/dashboard$/);
  }
}

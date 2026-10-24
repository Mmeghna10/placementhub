import { defineConfig, devices } from '@playwright/test';

// Runs against an already started stack (see run-e2e.ps1 or the CI job), never against
// the live site: the tests create users and jobs.
export default defineConfig({
  testDir: './tests',
  // The journeys share one database and build on each other's data, so run them in order.
  workers: 1,
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: 0,
  timeout: 60_000,
  expect: { timeout: 10_000 },
  reporter: process.env.CI ? [['list'], ['html', { open: 'never' }]] : [['list']],
  use: {
    baseURL: process.env.E2E_BASE_URL || 'http://localhost:3200',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
});

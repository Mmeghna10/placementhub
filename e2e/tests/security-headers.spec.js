import { test, expect } from '@playwright/test';
import { adminCredentials, loginInBrowser } from './helpers.js';

/**
 * The page is served with a Content-Security-Policy (limits what an injected script could
 * do with the token in localStorage) and anti-framing headers, and no page breaks under it.
 */
test.describe('Security headers', () => {
  test('the app page is served with a CSP and anti-framing headers', async ({ page }) => {
    const response = await page.goto('/login');
    const headers = response.headers();

    expect(headers['content-security-policy']).toContain("script-src 'self'");
    expect(headers['content-security-policy']).toContain("frame-ancestors 'none'");
    expect(headers['x-frame-options']).toBe('DENY');
    expect(headers['x-content-type-options']).toBe('nosniff');
  });

  test('no page violates the CSP, including charts and fonts', async ({ page }) => {
    const violations = [];
    page.on('console', (msg) => {
      if (/content security policy|refused to (load|apply|connect|execute)/i.test(msg.text())) {
        violations.push(msg.text());
      }
    });

    await page.goto('/');
    await expect(page.getByRole('heading', { name: /every application/i })).toBeVisible();
    await loginInBrowser(page, adminCredentials());
    for (const path of ['/admin/dashboard', '/admin/jobs', '/admin/analytics']) {
      await page.goto(path);
      await page.waitForLoadState('networkidle');
    }
    // The analytics page renders its charts (SVG with inline styles).
    await expect(page.locator('svg.recharts-surface').first()).toBeVisible();

    expect(violations).toEqual([]);
  });
});

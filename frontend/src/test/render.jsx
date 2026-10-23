import { render } from '@testing-library/react';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import { AuthProvider } from '../context/AuthContext';

/** Shows the current URL, so tests can assert where the app navigated to. */
function CurrentPath() {
  const location = useLocation();
  return <div data-testid="current-path">{location.pathname}</div>;
}

/**
 * Renders a page the way the app does (router + auth context), optionally logged in.
 *
 * @param ui        the page element, e.g. <JobDetails />
 * @param route     the starting URL, e.g. '/student/jobs/7'
 * @param path      the route pattern the page is mounted on, e.g. '/student/jobs/:jobId'
 * @param user      { email, role } to log in as, or null for a visitor
 *
 * Any navigation away from `path` lands on a stub that prints the new URL
 * (screen.getByTestId('current-path')).
 */
export function renderPage(ui, { route = '/', path = route, user = null } = {}) {
  if (user) {
    localStorage.setItem('token', 'test-token');
    localStorage.setItem('user', JSON.stringify(user));
  }
  return render(
    <AuthProvider>
      <MemoryRouter initialEntries={[route]}>
        <Routes>
          <Route path={path} element={ui} />
          <Route path="*" element={<CurrentPath />} />
        </Routes>
      </MemoryRouter>
    </AuthProvider>,
  );
}

export const STUDENT = { email: 'sam@student.test', role: 'STUDENT' };
export const RECRUITER = { email: 'rita@recruiter.test', role: 'RECRUITER' };
export const ADMIN = { email: 'ada@admin.test', role: 'ADMIN' };

/** An axios-style error, as the API helpers reject with. */
export function apiError(status, body = {}) {
  const error = new Error(`Request failed with status code ${status}`);
  error.response = { status, data: body };
  return error;
}

/** A promise that stays pending until resolve() is called (for loading states). */
export function deferred() {
  let resolve;
  let reject;
  const promise = new Promise((res, rej) => {
    resolve = res;
    reject = rej;
  });
  return { promise, resolve, reject };
}

import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { apiError, deferred, renderPage } from '../../test/render';
import { loginUser } from '../../api/authApi';
import Login from './Login';

vi.mock('../../api/authApi');

async function submit(email = 'sam@student.test', password = 'secret123') {
  const user = userEvent.setup();
  await user.type(screen.getByLabelText(/email/i), email);
  await user.type(screen.getByLabelText(/password/i), password);
  await user.click(screen.getByRole('button', { name: /sign in/i }));
}

describe('Login page', () => {
  it.each([
    ['STUDENT', '/student/dashboard'],
    ['RECRUITER', '/recruiter/dashboard'],
    ['ADMIN', '/admin/dashboard'],
  ])('logs a %s in and opens their dashboard', async (role, dashboard) => {
    loginUser.mockResolvedValue({ data: { token: 'jwt-token', email: 'sam@student.test', role } });
    renderPage(<Login />, { route: '/login' });

    await submit();

    expect(await screen.findByTestId('current-path')).toHaveTextContent(dashboard);
    expect(loginUser).toHaveBeenCalledWith({ email: 'sam@student.test', password: 'secret123' });
    expect(localStorage.getItem('token')).toBe('jwt-token');
    expect(JSON.parse(localStorage.getItem('user'))).toEqual({ email: 'sam@student.test', role });
  });

  it('shows the server message for a wrong password and stays on the page', async () => {
    loginUser.mockRejectedValue(apiError(401, { error: 'Invalid email or password', code: 'INVALID_CREDENTIALS' }));
    renderPage(<Login />, { route: '/login' });

    await submit();

    expect(await screen.findByText('Invalid email or password')).toBeInTheDocument();
    expect(screen.queryByTestId('current-path')).not.toBeInTheDocument();
    expect(localStorage.getItem('token')).toBeNull();
  });

  it('shows the rate-limit message after too many attempts', async () => {
    loginUser.mockRejectedValue(apiError(429, {
      error: 'Too many requests. Please wait 6 seconds and try again.', code: 'RATE_LIMITED',
    }));
    renderPage(<Login />, { route: '/login' });

    await submit();

    expect(await screen.findByText(/too many requests/i)).toBeInTheDocument();
  });

  it('falls back to a generic message when the server sends none', async () => {
    loginUser.mockRejectedValue(new Error('Network Error'));
    renderPage(<Login />, { route: '/login' });

    await submit();

    expect(await screen.findByText('Invalid email or password')).toBeInTheDocument();
  });

  it('disables the button while signing in, so the form is not sent twice', async () => {
    const pending = deferred();
    loginUser.mockReturnValue(pending.promise);
    renderPage(<Login />, { route: '/login' });

    await submit();

    const button = screen.getByRole('button', { name: /signing in|sign in/i });
    expect(button).toBeDisabled();
    pending.resolve({ data: { token: 't', email: 'sam@student.test', role: 'STUDENT' } });
    expect(await screen.findByTestId('current-path')).toHaveTextContent('/student/dashboard');
  });
});

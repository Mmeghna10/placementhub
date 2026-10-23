import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { apiError, renderPage } from '../../test/render';
import { registerUser } from '../../api/authApi';
import Register from './Register';

vi.mock('../../api/authApi');

async function fillAndSubmit({ role } = {}) {
  const user = userEvent.setup();
  if (role) await user.click(screen.getByRole('button', { name: role }));
  await user.type(screen.getByLabelText(/full name/i), 'Rita Recruiter');
  await user.type(screen.getByLabelText(/email/i), 'rita@company.test');
  await user.type(screen.getByLabelText(/password/i), 'secret123');
  await user.click(screen.getByRole('button', { name: /create account/i }));
}

describe('Register page', () => {
  it('registers a student by default and opens the student dashboard', async () => {
    registerUser.mockResolvedValue({ data: { token: 't', email: 'rita@company.test', role: 'STUDENT' } });
    renderPage(<Register />, { route: '/register' });

    await fillAndSubmit();

    expect(registerUser).toHaveBeenCalledWith(expect.objectContaining({ role: 'STUDENT', email: 'rita@company.test' }));
    expect(await screen.findByTestId('current-path')).toHaveTextContent('/student/dashboard');
  });

  it('sends RECRUITER when the recruiter option is chosen', async () => {
    registerUser.mockResolvedValue({ data: { token: 't', email: 'rita@company.test', role: 'RECRUITER' } });
    renderPage(<Register />, { route: '/register' });

    await fillAndSubmit({ role: 'Recruiter' });

    expect(registerUser).toHaveBeenCalledWith(expect.objectContaining({ role: 'RECRUITER', fullName: 'Rita Recruiter' }));
    expect(await screen.findByTestId('current-path')).toHaveTextContent('/recruiter/dashboard');
  });

  it('offers no way to sign up as an admin', () => {
    renderPage(<Register />, { route: '/register' });
    expect(screen.queryByRole('button', { name: /admin/i })).not.toBeInTheDocument();
  });

  it('shows the server message, e.g. for an email that is already registered', async () => {
    registerUser.mockRejectedValue(apiError(409, { error: 'Email already registered', code: 'EMAIL_ALREADY_REGISTERED' }));
    renderPage(<Register />, { route: '/register' });

    await fillAndSubmit();

    expect(await screen.findByText('Email already registered')).toBeInTheDocument();
    expect(localStorage.getItem('token')).toBeNull();
  });
});

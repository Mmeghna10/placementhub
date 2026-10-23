import { AxiosError } from 'axios';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import axiosInstance, { sessionNavigation } from './axiosInstance';

/** Replaces the network with a fake server that answers `status`. */
function respondWith(status, data = {}) {
  const seen = [];
  axiosInstance.defaults.adapter = (config) => {
    seen.push(config);
    const response = { status, statusText: String(status), data, headers: {}, config };
    if (status < 400) return Promise.resolve(response);
    return Promise.reject(new AxiosError(`status ${status}`, 'ERR_BAD_RESPONSE', config, null, response));
  };
  return seen;
}

describe('axiosInstance', () => {
  beforeEach(() => {
    vi.spyOn(sessionNavigation, 'toLogin').mockImplementation(() => {});
  });

  it('sends the stored token as a Bearer header', async () => {
    localStorage.setItem('token', 'abc.def.ghi');
    const seen = respondWith(200);

    await axiosInstance.get('/api/students/profile');

    expect(seen[0].headers.Authorization).toBe('Bearer abc.def.ghi');
  });

  it('sends no Authorization header when logged out', async () => {
    const seen = respondWith(200);
    await axiosInstance.get('/api/students/jobs/search');
    expect(seen[0].headers.Authorization).toBeUndefined();
  });

  it('ends the session and goes to login when a logged-in request gets 401', async () => {
    localStorage.setItem('token', 'expired');
    localStorage.setItem('user', JSON.stringify({ email: 'a@b.test', role: 'STUDENT' }));
    respondWith(401, { code: 'TOKEN_EXPIRED' });

    await expect(axiosInstance.get('/api/students/applications')).rejects.toThrow();

    expect(localStorage.getItem('token')).toBeNull();
    expect(localStorage.getItem('user')).toBeNull();
    expect(sessionNavigation.toLogin).toHaveBeenCalledOnce();
  });

  it('lets a wrong-password 401 from /api/auth/login reach the form (regression)', async () => {
    // Before the fix this reloaded the login page and the error message never showed.
    respondWith(401, { error: 'Invalid email or password' });

    const error = await axiosInstance.post('/api/auth/login', {}).catch((e) => e);

    expect(error.response.data.error).toBe('Invalid email or password');
    expect(sessionNavigation.toLogin).not.toHaveBeenCalled();
  });

  it('does not log out on other errors (403, 409, 500)', async () => {
    localStorage.setItem('token', 'valid');
    for (const status of [403, 409, 500]) {
      respondWith(status);
      await axiosInstance.get('/api/x').catch(() => {});
    }
    expect(localStorage.getItem('token')).toBe('valid');
    expect(sessionNavigation.toLogin).not.toHaveBeenCalled();
  });
});

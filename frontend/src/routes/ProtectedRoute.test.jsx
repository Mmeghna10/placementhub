import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { RECRUITER, STUDENT, renderPage } from '../test/render';
import ProtectedRoute from './ProtectedRoute';

const studentArea = (
  <ProtectedRoute allowedRole="STUDENT">
    <p>Student area</p>
  </ProtectedRoute>
);

describe('ProtectedRoute', () => {
  it('sends visitors who are not logged in to /login', () => {
    renderPage(studentArea, { route: '/student/jobs' });

    expect(screen.queryByText('Student area')).not.toBeInTheDocument();
    expect(screen.getByTestId('current-path')).toHaveTextContent('/login');
  });

  it('keeps users of another role out', () => {
    renderPage(studentArea, { route: '/student/jobs', user: RECRUITER });

    expect(screen.queryByText('Student area')).not.toBeInTheDocument();
    expect(screen.getByTestId('current-path')).toHaveTextContent('/login');
  });

  it('shows the page to the right role', () => {
    renderPage(studentArea, { route: '/student/jobs', user: STUDENT });

    expect(screen.getByText('Student area')).toBeInTheDocument();
  });
});

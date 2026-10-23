// Runs before every test file.
import '@testing-library/jest-dom/vitest';
import { afterEach, vi } from 'vitest';
import { cleanup } from '@testing-library/react';

afterEach(() => {
  // Unmount rendered components and forget the logged-in user, so tests never leak state.
  cleanup();
  localStorage.clear();
  vi.clearAllMocks();
});

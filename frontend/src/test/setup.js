// Runs before every test file.
import '@testing-library/jest-dom/vitest';
import { afterEach, vi } from 'vitest';
import { cleanup, configure } from '@testing-library/react';

// findBy*/waitFor give up after 1 s by default, which a busy CI runner can exceed while
// rendering a page. Waiting longer only matters when something is slow; passing tests
// still finish as soon as the element appears.
configure({ asyncUtilTimeout: 5000 });

afterEach(() => {
  // Unmount rendered components and forget the logged-in user, so tests never leak state.
  cleanup();
  localStorage.clear();
  vi.clearAllMocks();
});

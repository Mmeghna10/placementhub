import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  test: {
    // A simulated browser (DOM, localStorage, events) inside Node.
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.js'],
    include: ['src/**/*.test.{js,jsx}'],
    // Styles don't affect behaviour under test; skipping them keeps tests fast.
    css: false,
    // Typing tests (user-event, one keystroke at a time) need ~1 s normally but can pass 5 s
    // on a busy machine or CI runner; a timed-out test then keeps typing into the next one.
    testTimeout: 15_000,
    coverage: {
      provider: 'v8',
      include: ['src/**/*.{js,jsx}'],
      exclude: ['src/test/**', 'src/**/*.test.{js,jsx}', 'src/main.jsx'],
    },
  },
})

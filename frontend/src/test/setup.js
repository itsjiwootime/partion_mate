import '@testing-library/jest-dom/vitest';
import { afterEach, vi } from 'vitest';
import { cleanup } from '@testing-library/react';

if (!globalThis.crypto) {
  Object.defineProperty(globalThis, 'crypto', {
    value: {},
    configurable: true,
  });
}

if (!globalThis.crypto.randomUUID) {
  globalThis.crypto.randomUUID = () => 'test-random-uuid';
}

if (!window.scrollTo) {
  window.scrollTo = vi.fn();
}

afterEach(() => {
  cleanup();
  localStorage.clear();
  sessionStorage.clear();
  vi.restoreAllMocks();
  window.history.pushState({}, '', '/');
});

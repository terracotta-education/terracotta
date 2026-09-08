import { afterEach, beforeEach, vi } from "vitest";
import { enableAutoUnmount } from "@vue/test-utils";

import { createMockLocalStorage } from "@/test-utils/mockLocalStorage";

enableAutoUnmount(afterEach);

// jsdom's window.localStorage is unavailable in this environment; stub a fresh one before
// every test so tests are isolated from one another and code under test never sees undefined
beforeEach(() => {
  vi.stubGlobal("localStorage", createMockLocalStorage());
});

window.ResizeObserver = window.ResizeObserver || class ResizeObserver {
  observe() {}
  unobserve() {}
  disconnect() {}
};

window.visualViewport = window.visualViewport || {
  width: window.innerWidth,
  height: window.innerHeight,
  scale: 1,
  offsetLeft: 0,
  offsetTop: 0,
  addEventListener: vi.fn(),
  removeEventListener: vi.fn()
};

window.matchMedia = window.matchMedia || function matchMedia(query) {
  return {
    matches: false,
    media: query,
    onchange: null,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn()
  };
};

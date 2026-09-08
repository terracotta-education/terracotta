// jsdom's window.localStorage is unavailable in this project's test environment (likely an
// origin/config quirk - Storage is origin-scoped and jsdom's default test origin doesn't
// satisfy it), so tests exercising code that reads/writes localStorage need this stubbed in.
// Mirrors real Storage semantics closely enough for that: getItem/setItem/removeItem/clear,
// and stored keys are real enumerable own properties so Object.keys(localStorage) works too
// (production code, e.g. App.vue's clearStaleStorageExceptDrafts, relies on that).
export function createMockLocalStorage(initial = {}) {
  const storage = { ...initial };

  // configurable: true so tests can vi.spyOn() these methods (e.g. to simulate a browser
  // blocking storage access)
  Object.defineProperties(storage, {
    getItem: {
      value: key => (Object.prototype.hasOwnProperty.call(storage, key) ? storage[key] : null),
      enumerable: false,
      configurable: true,
      writable: true
    },
    setItem: {
      value: (key, value) => { storage[key] = String(value); },
      enumerable: false,
      configurable: true,
      writable: true
    },
    removeItem: {
      value: key => { delete storage[key]; },
      enumerable: false,
      configurable: true,
      writable: true
    },
    clear: {
      value: () => { Object.keys(storage).forEach(key => delete storage[key]); },
      enumerable: false,
      configurable: true,
      writable: true
    }
  });

  return storage;
}

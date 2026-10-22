import { defineConfig, devices } from "@playwright/test";

const PORT = 4173;
const ci = !!process.env.CI;

// Browser tests for the frontend: the real app in Chromium, served by Vite, with the API mocked
// (see e2e/support). Run them with `yarn test:e2e`.
export default defineConfig({
  testDir: "./e2e",
  testMatch: "**/*.e2e.js",
  globalSetup: "./e2e/support/warm-up.js",
  fullyParallel: true,
  forbidOnly: ci,
  retries: ci ? 1 : 0,
  reporter: ci ? [["list"], ["html", { open: "never" }]] : "list",
  use: {
    baseURL: `http://localhost:${PORT}`,
    trace: "on-first-retry",
    screenshot: "only-on-failure"
  },
  projects: [
    { name: "chromium", use: { ...devices["Desktop Chrome"] } }
  ],
  webServer: {
    command: `yarn vite --port ${PORT} --strictPort`,
    url: `http://localhost:${PORT}/e2e/harness/index.html`,
    reuseExistingServer: !ci,
    timeout: 120_000
  }
});

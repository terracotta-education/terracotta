import { defineConfig, mergeConfig } from "vitest/config";

import viteConfig from "./vite.config.mjs";

export default mergeConfig(
  viteConfig,
  defineConfig({
    test: {
      environment: "jsdom",
      include: ["src/**/*.spec.js"],
      setupFiles: ["./vitest.setup.js"],
      server: {
        deps: {
          inline: ["vuetify"]
        }
      }
    }
  })
);

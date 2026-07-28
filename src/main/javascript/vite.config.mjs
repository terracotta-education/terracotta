import { defineConfig } from "vite";
import { fileURLToPath, URL } from "node:url";

import vue from "@vitejs/plugin-vue";
import vuetify from "vite-plugin-vuetify";

export default defineConfig({
  base: "./",
  build: {
    outDir: "../resources/static/app",
    emptyOutDir: true,
    target: "esnext",
    reportCompressedSize: false,
    sourcemap: true,
    rolldownOptions: {
      input: {
        index: fileURLToPath(new URL("./index.html", import.meta.url)),
        app: fileURLToPath(new URL("./app.html", import.meta.url)),
        deepLink: fileURLToPath(new URL("./deepLink.html", import.meta.url))
      },
      output: {
        manualChunks(id) {
          if (!id.includes("node_modules")) return;
          // pdfjs-dist/vue-pdf-embed are exclusively used behind async boundaries
          // (route-level `component: () => import(...)` and defineAsyncComponent).
          // Leaving them out of manualChunks entirely lets Rolldown's automatic
          // chunking keep them lazy - assigning them a manual chunk name (even
          // their own dedicated one) causes Rolldown to eagerly <script>-tag that
          // chunk into every entry that can reach it at all, defeating the lazy
          // loading these libraries rely on to keep initial page load small.
          if (id.includes("pdfjs-dist") || id.includes("vue-pdf-embed")) return;
          if (id.includes("vuetify")) return "vendor-vuetify";
          if (id.includes("@tiptap")) return "vendor-tiptap";
          if (id.includes("highcharts")) return "vendor-highcharts";
          // These are all used synchronously (sweetalert2/tippy/floating-ui/flatpickr/
          // sortable are imported directly, not behind a dynamic import), so - unlike
          // pdfjs-dist above - grouping them into their own eager chunk is safe; it
          // just moves weight out of the generic "vendor" catch-all for better caching.
          if (
            id.includes("sweetalert2") ||
            id.includes("tippy.js") ||
            id.includes("@floating-ui") ||
            id.includes("flatpickr") ||
            id.includes("sortablejs") ||
            id.includes("vuedraggable")
          ) return "vendor-ui";
          if (
            id.includes("/vue/") ||
            id.includes("/vue-router/") ||
            id.includes("/pinia") ||
            id.includes("/@vue/")
          ) return "vendor-vue";
          return "vendor";
        }
      }
    }
  },
  plugins: [
    vue(),
    vuetify({
      autoImport: true,
      styles: {
        configFile: "src/styles/variables.scss"
      }
    })
  ],
  resolve: {
    alias: {
      "@": fileURLToPath(
        new URL("./src", import.meta.url)
      )
    },
    extensions: [
      ".mjs",
      ".js",
      ".ts",
      ".jsx",
      ".tsx",
      ".json",
      ".vue"
    ]
  },
  css: {
    preprocessorOptions: {
      scss: {
        api: "modern-compiler",
        additionalData: `
          @use "@/styles/variables.scss" as *;
          @use "sass:map";
          @use "sass:math";
        `
      }
    }
  }
});

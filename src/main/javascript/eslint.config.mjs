import js from "@eslint/js";
import vue from "eslint-plugin-vue";
import globals from "globals";

export default [
  {
    ignores: [
      "dist/**",
      "node/**",
      "node_modules/**",
      ".yarn/**"
    ]
  },
  js.configs.recommended,
  ...vue.configs["flat/essential"],
  {
    languageOptions: {
      ecmaVersion: 2022,
      sourceType: "module",
      globals: {
        ...globals.browser,
        ...globals.node
      }
    },
    rules: {
      "no-console": process.env.NODE_ENV === "production" ? "warn" : "off",
      "no-debugger": process.env.NODE_ENV === "production" ? "warn" : "off",
      // Vuetify's data-table scoped-slot naming convention (`#item.someColumn`, `#header.someColumn`)
      // parses as a v-slot "modifier" in Vue's template AST; this rule doesn't know Vuetify defines
      // those, so it flags valid, working slot names as invalid modifiers.
      "vue/valid-v-slot": "off"
    }
  },
  {
    // standalone snippets embedded into Qualtrics surveys; Qualtrics injects this global at runtime
    files: ["public/js/integrations/resize/*.qualtrics*.js"],
    languageOptions: {
      globals: {
        Qualtrics: "readonly"
      }
    }
  }
];

import { createVuetify } from "vuetify";
import { aliases, mdi } from "vuetify/iconsets/mdi";
import "@mdi/font/css/materialdesignicons.css";

export default createVuetify({
  defaults: {
    VTextField: { color: "primary" },
    VSelect: { color: "primary" },
    VAutocomplete: { color: "primary" },
    VCombobox: { color: "primary" },
    VTextarea: { color: "primary" },
    VCheckbox: {
      color: 'primary',
      style: '--v-selection-control-color: #0077d2;'
    },
    VRadio: { color: 'primary' },
  },
  icons: {
    defaultSet: "mdi",
    aliases,
    sets: { mdi }
  },
  theme: {
    defaultTheme: "light",
    themes: {
      light: {
        colors: {
          primary: "#0077d2",
          info: "#3173c9",
          success: "#008568",
          error: "#d60000",
          warning: "#cc4214"
        }
      }
    }
  }
});

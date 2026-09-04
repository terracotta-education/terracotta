import { defineStore } from "pinia";

const ALERT_TYPES = Object.freeze({
  info: "info",
  success: "success",
  warn: "warn",
  error: "error"
});

export const alert = defineStore("alert", {
  state: () => ({
    type: null,
    message: null,
    pendingClear: false
  }),

  getters: {
    alert: state => state,
    statuses: () => ALERT_TYPES,
    hasAlert: state => Boolean(state.type && state.message),
    alertType: state => state.type,
    alertMessage: state => state.message
  },

  actions: {
    info(message) {
      this.type = ALERT_TYPES.info;
      this.message = message;
      this.pendingClear = false;
    },

    success(message) {
      this.type = ALERT_TYPES.success;
      this.message = message;
      this.pendingClear = false;
    },

    warn(message) {
      this.type = ALERT_TYPES.warn;
      this.message = message;
      this.pendingClear = false;
    },

    error(message) {
      this.type = ALERT_TYPES.error;
      this.message = message;
      this.pendingClear = false;
    },

    clear() {
      this.type = null;
      this.message = null;
      this.pendingClear = false;
    }
  }
});

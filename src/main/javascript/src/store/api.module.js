import { defineStore } from "pinia";
import { jwtDecode } from "jwt-decode";

import { apiService } from "@/services";
import { userInfo } from "../helpers";

export const api = defineStore("api", {
  state: () => ({
    ltiToken: "",
    apiToken: "",
    aud: "",
    userInfo: "",
    experimentId: "",
    assignmentId: "",
    consent: "",
    userId: "",
    lmsApiOAuthURL: ""
  }),

  getters: {
    lti_token: state => state.ltiToken,
    api_token: state => state.apiToken,

    hasTokens: state =>
      state.ltiToken.length > 0 &&
      state.apiToken.length > 0
  },

  actions: {
    applyDecodedToken(decodedToken) {
      this.aud = decodedToken.aud || "";
      this.experimentId = decodedToken.experimentId || "";
      this.assignmentId = decodedToken.lmsAssignmentId || "";
      this.consent = decodedToken.consent || "";
      this.userId = decodedToken.userId || "";
      this.userInfo = userInfo(decodedToken.roles || []);
    },

    async setLtiToken(token) {
      this.ltiToken = token || "";

      if (token) {
        const decodedToken = jwtDecode(token);
        this.applyDecodedToken(decodedToken);
      }

      return this.setApiToken(token);
    },

    async setApiToken(token) {
      try {
        const data = await apiService.getApiToken(token);

        if (typeof data === "string") {
          const decodedToken = jwtDecode(data);

          this.apiToken = data;
          this.applyDecodedToken(decodedToken);
        }

        return data;
      } catch (error) {
        console.error("api/setApiToken | catch", error);
        return null;
      }
    },

    async refreshToken() {
      try {
        const data = await apiService.refreshToken();

        if (typeof data === "string") {
          const decodedToken = jwtDecode(data);

          this.apiToken = data;
          this.applyDecodedToken(decodedToken);
        }

        return data;
      } catch (error) {
        console.error("api/refreshToken | catch", error);
        return null;
      }
    },

    async reportStep({
      experimentId,
      step,
      parameters = null,
      preferLmsChecks = false
    }) {
      try {
        return await apiService.reportStep(
          experimentId,
          step,
          parameters,
          preferLmsChecks
        );
      } catch (error) {
        console.error("api/reportStep | catch", error);
        return null;
      }
    },

    async getStepStatus({ experimentId, batchId }) {
      try {
        return await apiService.getStepStatus(experimentId, batchId);
      } catch (error) {
        console.error("api/getStepStatus | catch", error);
        return null;
      }
    },

    async deepLinkJwt(id) {
      try {
        const data = await apiService.deepLinkJwt(id);

        return typeof data === "string"
          ? JSON.parse(data)
          : data;
      } catch (error) {
        console.error("api/deepLinkJwt | catch", error);
        return null;
      }
    },

    setLmsApiOAuthURL(url) {
      this.lmsApiOAuthURL = url || "";
    },

    reset() {
      this.ltiToken = "";
      this.apiToken = "";
      this.aud = "";
      this.userInfo = "";
      this.experimentId = "";
      this.assignmentId = "";
      this.consent = "";
      this.userId = "";
      this.lmsApiOAuthURL = "";
    }
  },

  persist: {
    key: "terracotta-api"
  }
});

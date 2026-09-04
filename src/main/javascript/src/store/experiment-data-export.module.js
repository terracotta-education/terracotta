import { defineStore } from "pinia";

import { experimentDataExportService } from "@/services";

const normalizeRequest = dataExportRequest => ({
  ...dataExportRequest,
  downloaded: dataExportRequest?.status === "DOWNLOADED",
  error: dataExportRequest?.status === "ERROR",
  errorAcknowledged: dataExportRequest?.status === "ERROR_ACKNOWLEDGED",
  outdated: dataExportRequest?.status === "OUTDATED",
  outdatedAcknowledged: dataExportRequest?.status === "OUTDATED_ACKNOWLEDGED",
  processing: dataExportRequest?.status === "PROCESSING",
  ready: dataExportRequest?.status === "READY",
  readyAcknowledged: dataExportRequest?.status === "READY_ACKNOWLEDGED",
  reprocessing: dataExportRequest?.status === "REPROCESSING"
});

export const dataExportRequest = defineStore("dataExportRequest", {
  state: () => ({
    dataExportRequests: []
  }),

  actions: {
    async prepare(payload) {
      try {
        const request =
          await experimentDataExportService.prepare(...payload);

        this.addDataExportRequest(request);

        return request;
      } catch (e) {
        console.error("prepare catch", e);

        return null;
      }
    },

    async poll(payload) {
      try {
        const request =
          await experimentDataExportService.poll(...payload);

        this.addDataExportRequest(request);

        return request;
      } catch (e) {
        console.error("poll catch", e);

        return null;
      }
    },

    async pollList(payload) {
      try {
        const requests =
          await experimentDataExportService.pollList(...payload);

        this.addDataExportRequests(requests);

        return requests;
      } catch (e) {
        console.error("pollList catch", e);

        return null;
      }
    },

    async retrieve(payload) {
      try {
        const request =
          await experimentDataExportService.retrieve(...payload);

        this.addDataExportRequest(request);

        return request;
      } catch (e) {
        console.error("retrieve catch", e);

        return null;
      }
    },

    async acknowledge(payload) {
      try {
        const request =
          await experimentDataExportService.acknowledge(...payload);

        this.addDataExportRequest(request);

        return request;
      } catch (e) {
        console.error("acknowledge catch", e);

        return null;
      }
    },

    reset() {
      this.dataExportRequests = [];
    },

    resetExportData() {
      this.reset();
    },

    addDataExportRequest(request) {
      if (!request?.id) {
        return;
      }

      const normalized = normalizeRequest(request);
      const foundIndex = this.dataExportRequests?.findIndex(
        r => r.experimentId === normalized.experimentId
      ) ?? -1;

      if (foundIndex >= 0) {
        this.dataExportRequests.splice(foundIndex, 1, normalized);
      } else {
        this.dataExportRequests.push(normalized);
      }
    },

    addDataExportRequests(requests) {
      this.dataExportRequests = [];

      if (!requests || requests.length === 0) {
        return;
      }

      requests.forEach(request => {
        if (!request.id) {
          return;
        }

        this.dataExportRequests.push(normalizeRequest(request));
      });
    }
  }
});

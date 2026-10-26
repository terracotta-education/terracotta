import { defineStore } from "pinia";

import { assignmentFileArchiveService } from "@/services";

const normalizeFileRequest = fileRequest => ({
  ...fileRequest,
  downloaded: fileRequest?.status === "DOWNLOADED",
  error: fileRequest?.status === "ERROR",
  outdated: fileRequest?.status === "OUTDATED",
  outdatedAcknowledged: fileRequest?.status === "OUTDATED_ACKNOWLEDGED",
  processing: fileRequest?.status === "PROCESSING",
  ready: fileRequest?.status === "READY",
  reprocessing: fileRequest?.status === "REPROCESSING"
});

export const assignmentFileArchive = defineStore("assignmentFileArchive", {
  state: () => ({
    fileRequest: null
  }),

  actions: {
    async prepare(payload) {
      try {
        const fileRequest =
          await assignmentFileArchiveService.prepare(...payload);

        this.addFileRequest(fileRequest);
      } catch (e) {
        console.error("prepare catch", e);
      }
    },

    async poll(payload) {
      try {
        const fileRequest =
          await assignmentFileArchiveService.poll(...payload);

        this.addFileRequest(fileRequest);
      } catch (e) {
        console.error("poll catch", e);
      }
    },

    async retrieve(payload) {
      try {
        const fileRequest =
          await assignmentFileArchiveService.retrieve(...payload);

        this.addFileRequest(fileRequest);
      } catch (e) {
        console.error("retrieve catch", e);
      }
    },

    async acknowledgeError(payload) {
      try {
        await assignmentFileArchiveService.acknowledgeError(...payload);
        this.fileRequest = null;
      } catch (e) {
        console.error("acknowledgeError catch", e);
      }
    },

    async acknowledgeOutdated(payload) {
      try {
        const response =
          await assignmentFileArchiveService.acknowledgeOutdated(...payload);

        if (response?.error) {
          return;
        }

        // the archive is still there, just no longer flagged as outdated
        this.fileRequest = normalizeFileRequest({
          ...this.fileRequest,
          status: "OUTDATED_ACKNOWLEDGED"
        });
      } catch (e) {
        console.error("acknowledgeOutdated catch", e);
      }
    },

    reset() {
      this.fileRequest = null;
    },

    addFileRequest(fileRequest) {
      if (!fileRequest?.id) {
        return;
      }

      this.fileRequest = normalizeFileRequest(fileRequest);
    }
  }
});

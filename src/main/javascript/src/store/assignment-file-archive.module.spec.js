import { beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";

vi.mock("@/services", () => ({
  assignmentFileArchiveService: {
    prepare: vi.fn(),
    poll: vi.fn(),
    retrieve: vi.fn(),
    acknowledgeError: vi.fn()
  }
}));

import { assignmentFileArchiveService } from "@/services";
import { assignmentFileArchive } from "./assignment-file-archive.module";

describe("assignmentFileArchive store", () => {
  let store;
  let consoleSpy;

  beforeEach(() => {
    setActivePinia(createPinia());
    store = assignmentFileArchive();
    vi.clearAllMocks();
    consoleSpy = vi.spyOn(console, "error").mockImplementation(() => {});
  });

  it("starts with no file request", () => {
    expect(store.fileRequest).toBeNull();
  });

  describe("prepare", () => {
    it("normalizes and stores the file request on success", async () => {
      assignmentFileArchiveService.prepare.mockResolvedValue({
        id: 1,
        status: "PROCESSING"
      });

      await store.prepare([1, 2, 3]);

      expect(assignmentFileArchiveService.prepare).toHaveBeenCalledWith(1, 2, 3);
      expect(store.fileRequest).toMatchObject({
        id: 1,
        processing: true,
        ready: false,
        downloaded: false,
        error: false,
        outdated: false,
        reprocessing: false
      });
    });

    it("logs and leaves state untouched on rejection", async () => {
      assignmentFileArchiveService.prepare.mockRejectedValue(new Error("fail"));

      await store.prepare([1, 2, 3]);

      expect(store.fileRequest).toBeNull();
      expect(consoleSpy).toHaveBeenCalled();
    });
  });

  describe("poll", () => {
    it("normalizes and stores the file request on success", async () => {
      assignmentFileArchiveService.poll.mockResolvedValue({
        id: 2,
        status: "READY"
      });

      await store.poll([1, 2, 3, true]);

      expect(assignmentFileArchiveService.poll).toHaveBeenCalledWith(1, 2, 3, true);
      expect(store.fileRequest.ready).toBe(true);
    });

    it("logs and leaves state untouched on rejection", async () => {
      assignmentFileArchiveService.poll.mockRejectedValue(new Error("fail"));

      await store.poll([1, 2, 3, true]);

      expect(store.fileRequest).toBeNull();
      expect(consoleSpy).toHaveBeenCalled();
    });
  });

  describe("retrieve", () => {
    it("normalizes and stores the file request on success", async () => {
      assignmentFileArchiveService.retrieve.mockResolvedValue({
        id: 3,
        status: "DOWNLOADED"
      });

      await store.retrieve([1, 2, 3, {}]);

      expect(store.fileRequest.downloaded).toBe(true);
    });

    it("logs and leaves state untouched on rejection", async () => {
      assignmentFileArchiveService.retrieve.mockRejectedValue(new Error("fail"));

      await store.retrieve([1, 2, 3, {}]);

      expect(store.fileRequest).toBeNull();
      expect(consoleSpy).toHaveBeenCalled();
    });
  });

  describe("acknowledgeError", () => {
    it("clears the file request on success", async () => {
      store.addFileRequest({ id: 4, status: "ERROR" });
      assignmentFileArchiveService.acknowledgeError.mockResolvedValue({});

      await store.acknowledgeError([1, 2, 3, 4]);

      expect(store.fileRequest).toBeNull();
    });

    it("logs and leaves state untouched on rejection", async () => {
      store.addFileRequest({ id: 4, status: "ERROR" });
      assignmentFileArchiveService.acknowledgeError.mockRejectedValue(
        new Error("fail")
      );

      await store.acknowledgeError([1, 2, 3, 4]);

      expect(store.fileRequest).not.toBeNull();
      expect(consoleSpy).toHaveBeenCalled();
    });
  });

  describe("reset", () => {
    it("clears the file request", () => {
      store.addFileRequest({ id: 5, status: "READY" });
      store.reset();

      expect(store.fileRequest).toBeNull();
    });
  });

  describe("addFileRequest", () => {
    it("is a no-op when the file request has no id", () => {
      store.addFileRequest({ status: "READY" });
      expect(store.fileRequest).toBeNull();

      store.addFileRequest(null);
      expect(store.fileRequest).toBeNull();
    });

    it("flags exactly one status as true per known status value", () => {
      const statuses = [
        ["DOWNLOADED", "downloaded"],
        ["ERROR", "error"],
        ["OUTDATED", "outdated"],
        ["PROCESSING", "processing"],
        ["READY", "ready"],
        ["REPROCESSING", "reprocessing"]
      ];

      statuses.forEach(([status, flag]) => {
        store.addFileRequest({ id: 1, status });

        const flags = ["downloaded", "error", "outdated", "processing", "ready", "reprocessing"];
        flags.forEach(f => {
          expect(store.fileRequest[f]).toBe(f === flag);
        });
      });
    });

    it("flags every status as false for an unknown status", () => {
      store.addFileRequest({ id: 1, status: "UNKNOWN" });

      expect(store.fileRequest.downloaded).toBe(false);
      expect(store.fileRequest.error).toBe(false);
      expect(store.fileRequest.outdated).toBe(false);
      expect(store.fileRequest.processing).toBe(false);
      expect(store.fileRequest.ready).toBe(false);
      expect(store.fileRequest.reprocessing).toBe(false);
    });
  });
});

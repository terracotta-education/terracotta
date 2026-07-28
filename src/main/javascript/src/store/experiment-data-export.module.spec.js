import { beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";

vi.mock("@/services", () => ({
  experimentDataExportService: {
    prepare: vi.fn(),
    poll: vi.fn(),
    pollList: vi.fn(),
    retrieve: vi.fn(),
    acknowledge: vi.fn()
  }
}));

import { experimentDataExportService } from "@/services";
import { dataExportRequest } from "./experiment-data-export.module";

describe("dataExportRequest store", () => {
  let store;

  beforeEach(() => {
    setActivePinia(createPinia());
    store = dataExportRequest();
    vi.clearAllMocks();
    vi.spyOn(console, "error").mockImplementation(() => {});
  });

  it("starts with no data export requests", () => {
    expect(store.dataExportRequests).toEqual([]);
  });

  describe("addDataExportRequest", () => {
    it("normalizes status flags and adds a new request", () => {
      store.addDataExportRequest({
        id: 1,
        experimentId: 10,
        status: "READY"
      });

      expect(store.dataExportRequests).toHaveLength(1);
      expect(store.dataExportRequests[0]).toMatchObject({
        id: 1,
        experimentId: 10,
        ready: true,
        downloaded: false,
        error: false,
        errorAcknowledged: false,
        outdated: false,
        outdatedAcknowledged: false,
        processing: false,
        readyAcknowledged: false,
        reprocessing: false
      });
    });

    it("replaces an existing request for the same experimentId", () => {
      store.addDataExportRequest({
        id: 1,
        experimentId: 10,
        status: "PROCESSING"
      });
      store.addDataExportRequest({
        id: 2,
        experimentId: 10,
        status: "READY"
      });

      expect(store.dataExportRequests).toHaveLength(1);
      expect(store.dataExportRequests[0].id).toBe(2);
      expect(store.dataExportRequests[0].ready).toBe(true);
    });

    it("ignores a request with no id", () => {
      store.addDataExportRequest({ experimentId: 10, status: "READY" });

      expect(store.dataExportRequests).toEqual([]);
    });

    it("ignores a null/undefined request", () => {
      store.addDataExportRequest(null);
      store.addDataExportRequest(undefined);

      expect(store.dataExportRequests).toEqual([]);
    });
  });

  describe("addDataExportRequests", () => {
    it("replaces the whole list, skipping entries without ids", () => {
      store.addDataExportRequest({
        id: 99,
        experimentId: 99,
        status: "READY"
      });

      store.addDataExportRequests([
        { id: 1, experimentId: 10, status: "OUTDATED" },
        { experimentId: 11, status: "READY" },
        { id: 2, experimentId: 12, status: "ERROR" }
      ]);

      expect(store.dataExportRequests).toHaveLength(2);
      expect(store.dataExportRequests.map(r => r.id)).toEqual([1, 2]);
      expect(store.dataExportRequests[0].outdated).toBe(true);
      expect(store.dataExportRequests[1].error).toBe(true);
    });

    it("clears the list when given an empty or falsy array", () => {
      store.addDataExportRequest({
        id: 1,
        experimentId: 10,
        status: "READY"
      });

      store.addDataExportRequests([]);
      expect(store.dataExportRequests).toEqual([]);

      store.addDataExportRequest({
        id: 1,
        experimentId: 10,
        status: "READY"
      });
      store.addDataExportRequests(null);
      expect(store.dataExportRequests).toEqual([]);
    });
  });

  describe("reset / resetExportData", () => {
    it("reset clears the list", () => {
      store.addDataExportRequest({
        id: 1,
        experimentId: 10,
        status: "READY"
      });
      store.reset();

      expect(store.dataExportRequests).toEqual([]);
    });

    it("resetExportData delegates to reset", () => {
      store.addDataExportRequest({
        id: 1,
        experimentId: 10,
        status: "READY"
      });
      store.resetExportData();

      expect(store.dataExportRequests).toEqual([]);
    });
  });

  describe("prepare", () => {
    it("adds the returned request on success", async () => {
      experimentDataExportService.prepare.mockResolvedValue({
        id: 1,
        experimentId: 10,
        status: "PROCESSING"
      });

      await store.prepare([10]);

      expect(experimentDataExportService.prepare).toHaveBeenCalledWith(10);
      expect(store.dataExportRequests).toHaveLength(1);
      expect(store.dataExportRequests[0].processing).toBe(true);
    });

    it("logs and swallows errors, leaving state untouched", async () => {
      experimentDataExportService.prepare.mockRejectedValue(
        new Error("boom")
      );

      const result = await store.prepare([10]);

      expect(result).toBeNull();
      expect(store.dataExportRequests).toEqual([]);
      expect(console.error).toHaveBeenCalled();
    });
  });

  describe("poll", () => {
    it("adds the returned request on success", async () => {
      experimentDataExportService.poll.mockResolvedValue({
        id: 1,
        experimentId: 10,
        status: "READY"
      });

      const result = await store.poll([10, true]);

      expect(experimentDataExportService.poll).toHaveBeenCalledWith(
        10,
        true
      );
      expect(store.dataExportRequests).toHaveLength(1);
      expect(result).toEqual({ id: 1, experimentId: 10, status: "READY" });
    });

    it("logs and swallows errors", async () => {
      experimentDataExportService.poll.mockRejectedValue(new Error("boom"));

      const result = await store.poll([10, true]);

      expect(store.dataExportRequests).toEqual([]);
      expect(console.error).toHaveBeenCalled();
      expect(result).toBeNull();
    });
  });

  describe("pollList", () => {
    it("adds the returned requests on success", async () => {
      experimentDataExportService.pollList.mockResolvedValue([
        { id: 1, experimentId: 10, status: "READY" },
        { id: 2, experimentId: 11, status: "ERROR" }
      ]);

      const result = await store.pollList([[10, 11], false]);

      expect(store.dataExportRequests).toHaveLength(2);
      expect(result).toHaveLength(2);
    });

    it("logs and swallows errors", async () => {
      experimentDataExportService.pollList.mockRejectedValue(
        new Error("boom")
      );

      const result = await store.pollList([[10, 11], false]);

      expect(store.dataExportRequests).toEqual([]);
      expect(console.error).toHaveBeenCalled();
      expect(result).toBeNull();
    });
  });

  describe("retrieve", () => {
    it("adds the returned request on success", async () => {
      experimentDataExportService.retrieve.mockResolvedValue({
        id: 1,
        experimentId: 10,
        status: "DOWNLOADED"
      });

      const result = await store.retrieve([10, { id: 1 }]);

      expect(store.dataExportRequests[0].downloaded).toBe(true);
      expect(result.status).toBe("DOWNLOADED");
    });

    it("logs and swallows errors", async () => {
      experimentDataExportService.retrieve.mockRejectedValue(
        new Error("boom")
      );

      const result = await store.retrieve([10, { id: 1 }]);

      expect(store.dataExportRequests).toEqual([]);
      expect(console.error).toHaveBeenCalled();
      expect(result).toBeNull();
    });
  });

  describe("acknowledge", () => {
    it("adds the returned request on success", async () => {
      experimentDataExportService.acknowledge.mockResolvedValue({
        id: 1,
        experimentId: 10,
        status: "READY_ACKNOWLEDGED"
      });

      const result = await store.acknowledge([10, 1, "READY_ACKNOWLEDGED"]);

      expect(store.dataExportRequests[0].readyAcknowledged).toBe(true);
      expect(result.status).toBe("READY_ACKNOWLEDGED");
    });

    it("logs and swallows errors", async () => {
      experimentDataExportService.acknowledge.mockRejectedValue(
        new Error("boom")
      );

      const result = await store.acknowledge([10, 1, "READY_ACKNOWLEDGED"]);

      expect(store.dataExportRequests).toEqual([]);
      expect(console.error).toHaveBeenCalled();
      expect(result).toBeNull();
    });
  });
});

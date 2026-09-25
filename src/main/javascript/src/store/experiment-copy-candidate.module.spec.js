import { beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";

vi.mock("@/services", () => ({
  experimentCopyCandidateService: {
    getAll: vi.fn(),
    resolve: vi.fn(),
    getCopyStatus: vi.fn(),
    acknowledgeCopyStatus: vi.fn(),
    retryCopy: vi.fn()
  }
}));

import { experimentCopyCandidateService } from "@/services";
import { experimentCopyCandidate } from "./experiment-copy-candidate.module";

describe("experimentCopyCandidate store", () => {
  let store;

  beforeEach(() => {
    setActivePinia(createPinia());
    store = experimentCopyCandidate();
    vi.clearAllMocks();
    vi.spyOn(console, "error").mockImplementation(() => {});
  });

  it("starts with no candidates", () => {
    expect(store.copyCandidates).toEqual([]);
  });

  describe("fetchAll", () => {
    it("stores the returned candidates on success", async () => {
      experimentCopyCandidateService.getAll.mockResolvedValue({
        data: [
          { id: "c1", experimentTitle: "Reading Study" },
          { id: "c2", experimentTitle: "Writing Study" }
        ]
      });

      const result = await store.fetchAll();

      expect(store.copyCandidates).toHaveLength(2);
      expect(result).toBe(store.copyCandidates);
    });

    it("defaults to an empty list when the response has no data", async () => {
      experimentCopyCandidateService.getAll.mockResolvedValue({});

      await store.fetchAll();

      expect(store.copyCandidates).toEqual([]);
    });

    it("logs and swallows errors, leaving state untouched", async () => {
      experimentCopyCandidateService.getAll.mockRejectedValue(new Error("boom"));

      const result = await store.fetchAll();

      expect(result).toEqual([]);
      expect(store.copyCandidates).toEqual([]);
      expect(console.error).toHaveBeenCalled();
    });
  });

  describe("resolve", () => {
    it("clears every candidate and returns the resolution on success", async () => {
      store.copyCandidates = [
        { id: "c1", experimentTitle: "Reading Study" },
        { id: "c2", experimentTitle: "Writing Study" }
      ];
      experimentCopyCandidateService.resolve.mockResolvedValue({
        data: { imports: [{ id: "import-1", status: "PROCESSING" }], declinedCandidateIds: ["c2"] }
      });

      const result = await store.resolve(["c1"]);

      expect(experimentCopyCandidateService.resolve).toHaveBeenCalledWith(["c1"]);
      expect(result).toEqual({ imports: [{ id: "import-1", status: "PROCESSING" }], declinedCandidateIds: ["c2"] });
      expect(store.copyCandidates).toEqual([]);
    });

    it("clears every candidate even when none are selected (the 'No Thanks' case)", async () => {
      store.copyCandidates = [{ id: "c1", experimentTitle: "Reading Study" }];
      experimentCopyCandidateService.resolve.mockResolvedValue({
        data: { imports: [], declinedCandidateIds: ["c1"] }
      });

      await store.resolve([]);

      expect(experimentCopyCandidateService.resolve).toHaveBeenCalledWith([]);
      expect(store.copyCandidates).toEqual([]);
    });

    it("logs and swallows errors, leaving state untouched", async () => {
      store.copyCandidates = [{ id: "c1", experimentTitle: "Reading Study" }];
      experimentCopyCandidateService.resolve.mockRejectedValue(new Error("boom"));

      const result = await store.resolve(["c1"]);

      expect(result).toBeNull();
      expect(store.copyCandidates).toHaveLength(1);
      expect(console.error).toHaveBeenCalled();
    });
  });

  describe("reset", () => {
    it("clears the list and the copy status", () => {
      store.copyCandidates = [{ id: "c1" }];
      store.copyStatus = { status: "COMPLETE", importIds: [] };
      store.reset();

      expect(store.copyCandidates).toEqual([]);
      expect(store.copyStatus).toBeNull();
    });
  });

  describe("fetchCopyStatus", () => {
    it("stores the returned copy status", async () => {
      const copyStatus = { status: "IN_PROGRESS", importIds: [] };
      experimentCopyCandidateService.getCopyStatus.mockResolvedValue({ data: copyStatus });

      const result = await store.fetchCopyStatus();

      expect(store.copyStatus).toEqual(copyStatus);
      expect(result).toEqual(copyStatus);
    });

    it("defaults to null when the response has no data", async () => {
      experimentCopyCandidateService.getCopyStatus.mockResolvedValue({});

      await store.fetchCopyStatus();

      expect(store.copyStatus).toBeNull();
    });

    it("logs and swallows errors", async () => {
      experimentCopyCandidateService.getCopyStatus.mockRejectedValue(new Error("boom"));

      expect(await store.fetchCopyStatus()).toBeNull();
      expect(console.error).toHaveBeenCalled();
    });
  });

  describe("acknowledgeCopyStatus", () => {
    it("acknowledges and clears the stored copy status", async () => {
      store.copyStatus = { status: "COMPLETE", importIds: [] };
      experimentCopyCandidateService.acknowledgeCopyStatus.mockResolvedValue({});

      await store.acknowledgeCopyStatus();

      expect(experimentCopyCandidateService.acknowledgeCopyStatus).toHaveBeenCalled();
      expect(store.copyStatus).toBeNull();
    });

    it("logs and swallows errors, leaving the copy status in place", async () => {
      store.copyStatus = { status: "COMPLETE", importIds: [] };
      experimentCopyCandidateService.acknowledgeCopyStatus.mockRejectedValue(new Error("boom"));

      await store.acknowledgeCopyStatus();

      expect(store.copyStatus).toEqual({ status: "COMPLETE", importIds: [] });
      expect(console.error).toHaveBeenCalled();
    });
  });

  describe("retryCopy", () => {
    it("stores the copy status returned by the retry", async () => {
      store.copyStatus = { status: "ERROR", importIds: [] };
      experimentCopyCandidateService.retryCopy.mockResolvedValue({ data: { status: "IN_PROGRESS", importIds: [] } });

      expect(await store.retryCopy()).toEqual({ status: "IN_PROGRESS", importIds: [] });
      expect(store.copyStatus).toEqual({ status: "IN_PROGRESS", importIds: [] });
    });

    it("keeps the existing copy status when the retry returns nothing", async () => {
      store.copyStatus = { status: "ERROR", importIds: [] };
      experimentCopyCandidateService.retryCopy.mockResolvedValue({});

      await store.retryCopy();

      expect(store.copyStatus).toEqual({ status: "ERROR", importIds: [] });
    });

    it("logs and swallows errors, keeping the existing copy status", async () => {
      store.copyStatus = { status: "ERROR", importIds: [] };
      experimentCopyCandidateService.retryCopy.mockRejectedValue(new Error("boom"));

      expect(await store.retryCopy()).toEqual({ status: "ERROR", importIds: [] });
      expect(console.error).toHaveBeenCalled();
    });
  });
});

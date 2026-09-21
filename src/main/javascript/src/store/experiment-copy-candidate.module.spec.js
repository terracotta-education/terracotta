import { beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";

vi.mock("@/services", () => ({
  experimentCopyCandidateService: {
    getAll: vi.fn(),
    resolve: vi.fn()
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
    it("clears the list", () => {
      store.copyCandidates = [{ id: "c1" }];
      store.reset();

      expect(store.copyCandidates).toEqual([]);
    });
  });
});

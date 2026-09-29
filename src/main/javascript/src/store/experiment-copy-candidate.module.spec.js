import { beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";

vi.mock("@/services", () => ({
  experimentCopyCandidateService: {
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

  describe("reset", () => {
    it("clears the copy status", () => {
      store.copyStatus = { status: "COMPLETE", importIds: [] };
      store.reset();

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

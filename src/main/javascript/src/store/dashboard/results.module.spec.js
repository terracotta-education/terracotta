import { beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";

vi.mock("@/services", () => ({
  resultsDashboardService: {
    overview: vi.fn(),
    outcomes: vi.fn()
  }
}));

import { resultsDashboardService } from "@/services";
import { resultsDashboard } from "./results.module";

describe("resultsDashboard store", () => {
  let store;
  let consoleSpy;

  beforeEach(() => {
    setActivePinia(createPinia());
    store = resultsDashboard();
    vi.clearAllMocks();
    consoleSpy = vi.spyOn(console, "error").mockImplementation(() => {});
  });

  it("starts with null overview and outcomes", () => {
    expect(store.overview).toBeNull();
    expect(store.outcomes).toBeNull();
  });

  describe("getOverview", () => {
    it("sets overview and experimentId on success", async () => {
      resultsDashboardService.overview.mockResolvedValue({
        data: { overview: { totalStudents: 10 } }
      });

      const result = await store.getOverview(1);

      expect(resultsDashboardService.overview).toHaveBeenCalledWith(1);
      expect(result).toEqual({ totalStudents: 10 });
      expect(store.overview).toEqual({ totalStudents: 10 });
      expect(store.resultsDashboard.experimentId).toBe(1);
    });

    it("sets overview to null when the response has no overview data", async () => {
      resultsDashboardService.overview.mockResolvedValue({});

      const result = await store.getOverview(1);

      expect(result).toBeNull();
      expect(store.overview).toBeNull();
    });

    it("resets overview to null and logs on rejection, preserving other fields", async () => {
      resultsDashboardService.outcomes.mockResolvedValue({
        data: { outcomes: { total: 5 } }
      });
      await store.getOutcomes([1, {}]);

      resultsDashboardService.overview.mockRejectedValue(new Error("fail"));
      const result = await store.getOverview(1);

      expect(result).toBeNull();
      expect(store.overview).toBeNull();
      expect(store.outcomes).toEqual({ total: 5 });
      expect(consoleSpy).toHaveBeenCalled();
    });
  });

  describe("getOutcomes", () => {
    it("sets outcomes and experimentId on success", async () => {
      resultsDashboardService.outcomes.mockResolvedValue({
        data: { outcomes: { total: 3 } }
      });

      const result = await store.getOutcomes([1, { filter: "x" }]);

      expect(resultsDashboardService.outcomes).toHaveBeenCalledWith(1, { filter: "x" });
      expect(result).toEqual({ total: 3 });
      expect(store.outcomes).toEqual({ total: 3 });
      expect(store.resultsDashboard.experimentId).toBe(1);
    });

    it("sets outcomes to null when the response has no outcomes data", async () => {
      resultsDashboardService.outcomes.mockResolvedValue({});

      const result = await store.getOutcomes([1, {}]);

      expect(result).toBeNull();
      expect(store.outcomes).toBeNull();
    });

    it("resets outcomes to null and logs on rejection, preserving other fields", async () => {
      resultsDashboardService.overview.mockResolvedValue({
        data: { overview: { totalStudents: 10 } }
      });
      await store.getOverview(1);

      resultsDashboardService.outcomes.mockRejectedValue(new Error("fail"));
      const result = await store.getOutcomes([1, {}]);

      expect(result).toBeNull();
      expect(store.outcomes).toBeNull();
      expect(store.overview).toEqual({ totalStudents: 10 });
      expect(consoleSpy).toHaveBeenCalled();
    });
  });

  describe("clearOutcomes", () => {
    it("clears outcomes but leaves overview alone", async () => {
      resultsDashboardService.overview.mockResolvedValue({
        data: { overview: { totalStudents: 10 } }
      });
      resultsDashboardService.outcomes.mockResolvedValue({
        data: { outcomes: { total: 3 } }
      });
      await store.getOverview(1);
      await store.getOutcomes([1, {}]);

      store.clearOutcomes();

      expect(store.outcomes).toBeNull();
      expect(store.overview).toEqual({ totalStudents: 10 });
    });
  });

  describe("resetResultsDashboard", () => {
    it("resets the whole resultsDashboard object", async () => {
      resultsDashboardService.overview.mockResolvedValue({
        data: { overview: { totalStudents: 10 } }
      });
      await store.getOverview(1);

      store.resetResultsDashboard();

      expect(store.resultsDashboard).toEqual({
        experimentId: null,
        overview: null,
        outcomes: null
      });
    });
  });
});

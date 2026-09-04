import { beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";

vi.mock("@/services", () => ({
  exposuresService: {
    getAll: vi.fn(),
    createExposures: vi.fn()
  }
}));

import { exposuresService } from "@/services";
import { exposures } from "./exposures.module";

describe("exposures store", () => {
  let store;

  beforeEach(() => {
    setActivePinia(createPinia());
    store = exposures();
    vi.clearAllMocks();
    vi.spyOn(console, "error").mockImplementation(() => {});
  });

  it("starts with no exposures", () => {
    expect(store.exposures).toEqual([]);
    expect(store.hasExposures).toBe(false);
  });

  describe("fetchExposures", () => {
    it("sets exposures from the service response", async () => {
      exposuresService.getAll.mockResolvedValue([{ exposureId: 1 }]);

      const result = await store.fetchExposures(10);

      expect(exposuresService.getAll).toHaveBeenCalledWith(10);
      expect(store.exposures).toEqual([{ exposureId: 1 }]);
      expect(store.hasExposures).toBe(true);
      expect(result).toEqual([{ exposureId: 1 }]);
    });

    it("defaults to an empty array when the response is falsy", async () => {
      exposuresService.getAll.mockResolvedValue(null);

      const result = await store.fetchExposures(10);

      expect(store.exposures).toEqual([]);
      expect(result).toEqual([]);
    });

    it("resets exposures to [] and logs on error", async () => {
      store.exposures = [{ exposureId: 1 }];
      exposuresService.getAll.mockRejectedValue(new Error("boom"));

      const result = await store.fetchExposures(10);

      expect(store.exposures).toEqual([]);
      expect(result).toEqual([]);
      expect(console.error).toHaveBeenCalled();
    });
  });

  describe("createExposures", () => {
    it("returns the service result on success", async () => {
      exposuresService.createExposures.mockResolvedValue([
        { exposureId: 1 }
      ]);

      const result = await store.createExposures(10);

      expect(exposuresService.createExposures).toHaveBeenCalledWith(10);
      expect(result).toEqual([{ exposureId: 1 }]);
    });

    it("returns null and logs on error", async () => {
      exposuresService.createExposures.mockRejectedValue(new Error("boom"));

      const result = await store.createExposures(10);

      expect(result).toBeNull();
      expect(console.error).toHaveBeenCalled();
    });
  });

  describe("resetExposures", () => {
    it("clears the exposures list", () => {
      store.exposures = [{ exposureId: 1 }];
      store.resetExposures();

      expect(store.exposures).toEqual([]);
    });
  });
});

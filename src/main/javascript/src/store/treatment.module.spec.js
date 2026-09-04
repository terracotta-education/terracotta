import { beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";

vi.mock("@/services", () => ({
  treatmentService: {
    create: vi.fn(),
    update: vi.fn(),
    fetchTreatment: vi.fn()
  }
}));

import { treatmentService } from "@/services";
import { treatment } from "./treatment.module";

describe("treatment store", () => {
  let store;

  beforeEach(() => {
    setActivePinia(createPinia());
    store = treatment();
    vi.clearAllMocks();
  });

  it("starts empty", () => {
    expect(store.hasTreatment).toBe(false);
    expect(store.hasTreatments).toBe(false);
    expect(store.treatments).toEqual([]);
    expect(store.treatment).toBeNull();
  });

  describe("createTreatment", () => {
    it("returns an existing treatment for the assignment without calling the service", async () => {
      const existing = { treatmentId: 1, assignmentId: 42 };
      store.treatments = [existing];

      const result = await store.createTreatment(["a", "b", "42"]);

      expect(treatmentService.create).not.toHaveBeenCalled();
      expect(result).toEqual({ status: 200, data: existing });
      expect(store.treatment).toEqual(existing);
    });

    it("creates a new treatment when none exists for the assignment", async () => {
      const created = { treatmentId: 2, assignmentId: 43 };
      treatmentService.create.mockResolvedValue({
        status: 201,
        data: created
      });

      const result = await store.createTreatment(["a", "b", "43"]);

      expect(treatmentService.create).toHaveBeenCalledWith("a", "b", "43");
      expect(result).toEqual({ status: 201, data: created });
      expect(store.treatment).toEqual(created);
      expect(store.treatments).toContainEqual(created);
    });

    it("returns the raw response when status is not 201", async () => {
      treatmentService.create.mockResolvedValue({ status: 400 });

      const result = await store.createTreatment(["a", "b", "99"]);

      expect(result).toEqual({ status: 400 });
      expect(store.treatment).toBeNull();
    });

    it("returns null on error", async () => {
      treatmentService.create.mockRejectedValue(new Error("fail"));

      const result = await store.createTreatment(["a", "b", "1"]);

      expect(result).toBeNull();
    });
  });

  describe("updateTreatment", () => {
    it("updates treatment and upserts it on 200", async () => {
      const updated = { treatmentId: 3 };
      treatmentService.update.mockResolvedValue({
        status: 200,
        data: updated
      });

      const result = await store.updateTreatment(["a"]);

      expect(result).toEqual({ status: 200, data: updated });
      expect(store.treatment).toEqual(updated);
      expect(store.treatments).toContainEqual(updated);
    });

    it("replaces an existing treatment with the same id", async () => {
      store.treatments = [{ treatmentId: 3, name: "old" }];
      const updated = { treatmentId: 3, name: "new" };
      treatmentService.update.mockResolvedValue({
        status: 201,
        data: updated
      });

      await store.updateTreatment(["a"]);

      expect(store.treatments).toEqual([updated]);
    });

    it("returns raw response when status is neither 200 nor 201", async () => {
      treatmentService.update.mockResolvedValue({ status: 500 });

      const result = await store.updateTreatment(["a"]);

      expect(result).toEqual({ status: 500 });
    });

    it("returns null on error", async () => {
      treatmentService.update.mockRejectedValue(new Error("fail"));

      const result = await store.updateTreatment(["a"]);

      expect(result).toBeNull();
    });
  });

  describe("checkTreatment", () => {
    it("returns status/data when service responds", async () => {
      treatmentService.fetchTreatment.mockResolvedValue({
        status: 200,
        data: { treatmentId: 1 }
      });

      const result = await store.checkTreatment(["a"]);

      expect(result).toEqual({ status: 200, data: { treatmentId: 1 } });
    });

    it("returns null when service resolves with a falsy response", async () => {
      treatmentService.fetchTreatment.mockResolvedValue(null);

      const result = await store.checkTreatment(["a"]);

      expect(result).toBeNull();
    });

    it("returns null on error", async () => {
      treatmentService.fetchTreatment.mockRejectedValue(new Error("fail"));

      const result = await store.checkTreatment(["a"]);

      expect(result).toBeNull();
    });
  });

  describe("upsertTreatment", () => {
    it("does nothing without a treatmentId", () => {
      store.treatments = [{ treatmentId: 1 }];

      store.upsertTreatment(null);
      store.upsertTreatment({});

      expect(store.treatments).toEqual([{ treatmentId: 1 }]);
    });
  });

  it("resetTreatments clears state", () => {
    store.treatments = [{ treatmentId: 1 }];
    store.treatment = { treatmentId: 1 };

    store.resetTreatments();

    expect(store.treatments).toEqual([]);
    expect(store.treatment).toBeNull();
  });
});

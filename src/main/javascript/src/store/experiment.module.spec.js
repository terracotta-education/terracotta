import { beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";

vi.mock("@/services", () => ({
  experimentService: {
    create: vi.fn(),
    getById: vi.fn(),
    getAll: vi.fn(),
    update: vi.fn(),
    delete: vi.fn(),
    export: vi.fn(),
    import: vi.fn(),
    pollImport: vi.fn(),
    pollImports: vi.fn(),
    acknowledgeImport: vi.fn()
  }
}));

import { experimentService } from "@/services";
import { experiment } from "./experiment.module";

describe("experiment store", () => {
  let store;

  beforeEach(() => {
    setActivePinia(createPinia());
    store = experiment();
    vi.clearAllMocks();
    vi.spyOn(console, "error").mockImplementation(() => {});
  });

  describe("initial state / getters", () => {
    it("starts empty", () => {
      expect(store.experiment).toBeNull();
      expect(store.experiments).toEqual([]);
      expect(store.importRequests).toEqual([]);
      expect(store.hasExperiment).toBe(false);
      expect(store.hasExperiments).toBe(false);
      expect(store.conditions).toEqual([]);
    });

    it("conditions getter reflects the current experiment", () => {
      store.setExperiment({
        experimentId: 1,
        conditions: [{ conditionId: 1 }]
      });

      expect(store.conditions).toEqual([{ conditionId: 1 }]);
      expect(store.hasExperiment).toBe(true);
    });

    it("hasExperiments reflects list length", () => {
      store.setExperiment({ experimentId: 1 });
      expect(store.hasExperiments).toBe(true);
    });
  });

  describe("resetExperiment / resetExperiments", () => {
    it("resetExperiment clears experiment and importRequests only", () => {
      store.setExperiment({ experimentId: 1 });
      store.upsertImportRequest({ id: 5, status: "PROCESSING" });

      store.resetExperiment();

      expect(store.experiment).toBeNull();
      expect(store.importRequests).toEqual([]);
      expect(store.experiments).toHaveLength(1);
    });

    it("resetExperiments clears experiments and importRequests", () => {
      store.setExperiment({ experimentId: 1 });
      store.upsertImportRequest({ id: 5, status: "PROCESSING" });

      store.resetExperiments();

      expect(store.experiments).toEqual([]);
      expect(store.importRequests).toEqual([]);
    });
  });

  describe("createExperiment", () => {
    it("delegates directly to the service and returns its value", async () => {
      experimentService.create.mockResolvedValue({ data: { id: 1 } });

      const result = await store.createExperiment();

      expect(experimentService.create).toHaveBeenCalled();
      expect(result).toEqual({ data: { id: 1 } });
    });

    it("logs and swallows errors, returning null", async () => {
      const consoleSpy = vi.spyOn(console, "error").mockImplementation(() => {});
      experimentService.create.mockRejectedValue(new Error("boom"));

      const result = await store.createExperiment();

      expect(result).toBeNull();
      expect(consoleSpy).toHaveBeenCalled();
      consoleSpy.mockRestore();
    });
  });

  describe("fetchExperimentById", () => {
    it("sets the experiment on a 200 response", async () => {
      const response = { status: 200, data: { experimentId: 1 } };
      experimentService.getById.mockResolvedValue(response);

      const result = await store.fetchExperimentById(1);

      expect(store.experiment).toEqual({ experimentId: 1 });
      expect(result).toBe(response);
    });

    it("does not set the experiment on a non-200 response", async () => {
      const response = { status: 404 };
      experimentService.getById.mockResolvedValue(response);

      const result = await store.fetchExperimentById(1);

      expect(store.experiment).toBeNull();
      expect(result).toBe(response);
    });

    it("returns null and logs on error", async () => {
      experimentService.getById.mockRejectedValue(new Error("boom"));

      const result = await store.fetchExperimentById(1);

      expect(result).toBeNull();
      expect(console.error).toHaveBeenCalled();
    });
  });

  describe("fetchExperiments", () => {
    it("sets experiments on a 200 response", async () => {
      const response = { status: 200, data: [{ experimentId: 1 }] };
      experimentService.getAll.mockResolvedValue(response);

      await store.fetchExperiments();

      expect(store.experiments).toEqual([{ experimentId: 1 }]);
    });

    it("leaves experiments untouched on non-200", async () => {
      experimentService.getAll.mockResolvedValue({ status: 500 });

      await store.fetchExperiments();

      expect(store.experiments).toEqual([]);
    });

    it("returns null and logs on error", async () => {
      experimentService.getAll.mockRejectedValue(new Error("boom"));

      const result = await store.fetchExperiments();

      expect(result).toBeNull();
      expect(console.error).toHaveBeenCalled();
    });
  });

  describe("updateExperiment", () => {
    it("sets the experiment on a 200 response", async () => {
      const response = { status: 200 };
      experimentService.update.mockResolvedValue(response);

      const payload = { experimentId: 1, name: "updated" };
      const result = await store.updateExperiment(payload);

      expect(store.experiment).toEqual(payload);
      expect(result).toBe(response);
    });

    it("does not set experiment on non-200", async () => {
      experimentService.update.mockResolvedValue({ status: 400 });

      await store.updateExperiment({ experimentId: 1 });

      expect(store.experiment).toBeNull();
    });

    it("returns null and logs on error", async () => {
      experimentService.update.mockRejectedValue(new Error("boom"));

      const result = await store.updateExperiment({ experimentId: 1 });

      expect(result).toBeNull();
      expect(console.error).toHaveBeenCalled();
    });
  });

  describe("deleteExperiment", () => {
    it("removes the experiment from the list on a 200 response", async () => {
      store.setExperiment({ experimentId: 1 });
      store.setExperiment({ experimentId: 2 });
      experimentService.delete.mockResolvedValue({ status: 200 });

      await store.deleteExperiment(1);

      expect(store.experiments.map(e => e.experimentId)).toEqual([2]);
    });

    it("leaves the list untouched on non-200", async () => {
      store.setExperiment({ experimentId: 1 });
      experimentService.delete.mockResolvedValue({ status: 500 });

      await store.deleteExperiment(1);

      expect(store.experiments).toHaveLength(1);
    });

    it("returns null and logs on error", async () => {
      experimentService.delete.mockRejectedValue(new Error("boom"));

      const result = await store.deleteExperiment(1);

      expect(result).toBeNull();
      expect(console.error).toHaveBeenCalled();
    });
  });

  describe("exportExperiment", () => {
    it("returns the service result on success", async () => {
      experimentService.export.mockResolvedValue({ status: 200 });

      const result = await store.exportExperiment(1);

      expect(result).toEqual({ status: 200 });
    });

    it("returns null and logs on error", async () => {
      experimentService.export.mockRejectedValue(new Error("boom"));

      const result = await store.exportExperiment(1);

      expect(result).toBeNull();
      expect(console.error).toHaveBeenCalled();
    });
  });

  describe("importExperiment", () => {
    it("upserts the returned import request", async () => {
      experimentService.import.mockResolvedValue({
        id: 5,
        status: "PROCESSING"
      });

      const result = await store.importExperiment(new Blob());

      expect(store.importRequests).toHaveLength(1);
      expect(store.importRequests[0].processing).toBe(true);
      expect(result).toEqual({ id: 5, status: "PROCESSING" });
    });

    it("returns null and logs on error", async () => {
      experimentService.import.mockRejectedValue(new Error("boom"));

      const result = await store.importExperiment(new Blob());

      expect(result).toBeNull();
      expect(store.importRequests).toEqual([]);
      expect(console.error).toHaveBeenCalled();
    });
  });

  describe("pollImport", () => {
    it("upserts the returned import request's data", async () => {
      experimentService.pollImport.mockResolvedValue({
        data: { id: 5, status: "COMPLETE" }
      });

      await store.pollImport({ id: 5 });

      expect(store.importRequests).toHaveLength(1);
      expect(store.importRequests[0].complete).toBe(true);
    });

    it("returns null and logs on error", async () => {
      experimentService.pollImport.mockRejectedValue(new Error("boom"));

      const result = await store.pollImport({ id: 5 });

      expect(result).toBeNull();
      expect(console.error).toHaveBeenCalled();
    });
  });

  describe("pollImports", () => {
    it("replaces importRequests with normalized, id-filtered entries", async () => {
      experimentService.pollImports.mockResolvedValue({
        data: [
          { id: 1, status: "PROCESSING" },
          { status: "ERROR" },
          { id: 2, status: "COMPLETE" }
        ]
      });

      await store.pollImports();

      expect(store.importRequests).toHaveLength(2);
      expect(store.importRequests.map(r => r.id)).toEqual([1, 2]);
    });

    it("defaults to an empty list when data is missing", async () => {
      experimentService.pollImports.mockResolvedValue({});

      await store.pollImports();

      expect(store.importRequests).toEqual([]);
    });

    it("returns null and logs on error", async () => {
      experimentService.pollImports.mockRejectedValue(new Error("boom"));

      const result = await store.pollImports();

      expect(result).toBeNull();
      expect(console.error).toHaveBeenCalled();
    });
  });

  describe("acknowledgeImport", () => {
    it("clears importRequests on success", async () => {
      store.upsertImportRequest({ id: 1, status: "PROCESSING" });
      experimentService.acknowledgeImport.mockResolvedValue({ status: 200 });

      await store.acknowledgeImport([1, "COMPLETE_ACKNOWLEDGED"]);

      expect(experimentService.acknowledgeImport).toHaveBeenCalledWith(
        1,
        "COMPLETE_ACKNOWLEDGED"
      );
      expect(store.importRequests).toEqual([]);
    });

    it("logs on error and leaves importRequests untouched", async () => {
      store.upsertImportRequest({ id: 1, status: "PROCESSING" });
      experimentService.acknowledgeImport.mockRejectedValue(
        new Error("boom")
      );

      await store.acknowledgeImport([1, "COMPLETE_ACKNOWLEDGED"]);

      expect(store.importRequests).toHaveLength(1);
      expect(console.error).toHaveBeenCalled();
    });
  });

  describe("resetImportRequests", () => {
    it("clears the import requests list", () => {
      store.upsertImportRequest({ id: 1, status: "PROCESSING" });
      store.resetImportRequests();

      expect(store.importRequests).toEqual([]);
    });
  });

  describe("setExperiment", () => {
    it("sets the current experiment and adds it to the list when new", () => {
      store.setExperiment({ experimentId: 1 });

      expect(store.experiment).toEqual({ experimentId: 1 });
      expect(store.experiments).toEqual([{ experimentId: 1 }]);
    });

    it("replaces an existing entry in the list", () => {
      store.setExperiment({ experimentId: 1, name: "first" });
      store.setExperiment({ experimentId: 1, name: "second" });

      expect(store.experiments).toHaveLength(1);
      expect(store.experiments[0].name).toBe("second");
    });

    it("sets experiment without touching the list when experimentId is missing", () => {
      store.setExperiment({ name: "no id" });

      expect(store.experiment).toEqual({ name: "no id" });
      expect(store.experiments).toEqual([]);
    });
  });

  describe("setConditions", () => {
    it("sets conditions on the current experiment", () => {
      store.setExperiment({ experimentId: 1 });
      store.setConditions([{ conditionId: 1 }]);

      expect(store.experiment.conditions).toEqual([{ conditionId: 1 }]);
    });

    it("does nothing when there is no current experiment", () => {
      store.setConditions([{ conditionId: 1 }]);

      expect(store.experiment).toBeNull();
    });
  });

  describe("setCondition", () => {
    it("adds a new condition", () => {
      store.setExperiment({ experimentId: 1, conditions: [] });
      store.setCondition({ conditionId: 1 });

      expect(store.experiment.conditions).toEqual([{ conditionId: 1 }]);
    });

    it("replaces an existing condition", () => {
      store.setExperiment({
        experimentId: 1,
        conditions: [{ conditionId: 1, name: "a" }]
      });
      store.setCondition({ conditionId: 1, name: "b" });

      expect(store.experiment.conditions).toEqual([
        { conditionId: 1, name: "b" }
      ]);
    });

    it("does nothing when there is no experiment/conditions", () => {
      store.setCondition({ conditionId: 1 });
      expect(store.experiment).toBeNull();
    });
  });

  describe("deleteCondition", () => {
    it("removes a matching condition", () => {
      store.setExperiment({
        experimentId: 1,
        conditions: [{ conditionId: 1 }, { conditionId: 2 }]
      });
      store.deleteCondition({ conditionId: 1 });

      expect(store.experiment.conditions).toEqual([{ conditionId: 2 }]);
    });

    it("does nothing when there is no experiment/conditions", () => {
      store.deleteCondition({ conditionId: 1 });
      expect(store.experiment).toBeNull();
    });
  });

  describe("upsertImportRequest", () => {
    it("ignores requests without an id", () => {
      store.upsertImportRequest({ status: "PROCESSING" });
      expect(store.importRequests).toEqual([]);
    });

    it("ignores a null request", () => {
      store.upsertImportRequest(null);
      expect(store.importRequests).toEqual([]);
    });
  });
});

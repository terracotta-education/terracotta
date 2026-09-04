import { beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";

vi.mock("@/services", () => ({
  conditionService: {
    create: vi.fn(),
    update: vi.fn(),
    updateAll: vi.fn(),
    delete: vi.fn()
  }
}));

import { conditionService } from "@/services";
import { condition } from "./condition.module";
import { exposures as useExposuresStore } from "./exposures.module";
import { experiment as useExperimentStore } from "./experiment.module";

describe("condition store", () => {
  let store;
  let consoleSpy;

  beforeEach(() => {
    setActivePinia(createPinia());
    store = condition();
    vi.clearAllMocks();
    consoleSpy = vi.spyOn(console, "error").mockImplementation(() => {});
  });

  describe("conditionColorMapping", () => {
    it("returns an empty object when there are no exposures", () => {
      expect(store.conditionColorMapping).toEqual({});
    });

    it("maps each condition name to a color by position", () => {
      const exposuresStore = useExposuresStore();
      exposuresStore.exposures = [
        {
          groupConditionList: [
            { conditionName: "Group A" },
            { conditionName: "Group B" }
          ]
        }
      ];

      const mapping = store.conditionColorMapping;

      expect(Object.keys(mapping)).toEqual(["Group A", "Group B"]);
      expect(mapping["Group A"]).not.toBe(mapping["Group B"]);
    });
  });

  describe("createDefaultConditions", () => {
    it("creates two blank conditions", async () => {
      conditionService.create.mockResolvedValue({ conditionId: 1 });

      const result = await store.createDefaultConditions(42);

      expect(conditionService.create).toHaveBeenCalledTimes(2);
      expect(conditionService.create).toHaveBeenCalledWith(42);
      expect(result).toHaveLength(2);
    });
  });

  describe("createConditions", () => {
    it("returns [] when there are no conditions to create", async () => {
      const result = await store.createConditions({ conditions: [], experimentId: 1 });

      expect(result).toEqual([]);
      expect(conditionService.create).not.toHaveBeenCalled();
    });

    it("creates one condition per entry via createCondition", async () => {
      conditionService.create.mockResolvedValue({ conditionId: 5 });

      const result = await store.createConditions({
        conditions: [{}, {}, {}],
        experimentId: 1
      });

      expect(conditionService.create).toHaveBeenCalledTimes(3);
      expect(result).toHaveLength(3);
    });
  });

  describe("createCondition", () => {
    it("returns the message payload as-is without touching state when the service reports a message", async () => {
      conditionService.create.mockResolvedValue({ message: "duplicate name" });

      const result = await store.createCondition(1);

      expect(result).toEqual({ message: "duplicate name" });
      expect(store.condition).toBeNull();
      expect(store.conditions).toEqual([]);
    });

    it("sets condition state and upserts on success", async () => {
      conditionService.create.mockResolvedValue({ conditionId: 1, experimentId: 1 });

      const result = await store.createCondition(1);

      expect(result).toEqual({ conditionId: 1, experimentId: 1 });
      expect(store.condition).toEqual({ conditionId: 1, experimentId: 1 });
      expect(store.conditions).toContainEqual({ conditionId: 1, experimentId: 1 });
    });

    it("returns null and logs on rejection", async () => {
      conditionService.create.mockRejectedValue(new Error("fail"));

      const result = await store.createCondition(1);

      expect(result).toBeNull();
      expect(consoleSpy).toHaveBeenCalled();
    });
  });

  describe("updateCondition", () => {
    it("updates state on a 200 response", async () => {
      conditionService.update.mockResolvedValue({ status: 200 });
      const cond = { conditionId: 1, experimentId: 1, name: "Updated" };

      const result = await store.updateCondition(cond);

      expect(result).toEqual({ status: 200 });
      expect(store.condition).toEqual(cond);
      expect(store.conditions).toContainEqual(cond);
    });

    it("leaves state untouched on a non-200 response", async () => {
      conditionService.update.mockResolvedValue({ status: 400 });

      const result = await store.updateCondition({ conditionId: 1 });

      expect(result).toEqual({ status: 400 });
      expect(store.condition).toBeNull();
    });

    it("returns null and logs on rejection", async () => {
      conditionService.update.mockRejectedValue(new Error("fail"));

      const result = await store.updateCondition({ conditionId: 1 });

      expect(result).toBeNull();
      expect(consoleSpy).toHaveBeenCalled();
    });
  });

  describe("updateConditions", () => {
    it("replaces the conditions list on a 200 response", async () => {
      conditionService.updateAll.mockResolvedValue({ status: 200 });
      const conditions = [{ conditionId: 1 }, { conditionId: 2 }];

      const result = await store.updateConditions(conditions);

      expect(result).toEqual({ status: 200 });
      expect(store.conditions).toEqual(conditions);
    });

    it("leaves state untouched on a non-200 response", async () => {
      conditionService.updateAll.mockResolvedValue({ status: 400 });

      const result = await store.updateConditions([{ conditionId: 1 }]);

      expect(result).toEqual({ status: 400 });
      expect(store.conditions).toEqual([]);
    });

    it("returns null and logs on rejection", async () => {
      conditionService.updateAll.mockRejectedValue(new Error("fail"));

      const result = await store.updateConditions([{ conditionId: 1 }]);

      expect(result).toBeNull();
      expect(consoleSpy).toHaveBeenCalled();
    });
  });

  describe("setDefaultCondition", () => {
    it("returns false when conditions or defaultConditionId are missing", async () => {
      expect(await store.setDefaultCondition({})).toBe(false);
      expect(await store.setDefaultCondition({ conditions: [] })).toBe(false);
      expect(
        await store.setDefaultCondition({ conditions: [{}], defaultConditionId: null })
      ).toBe(false);
      expect(conditionService.updateAll).not.toHaveBeenCalled();
    });

    it("flags only the matching condition as default and delegates to updateConditions", async () => {
      conditionService.updateAll.mockResolvedValue({ status: 200 });

      const payload = {
        conditions: [{ conditionId: 1 }, { conditionId: 2 }],
        defaultConditionId: 2
      };

      await store.setDefaultCondition(payload);

      expect(conditionService.updateAll).toHaveBeenCalledWith([
        { conditionId: 1, defaultCondition: 0 },
        { conditionId: 2, defaultCondition: 1 }
      ]);
    });
  });

  describe("deleteCondition", () => {
    beforeEach(() => {
      store.upsertCondition({ conditionId: 1, experimentId: 1 });
      store.upsertCondition({ conditionId: 2, experimentId: 1 });
      store.condition = { conditionId: 1, experimentId: 1 };
    });

    it("removes the condition from state and clears the current condition if it matches", async () => {
      conditionService.delete.mockResolvedValue({ status: 200 });

      const result = await store.deleteCondition({ conditionId: 1, experimentId: 1 });

      expect(result).toEqual({ status: 200 });
      expect(store.conditions.map(c => c.conditionId)).toEqual([2]);
      expect(store.condition).toBeNull();
    });

    it("keeps the current condition when deleting a different one", async () => {
      conditionService.delete.mockResolvedValue({ status: 200 });

      await store.deleteCondition({ conditionId: 2, experimentId: 1 });

      expect(store.condition).toEqual({ conditionId: 1, experimentId: 1 });
    });

    it("returns null and logs on rejection, leaving state untouched", async () => {
      conditionService.delete.mockRejectedValue(new Error("fail"));

      const result = await store.deleteCondition({ conditionId: 1, experimentId: 1 });

      expect(result).toBeNull();
      expect(consoleSpy).toHaveBeenCalled();
      // the filtering/experiment-sync logic runs after the awaited call, so a
      // rejection leaves the conditions list untouched
      expect(store.conditions).toHaveLength(2);
    });

    it("leaves local state untouched on a non-200 response that doesn't throw", async () => {
      conditionService.delete.mockResolvedValue({ status: 404 });

      const result = await store.deleteCondition({ conditionId: 1, experimentId: 1 });

      expect(result).toEqual({ status: 404 });
      expect(store.conditions).toHaveLength(2);
      expect(store.condition).toEqual({ conditionId: 1, experimentId: 1 });
    });
  });

  describe("resetConditions", () => {
    it("clears condition and conditions", () => {
      store.upsertCondition({ conditionId: 1 });
      store.condition = { conditionId: 1 };

      store.resetConditions();

      expect(store.condition).toBeNull();
      expect(store.conditions).toEqual([]);
    });
  });

  describe("upsertCondition", () => {
    it("is a no-op for falsy input", () => {
      store.upsertCondition(null);
      expect(store.conditions).toEqual([]);
    });

    it("replaces an existing condition matched by id", () => {
      store.upsertCondition({ conditionId: 1, name: "old" });
      store.upsertCondition({ conditionId: 1, name: "new" });

      expect(store.conditions).toEqual([{ conditionId: 1, name: "new" }]);
    });
  });

  it("createCondition syncs the condition into the experiment store", async () => {
    const experimentStore = useExperimentStore();
    experimentStore.experiment = { experimentId: 1, conditions: [] };
    conditionService.create.mockResolvedValue({ conditionId: 1, experimentId: 1 });

    await store.createCondition(1);

    expect(experimentStore.experiment.conditions).toContainEqual({
      conditionId: 1,
      experimentId: 1
    });
  });
});

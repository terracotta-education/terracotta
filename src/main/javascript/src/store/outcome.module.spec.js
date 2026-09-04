import { beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";

vi.mock("@/services", () => ({
  outcomeService: {
    create: vi.fn(),
    updateOutcome: vi.fn(),
    deleteOutcome: vi.fn(),
    getById: vi.fn(),
    getAll: vi.fn(),
    getAllByExperimentId: vi.fn(),
    getOutcomeScoresById: vi.fn(),
    updateOutcomeScores: vi.fn(),
    getOutcomePotentials: vi.fn()
  }
}));

import { outcomeService } from "@/services";
import { outcome } from "./outcome.module";

describe("outcome store", () => {
  let store;

  beforeEach(() => {
    setActivePinia(createPinia());
    store = outcome();
    vi.clearAllMocks();
  });

  it("starts empty", () => {
    expect(store.hasOutcome).toBe(false);
    expect(store.hasOutcomes).toBe(false);
    expect(store.outcomes).toEqual([]);
  });

  describe("createOutcome", () => {
    it("sets outcome on 200/201 status", async () => {
      const data = { outcomeId: 1 };
      outcomeService.create.mockResolvedValue({ status: 201, data });

      const result = await store.createOutcome(["a", "b"]);

      expect(outcomeService.create).toHaveBeenCalledWith("a", "b");
      expect(result).toEqual(data);
      expect(store.outcome).toEqual(data);
      expect(store.hasOutcome).toBe(true);
    });

    it("returns null when status is not 200/201", async () => {
      outcomeService.create.mockResolvedValue({ status: 400 });

      const result = await store.createOutcome(["a"]);

      expect(result).toBeNull();
      expect(store.outcome).toBeNull();
    });

    it("returns null on error", async () => {
      outcomeService.create.mockRejectedValue(new Error("fail"));

      const result = await store.createOutcome(["a"]);

      expect(result).toBeNull();
    });
  });

  describe("updateOutcome", () => {
    it("sets outcome from payload[2] on 200", async () => {
      const outcomeData = { outcomeId: 5 };
      outcomeService.updateOutcome.mockResolvedValue({ status: 200 });

      const result = await store.updateOutcome(["a", "b", outcomeData]);

      expect(outcomeService.updateOutcome).toHaveBeenCalledWith(
        "a",
        "b",
        outcomeData
      );
      expect(result).toEqual({ status: 200 });
      expect(store.outcome).toEqual(outcomeData);
    });

    it("does not set outcome when status is not 200", async () => {
      outcomeService.updateOutcome.mockResolvedValue({ status: 400 });

      const result = await store.updateOutcome(["a", "b", { outcomeId: 5 }]);

      expect(result).toEqual({ status: 400 });
      expect(store.outcome).toBeNull();
    });

    it("returns null on error", async () => {
      outcomeService.updateOutcome.mockRejectedValue(new Error("fail"));

      const result = await store.updateOutcome(["a", "b", {}]);

      expect(result).toBeNull();
    });
  });

  describe("deleteOutcome", () => {
    it("clears outcome on 200", async () => {
      store.outcome = { outcomeId: 1 };
      outcomeService.deleteOutcome.mockResolvedValue({ status: 200 });

      const result = await store.deleteOutcome(["a"]);

      expect(result).toEqual({ status: 200 });
      expect(store.outcome).toBeNull();
    });

    it("leaves outcome untouched when status is not 200", async () => {
      store.outcome = { outcomeId: 1 };
      outcomeService.deleteOutcome.mockResolvedValue({ status: 400 });

      await store.deleteOutcome(["a"]);

      expect(store.outcome).toEqual({ outcomeId: 1 });
    });

    it("returns null on error", async () => {
      outcomeService.deleteOutcome.mockRejectedValue(new Error("fail"));

      const result = await store.deleteOutcome(["a"]);

      expect(result).toBeNull();
    });
  });

  describe("fetchOutcomeById", () => {
    it("clears current outcome when id differs, then sets new outcome on 200", async () => {
      store.outcome = { outcomeId: 1 };
      const data = { outcomeId: 2 };
      outcomeService.getById.mockResolvedValue({ status: 200, data });

      const result = await store.fetchOutcomeById(["a", "b", "2"]);

      expect(outcomeService.getById).toHaveBeenCalledWith("a", "b", "2");
      expect(result).toEqual({ status: 200, data });
      expect(store.outcome).toEqual(data);
    });

    it("keeps outcome null and does not set it when status isn't 200", async () => {
      outcomeService.getById.mockResolvedValue({ status: 404 });

      const result = await store.fetchOutcomeById(["a", "b", "2"]);

      expect(result).toEqual({ status: 404 });
      expect(store.outcome).toBeNull();
    });

    it("returns null on error", async () => {
      outcomeService.getById.mockRejectedValue(new Error("fail"));

      const result = await store.fetchOutcomeById(["a", "b", "2"]);

      expect(result).toBeNull();
    });

    it("does not wipe the current outcome on a failed refetch for the same id", async () => {
      const data = { outcomeId: 2 };
      store.outcome = data;
      outcomeService.getById.mockResolvedValue({ status: 500 });

      const result = await store.fetchOutcomeById(["a", "b", "2"]);

      expect(result).toEqual({ status: 500 });
      expect(store.outcome).toEqual(data);
    });

    it("clears a stale outcome for a different id when the new fetch fails", async () => {
      store.outcome = { outcomeId: 1 };
      outcomeService.getById.mockResolvedValue({ status: 500 });

      await store.fetchOutcomeById(["a", "b", "2"]);

      expect(store.outcome).toBeNull();
    });
  });

  describe("fetchOutcomes", () => {
    it("sets outcomes on 200", async () => {
      const data = [{ outcomeId: 1 }];
      outcomeService.getAll.mockResolvedValue({ status: 200, data });

      const result = await store.fetchOutcomes(["a"]);

      expect(result).toEqual({ status: 200, data });
      expect(store.outcomes).toEqual(data);
      expect(store.hasOutcomes).toBe(true);
    });

    it("sets outcomes on 204 with empty data fallback", async () => {
      outcomeService.getAll.mockResolvedValue({ status: 204, data: null });

      await store.fetchOutcomes(["a"]);

      expect(store.outcomes).toEqual([]);
    });

    it("leaves outcomes untouched on other statuses", async () => {
      store.outcomes = [{ outcomeId: 9 }];
      outcomeService.getAll.mockResolvedValue({ status: 500 });

      await store.fetchOutcomes(["a"]);

      expect(store.outcomes).toEqual([{ outcomeId: 9 }]);
    });

    it("returns null on error", async () => {
      outcomeService.getAll.mockRejectedValue(new Error("fail"));

      const result = await store.fetchOutcomes(["a"]);

      expect(result).toBeNull();
    });
  });

  describe("fetchOutcomesByExposures", () => {
    it("calls getAllByExperimentId with only the experimentId and sets experimentOutcomes", async () => {
      const data = [{ outcomeId: 1 }];
      outcomeService.getAllByExperimentId.mockResolvedValue({ data });

      const result = await store.fetchOutcomesByExposures([123, "extra"]);

      expect(outcomeService.getAllByExperimentId).toHaveBeenCalledWith(123);
      expect(result).toEqual({ data });
      expect(store.experimentOutcomes).toEqual(data);
    });

    it("defaults experimentOutcomes to [] when data missing", async () => {
      outcomeService.getAllByExperimentId.mockResolvedValue({});

      await store.fetchOutcomesByExposures([123]);

      expect(store.experimentOutcomes).toEqual([]);
    });

    it("returns null on error", async () => {
      outcomeService.getAllByExperimentId.mockRejectedValue(new Error("fail"));

      const result = await store.fetchOutcomesByExposures([123]);

      expect(result).toBeNull();
    });
  });

  describe("fetchOutcomesByExperimentId", () => {
    it("spreads full payload into service call and sets outcomes", async () => {
      const data = [{ outcomeId: 1 }];
      outcomeService.getAllByExperimentId.mockResolvedValue({ data });

      const result = await store.fetchOutcomesByExperimentId([1, 2]);

      expect(outcomeService.getAllByExperimentId).toHaveBeenCalledWith(1, 2);
      expect(result).toEqual({ data });
      expect(store.outcomes).toEqual(data);
    });

    it("returns null on error", async () => {
      outcomeService.getAllByExperimentId.mockRejectedValue(new Error("fail"));

      const result = await store.fetchOutcomesByExperimentId([1]);

      expect(result).toBeNull();
    });
  });

  describe("fetchOutcomeScores", () => {
    it("sets outcomeScores on 200/204", async () => {
      const data = [{ score: 1 }];
      outcomeService.getOutcomeScoresById.mockResolvedValue({
        status: 200,
        data
      });

      const result = await store.fetchOutcomeScores(["a"]);

      expect(result).toEqual({ status: 200, data });
      expect(store.outcomeScores).toEqual(data);
    });

    it("returns null on error", async () => {
      outcomeService.getOutcomeScoresById.mockRejectedValue(new Error("fail"));

      const result = await store.fetchOutcomeScores(["a"]);

      expect(result).toBeNull();
    });
  });

  describe("updateOutcomeScores", () => {
    it("updates then re-fetches outcome scores", async () => {
      outcomeService.updateOutcomeScores.mockResolvedValue({ status: 200 });
      const data = [{ score: 1 }];
      outcomeService.getOutcomeScoresById.mockResolvedValue({
        status: 200,
        data
      });

      const result = await store.updateOutcomeScores(["a", "b"]);

      expect(outcomeService.updateOutcomeScores).toHaveBeenCalledWith(
        "a",
        "b"
      );
      expect(outcomeService.getOutcomeScoresById).toHaveBeenCalledWith(
        "a",
        "b"
      );
      expect(result).toEqual({ status: 200, data });
      expect(store.outcomeScores).toEqual(data);
    });

    it("returns null on error without calling fetchOutcomeScores", async () => {
      outcomeService.updateOutcomeScores.mockRejectedValue(new Error("fail"));

      const result = await store.updateOutcomeScores(["a"]);

      expect(result).toBeNull();
      expect(outcomeService.getOutcomeScoresById).not.toHaveBeenCalled();
    });
  });

  describe("fetchOutcomePotentials", () => {
    it("parses experimentId to int and sets outcomePotentials on 200", async () => {
      const data = [{ potential: 1 }];
      outcomeService.getOutcomePotentials.mockResolvedValue({
        status: 200,
        data
      });

      const result = await store.fetchOutcomePotentials("42");

      expect(outcomeService.getOutcomePotentials).toHaveBeenCalledWith(42);
      expect(result).toEqual({ status: 200, data });
      expect(store.outcomePotentials).toEqual(data);
    });

    it("returns null on error", async () => {
      outcomeService.getOutcomePotentials.mockRejectedValue(new Error("fail"));

      const result = await store.fetchOutcomePotentials("42");

      expect(result).toBeNull();
    });
  });

  describe("setOutcome", () => {
    it("takes data[2] when given an array", () => {
      store.setOutcome(["a", "b", { outcomeId: 7 }]);

      expect(store.outcome).toEqual({ outcomeId: 7 });
    });

    it("ignores data without an outcomeId", () => {
      store.outcome = { outcomeId: 1 };

      store.setOutcome({ name: "no id" });

      expect(store.outcome).toEqual({ outcomeId: 1 });
    });

    it("sets outcome directly when given a plain object with an outcomeId", () => {
      store.setOutcome({ outcomeId: 9 });

      expect(store.outcome).toEqual({ outcomeId: 9 });
    });
  });

  it("resetOutcome clears outcome only", () => {
    store.outcome = { outcomeId: 1 };
    store.outcomes = [{ outcomeId: 1 }];

    store.resetOutcome();

    expect(store.outcome).toBeNull();
    expect(store.outcomes).toEqual([{ outcomeId: 1 }]);
  });

  it("resetOutcomePotentials clears outcomePotentials only", () => {
    store.outcomePotentials = [{ potential: 1 }];
    store.outcome = { outcomeId: 1 };

    store.resetOutcomePotentials();

    expect(store.outcomePotentials).toEqual([]);
    expect(store.outcome).toEqual({ outcomeId: 1 });
  });
});

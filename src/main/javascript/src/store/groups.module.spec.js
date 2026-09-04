import { beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";

vi.mock("@/services", () => ({
  groupsService: {
    createAndAssignGroups: vi.fn()
  }
}));

import { groupsService } from "@/services";
import { groups } from "./groups.module";

describe("groups store", () => {
  let store;

  beforeEach(() => {
    setActivePinia(createPinia());
    store = groups();
    vi.clearAllMocks();
    vi.spyOn(console, "error").mockImplementation(() => {});
  });

  it("starts with no groups", () => {
    expect(store.groups).toEqual([]);
    expect(store.hasGroups).toBe(false);
  });

  describe("createAndAssignGroups", () => {
    it("sets groups when the response is an array", async () => {
      groupsService.createAndAssignGroups.mockResolvedValue([
        { groupId: 1 }
      ]);

      const result = await store.createAndAssignGroups(10);

      expect(groupsService.createAndAssignGroups).toHaveBeenCalledWith(10);
      expect(store.groups).toEqual([{ groupId: 1 }]);
      expect(store.hasGroups).toBe(true);
      expect(result).toEqual([{ groupId: 1 }]);
    });

    it("sets groups to [] when the response is truthy but not an array", async () => {
      groupsService.createAndAssignGroups.mockResolvedValue({
        message: "ok"
      });

      await store.createAndAssignGroups(10);

      expect(store.groups).toEqual([]);
    });

    it("resets groups to empty when the response is falsy", async () => {
      store.groups = [{ groupId: 1 }];
      groupsService.createAndAssignGroups.mockResolvedValue(null);

      await store.createAndAssignGroups(10);

      expect(store.groups).toEqual([]);
    });

    it("returns null and logs on error", async () => {
      groupsService.createAndAssignGroups.mockRejectedValue(
        new Error("boom")
      );

      const result = await store.createAndAssignGroups(10);

      expect(result).toBeNull();
      expect(console.error).toHaveBeenCalled();
    });
  });

  describe("resetGroups", () => {
    it("clears the groups list", () => {
      store.groups = [{ groupId: 1 }];
      store.resetGroups();

      expect(store.groups).toEqual([]);
    });
  });
});

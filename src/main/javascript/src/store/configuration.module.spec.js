import { beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";

vi.mock("@/services", () => ({
  configurationService: {
    get: vi.fn()
  }
}));

import { configurationService } from "@/services";
import { configuration } from "./configuration.module";

describe("configuration store", () => {
  let store;
  let consoleSpy;

  beforeEach(() => {
    setActivePinia(createPinia());
    store = configuration();
    vi.clearAllMocks();
    consoleSpy = vi.spyOn(console, "error").mockImplementation(() => {});
  });

  describe("getters", () => {
    it("get and hasConfigurations reflect the null initial state", () => {
      expect(store.get).toBeNull();
      expect(store.hasConfigurations).toBe(false);
    });

    it("getConfiguration returns the default value when missing", () => {
      expect(store.getConfiguration("featureX")).toBeNull();
      expect(store.getConfiguration("featureX", "fallback")).toBe("fallback");
    });

    it("getConfiguration returns the stored value when present", () => {
      store.update({ name: "featureX", value: true });

      expect(store.getConfiguration("featureX")).toBe(true);
      expect(store.hasConfigurations).toBe(true);
    });
  });

  describe("retrieve", () => {
    it("sets configurations on success", async () => {
      configurationService.get.mockResolvedValue({ featureX: true });

      const result = await store.retrieve();

      expect(result).toEqual({ featureX: true });
      expect(store.configurations).toEqual({ featureX: true });
    });

    it("falls back to an empty object when the service resolves falsy", async () => {
      configurationService.get.mockResolvedValue(null);

      const result = await store.retrieve();

      expect(result).toBeNull();
      expect(store.configurations).toEqual({});
    });

    it("returns the message payload as-is without setting state", async () => {
      configurationService.get.mockResolvedValue({ message: "error occurred" });

      const result = await store.retrieve();

      expect(result).toEqual({ message: "error occurred" });
      expect(store.configurations).toBeNull();
    });

    it("returns null and logs on rejection, leaving state untouched", async () => {
      configurationService.get.mockRejectedValue(new Error("fail"));

      const result = await store.retrieve();

      expect(result).toBeNull();
      expect(store.configurations).toBeNull();
      expect(consoleSpy).toHaveBeenCalled();
    });
  });

  describe("update", () => {
    it("is a no-op when data has no name", () => {
      store.update(null);
      expect(store.configurations).toBeNull();

      store.update({ value: true });
      expect(store.configurations).toBeNull();
    });

    it("merges the new value into existing configurations", () => {
      store.update({ name: "a", value: 1 });
      store.update({ name: "b", value: 2 });

      expect(store.configurations).toEqual({ a: 1, b: 2 });
    });

    it("overwrites an existing key", () => {
      store.update({ name: "a", value: 1 });
      store.update({ name: "a", value: 2 });

      expect(store.configurations).toEqual({ a: 2 });
    });
  });

  describe("reset", () => {
    it("clears configurations back to null", () => {
      store.update({ name: "a", value: 1 });
      store.reset();

      expect(store.configurations).toBeNull();
      expect(store.hasConfigurations).toBe(false);
    });
  });
});

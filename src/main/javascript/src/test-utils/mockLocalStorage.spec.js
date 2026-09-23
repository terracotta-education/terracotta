import { describe, it, expect, vi } from "vitest";

import { createMockLocalStorage } from "./mockLocalStorage";

describe("createMockLocalStorage", () => {
  it("starts empty when no initial seed is given", () => {
    const storage = createMockLocalStorage();

    expect(Object.keys(storage)).toEqual([]);
    expect(storage.getItem("missing")).toBe(null);
  });

  it("seeds stored keys from the initial object", () => {
    const storage = createMockLocalStorage({ a: "1", b: "2" });

    expect(Object.keys(storage).sort()).toEqual(["a", "b"]);
    expect(storage.getItem("a")).toBe("1");
    expect(storage.getItem("b")).toBe("2");
  });

  it("getItem returns null for a key that was never set", () => {
    const storage = createMockLocalStorage();

    expect(storage.getItem("nope")).toBe(null);
  });

  it("setItem stores a value retrievable via getItem", () => {
    const storage = createMockLocalStorage();

    storage.setItem("token", "abc123");

    expect(storage.getItem("token")).toBe("abc123");
  });

  it("setItem coerces non-string values via String()", () => {
    const storage = createMockLocalStorage();

    storage.setItem("count", 42);
    storage.setItem("flag", true);
    storage.setItem("data", { a: 1 });

    expect(storage.getItem("count")).toBe("42");
    expect(storage.getItem("flag")).toBe("true");
    expect(storage.getItem("data")).toBe(String({ a: 1 }));
  });

  it("removeItem deletes a stored key", () => {
    const storage = createMockLocalStorage({ keep: "1", drop: "2" });

    storage.removeItem("drop");

    expect(storage.getItem("drop")).toBe(null);
    expect(storage.getItem("keep")).toBe("1");
    expect(Object.keys(storage)).toEqual(["keep"]);
  });

  it("clear empties all keys set via setItem and from the initial seed", () => {
    const storage = createMockLocalStorage({ seeded: "1" });

    storage.setItem("added", "2");
    expect(Object.keys(storage).sort()).toEqual(["added", "seeded"]);

    storage.clear();

    expect(Object.keys(storage)).toEqual([]);
    expect(storage.getItem("seeded")).toBe(null);
    expect(storage.getItem("added")).toBe(null);
  });

  it("Object.keys only reflects real data keys, not the defined methods", () => {
    const storage = createMockLocalStorage({ a: "1" });

    const keys = Object.keys(storage);

    expect(keys).toEqual(["a"]);
    expect(keys).not.toContain("getItem");
    expect(keys).not.toContain("setItem");
    expect(keys).not.toContain("removeItem");
    expect(keys).not.toContain("clear");
  });

  it("allows vi.spyOn() on its methods because they are configurable", () => {
    const storage = createMockLocalStorage();
    const spy = vi.spyOn(storage, "getItem");

    storage.getItem("anything");

    expect(spy).toHaveBeenCalledWith("anything");
  });
});

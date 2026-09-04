import { describe, it, expect } from "vitest";

import { omitDeep } from "./deep-omit";
import omitDeepDefault from "./deep-omit";

describe("omitDeep", () => {
  it("removes a top-level key whose value is a primitive", () => {
    const result = omitDeep({ a: 1, password: "secret" }, ["password"]);

    expect(result).toEqual({ a: 1 });
  });

  it("removes matching keys nested inside plain objects", () => {
    const result = omitDeep(
      { a: 1, nested: { b: 2, password: "secret" } },
      ["password"]
    );

    expect(result).toEqual({ a: 1, nested: { b: 2 } });
  });

  it("removes matching keys from objects inside arrays", () => {
    const result = omitDeep(
      [{ a: 1, password: "s1" }, { a: 2, password: "s2" }],
      ["password"]
    );

    expect(result).toEqual([{ a: 1 }, { a: 2 }]);
  });

  it("removes matching keys from arrays nested under an object key", () => {
    const result = omitDeep(
      { list: [{ a: 1, token: "t1" }, { a: 2, token: "t2" }] },
      ["token"]
    );

    expect(result).toEqual({ list: [{ a: 1 }, { a: 2 }] });
  });

  it("leaves objects unchanged when no keys match", () => {
    const input = { a: 1, b: { c: 2 } };

    expect(omitDeep(input, ["notPresent"])).toEqual({ a: 1, b: { c: 2 } });
  });

  it("does not delete a key whose own value is a plain object (it recurses into it instead)", () => {
    // The implementation only deletes a key when its value is NOT itself a
    // plain object/array (the recursion branch takes priority), so a key
    // named like an omitted key that holds an object survives, unrecursed
    // primitive descendants of it are still filtered.
    const result = omitDeep(
      { password: { hint: "h", secret: "shh" } },
      ["password", "secret"]
    );

    expect(result).toEqual({ password: { hint: "h" } });
  });

  it("handles null values without throwing", () => {
    const result = omitDeep({ a: null, password: "secret" }, ["password"]);

    expect(result).toEqual({ a: null });
  });

  it("returns non-object, non-array input untouched", () => {
    expect(omitDeep("just a string", ["a"])).toBe("just a string");
    expect(omitDeep(42, ["a"])).toBe(42);
    expect(omitDeep(null, ["a"])).toBe(null);
  });

  it("mutates and returns the same object reference for plain objects", () => {
    const input = { a: 1, password: "secret" };
    const result = omitDeep(input, ["password"]);

    expect(result).toBe(input);
  });

  it("exposes the same function as default and named export", () => {
    expect(omitDeepDefault).toBe(omitDeep);
  });
});

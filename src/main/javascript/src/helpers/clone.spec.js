import { describe, it, expect } from "vitest";

import { clone } from "./clone";

describe("clone", () => {
  it("deep clones a plain object", () => {
    const original = { a: 1, b: { c: 2 } };
    const cloned = clone(original);

    expect(cloned).toEqual(original);
    expect(cloned).not.toBe(original);
    expect(cloned.b).not.toBe(original.b);
  });

  it("does not mutate the original when the clone is mutated", () => {
    const original = { a: 1, nested: { value: "x" } };
    const cloned = clone(original);

    cloned.nested.value = "y";

    expect(original.nested.value).toBe("x");
  });

  it("deep clones arrays", () => {
    const original = [1, 2, { a: 3 }];
    const cloned = clone(original);

    expect(cloned).toEqual(original);
    expect(cloned).not.toBe(original);
    expect(cloned[2]).not.toBe(original[2]);
  });

  it("clones primitives wrapped in objects", () => {
    expect(clone({ a: null, b: 0, c: "", d: false })).toEqual({
      a: null,
      b: 0,
      c: "",
      d: false
    });
  });

  it("drops keys with undefined values (JSON.stringify behavior)", () => {
    const original = { a: 1, b: undefined };
    const cloned = clone(original);

    expect(cloned).toEqual({ a: 1 });
    expect("b" in cloned).toBe(false);
  });

  it("throws when given a value that cannot be serialized (circular reference)", () => {
    const circular = {};
    circular.self = circular;

    expect(() => clone(circular)).toThrow();
  });
});

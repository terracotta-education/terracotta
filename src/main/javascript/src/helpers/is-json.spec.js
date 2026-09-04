import { describe, it, expect } from "vitest";

import { isJson } from "./is-json";

describe("isJson", () => {
  it("returns true for a valid JSON object string", () => {
    expect(isJson('{"a":1}')).toBe(true);
  });

  it("returns true for a valid JSON array string", () => {
    expect(isJson("[1,2,3]")).toBe(true);
  });

  it("returns true for a valid JSON primitive string", () => {
    expect(isJson("123")).toBe(true);
    expect(isJson('"hello"')).toBe(true);
    expect(isJson("true")).toBe(true);
    expect(isJson("null")).toBe(true);
  });

  it("returns false for an invalid JSON string", () => {
    expect(isJson("{invalid json}")).toBe(false);
  });

  it("returns false for a plain, non-JSON string", () => {
    expect(isJson("hello world")).toBe(false);
  });

  it("returns false for an empty string", () => {
    expect(isJson("")).toBe(false);
  });

  it("returns false for undefined input", () => {
    expect(isJson(undefined)).toBe(false);
  });
});

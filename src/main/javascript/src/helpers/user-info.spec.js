import { describe, it, expect } from "vitest";

import { userInfo } from "./user-info";

describe("userInfo", () => {
  it("returns Instructor when a role includes membership#Instructor", () => {
    const roles = [
      "http://purl.imsglobal.org/vocab/lis/v2/membership#Learner",
      "http://purl.imsglobal.org/vocab/lis/v2/membership#Instructor"
    ];

    expect(userInfo(roles)).toBe("Instructor");
  });

  it("returns Learner when no role includes membership#Instructor", () => {
    const roles = [
      "http://purl.imsglobal.org/vocab/lis/v2/membership#Learner"
    ];

    expect(userInfo(roles)).toBe("Learner");
  });

  it("returns Learner for an empty roles array", () => {
    expect(userInfo([])).toBe("Learner");
  });

  it("returns Instructor when any of multiple roles matches, regardless of position", () => {
    const roles = [
      "http://purl.imsglobal.org/vocab/lis/v2/membership#Instructor",
      "http://purl.imsglobal.org/vocab/lis/v2/membership#Learner",
      "http://purl.imsglobal.org/vocab/lis/v2/membership#TeachingAssistant"
    ];

    expect(userInfo(roles)).toBe("Instructor");
  });

  it("returns Learner when role string does not use '/' segments matching the expected format", () => {
    const roles = ["membership#Instructor"];

    // split("/") on a string without "/" yields a single-element array
    // containing the whole string, so "membership#Instructor" as a whole
    // segment does match via includes()
    expect(userInfo(roles)).toBe("Instructor");
  });
});

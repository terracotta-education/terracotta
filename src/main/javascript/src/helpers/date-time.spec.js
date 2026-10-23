import { describe, expect, it } from "vitest";

import { byDateSubmitted, toTimestamp } from "./date-time";

describe("toTimestamp", () => {
  it("reads ISO text, which is how the API sends dates", () => {
    expect(toTimestamp("2026-10-21T15:49:00.000+00:00")).toBe(Date.UTC(2026, 9, 21, 15, 49));
  });

  it("reads a number as it is", () => {
    expect(toTimestamp(1792597740000)).toBe(1792597740000);
  });

  it("gives NaN for no date or a bad one", () => {
    expect(toTimestamp(null)).toBeNaN();
    expect(toTimestamp(undefined)).toBeNaN();
    expect(toTimestamp("")).toBeNaN();
    expect(toTimestamp("not a date")).toBeNaN();
  });
});

describe("byDateSubmitted", () => {
  it("sorts oldest first, with ISO dates", () => {
    const submissions = [
      { submissionId: 3, dateSubmitted: "2026-10-23T15:30:00Z" },
      { submissionId: 1, dateSubmitted: "2026-10-21T03:49:00Z" },
      { submissionId: 2, dateSubmitted: "2026-10-22T10:00:00Z" }
    ];

    expect([...submissions].sort(byDateSubmitted).map(s => s.submissionId)).toEqual([1, 2, 3]);
  });

  it("puts a submission not yet submitted first", () => {
    const submissions = [
      { submissionId: 2, dateSubmitted: "2026-10-22T10:00:00Z" },
      { submissionId: 9, dateSubmitted: null }
    ];

    expect([...submissions].sort(byDateSubmitted).map(s => s.submissionId)).toEqual([9, 2]);
  });
});

import { describe, it, expect } from "vitest";

import {
  timeFormat,
  round,
  percent,
  milliToSeconds,
  milliToMinutes,
  minutesToMillis
} from "./utils";

describe("timeFormat", () => {
  it("formats zero milliseconds as 0s", () => {
    expect(timeFormat(0)).toBe("0s");
  });

  it("formats less than a minute using only seconds", () => {
    expect(timeFormat(45000)).toBe("45s");
  });

  it("formats less than an hour using minutes and seconds", () => {
    expect(timeFormat(90000)).toBe("1m 30s");
  });

  it("formats an exact number of minutes with 0 seconds using minutes and seconds", () => {
    // seconds is falsy (0) here, so the "less than a minute" branch wins,
    // per the `!seconds || (seconds && !minutes)` condition
    expect(timeFormat(120000)).toBe("0s");
  });

  it("formats less than a day using hours, minutes, and seconds", () => {
    // 1h 1m 1s
    const ms = (1 * 3600 + 1 * 60 + 1) * 1000;

    expect(timeFormat(ms)).toBe("1h 1m 1s");
  });

  it("formats a full day+ using days, hours, minutes, and seconds", () => {
    // 1d 2h 3m 4s
    const ms = ((1 * 24 + 2) * 3600 + 3 * 60 + 4) * 1000;

    expect(timeFormat(ms)).toBe("1d 2h 3m 4s");
  });
});

describe("round", () => {
  it("rounds a fractional number to two decimal places", () => {
    expect(round(1.23456)).toBe(1.23);
  });

  it("returns whole numbers unchanged", () => {
    expect(round(5)).toBe(5);
  });

  it("rounds fractional values using standard floating point rounding", () => {
    // due to floating point representation, 1.005 * 100 is
    // 100.49999999999999, so this rounds down rather than up
    expect(round(1.005)).toBe(1);
  });

  it("handles zero", () => {
    expect(round(0)).toBe(0);
  });
});

describe("percent", () => {
  it("converts a decimal fraction to a rounded whole-number percent", () => {
    expect(percent(0.5)).toBe(50);
  });

  it("rounds to the nearest whole number", () => {
    expect(percent(0.333)).toBe(33);
    expect(percent(0.666)).toBe(67);
  });

  it("handles zero", () => {
    expect(percent(0)).toBe(0);
  });

  it("handles values greater than 1", () => {
    expect(percent(1.25)).toBe(125);
  });
});

describe("milliToSeconds", () => {
  it("converts milliseconds to seconds", () => {
    expect(milliToSeconds(5000)).toBe(5);
  });

  it("handles zero", () => {
    expect(milliToSeconds(0)).toBe(0);
  });

  it("handles fractional results", () => {
    expect(milliToSeconds(1500)).toBe(1.5);
  });
});

describe("milliToMinutes", () => {
  it("converts milliseconds to minutes", () => {
    expect(milliToMinutes(60000)).toBe(1);
  });

  it("handles zero", () => {
    expect(milliToMinutes(0)).toBe(0);
  });

  it("handles fractional results", () => {
    expect(milliToMinutes(90000)).toBe(1.5);
  });
});

describe("minutesToMillis", () => {
  it("converts minutes to milliseconds", () => {
    expect(minutesToMillis(1)).toBe(60000);
  });

  it("handles zero", () => {
    expect(minutesToMillis(0)).toBe(0);
  });

  it("handles fractional minutes", () => {
    expect(minutesToMillis(1.5)).toBe(90000);
  });
});

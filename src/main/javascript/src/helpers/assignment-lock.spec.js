import { describe, expect, it } from "vitest";

import dayjs from "@/plugins/dayjs";
import { lockedAssignmentMessage } from "./assignment-lock";

describe("lockedAssignmentMessage", () => {
  const iso = "2026-10-01T04:59:59Z";
  const local = dayjs(iso).format("MMMM D [at] h:mm A");

  it("formats a locked-at message in the local time zone", () => {
    expect(lockedAssignmentMessage(`Assignment was locked at ${iso}`))
      .toBe(`This assignment was locked on ${local}.`);
  });

  it("formats a locked-until message in the local time zone", () => {
    expect(lockedAssignmentMessage(`Assignment is locked until ${iso}`))
      .toBe(`This assignment is locked until ${local}.`);
  });

  it("returns null for any other message, or a date it can't read", () => {
    expect(lockedAssignmentMessage("Error 150: Max submission attempts already reached")).toBeNull();
    expect(lockedAssignmentMessage("Assignment was locked at someday")).toBeNull();
    expect(lockedAssignmentMessage(undefined)).toBeNull();
  });
});

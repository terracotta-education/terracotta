import { describe, it, expect } from "vitest";

import { attachment, message, editableMessageStatuses } from "./status";

describe("messaging status constants", () => {
  it("exposes the expected attachment statuses", () => {
    expect(attachment).toEqual({
      created: "CREATED",
      deleted: "DELETED",
      error: "ERROR",
      uploaded: "UPLOADED"
    });
  });

  it("exposes the expected message statuses", () => {
    expect(message).toEqual({
      canceled: "CANCELED",
      copied: "COPIED",
      created: "CREATED",
      deleted: "DELETED",
      disabled: "DISABLED",
      edited: "EDITED",
      error: "ERROR",
      incomplete: "INCOMPLETE",
      processing: "PROCESSING",
      published: "PUBLISHED",
      queued: "QUEUED",
      ready: "READY",
      sent: "SENT",
      unpublished: "UNPUBLISHED"
    });
  });

  it("builds editableMessageStatuses from the corresponding message status values", () => {
    expect(editableMessageStatuses).toEqual([
      message.copied,
      message.created,
      message.disabled,
      message.edited,
      message.incomplete,
      message.published,
      message.ready,
      message.unpublished
    ]);
  });

  it("excludes non-editable statuses such as canceled, deleted, error, processing, queued, and sent", () => {
    expect(editableMessageStatuses).not.toContain(message.canceled);
    expect(editableMessageStatuses).not.toContain(message.deleted);
    expect(editableMessageStatuses).not.toContain(message.error);
    expect(editableMessageStatuses).not.toContain(message.processing);
    expect(editableMessageStatuses).not.toContain(message.queued);
    expect(editableMessageStatuses).not.toContain(message.sent);
  });

  it("has exactly 8 editable statuses", () => {
    expect(editableMessageStatuses).toHaveLength(8);
  });
});

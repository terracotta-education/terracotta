import { beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";

vi.mock("@/services", () => ({
  messageContentAttachmentService: {
    getAll: vi.fn()
  }
}));

import { messageContentAttachmentService } from "@/services";
import { attachment } from "./attachment.module";

describe("attachment store", () => {
  let store;

  beforeEach(() => {
    setActivePinia(createPinia());
    store = attachment();
    vi.clearAllMocks();
    vi.spyOn(console, "error").mockImplementation(() => {});
  });

  it("starts with no attachments", () => {
    expect(store.attachments).toEqual([]);
    expect(store.attachmentCount).toBe(0);
    expect(store.hasAttachments).toBe(false);
  });

  describe("getAll", () => {
    it("sets attachments from an array response", async () => {
      messageContentAttachmentService.getAll.mockResolvedValue([
        { id: 1 },
        { id: 2 }
      ]);

      const result = await store.getAll([1, 2, 3, 4, 5]);

      expect(messageContentAttachmentService.getAll).toHaveBeenCalledWith(
        1,
        2,
        3,
        4,
        5
      );
      expect(store.attachments).toEqual([{ id: 1 }, { id: 2 }]);
      expect(store.attachmentCount).toBe(2);
      expect(store.hasAttachments).toBe(true);
      expect(result).toEqual([{ id: 1 }, { id: 2 }]);
    });

    it("defaults to [] when the response is not an array", async () => {
      messageContentAttachmentService.getAll.mockResolvedValue({
        status: 404
      });

      const result = await store.getAll([1, 2, 3, 4, 5]);

      expect(store.attachments).toEqual([]);
      expect(result).toEqual([]);
    });

    it("resets attachments to [] and logs on error", async () => {
      store.messageContentAttachments = [{ id: 1 }];
      messageContentAttachmentService.getAll.mockRejectedValue(
        new Error("boom")
      );

      const result = await store.getAll([1, 2, 3, 4, 5]);

      expect(store.attachments).toEqual([]);
      expect(result).toEqual([]);
      expect(console.error).toHaveBeenCalled();
    });
  });

  describe("resetAttachments", () => {
    it("clears the attachments list", () => {
      store.messageContentAttachments = [{ id: 1 }];
      store.resetAttachments();

      expect(store.attachments).toEqual([]);
    });
  });
});

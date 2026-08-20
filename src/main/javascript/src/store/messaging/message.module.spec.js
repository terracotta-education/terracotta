import { beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";

vi.mock("@/services", () => ({
  messageService: {
    update: vi.fn(),
    fetchPreview: vi.fn(),
    sendTest: vi.fn(),
    getAssignments: vi.fn(),
    updatePlaceholders: vi.fn(),
    uploadPipedText: vi.fn()
  }
}));

import { messageService } from "@/services";
import { message } from "./message.module";

describe("message store", () => {
  let store;

  beforeEach(() => {
    setActivePinia(createPinia());
    store = message();
    vi.clearAllMocks();
  });

  it("starts with empty/default state", () => {
    expect(store.hasAssignments).toBe(false);
    expect(store.hasPipedText).toBe(false);
    expect(store.hasMessage).toBe(false);
    expect(store.isLoading).toBe(false);
    expect(store.preview).toBeNull();
  });

  it("setPreview stores the preview value", () => {
    store.setPreview({ html: "<p>hi</p>" });

    expect(store.preview).toEqual({ html: "<p>hi</p>" });
  });

  it("setPipedText stores the pipedText value directly", () => {
    store.setPipedText({ items: [{ id: "1" }] });

    expect(store.pipedText).toEqual({ items: [{ id: "1" }] });
    expect(store.hasPipedText).toBe(true);
  });

  describe("update", () => {
    it("returns raw service response on success", async () => {
      messageService.update.mockResolvedValue({ status: 200 });

      const result = await store.update(["a", "b"]);

      expect(messageService.update).toHaveBeenCalledWith("a", "b");
      expect(result).toEqual({ status: 200 });
    });

    it("returns null on error", async () => {
      messageService.update.mockRejectedValue(new Error("fail"));

      const result = await store.update(["a"]);

      expect(result).toBeNull();
    });
  });

  describe("fetchPreview", () => {
    it("stores response as preview on success", async () => {
      const response = { html: "<p>preview</p>" };
      messageService.fetchPreview.mockResolvedValue(response);

      const result = await store.fetchPreview(["a"]);

      expect(result).toEqual(response);
      expect(store.preview).toEqual(response);
    });

    it("clears preview and returns null on error", async () => {
      store.setPreview({ html: "old" });
      messageService.fetchPreview.mockRejectedValue(new Error("fail"));

      const result = await store.fetchPreview(["a"]);

      expect(result).toBeNull();
      expect(store.preview).toBeNull();
    });
  });

  describe("sendTest", () => {
    it("returns raw response on success", async () => {
      messageService.sendTest.mockResolvedValue({ status: 200 });

      const result = await store.sendTest(["a"]);

      expect(result).toEqual({ status: 200 });
    });

    it("returns null on error", async () => {
      messageService.sendTest.mockRejectedValue(new Error("fail"));

      const result = await store.sendTest(["a"]);

      expect(result).toBeNull();
    });
  });

  describe("getAssignments", () => {
    it("sets assignments and toggles isLoading on success", async () => {
      const data = [{ id: 1 }];
      messageService.getAssignments.mockResolvedValue(data);

      const result = await store.getAssignments([1, 2, "c"]);

      expect(messageService.getAssignments).toHaveBeenCalledWith(1, 2, "c");
      expect(result).toEqual(data);
      expect(store.assignments).toEqual(data);
      expect(store.hasAssignments).toBe(true);
      expect(store.isLoading).toBe(false);
    });

    it("falls back to [] when response is not an array", async () => {
      messageService.getAssignments.mockResolvedValue(null);

      const result = await store.getAssignments([1, 2, "c"]);

      expect(result).toEqual([]);
      expect(store.assignments).toEqual([]);
    });

    it("clears assignments, returns [] and resets isLoading on error", async () => {
      messageService.getAssignments.mockRejectedValue(new Error("fail"));

      const result = await store.getAssignments([1, 2, "c"]);

      expect(result).toEqual([]);
      expect(store.assignments).toEqual([]);
      expect(store.isLoading).toBe(false);
    });
  });

  describe("updatePlaceholders", () => {
    it("returns raw response on success", async () => {
      messageService.updatePlaceholders.mockResolvedValue({ status: 200 });

      const result = await store.updatePlaceholders(["a"]);

      expect(result).toEqual({ status: 200 });
    });

    it("returns null on error", async () => {
      messageService.updatePlaceholders.mockRejectedValue(new Error("fail"));

      const result = await store.updatePlaceholders(["a"]);

      expect(result).toBeNull();
    });
  });

  describe("uploadPipedText", () => {
    it("sets message and normalizes pipedText items, assigning ids when missing", async () => {
      const response = {
        content: {
          pipedText: {
            items: [{ text: "no id here" }, { id: "existing", text: "has id" }]
          }
        }
      };
      messageService.uploadPipedText.mockResolvedValue(response);

      const result = await store.uploadPipedText(["a"]);

      expect(result).toEqual(response);
      expect(store.message).toEqual(response);
      expect(store.pipedText.items).toHaveLength(2);
      expect(store.pipedText.items[0].id).toEqual(expect.any(String));
      expect(store.pipedText.items[1].id).toBe("existing");
      expect(store.hasPipedText).toBe(true);
      expect(store.hasMessage).toBe(true);
    });

    it("normalizes to empty items array when pipedText.items is not an array", async () => {
      const response = { content: { pipedText: { items: "not-an-array" } } };
      messageService.uploadPipedText.mockResolvedValue(response);

      await store.uploadPipedText(["a"]);

      expect(store.pipedText.items).toEqual([]);
    });

    it("sets pipedText to null when response has no pipedText content", async () => {
      const response = { content: {} };
      messageService.uploadPipedText.mockResolvedValue(response);

      await store.uploadPipedText(["a"]);

      expect(store.pipedText).toBeNull();
      expect(store.message).toEqual(response);
    });

    it("clears pipedText and message when response is falsy", async () => {
      messageService.uploadPipedText.mockResolvedValue(null);

      const result = await store.uploadPipedText(["a"]);

      expect(result).toBeNull();
      expect(store.pipedText).toBeNull();
      expect(store.message).toBeNull();
    });

    it("clears pipedText and message and returns null on error", async () => {
      store.setPipedText({ items: [] });
      store.message = { id: 1 };
      messageService.uploadPipedText.mockRejectedValue(new Error("fail"));

      const result = await store.uploadPipedText(["a"]);

      expect(result).toBeNull();
      expect(store.pipedText).toBeNull();
      expect(store.message).toBeNull();
    });
  });

  it("reset clears all state", () => {
    store.assignments = [{ id: 1 }];
    store.isLoading = true;
    store.preview = { html: "x" };
    store.pipedText = { items: [] };
    store.message = { id: 1 };

    store.reset();

    expect(store.assignments).toEqual([]);
    expect(store.isLoading).toBe(false);
    expect(store.preview).toBeNull();
    expect(store.pipedText).toBeNull();
    expect(store.message).toBeNull();
  });
});

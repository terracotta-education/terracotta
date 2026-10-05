import { beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";

import { api } from "@/store/api.module";
import { messageContentAttachmentService } from "./attachment.service";

function mockResponse({ ok = true, status = 200, text = "" } = {}) {
  return {
    ok,
    status,
    text: () => (text instanceof Error ? Promise.reject(text) : Promise.resolve(text))
  };
}

describe("messageContentAttachmentService", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    api().aud = "https://example.com";
    api().apiToken = "test-token";
    vi.stubGlobal("fetch", vi.fn());
    vi.clearAllMocks();
  });

  it("getAll fetches attachments for the given content", async () => {
    fetch.mockResolvedValue(mockResponse({ text: JSON.stringify([{ fileId: 1 }]) }));

    const result = await messageContentAttachmentService.getAll(1, 2, 3, 4, 5);

    expect(fetch).toHaveBeenCalledWith(
      "https://example.com/api/experiments/1/exposures/2/messaging/container/3/message/4/content/5/file",
      expect.objectContaining({ method: "GET" })
    );
    expect(result).toEqual([{ fileId: 1 }]);
  });

  describe("handleResponse branches", () => {
    it("returns [] on a 204", async () => {
      fetch.mockResolvedValue(mockResponse({ status: 204, text: "" }));

      const result = await messageContentAttachmentService.getAll(1, 2, 3, 4, 5);

      expect(result).toEqual([]);
    });

    it("logs a server error and returns an error payload on a 500", async () => {
      const consoleSpy = vi.spyOn(console, "error").mockImplementation(() => {});
      fetch.mockResolvedValue(
        mockResponse({ ok: false, status: 500, text: JSON.stringify({ error: "boom" }) })
      );

      const result = await messageContentAttachmentService.getAll(1, 2, 3, 4, 5);

      expect(result).toEqual({ status: 500, error: { error: "boom" } });
      expect(consoleSpy).toHaveBeenCalled();
      consoleSpy.mockRestore();
    });

    it("warns and returns an error payload on a 404", async () => {
      const warnSpy = vi.spyOn(console, "warn").mockImplementation(() => {});
      fetch.mockResolvedValue(mockResponse({ ok: false, status: 404, text: "" }));

      const result = await messageContentAttachmentService.getAll(1, 2, 3, 4, 5);

      expect(result.status).toBe(404);
      expect(warnSpy).toHaveBeenCalled();
      warnSpy.mockRestore();
    });

    it("logs and returns an error payload when reading the response fails", async () => {
      const consoleSpy = vi.spyOn(console, "error").mockImplementation(() => {});
      fetch.mockResolvedValue(mockResponse({ text: new Error("bad text") }));

      const result = await messageContentAttachmentService.getAll(1, 2, 3, 4, 5);

      expect(result.error).toBeInstanceOf(Error);
      expect(consoleSpy).toHaveBeenCalled();
      consoleSpy.mockRestore();
    });
  });
});

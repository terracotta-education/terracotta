import { beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";

vi.mock("axios");

import axios from "axios";
import { api } from "@/store/api.module";
import { messageService } from "./message.service";

function mockResponse({ ok = true, status = 200, text = "" } = {}) {
  return {
    ok,
    status,
    text: () => (text instanceof Error ? Promise.reject(text) : Promise.resolve(text))
  };
}

describe("messageService", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    api().aud = "https://example.com";
    api().apiToken = "test-token";
    vi.stubGlobal("fetch", vi.fn());
    vi.clearAllMocks();
  });

  it("update PUTs the message payload", async () => {
    fetch.mockResolvedValue(mockResponse({ text: JSON.stringify({ messageId: 1 }) }));

    const result = await messageService.update(1, 2, 3, 4, { subject: "Hi" });

    expect(fetch).toHaveBeenCalledWith(
      "https://example.com/api/experiments/1/exposures/2/messaging/container/3/message/4",
      expect.objectContaining({ method: "PUT", body: JSON.stringify({ subject: "Hi" }) })
    );
    expect(result).toEqual({ messageId: 1 });
  });

  it("fetchPreview POSTs preview parameters", async () => {
    fetch.mockResolvedValue(mockResponse({ text: JSON.stringify({ preview: "<p>hi</p>" }) }));

    const result = await messageService.fetchPreview(1, 2, 3, 4, { participantId: 9 });

    expect(fetch).toHaveBeenCalledWith(
      "https://example.com/api/experiments/1/exposures/2/messaging/container/3/message/4/preview",
      expect.objectContaining({ method: "POST", body: JSON.stringify({ participantId: 9 }) })
    );
    expect(result).toEqual({ preview: "<p>hi</p>" });
  });

  it("sendTest POSTs the test-send payload", async () => {
    fetch.mockResolvedValue(mockResponse({ status: 204, text: "" }));

    const result = await messageService.sendTest(1, 2, 3, 4, { to: "a@b.com" });

    expect(fetch).toHaveBeenCalledWith(
      "https://example.com/api/experiments/1/exposures/2/messaging/container/3/message/4/sendtest",
      expect.objectContaining({ method: "POST", body: JSON.stringify({ to: "a@b.com" }) })
    );
    expect(result).toEqual([]);
  });

  it("getAssignments fetches assignment options", async () => {
    fetch.mockResolvedValue(mockResponse({ text: JSON.stringify([{ assignmentId: 1 }]) }));

    const result = await messageService.getAssignments(1, 2, 3);

    expect(fetch).toHaveBeenCalledWith(
      "https://example.com/api/experiments/1/exposures/2/messaging/container/3/message/assignments",
      expect.objectContaining({ method: "GET" })
    );
    expect(result).toEqual([{ assignmentId: 1 }]);
  });

  it("updatePlaceholders POSTs the piped-content placeholder update", async () => {
    fetch.mockResolvedValue(mockResponse({ text: JSON.stringify({ contentId: 5 }) }));

    const result = await messageService.updatePlaceholders(1, 2, 3, 4, 5, { placeholders: {} });

    expect(fetch).toHaveBeenCalledWith(
      "https://example.com/api/experiments/1/exposures/2/messaging/container/3/message/4/content/5/piped/updatePlaceholders",
      expect.objectContaining({ method: "POST", body: JSON.stringify({ placeholders: {} }) })
    );
    expect(result).toEqual({ contentId: 5 });
  });

  describe("uploadPipedText", () => {
    it("uploads the piped text file and returns the response data", async () => {
      axios.post.mockResolvedValue({ data: { content: "parsed text" } });

      const result = await messageService.uploadPipedText(1, 2, 3, 4, 5, new File(["hi"], "text.txt"));

      expect(axios.post).toHaveBeenCalledWith(
        "https://example.com/api/experiments/1/exposures/2/messaging/container/3/message/4/content/5/piped/file",
        expect.any(FormData),
        expect.objectContaining({ headers: expect.any(Object) })
      );
      expect(result).toEqual({ content: "parsed text" });
    });

    it("returns a validation error payload when axios rejects with a response", async () => {
      axios.post.mockRejectedValue({ response: { status: 400 } });

      const result = await messageService.uploadPipedText(1, 2, 3, 4, 5, new File(["hi"], "text.txt"));

      expect(result).toEqual({
        content: null,
        validationErrors: ["An unspecified error occurred"]
      });
    });

    it("rethrows when axios rejects without a response", async () => {
      axios.post.mockRejectedValue(new Error("network down"));

      await expect(
        messageService.uploadPipedText(1, 2, 3, 4, 5, new File(["hi"], "text.txt"))
      ).rejects.toThrow("network down");
    });
  });

  describe("handleResponse branches", () => {
    it("logs a server error and returns an error payload on a 500", async () => {
      const consoleSpy = vi.spyOn(console, "error").mockImplementation(() => {});
      fetch.mockResolvedValue(mockResponse({ ok: false, status: 500, text: JSON.stringify({ error: "boom" }) }));

      const result = await messageService.getAssignments(1, 2, 3);

      expect(result).toEqual({ status: 500, error: { error: "boom" } });
      expect(consoleSpy).toHaveBeenCalled();
      consoleSpy.mockRestore();
    });

    it("warns and returns an error payload on a 404", async () => {
      const warnSpy = vi.spyOn(console, "warn").mockImplementation(() => {});
      fetch.mockResolvedValue(mockResponse({ ok: false, status: 404, text: "" }));

      const result = await messageService.getAssignments(1, 2, 3);

      expect(result.status).toBe(404);
      expect(warnSpy).toHaveBeenCalled();
      warnSpy.mockRestore();
    });

    it("logs and returns an error payload when reading the response fails", async () => {
      const consoleSpy = vi.spyOn(console, "error").mockImplementation(() => {});
      fetch.mockResolvedValue(mockResponse({ text: new Error("bad text") }));

      const result = await messageService.getAssignments(1, 2, 3);

      expect(result.error).toBeInstanceOf(Error);
      expect(consoleSpy).toHaveBeenCalled();
      consoleSpy.mockRestore();
    });
  });
});

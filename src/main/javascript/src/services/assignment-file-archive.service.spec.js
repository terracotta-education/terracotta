import { beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";

import { api } from "@/store/api.module";
import { assignmentFileArchiveService } from "./assignment-file-archive.service";

function mockResponse({ ok = true, status = 200, text = "" } = {}) {
  return {
    ok,
    status,
    text: () => (text instanceof Error ? Promise.reject(text) : Promise.resolve(text)),
    blob: () => Promise.resolve(new Blob(["file-bytes"]))
  };
}

describe("assignmentFileArchiveService", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    api().aud = "https://example.com";
    api().apiToken = "test-token";
    vi.stubGlobal("fetch", vi.fn());
    vi.clearAllMocks();
  });

  it("prepare requests file preparation for an assignment", async () => {
    fetch.mockResolvedValue(mockResponse({ text: JSON.stringify({ id: 1 }) }));

    const result = await assignmentFileArchiveService.prepare(1, 2, 3);

    expect(fetch).toHaveBeenCalledWith(
      "https://example.com/api/experiments/1/exposures/2/assignments/3/files",
      expect.objectContaining({ method: "GET" })
    );
    expect(result).toEqual({ id: 1 });
  });

  it("poll requests archive status with the createNewOnOutdated flag", async () => {
    fetch.mockResolvedValue(mockResponse({ text: JSON.stringify({ id: 1, status: "READY" }) }));

    const result = await assignmentFileArchiveService.poll(1, 2, 3, true);

    expect(fetch).toHaveBeenCalledWith(
      "https://example.com/api/experiments/1/exposures/2/assignments/3/files/poll?createNewOnOutdated=true",
      expect.objectContaining({ method: "GET" })
    );
    expect(result).toEqual({ id: 1, status: "READY" });
  });

  it("acknowledgeError PUTs an acknowledgement for a failed file", async () => {
    fetch.mockResolvedValue(mockResponse({ status: 204, text: "" }));

    const result = await assignmentFileArchiveService.acknowledgeError(1, 2, 3, 9);

    expect(fetch).toHaveBeenCalledWith(
      "https://example.com/api/experiments/1/exposures/2/assignments/3/files/9/error/acknowledge",
      expect.objectContaining({ method: "PUT" })
    );
    expect(result).toEqual([]);
  });

  describe("retrieve", () => {
    beforeEach(() => {
      window.URL.createObjectURL = vi.fn(() => "blob:url");
      window.URL.revokeObjectURL = vi.fn();
      vi.spyOn(document, "createElement").mockReturnValue({ click: vi.fn(), remove: vi.fn() });
      vi.spyOn(document.body, "appendChild").mockImplementation(() => {});
    });

    it("downloads the retrieved file when the response is a 200", async () => {
      fetch.mockResolvedValue(mockResponse({ status: 200 }));

      const result = await assignmentFileArchiveService.retrieve(1, 2, 3, {
        id: 9,
        mimeType: "application/zip",
        fileName: "archive.zip"
      });

      expect(fetch).toHaveBeenCalledWith(
        "https://example.com/api/experiments/1/exposures/2/assignments/3/files/9/retrieve",
        expect.objectContaining({ method: "GET" })
      );
      expect(window.URL.createObjectURL).toHaveBeenCalled();
      expect(window.URL.revokeObjectURL).toHaveBeenCalledWith("blob:url");
      expect(result).toEqual({ status: 200 });
    });

    it("delegates to handleResponse when the retrieval fails", async () => {
      fetch.mockResolvedValue(mockResponse({ ok: false, status: 404, text: "" }));

      const result = await assignmentFileArchiveService.retrieve(1, 2, 3, {
        id: 9,
        mimeType: "application/zip",
        fileName: "archive.zip"
      });

      expect(result).toEqual({ status: 404, error: expect.anything() });
      expect(window.URL.createObjectURL).not.toHaveBeenCalled();
    });
  });

  describe("handleResponse branches", () => {
    it("returns the parsed body on a 409", async () => {
      fetch.mockResolvedValue(
        mockResponse({ ok: false, status: 409, text: JSON.stringify({ message: "conflict" }) })
      );

      const result = await assignmentFileArchiveService.prepare(1, 2, 3);

      expect(result).toEqual({ message: "conflict" });
    });

    it("falls back to a status object on a 409 with no body", async () => {
      fetch.mockResolvedValue(mockResponse({ ok: false, status: 409, text: "" }));

      const result = await assignmentFileArchiveService.prepare(1, 2, 3);

      expect(result).toEqual({ status: 409 });
    });

    it("logs a server error and returns an error payload on a 500", async () => {
      const consoleSpy = vi.spyOn(console, "error").mockImplementation(() => {});
      fetch.mockResolvedValue(mockResponse({ ok: false, status: 500, text: JSON.stringify({ error: "boom" }) }));

      const result = await assignmentFileArchiveService.prepare(1, 2, 3);

      expect(result).toEqual({ status: 500, error: { error: "boom" } });
      expect(consoleSpy).toHaveBeenCalled();
      consoleSpy.mockRestore();
    });

    it("warns and returns an error payload on a 404", async () => {
      const warnSpy = vi.spyOn(console, "warn").mockImplementation(() => {});
      fetch.mockResolvedValue(mockResponse({ ok: false, status: 404, text: "" }));

      const result = await assignmentFileArchiveService.prepare(1, 2, 3);

      expect(result.status).toBe(404);
      expect(warnSpy).toHaveBeenCalled();
      warnSpy.mockRestore();
    });

    it("logs and returns an error payload when reading the response fails", async () => {
      const consoleSpy = vi.spyOn(console, "error").mockImplementation(() => {});
      fetch.mockResolvedValue(mockResponse({ text: new Error("bad text") }));

      const result = await assignmentFileArchiveService.prepare(1, 2, 3);

      expect(result.error).toBeInstanceOf(Error);
      expect(consoleSpy).toHaveBeenCalled();
      consoleSpy.mockRestore();
    });
  });
});

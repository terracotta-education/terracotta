import { beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";

vi.mock("axios");

import axios from "axios";
import { api } from "@/store/api.module";
import { experimentService } from "./experiment.service";

function mockResponse({ ok = true, status = 200, text = "", headers } = {}) {
  return {
    ok,
    status,
    text: () => (text instanceof Error ? Promise.reject(text) : Promise.resolve(text)),
    headers: headers || { get: () => null },
    blob: () => Promise.resolve(new Blob(["zip-bytes"]))
  };
}

describe("experimentService", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    api().aud = "https://example.com";
    api().apiToken = "test-token";
    vi.stubGlobal("fetch", vi.fn());
    vi.clearAllMocks();
  });

  it("getAll fetches experiments and returns the parsed list", async () => {
    fetch.mockResolvedValue(mockResponse({ text: JSON.stringify([{ experimentId: 1 }]) }));

    const result = await experimentService.getAll();

    expect(fetch).toHaveBeenCalledWith(
      "https://example.com/api/experiments",
      expect.objectContaining({ method: "GET" })
    );
    expect(result).toEqual({ data: [{ experimentId: 1 }], status: 200 });
  });

  it("getById requests the experiment with conditions", async () => {
    fetch.mockResolvedValue(mockResponse({ text: JSON.stringify({ experimentId: 5 }) }));

    const result = await experimentService.getById(5);

    expect(fetch).toHaveBeenCalledWith(
      "https://example.com/api/experiments/5?conditions=true",
      expect.objectContaining({ method: "GET" })
    );
    expect(result).toEqual({ data: { experimentId: 5 }, status: 200 });
  });

  it("create POSTs to create an experiment", async () => {
    fetch.mockResolvedValue(mockResponse({ text: JSON.stringify({ experimentId: 1 }) }));

    const result = await experimentService.create();

    expect(fetch).toHaveBeenCalledWith(
      "https://example.com/api/experiments",
      expect.objectContaining({ method: "POST" })
    );
    expect(result).toEqual({ data: { experimentId: 1 }, status: 200 });
  });

  it("update PUTs the updated experiment body", async () => {
    fetch.mockResolvedValue(mockResponse({ text: JSON.stringify({ experimentId: 1, name: "Updated" }) }));

    const result = await experimentService.update({ experimentId: 1, name: "Updated" });

    expect(fetch).toHaveBeenCalledWith(
      "https://example.com/api/experiments/1",
      expect.objectContaining({
        method: "PUT",
        body: JSON.stringify({ experimentId: 1, name: "Updated" })
      })
    );
    expect(result).toEqual({ data: { experimentId: 1, name: "Updated" }, status: 200 });
  });

  it("delete DELETEs the experiment and handles a 204", async () => {
    fetch.mockResolvedValue(mockResponse({ status: 204, text: "" }));

    const result = await experimentService.delete(1);

    expect(fetch).toHaveBeenCalledWith(
      "https://example.com/api/experiments/1",
      expect.objectContaining({ method: "DELETE" })
    );
    expect(result).toEqual([]);
  });

  it("pollImport polls a single import", async () => {
    fetch.mockResolvedValue(mockResponse({ text: JSON.stringify({ status: "done" }) }));

    const result = await experimentService.pollImport(9);

    expect(fetch).toHaveBeenCalledWith(
      "https://example.com/api/experiments/import/9/poll",
      expect.objectContaining({ method: "GET" })
    );
    expect(result).toEqual({ data: { status: "done" }, status: 200 });
  });

  it("pollImports polls all imports", async () => {
    fetch.mockResolvedValue(mockResponse({ text: JSON.stringify([{ importId: 1 }]) }));

    await experimentService.pollImports();

    expect(fetch).toHaveBeenCalledWith(
      "https://example.com/api/experiments/import/poll",
      expect.objectContaining({ method: "GET" })
    );
  });

  it("acknowledgeImport acknowledges an import with a status query param", async () => {
    fetch.mockResolvedValue(mockResponse({ text: JSON.stringify({ acknowledged: true }) }));

    await experimentService.acknowledgeImport(9, "COMPLETE");

    expect(fetch).toHaveBeenCalledWith(
      "https://example.com/api/experiments/import/9/acknowledge?status=COMPLETE",
      expect.objectContaining({ method: "PUT" })
    );
  });

  describe("export", () => {
    beforeEach(() => {
      window.URL.createObjectURL = vi.fn(() => "blob:url");
      window.URL.revokeObjectURL = vi.fn();
      vi.spyOn(document, "createElement").mockReturnValue({ click: vi.fn(), remove: vi.fn() });
      vi.spyOn(document.body, "appendChild").mockImplementation(() => {});
    });

    it("downloads the exported zip using the content-disposition filename", async () => {
      fetch.mockResolvedValue(
        mockResponse({
          status: 200,
          headers: { get: () => 'attachment; filename="my-export.zip"' }
        })
      );

      const result = await experimentService.export(1);

      expect(fetch).toHaveBeenCalledWith(
        "https://example.com/api/experiments/1/export",
        expect.objectContaining({ method: "GET" })
      );
      expect(window.URL.createObjectURL).toHaveBeenCalled();
      expect(window.URL.revokeObjectURL).toHaveBeenCalledWith("blob:url");
      expect(result).toEqual({ status: 200 });
    });

    it("falls back to a default filename with no content-disposition header", async () => {
      fetch.mockResolvedValue(mockResponse({ status: 200 }));

      const result = await experimentService.export(1);

      expect(result).toEqual({ status: 200 });
    });

    it("delegates to handleResponse when the export request fails", async () => {
      fetch.mockResolvedValue(
        mockResponse({ ok: false, status: 500, text: JSON.stringify({ error: "boom" }) })
      );

      const result = await experimentService.export(1);

      expect(result).toEqual({
        data: { error: "boom" },
        status: 500,
        error: { error: "boom" }
      });
    });
  });

  describe("import", () => {
    it("posts the zip file and returns the response data", async () => {
      axios.post.mockResolvedValue({ data: { importId: 1 } });

      const result = await experimentService.import(new File(["zip"], "test.zip"));

      expect(axios.post).toHaveBeenCalledWith(
        "https://example.com/api/experiments/import",
        expect.any(FormData),
        expect.objectContaining({ headers: expect.any(Object) })
      );
      expect(result).toEqual({ importId: 1 });
    });

    it("returns status/message when axios rejects with a response", async () => {
      axios.post.mockRejectedValue({ response: { status: 400, statusText: "Bad Request" } });

      const result = await experimentService.import(new File(["zip"], "test.zip"));

      expect(result).toEqual({ status: 400, message: "Bad Request" });
    });

    it("rethrows when axios rejects without a response", async () => {
      axios.post.mockRejectedValue(new Error("network down"));

      await expect(
        experimentService.import(new File(["zip"], "test.zip"))
      ).rejects.toThrow("network down");
    });
  });

  describe("handleResponse branches", () => {
    it("returns a message payload on 409", async () => {
      fetch.mockResolvedValue(
        mockResponse({ ok: false, status: 409, text: JSON.stringify({ message: "dup" }) })
      );

      const result = await experimentService.getAll();

      expect(result).toEqual({ message: { message: "dup" }, status: 409 });
    });

    it("returns the raw response when there is no body on success", async () => {
      const response = mockResponse({ status: 200, text: "" });
      fetch.mockResolvedValue(response);

      const result = await experimentService.getAll();

      expect(result).toBe(response);
    });

    it("logs and returns an error payload when reading the response fails", async () => {
      const consoleSpy = vi.spyOn(console, "error").mockImplementation(() => {});
      fetch.mockResolvedValue(mockResponse({ text: new Error("bad text") }));

      const result = await experimentService.getAll();

      expect(result.error).toBeInstanceOf(Error);
      expect(consoleSpy).toHaveBeenCalled();
      consoleSpy.mockRestore();
    });
  });
});

import { beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";

import { experimentDataExportService } from "./experiment-data-export.service";
import { api } from "@/store/api.module";

function mockResponse({ status = 200, ok = true, text = "", blob } = {}) {
  return {
    status,
    ok,
    text: vi.fn().mockResolvedValue(text),
    ...(blob !== undefined ? { blob: vi.fn().mockResolvedValue(blob) } : {})
  };
}

describe("experimentDataExportService", () => {
  let fetchMock;

  beforeEach(() => {
    setActivePinia(createPinia());
    api().aud = "https://example.com";
    api().apiToken = "test-token";

    fetchMock = vi.fn();
    vi.stubGlobal("fetch", fetchMock);

    vi.spyOn(console, "error").mockImplementation(() => {});
    vi.spyOn(console, "warn").mockImplementation(() => {});
  });

  describe("prepare", () => {
    it("issues a GET and returns the parsed data on success", async () => {
      fetchMock.mockResolvedValueOnce(
        mockResponse({ status: 200, ok: true, text: JSON.stringify({ id: 1 }) })
      );

      const result = await experimentDataExportService.prepare(5);

      expect(fetchMock).toHaveBeenCalledWith(
        "https://example.com/api/experiments/5/export/data",
        {
          method: "GET",
          headers: {
            Authorization: "Bearer test-token",
            "Content-Type": "application/json"
          }
        }
      );
      expect(result).toEqual({ id: 1 });
    });
  });

  describe("poll", () => {
    it("builds the poll query string and returns parsed data", async () => {
      fetchMock.mockResolvedValueOnce(
        mockResponse({ status: 200, ok: true, text: JSON.stringify({ status: "IN_PROGRESS" }) })
      );

      const result = await experimentDataExportService.poll(5, true);

      expect(fetchMock).toHaveBeenCalledWith(
        "https://example.com/api/experiments/5/export/data/poll?createNewOnOutdated=true",
        expect.objectContaining({ method: "GET" })
      );
      expect(result).toEqual({ status: "IN_PROGRESS" });
    });
  });

  describe("pollList", () => {
    it("POSTs the experiment ids and returns the raw response when the body is empty", async () => {
      const mockResp = mockResponse({ status: 200, ok: true, text: "" });
      fetchMock.mockResolvedValueOnce(mockResp);

      const result = await experimentDataExportService.pollList([1, 2, 3], false);

      expect(fetchMock).toHaveBeenCalledWith(
        "https://example.com/api/experiments/0/export/data/poll/list?createNewOnOutdated=false",
        {
          method: "POST",
          headers: {
            Authorization: "Bearer test-token",
            "Content-Type": "application/json"
          },
          body: JSON.stringify([1, 2, 3])
        }
      );
      expect(result).toBe(mockResp);
    });
  });

  describe("acknowledge", () => {
    it("PUTs with the status query param and returns [] on 204", async () => {
      fetchMock.mockResolvedValueOnce(mockResponse({ status: 204, ok: true, text: "" }));

      const result = await experimentDataExportService.acknowledge(5, 9, "ACCEPTED");

      expect(fetchMock).toHaveBeenCalledWith(
        "https://example.com/api/experiments/5/export/data/9/acknowledge?status=ACCEPTED",
        expect.objectContaining({ method: "PUT" })
      );
      expect(result).toEqual([]);
    });
  });

  describe("handleResponse branches (via prepare)", () => {
    it("returns the parsed data on a 409 with a body", async () => {
      fetchMock.mockResolvedValueOnce(
        mockResponse({ status: 409, ok: false, text: JSON.stringify({ message: "conflict" }) })
      );

      const result = await experimentDataExportService.prepare(5);

      expect(result).toEqual({ message: "conflict" });
    });

    it("returns just the status on a 409 with an empty body", async () => {
      fetchMock.mockResolvedValueOnce(mockResponse({ status: 409, ok: false, text: "" }));

      const result = await experimentDataExportService.prepare(5);

      expect(result).toEqual({ status: 409 });
    });

    it("logs via console.error and returns status/error for a 401", async () => {
      fetchMock.mockResolvedValueOnce(mockResponse({ status: 401, ok: false, text: "{}" }));

      const result = await experimentDataExportService.prepare(5);

      expect(console.error).toHaveBeenCalled();
      expect(result).toEqual({ status: 401, error: {} });
    });

    it("logs via console.warn and returns status/error for a 404", async () => {
      const mockResp = mockResponse({ status: 404, ok: false, text: "" });
      fetchMock.mockResolvedValueOnce(mockResp);

      const result = await experimentDataExportService.prepare(5);

      expect(console.warn).toHaveBeenCalled();
      expect(result).toEqual({ status: 404, error: mockResp });
    });

    it("returns status/error for a non-ok status outside the logged list, without extra logging", async () => {
      fetchMock.mockResolvedValueOnce(mockResponse({ status: 400, ok: false, text: "{}" }));

      const result = await experimentDataExportService.prepare(5);

      // authHeader() itself logs once (the test token isn't a real JWT, per the
      // documented harmless noise) - handleResponse should not add a second one
      // for a 400, since 400 isn't in its logged status list.
      expect(console.error).toHaveBeenCalledTimes(1);
      expect(console.warn).not.toHaveBeenCalled();
      expect(result).toEqual({ status: 400, error: {} });
    });

    it("returns the raw response when ok with an empty body", async () => {
      const mockResp = mockResponse({ status: 200, ok: true, text: "" });
      fetchMock.mockResolvedValueOnce(mockResp);

      const result = await experimentDataExportService.prepare(5);

      expect(result).toBe(mockResp);
    });

    it("catches JSON parse failures and returns error/status", async () => {
      fetchMock.mockResolvedValueOnce(mockResponse({ status: 200, ok: true, text: "not-json" }));

      const result = await experimentDataExportService.prepare(5);

      expect(console.error).toHaveBeenCalled();
      expect(result.status).toBe(200);
      expect(result.error).toBeInstanceOf(Error);
    });
  });

  describe("retrieve", () => {
    it("delegates to handleResponse when the status is not 200", async () => {
      fetchMock.mockResolvedValueOnce(
        mockResponse({ status: 404, ok: false, text: "" })
      );

      const result = await experimentDataExportService.retrieve(5, {
        id: 9,
        mimeType: "text/csv",
        fileName: "export.csv"
      });

      expect(result.status).toBe(404);
    });

    it("downloads the blob and returns the status on a 200", async () => {
      const blob = new Blob(["data"], { type: "text/csv" });

      window.URL.createObjectURL = vi.fn(() => "blob:mock-url");
      window.URL.revokeObjectURL = vi.fn();

      const clickSpy = vi
        .spyOn(HTMLAnchorElement.prototype, "click")
        .mockImplementation(() => {});

      fetchMock.mockResolvedValueOnce(
        mockResponse({ status: 200, ok: true, text: "", blob })
      );

      const result = await experimentDataExportService.retrieve(5, {
        id: 9,
        mimeType: "text/csv",
        fileName: "export.csv"
      });

      expect(fetchMock).toHaveBeenCalledWith(
        "https://example.com/api/experiments/5/export/data/9/retrieve",
        {
          method: "GET",
          headers: {
            Authorization: "Bearer test-token",
            "Content-Type": "application/json"
          }
        }
      );
      expect(window.URL.createObjectURL).toHaveBeenCalled();
      expect(window.URL.revokeObjectURL).toHaveBeenCalledWith("blob:mock-url");
      expect(clickSpy).toHaveBeenCalled();
      expect(result).toEqual({ status: 200 });

      clickSpy.mockRestore();
    });
  });
});

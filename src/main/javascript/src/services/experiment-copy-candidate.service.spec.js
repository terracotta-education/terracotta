import { beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";

import { experimentCopyCandidateService } from "./experiment-copy-candidate.service";
import { api } from "@/store/api.module";

function mockResponse({ ok = true, status = 200, text = "" } = {}) {
  return {
    ok,
    status,
    text: vi.fn().mockResolvedValue(text)
  };
}

describe("experiment-copy-candidate.service", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    api().aud = "https://example.com";
    api().apiToken = "test-token";
    global.fetch = vi.fn();
    vi.spyOn(console, "error").mockImplementation(() => {});
  });

  describe("getAll", () => {
    it("issues a GET with auth headers only (no Content-Type) and returns parsed data", async () => {
      const data = [{ id: 1, status: "PENDING" }];

      global.fetch.mockResolvedValue(
        mockResponse({ ok: true, status: 200, text: JSON.stringify(data) })
      );

      const result = await experimentCopyCandidateService.getAll();

      expect(global.fetch).toHaveBeenCalledWith(
        "https://example.com/api/experiments/copy-candidates",
        {
          method: "GET",
          headers: {
            Authorization: "Bearer test-token",
            "Content-Type": "application/json"
          }
        }
      );

      expect(result).toEqual({ data, status: 200 });
    });

    it("returns [] on a 204 response", async () => {
      global.fetch.mockResolvedValue(
        mockResponse({ ok: true, status: 204, text: "" })
      );

      const result = await experimentCopyCandidateService.getAll();

      expect(result).toEqual([]);
    });

    it("returns {data, status, error} and logs on a non-ok response", async () => {
      const errorBody = { error: "Server error" };

      global.fetch.mockResolvedValue(
        mockResponse({
          ok: false,
          status: 500,
          text: JSON.stringify(errorBody)
        })
      );

      const result = await experimentCopyCandidateService.getAll();

      expect(result).toEqual({
        data: errorBody,
        status: 500,
        error: errorBody
      });
      expect(console.error).toHaveBeenCalled();
    });

    it("returns the raw response when ok but the body is empty", async () => {
      const response = mockResponse({ ok: true, status: 200, text: "" });

      global.fetch.mockResolvedValue(response);

      const result = await experimentCopyCandidateService.getAll();

      expect(result).toBe(response);
    });

    it("returns {error, status} and logs when reading the response body throws", async () => {
      const response = {
        ok: true,
        status: 200,
        text: vi.fn().mockRejectedValue(new Error("boom"))
      };

      global.fetch.mockResolvedValue(response);

      const result = await experimentCopyCandidateService.getAll();

      expect(result.status).toBe(200);
      expect(result.error).toBeInstanceOf(Error);
      expect(console.error).toHaveBeenCalled();
    });
  });

  describe("resolve", () => {
    it("issues a POST with a JSON body and Content-Type header, returning parsed data", async () => {
      const responseData = { imported: [1, 2], declined: [3] };

      global.fetch.mockResolvedValue(
        mockResponse({
          ok: true,
          status: 200,
          text: JSON.stringify(responseData)
        })
      );

      const result = await experimentCopyCandidateService.resolve([1, 2]);

      expect(global.fetch).toHaveBeenCalledWith(
        "https://example.com/api/experiments/copy-candidates/resolve",
        {
          method: "POST",
          headers: {
            Authorization: "Bearer test-token",
            "Content-Type": "application/json"
          },
          body: JSON.stringify({ importCandidateIds: [1, 2] })
        }
      );

      expect(result).toEqual({ data: responseData, status: 200 });
    });

    it("propagates a non-ok error response", async () => {
      const errorBody = { error: "Conflict" };

      global.fetch.mockResolvedValue(
        mockResponse({
          ok: false,
          status: 409,
          text: JSON.stringify(errorBody)
        })
      );

      const result = await experimentCopyCandidateService.resolve([1]);

      expect(result).toEqual({
        data: errorBody,
        status: 409,
        error: errorBody
      });
    });
  });

  describe("getCopyStatus", () => {
    it("issues a GET for the copy status and returns parsed data", async () => {
      const data = { status: "COMPLETE", importIds: ["i1"] };

      global.fetch.mockResolvedValue(
        mockResponse({ ok: true, status: 200, text: JSON.stringify(data) })
      );

      const result = await experimentCopyCandidateService.getCopyStatus();

      expect(global.fetch).toHaveBeenCalledWith(
        "https://example.com/api/experiments/copy-status",
        expect.objectContaining({ method: "GET" })
      );
      expect(result).toEqual({ data, status: 200 });
    });
  });

  describe("acknowledgeCopyStatus", () => {
    it("issues a POST with no body", async () => {
      const response = mockResponse({ ok: true, status: 200, text: "" });
      global.fetch.mockResolvedValue(response);

      const result = await experimentCopyCandidateService.acknowledgeCopyStatus();

      const [url, options] = global.fetch.mock.calls[0];
      expect(url).toBe("https://example.com/api/experiments/copy-status/acknowledge");
      expect(options.method).toBe("POST");
      expect(options.body).toBeUndefined();
      expect(result).toBe(response);
    });
  });

  describe("retryCopy", () => {
    it("issues a POST to retry and returns the parsed copy status", async () => {
      const data = { status: "IN_PROGRESS", importIds: [] };

      global.fetch.mockResolvedValue(
        mockResponse({ ok: true, status: 200, text: JSON.stringify(data) })
      );

      const result = await experimentCopyCandidateService.retryCopy();

      const [url, options] = global.fetch.mock.calls[0];
      expect(url).toBe("https://example.com/api/experiments/copy-status/retry");
      expect(options.method).toBe("POST");
      expect(result).toEqual({ data, status: 200 });
    });
  });

  it("omits the Authorization header when there is no api token", async () => {
    api().apiToken = "";

    global.fetch.mockResolvedValue(
      mockResponse({ ok: true, status: 200, text: JSON.stringify({ a: 1 }) })
    );

    await experimentCopyCandidateService.getAll();

    expect(global.fetch).toHaveBeenCalledWith(
      expect.any(String),
      {
        method: "GET",
        headers: {}
      }
    );
  });
});

import { beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";

import { exposuresService } from "./exposures.service";
import { api } from "@/store/api.module";

function mockResponse({ status, ok, text = "" }) {
  return {
    ok,
    status,
    text: () => Promise.resolve(text)
  };
}

describe("exposuresService", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    api().aud = "https://example.com";
    api().apiToken = "test-token";
    vi.stubGlobal("fetch", vi.fn());
    vi.spyOn(console, "error").mockImplementation(() => {});
    vi.spyOn(console, "warn").mockImplementation(() => {});
  });

  describe("getAll", () => {
    it("requests the exposures list with auth headers", async () => {
      const payload = [{ exposureId: 1 }];
      fetch.mockResolvedValue(
        mockResponse({ status: 200, ok: true, text: JSON.stringify(payload) })
      );

      const result = await exposuresService.getAll(42);

      expect(fetch).toHaveBeenCalledWith(
        "https://example.com/api/experiments/42/exposures",
        {
          method: "GET",
          headers: {
            Authorization: "Bearer test-token",
            "Content-Type": "application/json"
          }
        }
      );
      expect(result).toEqual(payload);
    });
  });

  describe("getById", () => {
    it("requests a single exposure by id", async () => {
      const payload = { exposureId: 3 };
      fetch.mockResolvedValue(
        mockResponse({ status: 200, ok: true, text: JSON.stringify(payload) })
      );

      const result = await exposuresService.getById(42, 3);

      expect(fetch).toHaveBeenCalledWith(
        "https://example.com/api/experiments/42/exposures/3",
        expect.objectContaining({ method: "GET" })
      );
      expect(result).toEqual(payload);
    });
  });

  describe("createExposures", () => {
    it("POSTs to the create endpoint with no body", async () => {
      const payload = [{ exposureId: 1 }, { exposureId: 2 }];
      fetch.mockResolvedValue(
        mockResponse({ status: 200, ok: true, text: JSON.stringify(payload) })
      );

      const result = await exposuresService.createExposures(42);

      expect(fetch).toHaveBeenCalledWith(
        "https://example.com/api/experiments/42/exposures/create",
        {
          method: "POST",
          headers: {
            Authorization: "Bearer test-token",
            "Content-Type": "application/json"
          }
        }
      );
      expect(result).toEqual(payload);
    });
  });

  describe("handleResponse status branches", () => {
    it("returns [] on a 204 with no content", async () => {
      fetch.mockResolvedValue(mockResponse({ status: 204, ok: true, text: "" }));

      const result = await exposuresService.getAll(42);

      expect(result).toEqual([]);
    });

    it("logs and returns an error payload on a 401", async () => {
      fetch.mockResolvedValue(
        mockResponse({ status: 401, ok: false, text: JSON.stringify({ msg: "unauthorized" }) })
      );

      const result = await exposuresService.getAll(42);

      expect(result).toEqual({ status: 401, error: { msg: "unauthorized" } });
      expect(console.error).toHaveBeenCalled();
    });

    it("logs and returns an error payload on a 500", async () => {
      fetch.mockResolvedValue(
        mockResponse({ status: 500, ok: false, text: JSON.stringify({ msg: "boom" }) })
      );

      const result = await exposuresService.getAll(42);

      expect(result).toEqual({ status: 500, error: { msg: "boom" } });
      expect(console.error).toHaveBeenCalled();
    });

    it("warns and returns an error payload on a 404, falling back to the response when there is no body", async () => {
      const response = mockResponse({ status: 404, ok: false, text: "" });
      fetch.mockResolvedValue(response);

      const result = await exposuresService.getAll(42);

      expect(result.status).toBe(404);
      expect(result.error).toBe(response);
      expect(console.warn).toHaveBeenCalled();
    });

    it("returns the raw response when the body is empty on success", async () => {
      const response = mockResponse({ status: 200, ok: true, text: "" });
      fetch.mockResolvedValue(response);

      const result = await exposuresService.getAll(42);

      expect(result).toBe(response);
    });

    it("returns an error object when the response body fails to parse", async () => {
      fetch.mockResolvedValue(
        mockResponse({ status: 200, ok: true, text: "not valid json" })
      );

      const result = await exposuresService.getAll(42);

      expect(result.status).toBe(200);
      expect(result.error).toBeInstanceOf(SyntaxError);
      expect(console.error).toHaveBeenCalled();
    });
  });
});

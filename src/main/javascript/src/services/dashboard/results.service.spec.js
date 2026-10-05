import { beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";

import { resultsDashboardService } from "./results.service";
import { api } from "@/store/api.module";

function mockResponse({ status, ok, text = "" }) {
  return {
    ok,
    status,
    text: () => Promise.resolve(text)
  };
}

describe("resultsDashboardService", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    api().aud = "https://example.com";
    api().apiToken = "test-token";
    vi.stubGlobal("fetch", vi.fn());
    vi.spyOn(console, "error").mockImplementation(() => {});
  });

  describe("overview", () => {
    it("requests the overview endpoint with auth headers and wraps the data", async () => {
      const payload = { totalParticipants: 10 };
      fetch.mockResolvedValue(
        mockResponse({ status: 200, ok: true, text: JSON.stringify(payload) })
      );

      const result = await resultsDashboardService.overview(42);

      expect(fetch).toHaveBeenCalledWith(
        "https://example.com/api/experiments/42/dashboard/results/overview",
        {
          method: "GET",
          headers: {
            Authorization: "Bearer test-token",
            "Content-Type": "application/json"
          }
        }
      );
      expect(result).toEqual({ data: payload, status: 200 });
    });

    it("returns the raw response when the body is empty on success", async () => {
      const response = mockResponse({ status: 200, ok: true, text: "" });
      fetch.mockResolvedValue(response);

      const result = await resultsDashboardService.overview(42);

      expect(result).toBe(response);
    });
  });

  describe("outcomes", () => {
    it("POSTs the outcomes body and wraps the data", async () => {
      const body = { outcomeIds: [1, 2] };
      const payload = { results: [] };
      fetch.mockResolvedValue(
        mockResponse({ status: 200, ok: true, text: JSON.stringify(payload) })
      );

      const result = await resultsDashboardService.outcomes(42, body);

      expect(fetch).toHaveBeenCalledWith(
        "https://example.com/api/experiments/42/dashboard/results/outcomes",
        {
          method: "POST",
          headers: {
            Authorization: "Bearer test-token",
            "Content-Type": "application/json"
          },
          body: JSON.stringify(body)
        }
      );
      expect(result).toEqual({ data: payload, status: 200 });
    });
  });

  describe("handleResponse status branches", () => {
    it("returns [] on a 204 with no content", async () => {
      fetch.mockResolvedValue(mockResponse({ status: 204, ok: true, text: "" }));

      const result = await resultsDashboardService.overview(42);

      expect(result).toEqual([]);
    });

    it("returns a message payload on a 409", async () => {
      fetch.mockResolvedValue(
        mockResponse({ status: 409, ok: false, text: JSON.stringify({ msg: "conflict" }) })
      );

      const result = await resultsDashboardService.overview(42);

      expect(result).toEqual({ message: { msg: "conflict" }, status: 409 });
    });

    it("logs and returns a data/error payload on a non-ok status", async () => {
      fetch.mockResolvedValue(
        mockResponse({ status: 500, ok: false, text: JSON.stringify({ msg: "boom" }) })
      );

      const result = await resultsDashboardService.overview(42);

      expect(result).toEqual({
        data: { msg: "boom" },
        status: 500,
        error: { msg: "boom" }
      });
      expect(console.error).toHaveBeenCalled();
    });

    it("returns the raw response when handling the response throws", async () => {
      const response = {
        ok: true,
        status: 200,
        text: () => Promise.reject(new Error("stream error"))
      };
      fetch.mockResolvedValue(response);

      const result = await resultsDashboardService.overview(42);

      expect(result).toEqual({ error: expect.any(Error), status: 200 });
      expect(console.error).toHaveBeenCalled();
    });
  });
});

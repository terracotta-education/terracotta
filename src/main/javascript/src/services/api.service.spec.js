import { beforeEach, describe, expect, it, vi } from "vitest";

import { pinia } from "@/pinia";
import { api } from "@/store/api.module";
import { apiService } from "./api.service";

function mockResponse({ ok = true, status = 200, text = "" } = {}) {
  return {
    ok,
    status,
    text: () => (text instanceof Error ? Promise.reject(text) : Promise.resolve(text))
  };
}

describe("apiService", () => {
  beforeEach(() => {
    api(pinia).aud = "https://example.com";
    api(pinia).apiToken = "test-token";
    vi.stubGlobal("fetch", vi.fn());
    vi.clearAllMocks();
  });

  describe("getApiToken", () => {
    it("trades the lti token for an api token and returns the raw text on success", async () => {
      fetch.mockResolvedValue(mockResponse({ ok: true, text: "raw-jwt-token" }));

      const result = await apiService.getApiToken("lti-token");

      expect(fetch).toHaveBeenCalledWith(
        "https://example.com/api/oauth/trade",
        expect.objectContaining({
          method: "POST",
          headers: expect.objectContaining({ Authorization: "Bearer lti-token" })
        })
      );
      expect(result).toBe("raw-jwt-token");
    });

    it("returns null when the trade request fails", async () => {
      fetch.mockResolvedValue(mockResponse({ ok: false, status: 401 }));

      const result = await apiService.getApiToken("lti-token");

      expect(result).toBeNull();
    });
  });

  describe("refreshToken", () => {
    it("refreshes the api token and returns the raw text on success", async () => {
      fetch.mockResolvedValue(mockResponse({ ok: true, text: "new-jwt-token" }));

      const result = await apiService.refreshToken();

      expect(fetch).toHaveBeenCalledWith(
        "https://example.com/api/oauth/refresh",
        expect.objectContaining({ method: "POST" })
      );
      expect(result).toBe("new-jwt-token");
    });

    it("returns null when the refresh request fails", async () => {
      fetch.mockResolvedValue(mockResponse({ ok: false, status: 401 }));

      const result = await apiService.refreshToken();

      expect(result).toBeNull();
    });
  });

  describe("deepLinkJwt", () => {
    it("fetches the deep-link jwt and returns the raw text on success", async () => {
      fetch.mockResolvedValue(mockResponse({ ok: true, text: "deep-link-jwt" }));

      const result = await apiService.deepLinkJwt("123");

      expect(fetch).toHaveBeenCalledWith(
        "https://example.com/deeplink/toJwt/123",
        expect.objectContaining({ method: "GET" })
      );
      expect(result).toBe("deep-link-jwt");
    });

    it("returns null when the request fails", async () => {
      fetch.mockResolvedValue(mockResponse({ ok: false, status: 404 }));

      const result = await apiService.deepLinkJwt("123");

      expect(result).toBeNull();
    });
  });

  it("reportStep POSTs the step payload and returns the handled response", async () => {
    fetch.mockResolvedValue(mockResponse({ text: JSON.stringify({ ok: true }) }));

    const result = await apiService.reportStep(1, "STEP_NAME", { a: 1 }, true);

    expect(fetch).toHaveBeenCalledWith(
      "https://example.com/api/experiments/1/step?preferLmsChecks=true",
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({ step: "STEP_NAME", parameters: { a: 1 } })
      })
    );
    expect(result).toEqual({ data: { ok: true }, status: 200 });
  });

  it("getStepStatus fetches the status for a batch", async () => {
    fetch.mockResolvedValue(mockResponse({ text: JSON.stringify({ status: "COMPLETE" }) }));

    const result = await apiService.getStepStatus(1, "batch-1");

    expect(fetch).toHaveBeenCalledWith(
      "https://example.com/api/experiments/1/step/status/batch-1",
      expect.objectContaining({ method: "GET" })
    );
    expect(result).toEqual({ data: { status: "COMPLETE" }, status: 200 });
  });

  describe("handleResponse branches", () => {
    it("returns [] on a 204", async () => {
      fetch.mockResolvedValue(mockResponse({ status: 204, text: "" }));

      const result = await apiService.getStepStatus(1, "batch-1");

      expect(result).toEqual([]);
    });

    it("returns a message payload on a 409", async () => {
      fetch.mockResolvedValue(
        mockResponse({ ok: false, status: 409, text: JSON.stringify({ message: "dup" }) })
      );

      const result = await apiService.getStepStatus(1, "batch-1");

      expect(result).toEqual({ message: { message: "dup" }, status: 409 });
    });

    it("logs and returns an error payload on a non-ok response", async () => {
      const consoleSpy = vi.spyOn(console, "error").mockImplementation(() => {});
      fetch.mockResolvedValue(
        mockResponse({ ok: false, status: 500, text: JSON.stringify({ error: "boom" }) })
      );

      const result = await apiService.getStepStatus(1, "batch-1");

      expect(result).toEqual({ data: { error: "boom" }, status: 500, error: { error: "boom" } });
      expect(consoleSpy).toHaveBeenCalled();
      consoleSpy.mockRestore();
    });

    it("returns the raw response when there is no body on success", async () => {
      const response = mockResponse({ status: 200, text: "" });
      fetch.mockResolvedValue(response);

      const result = await apiService.getStepStatus(1, "batch-1");

      expect(result).toBe(response);
    });

    it("logs and returns an error payload when reading the response fails", async () => {
      const consoleSpy = vi.spyOn(console, "error").mockImplementation(() => {});
      fetch.mockResolvedValue(mockResponse({ text: new Error("bad text") }));

      const result = await apiService.getStepStatus(1, "batch-1");

      expect(result.error).toBeInstanceOf(Error);
      expect(consoleSpy).toHaveBeenCalled();
      consoleSpy.mockRestore();
    });
  });
});

import { beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";

import { api } from "@/store/api.module";
import { configurationService } from "./configuration.service";

function mockResponse({ ok = true, status = 200, text = "" } = {}) {
  return {
    ok,
    status,
    text: () => (text instanceof Error ? Promise.reject(text) : Promise.resolve(text))
  };
}

describe("configurationService", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    api().aud = "https://example.com";
    api().apiToken = "test-token";
    vi.stubGlobal("fetch", vi.fn());
    vi.clearAllMocks();
  });

  it("get fetches the app configuration", async () => {
    fetch.mockResolvedValue(mockResponse({ text: JSON.stringify({ featureFlag: true }) }));

    const result = await configurationService.get();

    expect(fetch).toHaveBeenCalledWith(
      "https://example.com/api/configuration",
      expect.objectContaining({ method: "GET" })
    );
    expect(result).toEqual({ featureFlag: true });
  });

  describe("handleResponse branches", () => {
    it("returns [] on a 204", async () => {
      fetch.mockResolvedValue(mockResponse({ status: 204, text: "" }));

      const result = await configurationService.get();

      expect(result).toEqual([]);
    });

    it("returns a message payload on a 409", async () => {
      fetch.mockResolvedValue(
        mockResponse({ ok: false, status: 409, text: JSON.stringify({ message: "dup" }) })
      );

      const result = await configurationService.get();

      expect(result).toEqual({ message: { message: "dup" }, status: 409 });
    });

    it("returns an error payload on a non-ok response without logging", async () => {
      fetch.mockResolvedValue(
        mockResponse({ ok: false, status: 500, text: JSON.stringify({ error: "boom" }) })
      );

      const result = await configurationService.get();

      expect(result).toEqual({ data: { error: "boom" }, status: 500, error: { error: "boom" } });
    });

    it("returns the raw response when there is no body on success", async () => {
      const response = mockResponse({ status: 200, text: "" });
      fetch.mockResolvedValue(response);

      const result = await configurationService.get();

      expect(result).toBe(response);
    });

    it("logs and returns an error payload when reading the response fails", async () => {
      const consoleSpy = vi.spyOn(console, "error").mockImplementation(() => {});
      fetch.mockResolvedValue(mockResponse({ text: new Error("bad text") }));

      const result = await configurationService.get();

      expect(result.error).toBeInstanceOf(Error);
      expect(consoleSpy).toHaveBeenCalled();
      consoleSpy.mockRestore();
    });
  });
});

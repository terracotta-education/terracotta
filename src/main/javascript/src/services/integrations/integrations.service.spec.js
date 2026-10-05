import { beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";

import { integrationsService } from "./integrations.service";
import { api } from "@/store/api.module";

function mockResponse({ ok = true, status = 200, text = "" } = {}) {
  return {
    ok,
    status,
    text: vi.fn().mockResolvedValue(text)
  };
}

describe("integrations.service", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    api().aud = "https://example.com";
    api().apiToken = "test-token";
    global.fetch = vi.fn();
    vi.spyOn(console, "error").mockImplementation(() => {});
    vi.spyOn(console, "warn").mockImplementation(() => {});
  });

  describe("validateIframeUrl", () => {
    it("calls the expected GET endpoint with an encoded url and auth headers, returning true when ok", async () => {
      global.fetch.mockResolvedValue(
        mockResponse({ ok: true, status: 200, text: "" })
      );

      const result = await integrationsService.validateIframeUrl(
        "https://lti.example.com/tool?foo=bar"
      );

      expect(global.fetch).toHaveBeenCalledWith(
        "https://example.com/integrations/validate/iframe?url=" +
          encodeURIComponent("https://lti.example.com/tool?foo=bar"),
        {
          method: "GET",
          headers: {
            Authorization: "Bearer test-token",
            "Content-Type": "application/json"
          }
        }
      );

      expect(result).toBe(true);
    });

    it("returns false and logs console.error for 401/402/500 responses", async () => {
      for (const status of [401, 402, 500]) {
        console.error.mockClear();

        global.fetch.mockResolvedValue(
          mockResponse({ ok: false, status, text: "" })
        );

        const result = await integrationsService.validateIframeUrl(
          "https://lti.example.com/tool"
        );

        expect(result).toBe(false);
        expect(console.error).toHaveBeenCalled();
      }
    });

    it("returns false and logs console.warn for a 404 response", async () => {
      global.fetch.mockResolvedValue(
        mockResponse({ ok: false, status: 404, text: "" })
      );

      const result = await integrationsService.validateIframeUrl(
        "https://lti.example.com/tool"
      );

      expect(result).toBe(false);
      expect(console.warn).toHaveBeenCalled();
      // console.error may still fire from authHeader()'s expired-token check
      // (the fixture token isn't a real JWT) - only the handleResponse-level
      // "auth/server error" log is what this status must NOT trigger.
      expect(console.error).not.toHaveBeenCalledWith(
        "handleResponse | auth/server error",
        expect.anything()
      );
    });

    it("returns false without logging for other non-ok statuses", async () => {
      global.fetch.mockResolvedValue(
        mockResponse({ ok: false, status: 400, text: "" })
      );

      const result = await integrationsService.validateIframeUrl(
        "https://lti.example.com/tool"
      );

      expect(result).toBe(false);
      // console.error may still fire from authHeader()'s expired-token check
      // (the fixture token isn't a real JWT) - only the handleResponse-level
      // logs are what this status must NOT trigger.
      expect(console.error).not.toHaveBeenCalledWith(
        "handleResponse | auth/server error",
        expect.anything()
      );
      expect(console.warn).not.toHaveBeenCalled();
    });

    it("returns false and logs console.error when reading the response body throws", async () => {
      const response = {
        ok: true,
        status: 200,
        text: vi.fn().mockRejectedValue(new Error("boom"))
      };

      global.fetch.mockResolvedValue(response);

      const result = await integrationsService.validateIframeUrl(
        "https://lti.example.com/tool"
      );

      expect(result).toBe(false);
      expect(console.error).toHaveBeenCalled();
    });

    it("omits the Authorization header when there is no api token", async () => {
      api().apiToken = "";

      global.fetch.mockResolvedValue(
        mockResponse({ ok: true, status: 200, text: "" })
      );

      await integrationsService.validateIframeUrl("https://lti.example.com/tool");

      expect(global.fetch).toHaveBeenCalledWith(
        expect.any(String),
        {
          method: "GET",
          headers: {}
        }
      );
    });
  });
});

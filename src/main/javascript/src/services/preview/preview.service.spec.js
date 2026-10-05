import { beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";

import { previewService } from "./preview.service";
import { api } from "@/store/api.module";

function mockResponse({ ok = true, status = 200, text = "" } = {}) {
  return {
    ok,
    status,
    text: vi.fn().mockResolvedValue(text)
  };
}

describe("preview.service", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    api().aud = "https://example.com";
    api().apiToken = "test-token";
    global.fetch = vi.fn();
    vi.spyOn(console, "error").mockImplementation(() => {});
  });

  describe("treatmentPreview", () => {
    it("calls the expected GET endpoint with auth headers and returns parsed data on success", async () => {
      const data = { previewUrl: "https://lms.example.com/launch" };

      global.fetch.mockResolvedValue(
        mockResponse({ ok: true, status: 200, text: JSON.stringify(data) })
      );

      const result = await previewService.treatmentPreview(
        1,
        2,
        3,
        4,
        99
      );

      expect(global.fetch).toHaveBeenCalledWith(
        "https://example.com/preview/experiments/1/conditions/2/treatments/3/id/4?ownerId=99",
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

    it("returns {data: null, status} on a 204 response", async () => {
      global.fetch.mockResolvedValue(
        mockResponse({ ok: true, status: 204, text: "" })
      );

      const result = await previewService.treatmentPreview(
        1,
        2,
        3,
        4,
        99
      );

      expect(result).toEqual({ data: null, status: 204 });
    });

    it("returns {message, status} on a 409 response", async () => {
      const conflictMessage = "Preview already consumed";

      global.fetch.mockResolvedValue(
        mockResponse({
          ok: false,
          status: 409,
          text: JSON.stringify(conflictMessage)
        })
      );

      const result = await previewService.treatmentPreview(
        1,
        2,
        3,
        4,
        99
      );

      expect(result).toEqual({ message: conflictMessage, status: 409 });
    });

    it("returns {data, status, error} and logs on a non-ok response", async () => {
      const errorBody = { error: "Forbidden" };

      global.fetch.mockResolvedValue(
        mockResponse({
          ok: false,
          status: 403,
          text: JSON.stringify(errorBody)
        })
      );

      const result = await previewService.treatmentPreview(
        1,
        2,
        3,
        4,
        99
      );

      expect(result).toEqual({
        data: errorBody,
        status: 403,
        error: errorBody
      });
      expect(console.error).toHaveBeenCalled();
    });

    it("returns the raw response when ok but the body is empty", async () => {
      const response = mockResponse({ ok: true, status: 200, text: "" });

      global.fetch.mockResolvedValue(response);

      const result = await previewService.treatmentPreview(
        1,
        2,
        3,
        4,
        99
      );

      expect(result).toBe(response);
    });

    it("returns {error, status} and logs when reading the response body throws", async () => {
      const response = {
        ok: true,
        status: 200,
        text: vi.fn().mockRejectedValue(new Error("boom"))
      };

      global.fetch.mockResolvedValue(response);

      const result = await previewService.treatmentPreview(
        1,
        2,
        3,
        4,
        99
      );

      expect(result.status).toBe(200);
      expect(result.error).toBeInstanceOf(Error);
      expect(console.error).toHaveBeenCalled();
    });

    it("omits the Authorization header when there is no api token", async () => {
      api().apiToken = "";

      global.fetch.mockResolvedValue(
        mockResponse({ ok: true, status: 200, text: JSON.stringify({ a: 1 }) })
      );

      await previewService.treatmentPreview(1, 2, 3, 4, 99);

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

import { beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";

import { mediaEventsService } from "./media-events.service";
import { api } from "@/store/api.module";

function mockResponse({ status, ok, text = "" }) {
  return {
    ok,
    status,
    text: () => Promise.resolve(text)
  };
}

const params = {
  experimentId: 1,
  conditionId: 2,
  treatmentId: 3,
  assessmentId: 4,
  submissionId: 5,
  questionId: 6,
  event: { type: "play", timestamp: 123 }
};

describe("mediaEventsService", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    api().aud = "https://example.com";
    api().apiToken = "test-token";
    vi.stubGlobal("fetch", vi.fn());
    vi.spyOn(console, "error").mockImplementation(() => {});
    vi.spyOn(console, "warn").mockImplementation(() => {});
  });

  describe("createVideoEvent", () => {
    it("POSTs the media event to the fully nested path with a JSON body", async () => {
      const payload = { status: "ok" };
      fetch.mockResolvedValue(
        mockResponse({ status: 200, ok: true, text: JSON.stringify(payload) })
      );

      const result = await mediaEventsService.createVideoEvent(params);

      expect(fetch).toHaveBeenCalledWith(
        "https://example.com/api/experiments/1/conditions/2/treatments/3/assessments/4/submissions/5/questions/6/media_event",
        {
          method: "POST",
          headers: {
            Authorization: "Bearer test-token",
            "Content-Type": "application/json"
          },
          body: JSON.stringify(params.event)
        }
      );
      expect(result).toEqual(payload);
    });
  });

  describe("handleResponse status branches", () => {
    it("returns [] on a 204 with no content", async () => {
      fetch.mockResolvedValue(mockResponse({ status: 204, ok: true, text: "" }));

      const result = await mediaEventsService.createVideoEvent(params);

      expect(result).toEqual([]);
    });

    it("logs and returns an error payload on a 401", async () => {
      fetch.mockResolvedValue(
        mockResponse({ status: 401, ok: false, text: JSON.stringify({ msg: "unauthorized" }) })
      );

      const result = await mediaEventsService.createVideoEvent(params);

      expect(result).toEqual({ status: 401, error: { msg: "unauthorized" } });
      expect(console.error).toHaveBeenCalled();
    });

    it("logs and returns an error payload on a 500", async () => {
      fetch.mockResolvedValue(
        mockResponse({ status: 500, ok: false, text: JSON.stringify({ msg: "boom" }) })
      );

      const result = await mediaEventsService.createVideoEvent(params);

      expect(result).toEqual({ status: 500, error: { msg: "boom" } });
      expect(console.error).toHaveBeenCalled();
    });

    it("warns and returns an error payload on a 404, falling back to the response when there is no body", async () => {
      const response = mockResponse({ status: 404, ok: false, text: "" });
      fetch.mockResolvedValue(response);

      const result = await mediaEventsService.createVideoEvent(params);

      expect(result.status).toBe(404);
      expect(result.error).toBe(response);
      expect(console.warn).toHaveBeenCalled();
    });

    it("returns an error payload on other non-ok statuses without special logging", async () => {
      fetch.mockResolvedValue(
        mockResponse({ status: 400, ok: false, text: JSON.stringify({ msg: "bad" }) })
      );

      const result = await mediaEventsService.createVideoEvent(params);

      expect(result).toEqual({ status: 400, error: { msg: "bad" } });
      // console.error/warn may still fire from the unrelated auth-header token-expiry
      // check (api().apiToken isn't a real JWT here) - only assert handleResponse's own
      // status-specific logging (401/402/500/404) was not triggered for this 400 status.
      expect(console.error).not.toHaveBeenCalledWith(
        "handleResponse | auth/server error",
        expect.anything()
      );
      expect(console.warn).not.toHaveBeenCalledWith(
        "handleResponse | not found",
        expect.anything()
      );
    });

    it("returns the raw response when the body is empty on success", async () => {
      const response = mockResponse({ status: 200, ok: true, text: "" });
      fetch.mockResolvedValue(response);

      const result = await mediaEventsService.createVideoEvent(params);

      expect(result).toBe(response);
    });

    it("returns an error object when handling the response throws", async () => {
      const response = {
        ok: true,
        status: 200,
        text: () => Promise.reject(new Error("stream error"))
      };
      fetch.mockResolvedValue(response);

      const result = await mediaEventsService.createVideoEvent(params);

      expect(result).toEqual({ error: expect.any(Error), status: 200 });
      expect(console.error).toHaveBeenCalled();
    });
  });
});

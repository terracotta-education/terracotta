import { beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";

import { participantService } from "./participants.service";
import { api } from "@/store/api.module";

function mockResponse({ status, ok, text = "" }) {
  return {
    ok,
    status,
    text: () => Promise.resolve(text)
  };
}

describe("participantService", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    api().aud = "https://example.com";
    api().apiToken = "test-token";
    vi.stubGlobal("fetch", vi.fn());
    vi.spyOn(console, "error").mockImplementation(() => {});
    vi.spyOn(console, "warn").mockImplementation(() => {});
  });

  describe("getAll", () => {
    it("requests the participants list with the refresh flag and auth headers", async () => {
      const payload = [{ participantId: 1 }];
      fetch.mockResolvedValue(
        mockResponse({ status: 200, ok: true, text: JSON.stringify(payload) })
      );

      const result = await participantService.getAll(42, true);

      expect(fetch).toHaveBeenCalledWith(
        "https://example.com/api/experiments/42/participants?refresh=true",
        expect.objectContaining({
          method: "GET",
          headers: expect.objectContaining({
            Authorization: "Bearer test-token"
          })
        })
      );
      expect(result).toEqual(payload);
    });

    it("defaults refresh to false when omitted", async () => {
      fetch.mockResolvedValue(
        mockResponse({ status: 200, ok: true, text: JSON.stringify([]) })
      );

      await participantService.getAll(42);

      expect(fetch).toHaveBeenCalledWith(
        "https://example.com/api/experiments/42/participants?refresh=false",
        expect.anything()
      );
    });
  });

  describe("getById", () => {
    it("requests a single participant by id", async () => {
      const payload = { participantId: 7 };
      fetch.mockResolvedValue(
        mockResponse({ status: 200, ok: true, text: JSON.stringify(payload) })
      );

      const result = await participantService.getById(42, 7);

      expect(fetch).toHaveBeenCalledWith(
        "https://example.com/api/experiments/42/participants/7",
        expect.objectContaining({ method: "GET" })
      );
      expect(result).toEqual(payload);
    });
  });

  describe("updateParticipants", () => {
    it("PUTs the participant list with a JSON body", async () => {
      const details = [{ participantId: 1 }, { participantId: 2 }];
      fetch.mockResolvedValue(
        mockResponse({ status: 200, ok: true, text: JSON.stringify({ status: 200 }) })
      );

      const result = await participantService.updateParticipants(42, details);

      expect(fetch).toHaveBeenCalledWith(
        "https://example.com/api/experiments/42/participants",
        {
          method: "PUT",
          headers: {
            Authorization: "Bearer test-token",
            "Content-Type": "application/json"
          },
          body: JSON.stringify(details)
        }
      );
      expect(result).toEqual({ status: 200 });
    });
  });

  describe("updateParticipant", () => {
    it("PUTs a single participant using its participantId in the path", async () => {
      const details = { participantId: 9, consent: true };
      fetch.mockResolvedValue(
        mockResponse({ status: 200, ok: true, text: JSON.stringify(details) })
      );

      const result = await participantService.updateParticipant(42, details);

      expect(fetch).toHaveBeenCalledWith(
        "https://example.com/api/experiments/42/participants/9",
        {
          method: "PUT",
          headers: {
            Authorization: "Bearer test-token",
            "Content-Type": "application/json"
          },
          body: JSON.stringify(details)
        }
      );
      expect(result).toEqual(details);
    });
  });

  describe("handleResponse status branches", () => {
    it("returns [] on a 204 with no content", async () => {
      fetch.mockResolvedValue(mockResponse({ status: 204, ok: true, text: "" }));

      const result = await participantService.getAll(42);

      expect(result).toEqual([]);
    });

    it("returns a message payload on a 401", async () => {
      fetch.mockResolvedValue(
        mockResponse({ status: 401, ok: false, text: JSON.stringify({ msg: "unauthorized" }) })
      );

      const result = await participantService.getAll(42);

      expect(result).toEqual({ message: { msg: "unauthorized" }, status: 401 });
    });

    it("logs and returns an error payload on a 500", async () => {
      fetch.mockResolvedValue(
        mockResponse({ status: 500, ok: false, text: JSON.stringify({ msg: "boom" }) })
      );

      const result = await participantService.getAll(42);

      expect(result).toEqual({ status: 500, error: { msg: "boom" } });
      expect(console.error).toHaveBeenCalled();
    });

    it("warns and returns an error payload on a 404", async () => {
      fetch.mockResolvedValue(mockResponse({ status: 404, ok: false, text: "" }));

      const result = await participantService.getAll(42);

      expect(result).toEqual({ status: 404, error: expect.anything() });
      expect(console.warn).toHaveBeenCalled();
    });

    it("returns an error payload on other non-ok statuses without special logging", async () => {
      fetch.mockResolvedValue(
        mockResponse({ status: 400, ok: false, text: JSON.stringify({ msg: "bad" }) })
      );

      const result = await participantService.getAll(42);

      expect(result).toEqual({ status: 400, error: { msg: "bad" } });
      // console.error/warn may still fire from the unrelated auth-header token-expiry
      // check (api().apiToken isn't a real JWT here) - only assert handleResponse's own
      // status-specific logging (402/500/404) was not triggered for this 400 status.
      expect(console.error).not.toHaveBeenCalledWith(
        "handleResponse | auth/server error",
        expect.anything()
      );
      expect(console.warn).not.toHaveBeenCalledWith(
        "handleResponse | not found",
        expect.anything()
      );
    });

    it("returns the raw response when handling the response throws", async () => {
      const response = {
        ok: true,
        status: 200,
        text: () => Promise.reject(new Error("stream error"))
      };
      fetch.mockResolvedValue(response);

      const result = await participantService.getAll(42);

      expect(result).toEqual({ error: expect.any(Error), status: 200 });
      expect(console.error).toHaveBeenCalled();
    });
  });

  it("omits the Authorization header when there is no api token", async () => {
    api().apiToken = "";
    fetch.mockResolvedValue(
      mockResponse({ status: 200, ok: true, text: JSON.stringify([]) })
    );

    await participantService.getAll(42);

    expect(fetch).toHaveBeenCalledWith(
      "https://example.com/api/experiments/42/participants?refresh=false",
      { method: "GET", headers: {} }
    );
  });
});

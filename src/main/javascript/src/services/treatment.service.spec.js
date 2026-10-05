import { beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";

import { api } from "@/store/api.module";
import { treatmentService } from "./treatment.service";

function mockResponse({ ok = true, status = 200, text = "" } = {}) {
  return {
    ok,
    status,
    text: () => (text instanceof Error ? Promise.reject(text) : Promise.resolve(text))
  };
}

describe("treatmentService", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    api().aud = "https://example.com";
    api().apiToken = "test-token";
    vi.stubGlobal("fetch", vi.fn());
    vi.clearAllMocks();
  });

  it("fetchTreatment fetches the treatments for a condition", async () => {
    fetch.mockResolvedValue(mockResponse({ text: JSON.stringify([{ treatmentId: 1 }]) }));

    const result = await treatmentService.fetchTreatment(1, 2);

    expect(fetch).toHaveBeenCalledWith(
      "https://example.com/api/experiments/1/conditions/2/treatments",
      expect.objectContaining({ method: "GET" })
    );
    expect(result).toEqual({ data: [{ treatmentId: 1 }], status: 200 });
  });

  it("create POSTs a new treatment for an assignment", async () => {
    fetch.mockResolvedValue(mockResponse({ text: JSON.stringify({ treatmentId: 1 }) }));

    const result = await treatmentService.create(1, 2, 3);

    expect(fetch).toHaveBeenCalledWith(
      "https://example.com/api/experiments/1/conditions/2/treatments",
      expect.objectContaining({ method: "POST", body: JSON.stringify({ assignmentId: 3 }) })
    );
    expect(result).toEqual({ data: { treatmentId: 1 }, status: 200 });
  });

  it("update PUTs the treatment body, defaulting to an empty object", async () => {
    fetch.mockResolvedValue(mockResponse({ text: JSON.stringify({ treatmentId: 1 }) }));

    await treatmentService.update(1, 2, 3);

    expect(fetch).toHaveBeenCalledWith(
      "https://example.com/api/experiments/1/conditions/2/treatments/3",
      expect.objectContaining({ method: "PUT", body: JSON.stringify({}) })
    );
  });

  describe("handleResponse branches", () => {
    it("returns a null-data payload on a 204", async () => {
      fetch.mockResolvedValue(mockResponse({ status: 204, text: "" }));

      const result = await treatmentService.fetchTreatment(1, 2);

      expect(result).toEqual({ data: null, status: 204 });
    });

    it("returns a message payload on a 409", async () => {
      fetch.mockResolvedValue(
        mockResponse({ ok: false, status: 409, text: JSON.stringify({ message: "dup" }) })
      );

      const result = await treatmentService.fetchTreatment(1, 2);

      expect(result).toEqual({ message: { message: "dup" }, status: 409 });
    });

    it("logs and returns an error payload on a non-ok response", async () => {
      const consoleSpy = vi.spyOn(console, "error").mockImplementation(() => {});
      fetch.mockResolvedValue(
        mockResponse({ ok: false, status: 500, text: JSON.stringify({ error: "boom" }) })
      );

      const result = await treatmentService.fetchTreatment(1, 2);

      expect(result).toEqual({ data: { error: "boom" }, status: 500, error: { error: "boom" } });
      expect(consoleSpy).toHaveBeenCalled();
      consoleSpy.mockRestore();
    });

    it("returns the raw response when there is no body on success", async () => {
      const response = mockResponse({ status: 200, text: "" });
      fetch.mockResolvedValue(response);

      const result = await treatmentService.fetchTreatment(1, 2);

      expect(result).toBe(response);
    });

    it("logs and returns an error payload when reading the response fails", async () => {
      const consoleSpy = vi.spyOn(console, "error").mockImplementation(() => {});
      fetch.mockResolvedValue(mockResponse({ text: new Error("bad text") }));

      const result = await treatmentService.fetchTreatment(1, 2);

      expect(result.error).toBeInstanceOf(Error);
      expect(consoleSpy).toHaveBeenCalled();
      consoleSpy.mockRestore();
    });
  });
});

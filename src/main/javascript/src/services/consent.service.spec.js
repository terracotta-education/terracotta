import { beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";

vi.mock("axios");

import axios from "axios";
import { api } from "@/store/api.module";
import { consentService } from "./consent.service";

function mockResponse({ ok = true, status = 200, text = "" } = {}) {
  return {
    ok,
    status,
    text: () => (text instanceof Error ? Promise.reject(text) : Promise.resolve(text)),
    arrayBuffer: () => Promise.resolve(new TextEncoder().encode("pdf-bytes").buffer)
  };
}

describe("consentService", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    api().aud = "https://example.com";
    api().apiToken = "test-token";
    vi.stubGlobal("fetch", vi.fn());
    vi.clearAllMocks();
  });

  describe("create", () => {
    it("uploads the consent PDF with a title and returns the status/message", async () => {
      axios.post.mockResolvedValue({ status: 200, statusText: "OK" });

      const result = await consentService.create(1, new File(["pdf"], "consent.pdf"), "My Title");

      expect(axios.post).toHaveBeenCalledWith(
        "https://example.com/api/experiments/1/consent?title=My+Title",
        expect.any(FormData),
        expect.objectContaining({ headers: expect.any(Object) })
      );
      expect(result).toEqual({ status: 200, message: "OK" });
    });

    it("defaults the title query param to an empty string when omitted", async () => {
      axios.post.mockResolvedValue({ status: 200, statusText: "OK" });

      await consentService.create(1, new File(["pdf"], "consent.pdf"));

      expect(axios.post).toHaveBeenCalledWith(
        "https://example.com/api/experiments/1/consent?title=",
        expect.any(FormData),
        expect.anything()
      );
    });

    it("returns status/message when axios rejects with a response", async () => {
      axios.post.mockRejectedValue({ response: { status: 400, statusText: "Bad Request" } });

      const result = await consentService.create(1, new File(["pdf"], "consent.pdf"), "t");

      expect(result).toEqual({ status: 400, message: "Bad Request" });
    });

    it("rethrows when axios rejects without a response", async () => {
      axios.post.mockRejectedValue(new Error("network down"));

      await expect(
        consentService.create(1, new File(["pdf"], "consent.pdf"), "t")
      ).rejects.toThrow("network down");
    });
  });

  it("update PUTs to refresh the consent document", async () => {
    fetch.mockResolvedValue(mockResponse({ text: JSON.stringify({ consentDocumentId: 1 }) }));

    const result = await consentService.update(1);

    expect(fetch).toHaveBeenCalledWith(
      "https://example.com/api/experiments/1/consent",
      expect.objectContaining({ method: "PUT" })
    );
    expect(result).toEqual({ consentDocumentId: 1 });
  });

  it("delete removes the consent document", async () => {
    fetch.mockResolvedValue(mockResponse({ status: 204, text: "" }));

    const result = await consentService.delete(1);

    expect(fetch).toHaveBeenCalledWith(
      "https://example.com/api/experiments/1/consent",
      expect.objectContaining({ method: "DELETE" })
    );
    expect(result).toEqual([]);
  });

  it("getConsentFile fetches the file and returns a base64 payload", async () => {
    fetch.mockResolvedValue(mockResponse({ status: 200 }));

    const result = await consentService.getConsentFile(1);

    expect(fetch).toHaveBeenCalledWith(
      "https://example.com/api/experiments/1/consent",
      expect.objectContaining({ method: "GET" })
    );
    expect(result.status).toBe(200);
    expect(typeof result.base).toBe("string");
    expect(result.base.length).toBeGreaterThan(0);
  });

  describe("handleResponse branches", () => {
    it("returns a message payload on a 409", async () => {
      fetch.mockResolvedValue(
        mockResponse({ ok: false, status: 409, text: JSON.stringify({ reason: "dup" }) })
      );

      const result = await consentService.update(1);

      expect(result).toEqual({ message: { reason: "dup" }, status: 409 });
    });

    it("logs a server error and returns an error payload on a 500", async () => {
      const consoleSpy = vi.spyOn(console, "error").mockImplementation(() => {});
      fetch.mockResolvedValue(mockResponse({ ok: false, status: 500, text: JSON.stringify({ error: "boom" }) }));

      const result = await consentService.update(1);

      expect(result).toEqual({ status: 500, error: { error: "boom" } });
      expect(consoleSpy).toHaveBeenCalled();
      consoleSpy.mockRestore();
    });

    it("logs and returns an error payload when reading the response fails", async () => {
      const consoleSpy = vi.spyOn(console, "error").mockImplementation(() => {});
      fetch.mockResolvedValue(mockResponse({ text: new Error("bad text") }));

      const result = await consentService.update(1);

      expect(result.error).toBeInstanceOf(Error);
      expect(consoleSpy).toHaveBeenCalled();
      consoleSpy.mockRestore();
    });
  });
});

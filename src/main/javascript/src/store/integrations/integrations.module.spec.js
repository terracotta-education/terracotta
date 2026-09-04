import { beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";

vi.mock("@/services", () => ({
  integrationsService: {
    validateIframeUrl: vi.fn()
  }
}));

import { integrationsService } from "@/services";
import { integrations } from "./integrations.module";

describe("integrations store", () => {
  let store;

  beforeEach(() => {
    setActivePinia(createPinia());
    store = integrations();
    vi.clearAllMocks();
    vi.spyOn(console, "error").mockImplementation(() => {});
  });

  it("starts with iframeUrlValid false", () => {
    expect(store.isIframeUrlValid).toBe(false);
  });

  describe("setIframeValid", () => {
    it("coerces truthy values to true", () => {
      store.setIframeValid("yes");
      expect(store.isIframeUrlValid).toBe(true);
    });

    it("coerces falsy values to false", () => {
      store.setIframeValid(true);
      store.setIframeValid(0);
      expect(store.isIframeUrlValid).toBe(false);
    });
  });

  describe("validateIframeUrl", () => {
    it("sets validation true on a truthy response", async () => {
      integrationsService.validateIframeUrl.mockResolvedValue(true);

      const result = await store.validateIframeUrl("https://example.com");

      expect(integrationsService.validateIframeUrl).toHaveBeenCalledWith(
        "https://example.com"
      );
      expect(store.isIframeUrlValid).toBe(true);
      expect(result).toBe(true);
    });

    it("sets validation false on a falsy response", async () => {
      store.setIframeValid(true);
      integrationsService.validateIframeUrl.mockResolvedValue(false);

      const result = await store.validateIframeUrl("https://example.com");

      expect(store.isIframeUrlValid).toBe(false);
      expect(result).toBe(false);
    });

    it("sets validation false and returns false on error", async () => {
      store.setIframeValid(true);
      integrationsService.validateIframeUrl.mockRejectedValue(
        new Error("boom")
      );

      const result = await store.validateIframeUrl("https://example.com");

      expect(store.isIframeUrlValid).toBe(false);
      expect(result).toBe(false);
      expect(console.error).toHaveBeenCalled();
    });
  });

  describe("resetValidation", () => {
    it("resets validation state", () => {
      store.setIframeValid(true);
      store.resetValidation();

      expect(store.isIframeUrlValid).toBe(false);
    });
  });
});

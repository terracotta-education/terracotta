import { beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";

vi.mock("@/services", () => ({
  consentService: {
    create: vi.fn(),
    getConsentFile: vi.fn()
  }
}));

import { consentService } from "@/services";
import { consent } from "./consent.module";

describe("consent store", () => {
  let store;
  let consoleSpy;

  beforeEach(() => {
    setActivePinia(createPinia());
    store = consent();
    vi.clearAllMocks();
    consoleSpy = vi.spyOn(console, "error").mockImplementation(() => {});
  });

  describe("getters", () => {
    it("starts with no consent", () => {
      expect(store.consentFile).toBeNull();
      expect(store.consentTitle).toBe("");
      expect(store.hasConsent).toBe(false);
    });

    it("hasConsent is true when a file is set", () => {
      store.setConsentFile("file-data");
      expect(store.hasConsent).toBe(true);
    });

    it("hasConsent is true when a title is set", () => {
      store.setConsentTitle("My Consent");
      expect(store.hasConsent).toBe(true);
    });

    it("consent getter returns the full state object", () => {
      store.setConsentTitle("Title");
      expect(store.consent.title).toBe("Title");
    });
  });

  describe("resetConsent", () => {
    it("clears file and title", () => {
      store.setConsentFile("file-data");
      store.setConsentTitle("Title");

      store.resetConsent();

      expect(store.file).toBeNull();
      expect(store.title).toBe("");
    });
  });

  describe("setConsentTitle / setConsentFile", () => {
    it("falls back to empty string / null for falsy input", () => {
      store.setConsentTitle(null);
      expect(store.title).toBe("");

      store.setConsentFile(undefined);
      expect(store.file).toBeNull();
    });

    it("sets the given values", () => {
      store.setConsentTitle("Title");
      store.setConsentFile("file-data");

      expect(store.title).toBe("Title");
      expect(store.file).toBe("file-data");
    });
  });

  describe("createConsent", () => {
    it("returns the response on a 200 status", async () => {
      consentService.create.mockResolvedValue({ status: 200, message: "OK" });

      const result = await store.createConsent([1, "file.pdf", "Title"]);

      expect(consentService.create).toHaveBeenCalledWith(1, "file.pdf", "Title");
      expect(result).toEqual({ status: 200, message: "OK" });
    });

    it("throws and logs when the status is not 200", async () => {
      consentService.create.mockResolvedValue({ status: 400, message: "Bad" });

      await expect(store.createConsent([1, "file.pdf", "Title"])).rejects.toThrow(
        "Consent file upload failed"
      );
      expect(consoleSpy).toHaveBeenCalled();
    });

    it("re-throws and logs when the service rejects", async () => {
      const error = new Error("network down");
      consentService.create.mockRejectedValue(error);

      await expect(store.createConsent([1, "file.pdf", "Title"])).rejects.toThrow(
        "network down"
      );
      expect(consoleSpy).toHaveBeenCalled();
    });
  });

  describe("getConsentFile", () => {
    it("returns the base64 payload on a 200 response", async () => {
      consentService.getConsentFile.mockResolvedValue({
        status: 200,
        base: "base64data"
      });

      const result = await store.getConsentFile(1);

      expect(result).toBe("base64data");
    });

    it("returns null on a 404 response without logging", async () => {
      consentService.getConsentFile.mockResolvedValue({ status: 404 });

      const result = await store.getConsentFile(1);

      expect(result).toBeNull();
      expect(consoleSpy).not.toHaveBeenCalled();
    });

    it("returns null and logs on an unexpected status", async () => {
      consentService.getConsentFile.mockResolvedValue({ status: 500 });

      const result = await store.getConsentFile(1);

      expect(result).toBeNull();
      expect(consoleSpy).toHaveBeenCalled();
    });

    it("returns null and logs on rejection", async () => {
      consentService.getConsentFile.mockRejectedValue(new Error("fail"));

      const result = await store.getConsentFile(1);

      expect(result).toBeNull();
      expect(consoleSpy).toHaveBeenCalled();
    });
  });
});

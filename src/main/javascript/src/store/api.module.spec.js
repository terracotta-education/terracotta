import { beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";

vi.mock("@/services", () => ({
  apiService: {
    getApiToken: vi.fn(),
    refreshToken: vi.fn(),
    reportStep: vi.fn(),
    deepLinkJwt: vi.fn()
  }
}));

import { apiService } from "@/services";
import { api } from "./api.module";

function base64url(obj) {
  return Buffer.from(JSON.stringify(obj))
    .toString("base64")
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/, "");
}

function makeToken(payload) {
  const header = base64url({ alg: "HS256", typ: "JWT" });
  const body = base64url(payload);

  return `${header}.${body}.signature`;
}

const instructorToken = makeToken({
  aud: "https://example.com",
  experimentId: "1",
  lmsAssignmentId: "2",
  consent: "yes",
  userId: "u1",
  roles: ["urn:lti:role/membership#Instructor"]
});

const learnerToken = makeToken({
  aud: "https://example.com",
  experimentId: "3",
  lmsAssignmentId: "4",
  consent: "no",
  userId: "u2",
  roles: ["urn:lti:role/membership#Learner"]
});

describe("api store", () => {
  let store;

  beforeEach(() => {
    setActivePinia(createPinia());
    store = api();
    vi.clearAllMocks();
  });

  it("starts with empty tokens and hasTokens false", () => {
    expect(store.hasTokens).toBe(false);
    expect(store.lti_token).toBe("");
    expect(store.api_token).toBe("");
  });

  it("applyDecodedToken applies fields with fallbacks for missing values", () => {
    store.applyDecodedToken({ roles: [] });

    expect(store.aud).toBe("");
    expect(store.experimentId).toBe("");
    expect(store.assignmentId).toBe("");
    expect(store.consent).toBe("");
    expect(store.userId).toBe("");
    expect(store.userInfo).toBe("Learner");
  });

  it("applyDecodedToken defaults userInfo to Learner when roles is missing", () => {
    expect(() => store.applyDecodedToken({})).not.toThrow();
    expect(store.userInfo).toBe("Learner");
  });

  it("applyDecodedToken derives Instructor userInfo from roles", () => {
    store.applyDecodedToken({
      aud: "aud1",
      experimentId: "e1",
      lmsAssignmentId: "a1",
      consent: "c1",
      userId: "u1",
      roles: ["urn:lti:role/membership#Instructor"]
    });

    expect(store.userInfo).toBe("Instructor");
    expect(store.aud).toBe("aud1");
    expect(store.assignmentId).toBe("a1");
  });

  it("applyDecodedToken derives Learner userInfo when no instructor role matches", () => {
    store.applyDecodedToken({ roles: ["urn:lti:role/membership#Learner"] });

    expect(store.userInfo).toBe("Learner");
  });

  it("setLtiToken decodes and stores the lti token, then fetches api token", async () => {
    apiService.getApiToken.mockResolvedValue(learnerToken);

    const result = await store.setLtiToken(instructorToken);

    expect(store.ltiToken).toBe(instructorToken);
    expect(store.apiToken).toBe(learnerToken);
    // applyDecodedToken runs twice: once for the lti token, then again for
    // the api token's payload, so the final state reflects the api token
    expect(store.experimentId).toBe("3");
    expect(store.userInfo).toBe("Learner");
    expect(store.hasTokens).toBe(true);
    expect(result).toBe(learnerToken);
  });

  it("setLtiToken handles a falsy token without throwing", async () => {
    apiService.getApiToken.mockResolvedValue(null);

    await store.setLtiToken(undefined);

    expect(store.ltiToken).toBe("");
    expect(store.aud).toBe("");
  });

  it("setApiToken sets apiToken and decodes fields when service resolves a string", async () => {
    apiService.getApiToken.mockResolvedValue(instructorToken);

    const result = await store.setApiToken("some-lti-token");

    expect(store.apiToken).toBe(instructorToken);
    expect(store.userId).toBe("u1");
    expect(result).toBe(instructorToken);
  });

  it("setApiToken leaves apiToken unset when service resolves a non-string", async () => {
    apiService.getApiToken.mockResolvedValue(null);

    const result = await store.setApiToken("some-lti-token");

    expect(store.apiToken).toBe("");
    expect(result).toBeNull();
  });

  it("setApiToken returns null and logs on rejection", async () => {
    const consoleSpy = vi.spyOn(console, "error").mockImplementation(() => {});
    apiService.getApiToken.mockRejectedValue(new Error("network"));

    const result = await store.setApiToken("bad-token");

    expect(result).toBeNull();
    expect(consoleSpy).toHaveBeenCalled();
    consoleSpy.mockRestore();
  });

  it("refreshToken sets apiToken on success", async () => {
    apiService.refreshToken.mockResolvedValue(learnerToken);

    const result = await store.refreshToken();

    expect(store.apiToken).toBe(learnerToken);
    expect(result).toBe(learnerToken);
  });

  it("refreshToken returns null and logs on rejection", async () => {
    const consoleSpy = vi.spyOn(console, "error").mockImplementation(() => {});
    apiService.refreshToken.mockRejectedValue(new Error("boom"));

    const result = await store.refreshToken();

    expect(result).toBeNull();
    expect(consoleSpy).toHaveBeenCalled();
    consoleSpy.mockRestore();
  });

  it("reportStep resolves with the service's response", async () => {
    apiService.reportStep.mockResolvedValue({ status: 200 });

    const result = await store.reportStep({
      experimentId: "1",
      step: "STEP",
      parameters: { a: 1 },
      preferLmsChecks: true
    });

    expect(apiService.reportStep).toHaveBeenCalledWith("1", "STEP", { a: 1 }, true);
    expect(result).toEqual({ status: 200 });
  });

  it("reportStep returns null when the service rejects", async () => {
    const consoleSpy = vi.spyOn(console, "error").mockImplementation(() => {});
    apiService.reportStep.mockRejectedValue(new Error("failed step"));

    const result = await store.reportStep({ experimentId: "1", step: "STEP" });

    expect(result).toBeNull();
    consoleSpy.mockRestore();
  });

  it("deepLinkJwt parses a stringified JSON response", async () => {
    apiService.deepLinkJwt.mockResolvedValue(JSON.stringify({ ok: true }));

    const result = await store.deepLinkJwt("123");

    expect(result).toEqual({ ok: true });
  });

  it("deepLinkJwt returns object responses as-is", async () => {
    apiService.deepLinkJwt.mockResolvedValue({ ok: true });

    const result = await store.deepLinkJwt("123");

    expect(result).toEqual({ ok: true });
  });

  it("deepLinkJwt returns null when the service rejects", async () => {
    const consoleSpy = vi.spyOn(console, "error").mockImplementation(() => {});
    apiService.deepLinkJwt.mockRejectedValue(new Error("deep link failed"));

    const result = await store.deepLinkJwt("123");

    expect(result).toBeNull();
    consoleSpy.mockRestore();
  });

  it("setLmsApiOAuthURL sets the url or falls back to empty string", () => {
    store.setLmsApiOAuthURL("https://lms.example.com");
    expect(store.lmsApiOAuthURL).toBe("https://lms.example.com");

    store.setLmsApiOAuthURL(null);
    expect(store.lmsApiOAuthURL).toBe("");
  });

  it("reset clears all fields", async () => {
    apiService.getApiToken.mockResolvedValue(learnerToken);
    await store.setLtiToken(instructorToken);
    store.setLmsApiOAuthURL("https://lms.example.com");

    store.reset();

    expect(store.ltiToken).toBe("");
    expect(store.apiToken).toBe("");
    expect(store.aud).toBe("");
    expect(store.userInfo).toBe("");
    expect(store.experimentId).toBe("");
    expect(store.assignmentId).toBe("");
    expect(store.consent).toBe("");
    expect(store.userId).toBe("");
    expect(store.lmsApiOAuthURL).toBe("");
    expect(store.hasTokens).toBe(false);
  });
});

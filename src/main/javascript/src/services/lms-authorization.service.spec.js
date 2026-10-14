import { beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";

import { api } from "@/store/api.module";
import { lmsAuthorizationService } from "./lms-authorization.service";

describe("lmsAuthorizationService", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    api().aud = "https://example.com";
    api().apiToken = "test-token";
    vi.stubGlobal("fetch", vi.fn());
    vi.spyOn(console, "error").mockImplementation(() => {});
  });

  it("asks the server whether the instructor is authorized", async () => {
    fetch.mockResolvedValue({ ok: true, json: () => Promise.resolve({ authorized: true }) });

    const result = await lmsAuthorizationService.getAuthorization();

    expect(fetch).toHaveBeenCalledWith(
      "https://example.com/api/lms/authorization",
      expect.objectContaining({ method: "GET" })
    );
    expect(result).toEqual({ authorized: true });
  });

  it("reports not authorized when the server says so", async () => {
    fetch.mockResolvedValue({ ok: true, json: () => Promise.resolve({ authorized: false }) });

    expect(await lmsAuthorizationService.getAuthorization()).toEqual({ authorized: false });
  });

  it("reports not authorized on an error response", async () => {
    fetch.mockResolvedValue({ ok: false, status: 500 });

    expect(await lmsAuthorizationService.getAuthorization()).toEqual({ authorized: false });
  });

  it("reports not authorized when the request fails", async () => {
    fetch.mockRejectedValue(new Error("offline"));

    expect(await lmsAuthorizationService.getAuthorization()).toEqual({ authorized: false });
  });
});

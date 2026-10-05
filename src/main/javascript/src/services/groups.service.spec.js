import { beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";

import { api } from "@/store/api.module";
import { groupsService } from "./groups.service";

function mockResponse({ ok = true, status = 200, text = "" } = {}) {
  return {
    ok,
    status,
    text: () => (text instanceof Error ? Promise.reject(text) : Promise.resolve(text))
  };
}

describe("groupsService", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    api().aud = "https://example.com";
    api().apiToken = "test-token";
    vi.stubGlobal("fetch", vi.fn());
    vi.clearAllMocks();
  });

  it("createAndAssignGroups POSTs to create and assign groups", async () => {
    fetch.mockResolvedValue(mockResponse({ text: JSON.stringify([{ groupId: 1 }]) }));

    const result = await groupsService.createAndAssignGroups(1);

    expect(fetch).toHaveBeenCalledWith(
      "https://example.com/api/experiments/1/groups/create",
      expect.objectContaining({ method: "POST" })
    );
    expect(result).toEqual([{ groupId: 1 }]);
  });

  it("omits Authorization header when no api token is set", async () => {
    api().apiToken = "";
    fetch.mockResolvedValue(mockResponse({ text: JSON.stringify([]) }));

    await groupsService.createAndAssignGroups(1);

    const [, options] = fetch.mock.calls[0];
    expect(options.headers.Authorization).toBeUndefined();
  });

  describe("handleResponse branches", () => {
    it("returns [] on a 204", async () => {
      fetch.mockResolvedValue(mockResponse({ status: 204, text: "" }));

      const result = await groupsService.createAndAssignGroups(1);

      expect(result).toEqual([]);
    });

    it("logs a server error and returns an error payload on a 500", async () => {
      const consoleSpy = vi.spyOn(console, "error").mockImplementation(() => {});
      fetch.mockResolvedValue(
        mockResponse({ ok: false, status: 500, text: JSON.stringify({ error: "boom" }) })
      );

      const result = await groupsService.createAndAssignGroups(1);

      expect(result).toEqual({ status: 500, error: { error: "boom" } });
      expect(consoleSpy).toHaveBeenCalled();
      consoleSpy.mockRestore();
    });

    it("warns and returns an error payload on a 404", async () => {
      const warnSpy = vi.spyOn(console, "warn").mockImplementation(() => {});
      fetch.mockResolvedValue(mockResponse({ ok: false, status: 404, text: "" }));

      const result = await groupsService.createAndAssignGroups(1);

      expect(result.status).toBe(404);
      expect(warnSpy).toHaveBeenCalled();
      warnSpy.mockRestore();
    });

    it("returns an error payload without logging on other non-ok statuses", async () => {
      fetch.mockResolvedValue(mockResponse({ ok: false, status: 400, text: "" }));

      const result = await groupsService.createAndAssignGroups(1);

      expect(result.status).toBe(400);
    });

    it("logs and returns an error payload when reading the response fails", async () => {
      const consoleSpy = vi.spyOn(console, "error").mockImplementation(() => {});
      fetch.mockResolvedValue(mockResponse({ text: new Error("bad text") }));

      const result = await groupsService.createAndAssignGroups(1);

      expect(result.error).toBeInstanceOf(Error);
      expect(consoleSpy).toHaveBeenCalled();
      consoleSpy.mockRestore();
    });
  });
});

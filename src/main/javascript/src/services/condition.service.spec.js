import { beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";

import { api } from "@/store/api.module";
import { conditionService } from "./condition.service";

function mockResponse({ ok = true, status = 200, text = "" } = {}) {
  return {
    ok,
    status,
    text: () => (text instanceof Error ? Promise.reject(text) : Promise.resolve(text))
  };
}

describe("conditionService", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    api().aud = "https://example.com";
    api().apiToken = "test-token";
    vi.stubGlobal("fetch", vi.fn());
    vi.clearAllMocks();
  });

  it("create POSTs a new condition for an experiment", async () => {
    fetch.mockResolvedValue(mockResponse({ text: JSON.stringify({ conditionId: 1 }) }));

    const result = await conditionService.create(1);

    expect(fetch).toHaveBeenCalledWith(
      "https://example.com/api/experiments/1/conditions",
      expect.objectContaining({ method: "POST" })
    );
    expect(result).toEqual({ conditionId: 1 });
  });

  it("update PUTs the condition body", async () => {
    fetch.mockResolvedValue(mockResponse({ text: JSON.stringify({ conditionId: 1, name: "Updated" }) }));

    const result = await conditionService.update({ experimentId: 1, conditionId: 1, name: "Updated" });

    expect(fetch).toHaveBeenCalledWith(
      "https://example.com/api/experiments/1/conditions/1",
      expect.objectContaining({
        method: "PUT",
        body: JSON.stringify({ experimentId: 1, conditionId: 1, name: "Updated" })
      })
    );
    expect(result).toEqual({ conditionId: 1, name: "Updated" });
  });

  it("updateAll PUTs the full condition list keyed off the first entry's experimentId", async () => {
    fetch.mockResolvedValue(mockResponse({ text: JSON.stringify([{ conditionId: 1 }]) }));

    const conditions = [{ experimentId: 1, conditionId: 1 }, { experimentId: 1, conditionId: 2 }];
    await conditionService.updateAll(conditions);

    expect(fetch).toHaveBeenCalledWith(
      "https://example.com/api/experiments/1/conditions",
      expect.objectContaining({ method: "PUT", body: JSON.stringify(conditions) })
    );
  });

  it("delete DELETEs the condition", async () => {
    fetch.mockResolvedValue(mockResponse({ status: 204, text: "" }));

    const result = await conditionService.delete({ experimentId: 1, conditionId: 1 });

    expect(fetch).toHaveBeenCalledWith(
      "https://example.com/api/experiments/1/conditions/1",
      expect.objectContaining({ method: "DELETE" })
    );
    expect(result).toEqual([]);
  });

  describe("handleResponse branches", () => {
    it("returns a message payload on a 409", async () => {
      fetch.mockResolvedValue(
        mockResponse({ ok: false, status: 409, text: JSON.stringify({ message: "dup" }) })
      );

      const result = await conditionService.create(1);

      expect(result).toEqual({ message: { message: "dup" }, status: 409 });
    });

    it("returns an error payload on a non-ok response without logging", async () => {
      fetch.mockResolvedValue(
        mockResponse({ ok: false, status: 500, text: JSON.stringify({ error: "boom" }) })
      );

      const result = await conditionService.create(1);

      expect(result).toEqual({ data: { error: "boom" }, status: 500, error: { error: "boom" } });
    });

    it("returns the raw response when there is no body on success", async () => {
      const response = mockResponse({ status: 200, text: "" });
      fetch.mockResolvedValue(response);

      const result = await conditionService.create(1);

      expect(result).toBe(response);
    });

    it("logs and returns an error payload when reading the response fails", async () => {
      const consoleSpy = vi.spyOn(console, "error").mockImplementation(() => {});
      fetch.mockResolvedValue(mockResponse({ text: new Error("bad text") }));

      const result = await conditionService.create(1);

      expect(result.error).toBeInstanceOf(Error);
      expect(consoleSpy).toHaveBeenCalled();
      consoleSpy.mockRestore();
    });
  });
});

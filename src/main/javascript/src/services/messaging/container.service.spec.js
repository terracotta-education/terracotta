import { beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";

import { api } from "@/store/api.module";
import { messageContainerService } from "./container.service";

function mockResponse({ ok = true, status = 200, text = "" } = {}) {
  return {
    ok,
    status,
    text: () => (text instanceof Error ? Promise.reject(text) : Promise.resolve(text))
  };
}

describe("messageContainerService", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    api().aud = "https://example.com";
    api().apiToken = "test-token";
    vi.stubGlobal("fetch", vi.fn());
    vi.clearAllMocks();
  });

  it("getAll fetches the containers for an exposure", async () => {
    fetch.mockResolvedValue(mockResponse({ text: JSON.stringify([{ containerId: 1 }]) }));

    const result = await messageContainerService.getAll(1, 2);

    expect(fetch).toHaveBeenCalledWith(
      "https://example.com/api/experiments/1/exposures/2/messaging/container",
      expect.objectContaining({ method: "GET" })
    );
    expect(result).toEqual([{ containerId: 1 }]);
  });

  it("create POSTs a new container with the single flag", async () => {
    fetch.mockResolvedValue(mockResponse({ text: JSON.stringify({ containerId: 1 }) }));

    const result = await messageContainerService.create(1, 2, true);

    expect(fetch).toHaveBeenCalledWith(
      "https://example.com/api/experiments/1/exposures/2/messaging/container?single=true",
      expect.objectContaining({ method: "POST" })
    );
    expect(result).toEqual({ containerId: 1 });
  });

  it("update PUTs the container DTO", async () => {
    fetch.mockResolvedValue(mockResponse({ text: JSON.stringify({ containerId: 1, name: "Updated" }) }));

    const result = await messageContainerService.update(1, 2, 3, { name: "Updated" });

    expect(fetch).toHaveBeenCalledWith(
      "https://example.com/api/experiments/1/exposures/2/messaging/container/3",
      expect.objectContaining({ method: "PUT", body: JSON.stringify({ name: "Updated" }) })
    );
    expect(result).toEqual({ containerId: 1, name: "Updated" });
  });

  it("updateAll PUTs a spread copy of the container list", async () => {
    fetch.mockResolvedValue(mockResponse({ text: JSON.stringify([{ containerId: 1 }]) }));

    await messageContainerService.updateAll(1, 2, [{ containerId: 1 }]);

    expect(fetch).toHaveBeenCalledWith(
      "https://example.com/api/experiments/1/exposures/2/messaging/container",
      expect.objectContaining({ method: "PUT", body: JSON.stringify([{ containerId: 1 }]) })
    );
  });

  it("send POSTs to send the container", async () => {
    fetch.mockResolvedValue(mockResponse({ status: 204, text: "" }));

    const result = await messageContainerService.send(1, 2, 3);

    expect(fetch).toHaveBeenCalledWith(
      "https://example.com/api/experiments/1/exposures/2/messaging/container/3/send",
      expect.objectContaining({ method: "POST" })
    );
    expect(result).toEqual([]);
  });

  it("deleteContainer removes the container", async () => {
    fetch.mockResolvedValue(mockResponse({ status: 204, text: "" }));

    const result = await messageContainerService.deleteContainer(1, 2, 3);

    expect(fetch).toHaveBeenCalledWith(
      "https://example.com/api/experiments/1/exposures/2/messaging/container/3",
      expect.objectContaining({ method: "DELETE" })
    );
    expect(result).toEqual([]);
  });

  it("move POSTs the reorder DTO", async () => {
    fetch.mockResolvedValue(mockResponse({ text: JSON.stringify({ containerId: 1 }) }));

    await messageContainerService.move(1, 2, 3, { position: 2 });

    expect(fetch).toHaveBeenCalledWith(
      "https://example.com/api/experiments/1/exposures/2/messaging/container/3/move",
      expect.objectContaining({ method: "POST", body: JSON.stringify({ position: 2 }) })
    );
  });

  it("duplicate POSTs to copy the container", async () => {
    fetch.mockResolvedValue(mockResponse({ text: JSON.stringify({ containerId: 2 }) }));

    const result = await messageContainerService.duplicate(1, 2, 3);

    expect(fetch).toHaveBeenCalledWith(
      "https://example.com/api/experiments/1/exposures/2/messaging/container/3/duplicate",
      expect.objectContaining({ method: "POST" })
    );
    expect(result).toEqual({ containerId: 2 });
  });

  describe("handleResponse branches", () => {
    it("logs a server error and returns an error payload on a 500", async () => {
      const consoleSpy = vi.spyOn(console, "error").mockImplementation(() => {});
      fetch.mockResolvedValue(mockResponse({ ok: false, status: 500, text: JSON.stringify({ error: "boom" }) }));

      const result = await messageContainerService.getAll(1, 2);

      expect(result).toEqual({ status: 500, error: { error: "boom" } });
      expect(consoleSpy).toHaveBeenCalled();
      consoleSpy.mockRestore();
    });

    it("warns and returns an error payload on a 404", async () => {
      const warnSpy = vi.spyOn(console, "warn").mockImplementation(() => {});
      fetch.mockResolvedValue(mockResponse({ ok: false, status: 404, text: "" }));

      const result = await messageContainerService.getAll(1, 2);

      expect(result.status).toBe(404);
      expect(warnSpy).toHaveBeenCalled();
      warnSpy.mockRestore();
    });

    it("logs and returns an error payload when reading the response fails", async () => {
      const consoleSpy = vi.spyOn(console, "error").mockImplementation(() => {});
      fetch.mockResolvedValue(mockResponse({ text: new Error("bad text") }));

      const result = await messageContainerService.getAll(1, 2);

      expect(result.error).toBeInstanceOf(Error);
      expect(consoleSpy).toHaveBeenCalled();
      consoleSpy.mockRestore();
    });
  });
});

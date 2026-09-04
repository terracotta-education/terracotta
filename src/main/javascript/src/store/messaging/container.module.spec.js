import { beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";

vi.mock("@/services", () => ({
  messageContainerService: {
    get: vi.fn(),
    getAll: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    updateAll: vi.fn(),
    send: vi.fn(),
    deleteContainer: vi.fn(),
    move: vi.fn(),
    duplicate: vi.fn()
  }
}));

import { messageContainerService } from "@/services";
import { container } from "./container.module";

describe("messaging container store", () => {
  let store;

  beforeEach(() => {
    setActivePinia(createPinia());
    store = container();
    vi.clearAllMocks();
  });

  it("starts empty", () => {
    expect(store.hasMessageContainers).toBe(false);
    expect(store.messageContainers).toEqual([]);
    expect(store.messageContainer).toBeNull();
  });

  describe("get", () => {
    it("sets messageContainer and upserts it into the list on success", async () => {
      const response = { id: 1, name: "c1" };
      messageContainerService.get.mockResolvedValue(response);

      const result = await store.get(["a", "b"]);

      expect(messageContainerService.get).toHaveBeenCalledWith("a", "b");
      expect(result).toEqual(response);
      expect(store.messageContainer).toEqual(response);
      expect(store.messageContainers).toEqual([response]);
    });

    it("returns null on error", async () => {
      messageContainerService.get.mockRejectedValue(new Error("fail"));

      const result = await store.get(["a"]);

      expect(result).toBeNull();
      expect(store.messageContainer).toBeNull();
    });
  });

  describe("getAll", () => {
    it("upserts all returned containers", async () => {
      const response = [{ id: 1 }, { id: 2 }];
      messageContainerService.getAll.mockResolvedValue(response);

      const result = await store.getAll(["a"]);

      expect(result).toEqual(response);
      expect(store.messageContainers).toEqual(response);
    });

    it("falls back to [] when response is not an array", async () => {
      messageContainerService.getAll.mockResolvedValue(null);

      const result = await store.getAll(["a"]);

      expect(result).toEqual([]);
      expect(store.messageContainers).toEqual([]);
    });

    it("returns [] on error", async () => {
      messageContainerService.getAll.mockRejectedValue(new Error("fail"));

      const result = await store.getAll(["a"]);

      expect(result).toEqual([]);
    });
  });

  describe("create", () => {
    it("sets messageContainer and upserts on success", async () => {
      const response = { id: 3 };
      messageContainerService.create.mockResolvedValue(response);

      const result = await store.create(["a"]);

      expect(result).toEqual(response);
      expect(store.messageContainer).toEqual(response);
      expect(store.messageContainers).toEqual([response]);
    });

    it("returns null on error", async () => {
      messageContainerService.create.mockRejectedValue(new Error("fail"));

      const result = await store.create(["a"]);

      expect(result).toBeNull();
    });
  });

  describe("update", () => {
    it("sets messageContainer, replacing the existing entry in the list", async () => {
      store.upsertMessageContainers([{ id: 1, name: "old" }]);
      const response = { id: 1, name: "new" };
      messageContainerService.update.mockResolvedValue(response);

      const result = await store.update(["a"]);

      expect(result).toEqual(response);
      expect(store.messageContainer).toEqual(response);
      expect(store.messageContainers).toEqual([response]);
    });

    it("returns null on error", async () => {
      messageContainerService.update.mockRejectedValue(new Error("fail"));

      const result = await store.update(["a"]);

      expect(result).toBeNull();
    });
  });

  describe("updateAll", () => {
    it("replaces the entire messageContainers list with the response", async () => {
      store.upsertMessageContainers([{ id: 1 }]);
      const response = [{ id: 2 }, { id: 3 }];
      messageContainerService.updateAll.mockResolvedValue(response);

      const result = await store.updateAll(["a"]);

      expect(result).toEqual(response);
      expect(store.messageContainers).toEqual(response);
    });

    it("falls back to [] when response is not an array", async () => {
      store.upsertMessageContainers([{ id: 1 }]);
      messageContainerService.updateAll.mockResolvedValue(undefined);

      const result = await store.updateAll(["a"]);

      expect(result).toEqual([]);
      expect(store.messageContainers).toEqual([]);
    });

    it("returns [] on error", async () => {
      messageContainerService.updateAll.mockRejectedValue(new Error("fail"));

      const result = await store.updateAll(["a"]);

      expect(result).toEqual([]);
    });
  });

  describe("send", () => {
    it("sets messageContainer and upserts on success", async () => {
      const response = { id: 4 };
      messageContainerService.send.mockResolvedValue(response);

      const result = await store.send(["a"]);

      expect(result).toEqual(response);
      expect(store.messageContainer).toEqual(response);
      expect(store.messageContainers).toEqual([response]);
    });

    it("returns null on error", async () => {
      messageContainerService.send.mockRejectedValue(new Error("fail"));

      const result = await store.send(["a"]);

      expect(result).toBeNull();
    });
  });

  describe("deleteContainer", () => {
    it("removes the returned container from the list", async () => {
      store.upsertMessageContainers([{ id: 1 }, { id: 2 }]);
      const response = { id: 1 };
      messageContainerService.deleteContainer.mockResolvedValue(response);

      const result = await store.deleteContainer(["a"]);

      expect(result).toEqual(response);
      expect(store.messageContainers).toEqual([{ id: 2 }]);
    });

    it("returns null on error", async () => {
      messageContainerService.deleteContainer.mockRejectedValue(
        new Error("fail")
      );

      const result = await store.deleteContainer(["a"]);

      expect(result).toBeNull();
    });
  });

  describe("move", () => {
    it("upserts the moved container", async () => {
      const response = { id: 1, position: 2 };
      messageContainerService.move.mockResolvedValue(response);

      const result = await store.move(["a"]);

      expect(result).toEqual(response);
      expect(store.messageContainers).toEqual([response]);
    });

    it("returns null on error", async () => {
      messageContainerService.move.mockRejectedValue(new Error("fail"));

      const result = await store.move(["a"]);

      expect(result).toBeNull();
    });
  });

  describe("duplicate", () => {
    it("upserts the duplicated container", async () => {
      const response = { id: 5 };
      messageContainerService.duplicate.mockResolvedValue(response);

      const result = await store.duplicate(["a"]);

      expect(result).toEqual(response);
      expect(store.messageContainers).toEqual([response]);
    });

    it("returns null on error", async () => {
      messageContainerService.duplicate.mockRejectedValue(new Error("fail"));

      const result = await store.duplicate(["a"]);

      expect(result).toBeNull();
    });
  });

  describe("upsertMessageContainers", () => {
    it("does nothing when given a non-array", () => {
      store.upsertMessageContainers([{ id: 1 }]);

      store.upsertMessageContainers(null);
      store.upsertMessageContainers("not-array");

      expect(store.messageContainers).toEqual([{ id: 1 }]);
    });

    it("filters out falsy entries", () => {
      store.upsertMessageContainers([null, { id: 1 }, undefined]);

      expect(store.messageContainers).toEqual([{ id: 1 }]);
    });
  });

  describe("deleteMessageContainers", () => {
    it("does nothing when given a non-array", () => {
      store.upsertMessageContainers([{ id: 1 }]);

      store.deleteMessageContainers(null);

      expect(store.messageContainers).toEqual([{ id: 1 }]);
    });

    it("ignores containers that aren't present in the list", () => {
      store.upsertMessageContainers([{ id: 1 }]);

      store.deleteMessageContainers([{ id: 99 }]);

      expect(store.messageContainers).toEqual([{ id: 1 }]);
    });
  });

  it("reset clears all state", () => {
    store.upsertMessageContainers([{ id: 1 }]);
    store.messageContainer = { id: 1 };

    store.reset();

    expect(store.messageContainers).toEqual([]);
    expect(store.messageContainer).toBeNull();
  });
});

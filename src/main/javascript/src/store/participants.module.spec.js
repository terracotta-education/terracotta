import { beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";

vi.mock("@/services", () => ({
  participantService: {
    getAll: vi.fn(),
    updateParticipants: vi.fn(),
    updateParticipant: vi.fn()
  }
}));

import { participantService } from "@/services";
import { participants } from "./participants.module";

describe("participants store", () => {
  let store;

  beforeEach(() => {
    setActivePinia(createPinia());
    store = participants();
    vi.clearAllMocks();
  });

  it("starts empty", () => {
    expect(store.hasParticipants).toBe(false);
    expect(store.participants).toEqual([]);
    expect(store.participant).toBeNull();
    expect(store.groups).toEqual([]);
  });

  describe("fetchParticipants", () => {
    it("sets participants on success", async () => {
      const list = [{ participantId: 1 }, { participantId: 2 }];
      participantService.getAll.mockResolvedValue(list);

      const result = await store.fetchParticipants([1, true]);

      expect(participantService.getAll).toHaveBeenCalledWith(1, true);
      expect(result).toEqual(list);
      expect(store.participants).toEqual(list);
      expect(store.hasParticipants).toBe(true);
    });

    it("falls back to empty array when data is falsy", async () => {
      participantService.getAll.mockResolvedValue(null);

      const result = await store.fetchParticipants([1]);

      expect(result).toEqual([]);
      expect(store.participants).toEqual([]);
    });

    it("clears participants and returns [] on error", async () => {
      store.setParticipants([{ participantId: 1 }]);
      participantService.getAll.mockRejectedValue(new Error("fail"));

      const result = await store.fetchParticipants([1]);

      expect(result).toEqual([]);
      expect(store.participants).toEqual([]);
    });
  });

  it("setParticipantsGroup / setParticipants set the full list", () => {
    const list = [{ participantId: 5 }];
    store.setParticipants(list);

    expect(store.participants).toEqual(list);
  });

  describe("updateParticipants", () => {
    it("maps participants to request body and calls the service", async () => {
      store.setParticipants([
        { participantId: 1, consent: true, dropped: false, groupId: 10, extra: "x" }
      ]);
      participantService.updateParticipants.mockResolvedValue({ status: 200 });

      const result = await store.updateParticipants(99);

      expect(participantService.updateParticipants).toHaveBeenCalledWith(99, [
        { participantId: 1, consent: true, dropped: false, groupId: 10 }
      ]);
      expect(result).toEqual({ status: 200 });
    });

    it("returns null on error", async () => {
      participantService.updateParticipants.mockRejectedValue(new Error("fail"));

      const result = await store.updateParticipants(99);

      expect(result).toBeNull();
    });
  });

  describe("updateParticipant", () => {
    it("updates single participant, sets participant and upserts into list", async () => {
      store.setParticipants([{ participantId: 1, consent: false }]);
      participantService.updateParticipant.mockResolvedValue({ status: 200 });

      const participantData = { participantId: 1, consent: true };
      const result = await store.updateParticipant({
        experimentId: 5,
        participantData
      });

      expect(participantService.updateParticipant).toHaveBeenCalledWith(
        5,
        participantData
      );
      expect(result).toEqual({ status: 200 });
      expect(store.participant).toEqual(participantData);
      expect(store.participants).toEqual([participantData]);
    });

    it("pushes a new participant when it doesn't already exist", async () => {
      participantService.updateParticipant.mockResolvedValue({ status: 200 });
      const participantData = { participantId: 2, consent: true };

      await store.updateParticipant({ experimentId: 5, participantData });

      expect(store.participants).toEqual([participantData]);
    });

    it("returns null on error", async () => {
      participantService.updateParticipant.mockRejectedValue(new Error("fail"));

      const result = await store.updateParticipant({
        experimentId: 5,
        participantData: { participantId: 1 }
      });

      expect(result).toBeNull();
    });
  });

  describe("upsertParticipant", () => {
    it("does nothing when participantData has no participantId", () => {
      store.setParticipants([{ participantId: 1 }]);

      store.upsertParticipant(null);
      store.upsertParticipant({});

      expect(store.participants).toEqual([{ participantId: 1 }]);
    });

    it("matches participantId across string/number types via parseInt", () => {
      store.setParticipants([{ participantId: "1" }]);

      store.upsertParticipant({ participantId: 1, consent: true });

      expect(store.participants).toEqual([{ participantId: 1, consent: true }]);
    });
  });

  it("resetParticipants clears all state", () => {
    store.setParticipants([{ participantId: 1 }]);
    store.participant = { participantId: 1 };
    store.groups = [{ id: 1 }];

    store.resetParticipants();

    expect(store.participants).toEqual([]);
    expect(store.participant).toBeNull();
    expect(store.groups).toEqual([]);
  });
});

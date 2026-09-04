import { beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";

vi.mock("@/services", () => ({
  previewService: {
    treatmentPreview: vi.fn()
  }
}));

import { previewService } from "@/services";
import { preview } from "./preview.module";

describe("preview store", () => {
  let store;

  beforeEach(() => {
    setActivePinia(createPinia());
    store = preview();
    vi.clearAllMocks();
  });

  it("starts with no preview", () => {
    expect(store.hasPreview).toBe(false);
    expect(store.treatmentPreview).toBeNull();
    expect(store.isLoading).toBe(false);
    expect(store.error).toBeNull();
  });

  it("treatment sets the treatment preview data on success", async () => {
    previewService.treatmentPreview.mockResolvedValue({
      data: { id: 1, html: "<p>preview</p>" }
    });

    const result = await store.treatment([1, 2, 3]);

    expect(previewService.treatmentPreview).toHaveBeenCalledWith(1, 2, 3);
    expect(result).toEqual({ id: 1, html: "<p>preview</p>" });
    expect(store.treatmentPreview).toEqual({ id: 1, html: "<p>preview</p>" });
    expect(store.hasPreview).toBe(true);
    expect(store.isLoading).toBe(false);
    expect(store.error).toBeNull();
  });

  it("treatment falls back to null when response has no data", async () => {
    previewService.treatmentPreview.mockResolvedValue({});

    const result = await store.treatment([1]);

    expect(result).toBeNull();
    expect(store.treatmentPreview).toBeNull();
    expect(store.hasPreview).toBe(false);
  });

  it("treatment sets error state and returns null on rejection", async () => {
    const error = new Error("network down");
    previewService.treatmentPreview.mockRejectedValue(error);

    const result = await store.treatment([1]);

    expect(result).toBeNull();
    expect(store.error).toBe(error);
    expect(store.treatmentPreview).toBeNull();
    expect(store.isLoading).toBe(false);
  });

  it("reset clears all state", () => {
    store.treatmentPreview = { id: 1 };
    store.isLoading = true;
    store.error = new Error("boom");

    store.reset();

    expect(store.treatmentPreview).toBeNull();
    expect(store.isLoading).toBe(false);
    expect(store.error).toBeNull();
  });
});

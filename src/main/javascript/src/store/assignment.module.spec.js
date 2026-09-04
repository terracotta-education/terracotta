import { beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";

vi.mock("@/services", () => ({
  assignmentService: {
    updateAssignment: vi.fn(),
    updateAssignments: vi.fn(),
    fetchAssignment: vi.fn(),
    fetchAssignmentsByExposure: vi.fn(),
    deleteAssignment: vi.fn(),
    duplicateAssignment: vi.fn(),
    create: vi.fn(),
    moveAssignment: vi.fn()
  }
}));

import { assignmentService } from "@/services";
import { assignment } from "./assignment.module";

describe("assignment store", () => {
  let store;
  let consoleSpy;

  beforeEach(() => {
    setActivePinia(createPinia());
    store = assignment();
    vi.clearAllMocks();
    consoleSpy = vi.spyOn(console, "error").mockImplementation(() => {});
  });

  it("hasAssignments reflects whether assignments exist", () => {
    expect(store.hasAssignments).toBe(false);
    store.upsertAssignments([{ assignmentId: 1 }]);
    expect(store.hasAssignments).toBe(true);
  });

  describe("updateAssignment", () => {
    it("sets assignment and upserts on success, defaulting status to 200", async () => {
      assignmentService.updateAssignment.mockResolvedValue({ assignmentId: 1 });

      const result = await store.updateAssignment([1, 2, 3, {}]);

      expect(result).toEqual({ status: 200, data: { assignmentId: 1 } });
      expect(store.assignment).toEqual({ assignmentId: 1 });
      expect(store.assignments).toContainEqual({ assignmentId: 1 });
    });

    it("uses the response status when provided", async () => {
      assignmentService.updateAssignment.mockResolvedValue({
        assignmentId: 1,
        status: 202
      });

      const result = await store.updateAssignment([1, 2, 3, {}]);

      expect(result.status).toBe(202);
    });

    it("returns null and logs on rejection", async () => {
      assignmentService.updateAssignment.mockRejectedValue(new Error("fail"));

      const result = await store.updateAssignment([1, 2, 3, {}]);

      expect(result).toBeNull();
      expect(consoleSpy).toHaveBeenCalled();
    });
  });

  describe("saveAssignmentOrder", () => {
    it("upserts and returns the assignments array on success", async () => {
      assignmentService.updateAssignments.mockResolvedValue([
        { assignmentId: 1 },
        { assignmentId: 2 }
      ]);

      const result = await store.saveAssignmentOrder([1, 2, []]);

      expect(result).toHaveLength(2);
      expect(store.assignments).toHaveLength(2);
    });

    it("treats a non-array response as an empty list", async () => {
      assignmentService.updateAssignments.mockResolvedValue({ error: "bad" });

      const result = await store.saveAssignmentOrder([1, 2, []]);

      expect(result).toEqual([]);
      expect(store.assignments).toEqual([]);
    });

    it("returns [] and logs on rejection", async () => {
      assignmentService.updateAssignments.mockRejectedValue(new Error("fail"));

      const result = await store.saveAssignmentOrder([1, 2, []]);

      expect(result).toEqual([]);
      expect(consoleSpy).toHaveBeenCalled();
    });
  });

  describe("fetchAssignment", () => {
    it("sets assignment and upserts on success", async () => {
      assignmentService.fetchAssignment.mockResolvedValue({ assignmentId: 5 });

      const result = await store.fetchAssignment([1, 2, 3]);

      expect(result).toEqual({ assignmentId: 5 });
      expect(store.assignment).toEqual({ assignmentId: 5 });
    });

    it("returns null and logs on rejection", async () => {
      assignmentService.fetchAssignment.mockRejectedValue(new Error("fail"));

      const result = await store.fetchAssignment([1, 2, 3]);

      expect(result).toBeNull();
      expect(consoleSpy).toHaveBeenCalled();
    });
  });

  describe("fetchAssignmentsByExposure", () => {
    it("upserts and returns assignments when an array is returned", async () => {
      assignmentService.fetchAssignmentsByExposure.mockResolvedValue([
        { assignmentId: 1 }
      ]);

      const result = await store.fetchAssignmentsByExposure([1, 2]);

      expect(result).toEqual([{ assignmentId: 1 }]);
      expect(store.assignments).toEqual([{ assignmentId: 1 }]);
    });

    it("upserts nothing when a non-array is returned", async () => {
      assignmentService.fetchAssignmentsByExposure.mockResolvedValue(null);

      const result = await store.fetchAssignmentsByExposure([1, 2]);

      expect(result).toBeNull();
      expect(store.assignments).toEqual([]);
    });

    it("returns [] and logs on rejection", async () => {
      assignmentService.fetchAssignmentsByExposure.mockRejectedValue(
        new Error("fail")
      );

      const result = await store.fetchAssignmentsByExposure([1, 2]);

      expect(result).toEqual([]);
      expect(consoleSpy).toHaveBeenCalled();
    });
  });

  describe("deleteAssignment", () => {
    beforeEach(() => {
      store.upsertAssignments([{ assignmentId: 1 }, { assignmentId: 2 }]);
    });

    it("removes the matching assignment on a 200 response", async () => {
      assignmentService.deleteAssignment.mockResolvedValue({ status: 200 });

      const result = await store.deleteAssignment([1, 2, 2]);

      expect(store.assignments.map(a => a.assignmentId)).toEqual([1]);
      expect(result).toEqual({ status: 200, data: null });
    });

    it("returns the raw response on a non-200 status", async () => {
      assignmentService.deleteAssignment.mockResolvedValue({ status: 400 });

      const result = await store.deleteAssignment([1, 2, 2]);

      expect(result).toEqual({ status: 400 });
      expect(store.assignments).toHaveLength(2);
    });

    it("returns null and logs on rejection", async () => {
      assignmentService.deleteAssignment.mockRejectedValue(new Error("fail"));

      const result = await store.deleteAssignment([1, 2, 2]);

      expect(result).toBeNull();
      expect(consoleSpy).toHaveBeenCalled();
    });
  });

  describe("duplicateAssignment", () => {
    it("sets assignment and upserts when the response has an assignmentId", async () => {
      assignmentService.duplicateAssignment.mockResolvedValue({
        assignmentId: 9
      });

      const result = await store.duplicateAssignment([1, 2, 3]);

      expect(result).toEqual({ status: 201, data: { assignmentId: 9 } });
      expect(store.assignment).toEqual({ assignmentId: 9 });
    });

    it("returns the raw response when there is no assignmentId", async () => {
      assignmentService.duplicateAssignment.mockResolvedValue({ error: "bad" });

      const result = await store.duplicateAssignment([1, 2, 3]);

      expect(result).toEqual({ error: "bad" });
      expect(store.assignment).toBeNull();
    });

    it("returns null and logs on rejection", async () => {
      assignmentService.duplicateAssignment.mockRejectedValue(new Error("fail"));

      const result = await store.duplicateAssignment([1, 2, 3]);

      expect(result).toBeNull();
      expect(consoleSpy).toHaveBeenCalled();
    });
  });

  describe("createAssignment", () => {
    it("sets assignment and upserts when the response has an assignmentId", async () => {
      assignmentService.create.mockResolvedValue({ assignmentId: 11 });

      const result = await store.createAssignment([1, 2, {}, 0]);

      expect(result).toEqual({ status: 201, data: { assignmentId: 11 } });
      expect(store.assignment).toEqual({ assignmentId: 11 });
    });

    it("returns the raw response when there is no assignmentId", async () => {
      assignmentService.create.mockResolvedValue({ error: "bad" });

      const result = await store.createAssignment([1, 2, {}, 0]);

      expect(result).toEqual({ error: "bad" });
    });

    it("returns null and logs on rejection", async () => {
      assignmentService.create.mockRejectedValue(new Error("fail"));

      const result = await store.createAssignment([1, 2, {}, 0]);

      expect(result).toBeNull();
      expect(consoleSpy).toHaveBeenCalled();
    });
  });

  describe("moveAssignment", () => {
    beforeEach(() => {
      store.upsertAssignments([{ assignmentId: 1 }, { assignmentId: 2 }]);
    });

    it("removes the assignment from the current list and upserts the moved assignment", async () => {
      assignmentService.moveAssignment.mockResolvedValue({ assignmentId: 2, exposureId: 99 });

      const result = await store.moveAssignment([1, 2, 2, {}]);

      expect(store.assignments.map(a => a.assignmentId)).toEqual([1, 2]);
      expect(result).toEqual({
        status: 201,
        data: { assignmentId: 2, exposureId: 99 }
      });
      expect(store.assignment).toEqual({ assignmentId: 2, exposureId: 99 });
    });

    it("returns the raw response when the response carries an error", async () => {
      assignmentService.moveAssignment.mockResolvedValue({ error: "bad" });

      const result = await store.moveAssignment([1, 2, 2, {}]);

      expect(result).toEqual({ error: "bad" });
      expect(store.assignments).toHaveLength(2);
    });

    it("returns null and logs on rejection", async () => {
      assignmentService.moveAssignment.mockRejectedValue(new Error("fail"));

      const result = await store.moveAssignment([1, 2, 2, {}]);

      expect(result).toBeNull();
      expect(consoleSpy).toHaveBeenCalled();
    });
  });

  describe("simple setters and resets", () => {
    it("setCurrentAssignment and setAssignment both set the current assignment", () => {
      store.setCurrentAssignment({ assignmentId: 1 });
      expect(store.assignment).toEqual({ assignmentId: 1 });

      store.setAssignment({ assignmentId: 2 });
      expect(store.assignment).toEqual({ assignmentId: 2 });
    });

    it("resetAssignments clears the assignments list only", () => {
      store.upsertAssignments([{ assignmentId: 1 }]);
      store.setAssignment({ assignmentId: 1 });

      store.resetAssignments();

      expect(store.assignments).toEqual([]);
      expect(store.assignment).toEqual({ assignmentId: 1 });
    });

    it("resetAssignment clears the current assignment only", () => {
      store.upsertAssignments([{ assignmentId: 1 }]);
      store.setAssignment({ assignmentId: 1 });

      store.resetAssignment();

      expect(store.assignment).toBeNull();
      expect(store.assignments).toHaveLength(1);
    });

    it("reset clears assignments, assignment and fileRequest", () => {
      store.upsertAssignments([{ assignmentId: 1 }]);
      store.setAssignment({ assignmentId: 1 });

      store.reset();

      expect(store.assignments).toEqual([]);
      expect(store.assignment).toBeNull();
      expect(store.fileRequest).toBeNull();
    });
  });

  describe("upsertAssignments", () => {
    it("is a no-op for non-array input", () => {
      store.upsertAssignments("not-an-array");
      expect(store.assignments).toEqual([]);
    });

    it("filters out falsy entries", () => {
      store.upsertAssignments([null, { assignmentId: 1 }, undefined]);
      expect(store.assignments).toEqual([{ assignmentId: 1 }]);
    });

    it("replaces an existing assignment matched by id", () => {
      store.upsertAssignments([{ assignmentId: 1, title: "old" }]);
      store.upsertAssignments([{ assignmentId: 1, title: "new" }]);

      expect(store.assignments).toEqual([{ assignmentId: 1, title: "new" }]);
    });
  });
});

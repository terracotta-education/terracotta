import { beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";

import { assignmentService } from "./assignment.service";
import { api } from "@/store/api.module";

const BASE = "https://example.com/api/experiments/1/exposures/2/assignments";

function mockResponse({ status = 200, ok = true, text = "" } = {}) {
  return {
    status,
    ok,
    text: vi.fn().mockResolvedValue(text)
  };
}

function expectJsonHeaders() {
  return {
    Authorization: "Bearer test-token",
    "Content-Type": "application/json"
  };
}

describe("assignmentService", () => {
  let fetchMock;

  beforeEach(() => {
    setActivePinia(createPinia());
    api().aud = "https://example.com";
    api().apiToken = "test-token";

    fetchMock = vi.fn();
    vi.stubGlobal("fetch", fetchMock);

    vi.spyOn(console, "error").mockImplementation(() => {});
    vi.spyOn(console, "warn").mockImplementation(() => {});
  });

  describe("fetchAssignment", () => {
    it("GETs a single assignment without the submissions query by default", async () => {
      fetchMock.mockResolvedValueOnce(
        mockResponse({ status: 200, ok: true, text: JSON.stringify({ assignmentId: 3 }) })
      );

      const result = await assignmentService.fetchAssignment(1, 2, 3);

      expect(fetchMock).toHaveBeenCalledWith(`${BASE}/3`, {
        method: "GET",
        headers: expectJsonHeaders()
      });
      expect(result).toEqual({ assignmentId: 3 });
    });

    it("appends the submissions query param when requested", async () => {
      fetchMock.mockResolvedValueOnce(
        mockResponse({ status: 200, ok: true, text: JSON.stringify({ assignmentId: 3 }) })
      );

      await assignmentService.fetchAssignment(1, 2, 3, true);

      expect(fetchMock).toHaveBeenCalledWith(
        `${BASE}/3?submissions=true`,
        expect.objectContaining({ method: "GET" })
      );
    });
  });

  describe("fetchAssignmentsByExposure", () => {
    it("GETs the assignment list without submissions by default", async () => {
      fetchMock.mockResolvedValueOnce(
        mockResponse({ status: 200, ok: true, text: JSON.stringify([{ assignmentId: 1 }]) })
      );

      const result = await assignmentService.fetchAssignmentsByExposure(1, 2);

      expect(fetchMock).toHaveBeenCalledWith(BASE, {
        method: "GET",
        headers: expectJsonHeaders()
      });
      expect(result).toEqual([{ assignmentId: 1 }]);
    });

    it("appends the submissions query param when requested", async () => {
      fetchMock.mockResolvedValueOnce(
        mockResponse({ status: 200, ok: true, text: JSON.stringify([]) })
      );

      await assignmentService.fetchAssignmentsByExposure(1, 2, true);

      expect(fetchMock).toHaveBeenCalledWith(
        `${BASE}?submissions=true`,
        expect.objectContaining({ method: "GET" })
      );
    });
  });

  describe("create", () => {
    it("POSTs the new assignment with the given order", async () => {
      fetchMock.mockResolvedValueOnce(
        mockResponse({ status: 200, ok: true, text: JSON.stringify({ assignmentId: 9 }) })
      );

      const result = await assignmentService.create(1, 2, { title: "New" }, 3);

      expect(fetchMock).toHaveBeenCalledWith(BASE, {
        method: "POST",
        headers: expectJsonHeaders(),
        body: JSON.stringify({ title: "New", assignmentOrder: 3 })
      });
      expect(result).toEqual({ assignmentId: 9 });
    });
  });

  describe("duplicateAssignment", () => {
    it("POSTs to the duplicate endpoint with an empty body", async () => {
      fetchMock.mockResolvedValueOnce(
        mockResponse({ status: 200, ok: true, text: JSON.stringify({ assignmentId: 10 }) })
      );

      const result = await assignmentService.duplicateAssignment(1, 2, 3);

      expect(fetchMock).toHaveBeenCalledWith(`${BASE}/3/duplicate`, {
        method: "POST",
        headers: expectJsonHeaders(),
        body: JSON.stringify({})
      });
      expect(result).toEqual({ assignmentId: 10 });
    });
  });

  describe("deleteAssignment", () => {
    it("DELETEs the assignment", async () => {
      fetchMock.mockResolvedValueOnce(mockResponse({ status: 204, ok: true, text: "" }));

      const result = await assignmentService.deleteAssignment(1, 2, 3);

      expect(fetchMock).toHaveBeenCalledWith(`${BASE}/3`, {
        method: "DELETE",
        headers: expectJsonHeaders()
      });
      expect(result).toEqual([]);
    });
  });

  describe("updateAssignments", () => {
    it("PUTs the full payload of assignments", async () => {
      fetchMock.mockResolvedValueOnce(
        mockResponse({ status: 200, ok: true, text: JSON.stringify([{ assignmentId: 1 }]) })
      );

      const payload = [{ assignmentId: 1 }, { assignmentId: 2 }];
      const result = await assignmentService.updateAssignments(1, 2, payload);

      expect(fetchMock).toHaveBeenCalledWith(BASE, {
        method: "PUT",
        headers: expectJsonHeaders(),
        body: JSON.stringify(payload)
      });
      expect(result).toEqual([{ assignmentId: 1 }]);
    });
  });

  describe("updateAssignment", () => {
    it("PUTs a single assignment's fields", async () => {
      fetchMock.mockResolvedValueOnce(mockResponse({ status: 204, ok: true, text: "" }));

      const body = { title: "Updated" };
      const result = await assignmentService.updateAssignment(1, 2, 3, body);

      expect(fetchMock).toHaveBeenCalledWith(`${BASE}/3`, {
        method: "PUT",
        headers: expectJsonHeaders(),
        body: JSON.stringify(body)
      });
      expect(result).toEqual([]);
    });
  });

  describe("moveAssignment", () => {
    it("POSTs the move update payload", async () => {
      fetchMock.mockResolvedValueOnce(mockResponse({ status: 204, ok: true, text: "" }));

      const update = { newExposureId: 5, order: 1 };
      const result = await assignmentService.moveAssignment(1, 2, 3, update);

      expect(fetchMock).toHaveBeenCalledWith(`${BASE}/3/move`, {
        method: "POST",
        headers: expectJsonHeaders(),
        body: JSON.stringify(update)
      });
      expect(result).toEqual([]);
    });
  });

  describe("handleResponse branches (via fetchAssignment)", () => {
    it("logs via console.error and returns status/error for a 401", async () => {
      fetchMock.mockResolvedValueOnce(mockResponse({ status: 401, ok: false, text: "{}" }));

      const result = await assignmentService.fetchAssignment(1, 2, 3);

      expect(console.error).toHaveBeenCalled();
      expect(result).toEqual({ status: 401, error: {} });
    });

    it("logs via console.warn and returns status/error for a 404", async () => {
      const mockResp = mockResponse({ status: 404, ok: false, text: "" });
      fetchMock.mockResolvedValueOnce(mockResp);

      const result = await assignmentService.fetchAssignment(1, 2, 3);

      expect(console.warn).toHaveBeenCalled();
      expect(result).toEqual({ status: 404, error: mockResp });
    });

    it("returns status/error for a non-ok status outside the logged list, without extra logging", async () => {
      fetchMock.mockResolvedValueOnce(mockResponse({ status: 400, ok: false, text: "{}" }));

      const result = await assignmentService.fetchAssignment(1, 2, 3);

      // authHeader() itself logs once (the test token isn't a real JWT, per the
      // documented harmless noise) - handleResponse should not add a second one
      // for a 400, since 400 isn't in its logged status list.
      expect(console.error).toHaveBeenCalledTimes(1);
      expect(console.warn).not.toHaveBeenCalled();
      expect(result).toEqual({ status: 400, error: {} });
    });

    it("returns the raw response when ok with an empty body", async () => {
      const mockResp = mockResponse({ status: 200, ok: true, text: "" });
      fetchMock.mockResolvedValueOnce(mockResp);

      const result = await assignmentService.fetchAssignment(1, 2, 3);

      expect(result).toBe(mockResp);
    });

    it("catches JSON parse failures and returns error/status", async () => {
      fetchMock.mockResolvedValueOnce(mockResponse({ status: 200, ok: true, text: "not-json" }));

      const result = await assignmentService.fetchAssignment(1, 2, 3);

      expect(console.error).toHaveBeenCalled();
      expect(result.status).toBe(200);
      expect(result.error).toBeInstanceOf(Error);
    });
  });
});

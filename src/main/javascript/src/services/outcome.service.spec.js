import { beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";

import { outcomeService } from "./outcome.service";
import { api } from "@/store/api.module";

function mockResponse({ status = 200, ok = true, text = "" } = {}) {
  return {
    status,
    ok,
    text: vi.fn().mockResolvedValue(text)
  };
}

describe("outcomeService", () => {
  let fetchMock;

  beforeEach(() => {
    setActivePinia(createPinia());
    api().aud = "https://example.com";
    api().apiToken = "test-token";

    fetchMock = vi.fn();
    vi.stubGlobal("fetch", fetchMock);

    vi.spyOn(console, "error").mockImplementation(() => {});
  });

  describe("getAll", () => {
    it("GETs the outcomes list and wraps the parsed data", async () => {
      fetchMock.mockResolvedValueOnce(
        mockResponse({ status: 200, ok: true, text: JSON.stringify([{ outcomeId: 1 }]) })
      );

      const result = await outcomeService.getAll(1, 2);

      expect(fetchMock).toHaveBeenCalledWith(
        "https://example.com/api/experiments/1/exposures/2/outcomes",
        {
          method: "GET",
          headers: {
            Authorization: "Bearer test-token",
            "Content-Type": "application/json"
          }
        }
      );
      expect(result).toEqual({ data: [{ outcomeId: 1 }], status: 200 });
    });
  });

  describe("getById", () => {
    it("GETs a single outcome by id", async () => {
      fetchMock.mockResolvedValueOnce(
        mockResponse({ status: 200, ok: true, text: JSON.stringify({ outcomeId: 3 }) })
      );

      const result = await outcomeService.getById(1, 2, 3);

      expect(fetchMock).toHaveBeenCalledWith(
        "https://example.com/api/experiments/1/exposures/2/outcomes/3",
        expect.objectContaining({ method: "GET" })
      );
      expect(result).toEqual({ data: { outcomeId: 3 }, status: 200 });
    });
  });

  describe("getAllByExperimentId", () => {
    it("GETs outcomes scoped only by experiment", async () => {
      fetchMock.mockResolvedValueOnce(
        mockResponse({ status: 200, ok: true, text: JSON.stringify([]) })
      );

      const result = await outcomeService.getAllByExperimentId(1);

      expect(fetchMock).toHaveBeenCalledWith(
        "https://example.com/api/experiments/1/outcomes",
        expect.objectContaining({ method: "GET" })
      );
      expect(result).toEqual({ data: [], status: 200 });
    });
  });

  describe("create", () => {
    it("POSTs the new outcome payload", async () => {
      fetchMock.mockResolvedValueOnce(
        mockResponse({ status: 200, ok: true, text: JSON.stringify({ outcomeId: 4 }) })
      );

      const result = await outcomeService.create(1, 2, "Title", 10, true, "CANVAS", "lms-1");

      expect(fetchMock).toHaveBeenCalledWith(
        "https://example.com/api/experiments/1/exposures/2/outcomes",
        {
          method: "POST",
          headers: {
            Authorization: "Bearer test-token",
            "Content-Type": "application/json"
          },
          body: JSON.stringify({
            title: "Title",
            maxPoints: 10,
            external: true,
            lmsType: "CANVAS",
            lmsOutcomeId: "lms-1"
          })
        }
      );
      expect(result).toEqual({ data: { outcomeId: 4 }, status: 200 });
    });
  });

  describe("updateOutcome", () => {
    it("PUTs the updated outcome fields", async () => {
      fetchMock.mockResolvedValueOnce(mockResponse({ status: 204, ok: true, text: "" }));

      const result = await outcomeService.updateOutcome(1, 2, {
        outcomeId: 3,
        title: "New title",
        maxPoints: 20,
        external: false
      });

      expect(fetchMock).toHaveBeenCalledWith(
        "https://example.com/api/experiments/1/exposures/2/outcomes/3",
        {
          method: "PUT",
          headers: {
            Authorization: "Bearer test-token",
            "Content-Type": "application/json"
          },
          body: JSON.stringify({
            title: "New title",
            maxPoints: 20,
            external: false
          })
        }
      );
      expect(result).toEqual([]);
    });
  });

  describe("deleteOutcome", () => {
    it("DELETEs the outcome", async () => {
      fetchMock.mockResolvedValueOnce(mockResponse({ status: 204, ok: true, text: "" }));

      const result = await outcomeService.deleteOutcome(1, 2, 3);

      expect(fetchMock).toHaveBeenCalledWith(
        "https://example.com/api/experiments/1/exposures/2/outcomes/3",
        expect.objectContaining({ method: "DELETE" })
      );
      expect(result).toEqual([]);
    });
  });

  describe("getOutcomeScoresById", () => {
    it("GETs the outcome scores", async () => {
      fetchMock.mockResolvedValueOnce(
        mockResponse({ status: 200, ok: true, text: JSON.stringify([{ id: 1 }]) })
      );

      const result = await outcomeService.getOutcomeScoresById(1, 2, 3);

      expect(fetchMock).toHaveBeenCalledWith(
        "https://example.com/api/experiments/1/exposures/2/outcomes/3/outcome_scores",
        expect.objectContaining({ method: "GET" })
      );
      expect(result).toEqual({ data: [{ id: 1 }], status: 200 });
    });
  });

  describe("getScoreById", () => {
    it("GETs a single outcome score", async () => {
      fetchMock.mockResolvedValueOnce(
        mockResponse({ status: 200, ok: true, text: JSON.stringify({ id: 5 }) })
      );

      const result = await outcomeService.getScoreById(1, 2, 3, 5);

      expect(fetchMock).toHaveBeenCalledWith(
        "https://example.com/api/experiments/1/exposures/2/outcomes/3/outcome_scores/5",
        expect.objectContaining({ method: "GET" })
      );
      expect(result).toEqual({ data: { id: 5 }, status: 200 });
    });
  });

  describe("createOutcomeScores", () => {
    it("returns false without calling fetch for an invalid payload", async () => {
      const result = await outcomeService.createOutcomeScores(1, 2, 3, "nope");

      expect(result).toBe(false);
      expect(fetchMock).not.toHaveBeenCalled();
    });

    it("POSTs when scores is an array", async () => {
      fetchMock.mockResolvedValueOnce(
        mockResponse({ status: 200, ok: true, text: JSON.stringify([{ id: 1 }]) })
      );

      const scores = [{ participantId: 1, scoreNumeric: 10 }];
      const result = await outcomeService.createOutcomeScores(1, 2, 3, scores);

      expect(fetchMock).toHaveBeenCalledWith(
        "https://example.com/api/experiments/1/exposures/2/outcomes/3/outcome_scores",
        expect.objectContaining({
          method: "POST",
          body: JSON.stringify(scores)
        })
      );
      expect(result).toEqual({ data: [{ id: 1 }], status: 200 });
    });

    it("POSTs when scores is a single object with a participantId", async () => {
      fetchMock.mockResolvedValueOnce(mockResponse({ status: 204, ok: true, text: "" }));

      const score = { participantId: 7, scoreNumeric: 5 };
      const result = await outcomeService.createOutcomeScores(1, 2, 3, score);

      expect(fetchMock).toHaveBeenCalledWith(
        "https://example.com/api/experiments/1/exposures/2/outcomes/3/outcome_scores",
        expect.objectContaining({
          method: "POST",
          body: JSON.stringify(score)
        })
      );
      expect(result).toEqual([]);
    });
  });

  describe("updateOutcomeScores", () => {
    it("returns false for falsy scores", async () => {
      expect(await outcomeService.updateOutcomeScores(1, 2, 3, null)).toBe(false);
      expect(fetchMock).not.toHaveBeenCalled();
    });

    it("returns false for a non-array", async () => {
      expect(await outcomeService.updateOutcomeScores(1, 2, 3, "nope")).toBe(false);
      expect(fetchMock).not.toHaveBeenCalled();
    });

    it("returns false when no score has a participantId", async () => {
      expect(
        await outcomeService.updateOutcomeScores(1, 2, 3, [{ outcomeScoreId: 1 }])
      ).toBe(false);
      expect(fetchMock).not.toHaveBeenCalled();
    });

    it("PUTs the mapped score payload when valid", async () => {
      fetchMock.mockResolvedValueOnce(
        mockResponse({ status: 200, ok: true, text: JSON.stringify([{ id: 1 }]) })
      );

      const scores = [
        { outcomeScoreId: 9, participantId: 1, scoreNumeric: 10, extraField: "ignored" }
      ];
      const result = await outcomeService.updateOutcomeScores(1, 2, 3, scores);

      expect(fetchMock).toHaveBeenCalledWith(
        "https://example.com/api/experiments/1/exposures/2/outcomes/3/outcome_scores",
        expect.objectContaining({
          method: "PUT",
          body: JSON.stringify([
            { outcomeScoreId: 9, participantId: 1, scoreNumeric: 10 }
          ])
        })
      );
      expect(result).toEqual({ data: [{ id: 1 }], status: 200 });
    });
  });

  describe("getOutcomePotentials", () => {
    it("GETs the outcome potentials", async () => {
      fetchMock.mockResolvedValueOnce(
        mockResponse({ status: 200, ok: true, text: JSON.stringify([{ id: 1 }]) })
      );

      const result = await outcomeService.getOutcomePotentials(1);

      expect(fetchMock).toHaveBeenCalledWith(
        "https://example.com/api/experiments/1/outcome_potentials",
        expect.objectContaining({ method: "GET" })
      );
      expect(result).toEqual({ data: [{ id: 1 }], status: 200 });
    });
  });

  describe("handleResponse branches (via getAll)", () => {
    it("returns [] on a 204", async () => {
      fetchMock.mockResolvedValueOnce(mockResponse({ status: 204, ok: true, text: "" }));

      const result = await outcomeService.getAll(1, 2);

      expect(result).toEqual([]);
    });

    it("wraps a message on a 409", async () => {
      fetchMock.mockResolvedValueOnce(
        mockResponse({ status: 409, ok: false, text: JSON.stringify({ reason: "conflict" }) })
      );

      const result = await outcomeService.getAll(1, 2);

      expect(result).toEqual({ message: { reason: "conflict" }, status: 409 });
    });

    it("logs and returns status/error for a non-ok response", async () => {
      fetchMock.mockResolvedValueOnce(
        mockResponse({ status: 400, ok: false, text: JSON.stringify({ msg: "bad" }) })
      );

      const result = await outcomeService.getAll(1, 2);

      expect(console.error).toHaveBeenCalled();
      expect(result).toEqual({ status: 400, error: { msg: "bad" } });
    });

    it("returns the raw response when ok with an empty body", async () => {
      const mockResp = mockResponse({ status: 200, ok: true, text: "" });
      fetchMock.mockResolvedValueOnce(mockResp);

      const result = await outcomeService.getAll(1, 2);

      expect(result).toBe(mockResp);
    });

    it("catches a failure reading the response body", async () => {
      const mockResp = {
        status: 200,
        ok: true,
        text: vi.fn().mockRejectedValue(new Error("boom"))
      };
      fetchMock.mockResolvedValueOnce(mockResp);

      const result = await outcomeService.getAll(1, 2);

      expect(console.error).toHaveBeenCalled();
      expect(result).toEqual({ error: expect.any(Error), status: 200 });
    });
  });
});

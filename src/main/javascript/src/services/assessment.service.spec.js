import { beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";

import { assessmentService } from "./assessment.service";
import { api } from "@/store/api.module";

const BASE = "https://example.com/api/experiments/1/conditions/2/treatments/3/assessments";

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

describe("assessmentService", () => {
  let fetchMock;

  beforeEach(() => {
    setActivePinia(createPinia());
    api().aud = "https://example.com";
    api().apiToken = "test-token";

    fetchMock = vi.fn();
    vi.stubGlobal("fetch", fetchMock);

    vi.spyOn(console, "error").mockImplementation(() => {});
  });

  describe("fetchAssessment", () => {
    it("GETs a single assessment with full detail query params", async () => {
      fetchMock.mockResolvedValueOnce(
        mockResponse({ status: 200, ok: true, text: JSON.stringify({ assessmentId: 4 }) })
      );

      const result = await assessmentService.fetchAssessment(1, 2, 3, 4);

      expect(fetchMock).toHaveBeenCalledWith(
        `${BASE}/4?questions=true&answers=true&submissions=true`,
        { method: "GET", headers: expectJsonHeaders() }
      );
      expect(result).toEqual({ data: { assessmentId: 4 }, status: 200 });
    });
  });

  describe("fetchAssessmentForSubmission", () => {
    it("GETs an assessment scoped to a submission", async () => {
      fetchMock.mockResolvedValueOnce(
        mockResponse({ status: 200, ok: true, text: JSON.stringify({ assessmentId: 4 }) })
      );

      const result = await assessmentService.fetchAssessmentForSubmission(1, 2, 3, 4, 9);

      expect(fetchMock).toHaveBeenCalledWith(
        `${BASE}/4?questions=true&answers=true&submission_id=9`,
        { method: "GET", headers: expectJsonHeaders() }
      );
      expect(result).toEqual({ data: { assessmentId: 4 }, status: 200 });
    });
  });

  describe("fetchAssessments", () => {
    it("GETs the assessments list for a treatment", async () => {
      fetchMock.mockResolvedValueOnce(
        mockResponse({ status: 200, ok: true, text: JSON.stringify([{ assessmentId: 1 }]) })
      );

      const result = await assessmentService.fetchAssessments(1, 2, 3);

      expect(fetchMock).toHaveBeenCalledWith(BASE, { method: "GET", headers: expectJsonHeaders() });
      expect(result).toEqual({ data: [{ assessmentId: 1 }], status: 200 });
    });
  });

  describe("createAssessment", () => {
    it("POSTs the title and html body", async () => {
      fetchMock.mockResolvedValueOnce(
        mockResponse({ status: 200, ok: true, text: JSON.stringify({ assessmentId: 5 }) })
      );

      const result = await assessmentService.createAssessment(1, 2, 3, "Quiz", "<p>body</p>");

      expect(fetchMock).toHaveBeenCalledWith(BASE, {
        method: "POST",
        headers: expectJsonHeaders(),
        body: JSON.stringify({ title: "Quiz", html: "<p>body</p>" })
      });
      expect(result).toEqual({ data: { assessmentId: 5 }, status: 200 });
    });
  });

  describe("updateAssessment", () => {
    it("PUTs the full assessment settings payload", async () => {
      fetchMock.mockResolvedValueOnce(mockResponse({ status: 204, ok: true, text: "" }));

      const result = await assessmentService.updateAssessment(
        1, 2, 3, 4, "<p>new</p>", true, "2026-01-01", "2026-02-01",
        false, null, null, 2, "MOST_RECENT", 24, 50
      );

      expect(fetchMock).toHaveBeenCalledWith(`${BASE}/4`, {
        method: "PUT",
        headers: expectJsonHeaders(),
        body: JSON.stringify({
          html: "<p>new</p>",
          allowStudentViewResponses: true,
          studentViewResponsesAfter: "2026-01-01",
          studentViewResponsesBefore: "2026-02-01",
          allowStudentViewCorrectAnswers: false,
          studentViewCorrectAnswersAfter: null,
          studentViewCorrectAnswersBefore: null,
          numOfSubmissions: 2,
          multipleSubmissionScoringScheme: "MOST_RECENT",
          hoursBetweenSubmissions: 24,
          cumulativeScoringInitialPercentage: 50
        })
      });
      expect(result).toEqual([]);
    });
  });

  describe("regradeQuestions", () => {
    it("POSTs the regrade payload as-is", async () => {
      fetchMock.mockResolvedValueOnce(mockResponse({ status: 204, ok: true, text: "" }));

      const body = { questionIds: [1, 2] };
      const result = await assessmentService.regradeQuestions(1, 2, 3, 4, body);

      expect(fetchMock).toHaveBeenCalledWith(`${BASE}/4/regrade`, {
        method: "POST",
        headers: expectJsonHeaders(),
        body: JSON.stringify(body)
      });
      expect(result).toEqual([]);
    });
  });

  describe("createQuestion", () => {
    it("POSTs the new question payload", async () => {
      fetchMock.mockResolvedValueOnce(
        mockResponse({ status: 200, ok: true, text: JSON.stringify({ questionId: 7 }) })
      );

      const result = await assessmentService.createQuestion(
        1, 2, 3, 4, 1, "ESSAY", 10, "<p>Q</p>", "client-1"
      );

      expect(fetchMock).toHaveBeenCalledWith(`${BASE}/4/questions`, {
        method: "POST",
        headers: expectJsonHeaders(),
        body: JSON.stringify({
          questionOrder: 1,
          questionType: "ESSAY",
          points: 10,
          html: "<p>Q</p>",
          integrationClientId: "client-1"
        })
      });
      expect(result).toEqual({ data: { questionId: 7 }, status: 200 });
    });
  });

  describe("updateQuestion", () => {
    it("PUTs the updated question payload", async () => {
      fetchMock.mockResolvedValueOnce(mockResponse({ status: 204, ok: true, text: "" }));

      const result = await assessmentService.updateQuestion(
        1, 2, 3, 4, 7, "<p>Q2</p>", 20, 2, "MC", true, [], null
      );

      expect(fetchMock).toHaveBeenCalledWith(`${BASE}/4/questions/7`, {
        method: "PUT",
        headers: expectJsonHeaders(),
        body: JSON.stringify({
          html: "<p>Q2</p>",
          points: 20,
          questionOrder: 2,
          questionType: "MC",
          randomizeAnswers: true,
          answers: [],
          integration: null
        })
      });
      expect(result).toEqual([]);
    });
  });

  describe("updateQuestions", () => {
    it("PUTs the full questions list", async () => {
      fetchMock.mockResolvedValueOnce(mockResponse({ status: 204, ok: true, text: "" }));

      const questions = [{ questionId: 1 }, { questionId: 2 }];
      const result = await assessmentService.updateQuestions(1, 2, 3, 4, questions);

      expect(fetchMock).toHaveBeenCalledWith(`${BASE}/4/questions`, {
        method: "PUT",
        headers: expectJsonHeaders(),
        body: JSON.stringify(questions)
      });
      expect(result).toEqual([]);
    });
  });

  describe("deleteQuestion", () => {
    it("DELETEs a single question", async () => {
      fetchMock.mockResolvedValueOnce(mockResponse({ status: 204, ok: true, text: "" }));

      const result = await assessmentService.deleteQuestion(1, 2, 3, 4, 7);

      expect(fetchMock).toHaveBeenCalledWith(`${BASE}/4/questions/7`, {
        method: "DELETE",
        headers: expectJsonHeaders()
      });
      expect(result).toEqual([]);
    });
  });

  describe("deleteQuestions", () => {
    it("DELETEs a batch of questions with a body", async () => {
      fetchMock.mockResolvedValueOnce(mockResponse({ status: 204, ok: true, text: "" }));

      const questions = [{ questionId: 1 }];
      const result = await assessmentService.deleteQuestions(1, 2, 3, 4, questions);

      expect(fetchMock).toHaveBeenCalledWith(`${BASE}/4/questions`, {
        method: "DELETE",
        headers: expectJsonHeaders(),
        body: JSON.stringify(questions)
      });
      expect(result).toEqual([]);
    });
  });

  describe("createAnswer", () => {
    it("POSTs a new answer", async () => {
      fetchMock.mockResolvedValueOnce(
        mockResponse({ status: 200, ok: true, text: JSON.stringify({ answerId: 3 }) })
      );

      const result = await assessmentService.createAnswer(1, 2, 3, 4, 7, "<p>A</p>", true, 1);

      expect(fetchMock).toHaveBeenCalledWith(`${BASE}/4/questions/7/answers`, {
        method: "POST",
        headers: expectJsonHeaders(),
        body: JSON.stringify({ html: "<p>A</p>", correct: true, answerOrder: 1 })
      });
      expect(result).toEqual({ data: { answerId: 3 }, status: 200 });
    });
  });

  describe("updateAnswer", () => {
    it("PUTs an updated answer", async () => {
      fetchMock.mockResolvedValueOnce(mockResponse({ status: 204, ok: true, text: "" }));

      const result = await assessmentService.updateAnswer(
        1, 2, 3, 4, 7, 3, "TEXT", "<p>A2</p>", false, 2
      );

      expect(fetchMock).toHaveBeenCalledWith(`${BASE}/4/questions/7/answers/3`, {
        method: "PUT",
        headers: expectJsonHeaders(),
        body: JSON.stringify({
          answerType: "TEXT",
          html: "<p>A2</p>",
          correct: false,
          answerOrder: 2
        })
      });
      expect(result).toEqual([]);
    });
  });

  describe("updateAnswers", () => {
    it("PUTs a batch of answers", async () => {
      fetchMock.mockResolvedValueOnce(mockResponse({ status: 204, ok: true, text: "" }));

      const answers = [{ answerId: 1 }, { answerId: 2 }];
      const result = await assessmentService.updateAnswers(1, 2, 3, 4, 7, answers);

      expect(fetchMock).toHaveBeenCalledWith(`${BASE}/4/questions/7/answers`, {
        method: "PUT",
        headers: expectJsonHeaders(),
        body: JSON.stringify(answers)
      });
      expect(result).toEqual([]);
    });
  });

  describe("deleteAnswer", () => {
    it("DELETEs a single answer", async () => {
      fetchMock.mockResolvedValueOnce(mockResponse({ status: 204, ok: true, text: "" }));

      const result = await assessmentService.deleteAnswer(1, 2, 3, 4, 7, 3);

      expect(fetchMock).toHaveBeenCalledWith(`${BASE}/4/questions/7/answers/3`, {
        method: "DELETE",
        headers: expectJsonHeaders()
      });
      expect(result).toEqual([]);
    });
  });

  describe("handleResponse branches (via fetchAssessments)", () => {
    it("wraps a message on a 409", async () => {
      fetchMock.mockResolvedValueOnce(
        mockResponse({ status: 409, ok: false, text: JSON.stringify({ reason: "conflict" }) })
      );

      const result = await assessmentService.fetchAssessments(1, 2, 3);

      expect(result).toEqual({ message: { reason: "conflict" }, status: 409 });
    });

    it("logs and returns data/status/error on a non-ok response", async () => {
      fetchMock.mockResolvedValueOnce(
        mockResponse({ status: 500, ok: false, text: JSON.stringify({ msg: "server error" }) })
      );

      const result = await assessmentService.fetchAssessments(1, 2, 3);

      expect(console.error).toHaveBeenCalled();
      expect(result).toEqual({
        data: { msg: "server error" },
        status: 500,
        error: { msg: "server error" }
      });
    });

    it("returns the raw response when ok with an empty body", async () => {
      const mockResp = mockResponse({ status: 200, ok: true, text: "" });
      fetchMock.mockResolvedValueOnce(mockResp);

      const result = await assessmentService.fetchAssessments(1, 2, 3);

      expect(result).toBe(mockResp);
    });

    it("catches a failure reading the response body", async () => {
      const mockResp = {
        status: 200,
        ok: true,
        text: vi.fn().mockRejectedValue(new Error("boom"))
      };
      fetchMock.mockResolvedValueOnce(mockResp);

      const result = await assessmentService.fetchAssessments(1, 2, 3);

      expect(console.error).toHaveBeenCalled();
      expect(result).toEqual({ error: expect.any(Error), status: 200 });
    });
  });
});

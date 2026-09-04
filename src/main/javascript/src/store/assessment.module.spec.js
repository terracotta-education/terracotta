import { beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";

vi.mock("@/services", () => ({
  assessmentService: {
    fetchAssessment: vi.fn(),
    fetchAssessmentForSubmission: vi.fn(),
    fetchAssessments: vi.fn(),
    createAssessment: vi.fn(),
    updateAssessment: vi.fn(),
    createQuestion: vi.fn(),
    updateQuestion: vi.fn(),
    updateQuestions: vi.fn(),
    deleteQuestion: vi.fn(),
    deleteQuestions: vi.fn(),
    createAnswer: vi.fn(),
    updateAnswer: vi.fn(),
    updateAnswers: vi.fn(),
    deleteAnswer: vi.fn(),
    regradeQuestions: vi.fn()
  }
}));

import { assessmentService } from "@/services";
import { assessment } from "./assessment.module";

describe("assessment store", () => {
  let store;
  let consoleSpy;

  beforeEach(() => {
    setActivePinia(createPinia());
    store = assessment();
    vi.clearAllMocks();
    consoleSpy = vi.spyOn(console, "error").mockImplementation(() => {});
  });

  describe("getters", () => {
    it("questions returns an empty array when there is no assessment", () => {
      expect(store.questions).toEqual([]);
    });

    it("questions sorts by questionOrder, treating missing order as 0", () => {
      store.setAssessment({
        questions: [
          { questionId: 3, questionOrder: 2 },
          { questionId: 1, questionOrder: undefined },
          { questionId: 2, questionOrder: 1 }
        ]
      });

      expect(store.questions.map(q => q.questionId)).toEqual([1, 2, 3]);
    });

    it("answerableQuestions filters out PAGE_BREAK questions", () => {
      store.setAssessment({
        questions: [
          { questionId: 1, questionOrder: 1, questionType: "MC" },
          { questionId: 2, questionOrder: 2, questionType: "PAGE_BREAK" },
          { questionId: 3, questionOrder: 3, questionType: "ESSAY" }
        ]
      });

      expect(store.answerableQuestions.map(q => q.questionId)).toEqual([1, 3]);
    });

    it("questionPages returns an empty array when there are no questions", () => {
      expect(store.questionPages).toEqual([]);
    });

    it("questionPages groups questions into pages split by PAGE_BREAK", () => {
      store.setAssessment({
        questions: [
          { questionId: 1, questionOrder: 1, questionType: "MC" },
          { questionId: 2, questionOrder: 2, questionType: "PAGE_BREAK" },
          { questionId: 3, questionOrder: 3, questionType: "ESSAY" }
        ]
      });

      const pages = store.questionPages;

      expect(pages).toHaveLength(2);
      expect(pages[0].pageBreakAfter).toBe(true);
      expect(pages[0].questions.map(q => q.questionId)).toEqual([1]);
      expect(pages[1].pageBreakAfter).toBe(false);
      expect(pages[1].questions.map(q => q.questionId)).toEqual([3]);
      expect(pages[1].questionStartIndex).toBe(1);
    });

    it("questionPages does not start a new page if the trailing question is a PAGE_BREAK", () => {
      store.setAssessment({
        questions: [
          { questionId: 1, questionOrder: 1, questionType: "MC" },
          { questionId: 2, questionOrder: 2, questionType: "PAGE_BREAK" }
        ]
      });

      const pages = store.questionPages;

      expect(pages).toHaveLength(1);
      expect(pages[0].pageBreakAfter).toBe(true);
    });
  });

  describe("simple mutations", () => {
    it("setAssessment sets the assessment", () => {
      const data = { assessmentId: 1, questions: [] };
      store.setAssessment(data);

      expect(store.assessment).toEqual(data);
    });

    it("updateQuestions delegates to upsertQuestion", () => {
      store.setAssessment({ questions: [{ questionId: 1 }] });
      store.updateQuestions({ questionId: 1, html: "updated" });

      expect(store.assessment.questions[0].html).toBe("updated");
    });

    it("updateAnswers delegates to upsertAnswer", () => {
      store.setAssessment({
        questions: [{ questionId: 1, answers: [{ answerId: 5 }] }]
      });
      store.updateAnswers({ questionId: 1, answerId: 5, html: "new answer" });

      expect(store.assessment.questions[0].answers[0].html).toBe("new answer");
    });
  });

  describe("fetchAssessment", () => {
    it("sets and returns assessment data on success", async () => {
      assessmentService.fetchAssessment.mockResolvedValue({
        data: { assessmentId: 1 }
      });

      const result = await store.fetchAssessment([1, 2, 3, 4]);

      expect(assessmentService.fetchAssessment).toHaveBeenCalledWith(1, 2, 3, 4);
      expect(result).toEqual({ assessmentId: 1 });
      expect(store.assessment).toEqual({ assessmentId: 1 });
    });

    it("sets assessment to null when the response has no data", async () => {
      assessmentService.fetchAssessment.mockResolvedValue({});

      const result = await store.fetchAssessment([1, 2, 3, 4]);

      expect(result).toBeNull();
      expect(store.assessment).toBeNull();
    });

    it("returns null and logs on rejection", async () => {
      assessmentService.fetchAssessment.mockRejectedValue(new Error("fail"));

      const result = await store.fetchAssessment([1, 2, 3, 4]);

      expect(result).toBeNull();
      expect(consoleSpy).toHaveBeenCalled();
    });
  });

  describe("fetchAssessmentForSubmission", () => {
    it("sets and returns assessment data on success", async () => {
      assessmentService.fetchAssessmentForSubmission.mockResolvedValue({
        data: { assessmentId: 2 }
      });

      const result = await store.fetchAssessmentForSubmission([1, 2, 3, 4, 5]);

      expect(result).toEqual({ assessmentId: 2 });
    });

    it("returns null and logs on rejection", async () => {
      assessmentService.fetchAssessmentForSubmission.mockRejectedValue(
        new Error("fail")
      );

      const result = await store.fetchAssessmentForSubmission([1, 2, 3, 4, 5]);

      expect(result).toBeNull();
      expect(consoleSpy).toHaveBeenCalled();
    });
  });

  describe("createAssessment", () => {
    it("reuses an existing assessment when fetchAssessments returns one, without calling createAssessment", async () => {
      assessmentService.fetchAssessments.mockResolvedValue({
        status: 200,
        data: [{ assessmentId: 9 }]
      });

      const result = await store.createAssessment([1, 2, 3]);

      expect(assessmentService.createAssessment).not.toHaveBeenCalled();
      expect(result).toEqual({ status: 200, data: { assessmentId: 9 } });
      expect(store.assessment).toEqual({ assessmentId: 9 });
      expect(store.assessments).toContainEqual({ assessmentId: 9 });
    });

    it("creates a new assessment when none exists", async () => {
      assessmentService.fetchAssessments.mockResolvedValue({ data: [] });
      assessmentService.createAssessment.mockResolvedValue({
        status: 201,
        data: { assessmentId: 10 }
      });

      const result = await store.createAssessment([1, 2, 3, "title", "body"]);

      expect(result).toEqual({ status: 201, data: { assessmentId: 10 } });
      expect(store.assessment).toEqual({ assessmentId: 10 });
    });

    it("returns the raw response without setting state when creation status is not 201", async () => {
      assessmentService.fetchAssessments.mockResolvedValue({ data: [] });
      assessmentService.createAssessment.mockResolvedValue({ status: 500 });

      const result = await store.createAssessment([1, 2, 3, "title", "body"]);

      expect(result).toEqual({ status: 500 });
      expect(store.assessment).toBeNull();
    });

    it("returns null and logs on rejection", async () => {
      assessmentService.fetchAssessments.mockRejectedValue(new Error("fail"));

      const result = await store.createAssessment([1, 2, 3]);

      expect(result).toBeNull();
      expect(consoleSpy).toHaveBeenCalled();
    });
  });

  describe("regradeQuestions", () => {
    it("returns the service response on success", async () => {
      assessmentService.regradeQuestions.mockResolvedValue({ status: 200 });

      const result = await store.regradeQuestions([1, 2, 3, 4, {}]);

      expect(result).toEqual({ status: 200 });
    });

    it("returns null and logs on rejection", async () => {
      assessmentService.regradeQuestions.mockRejectedValue(new Error("fail"));

      const result = await store.regradeQuestions([1, 2, 3, 4, {}]);

      expect(result).toBeNull();
      expect(consoleSpy).toHaveBeenCalled();
    });
  });

  describe("updateAssessment", () => {
    it("returns status/data on a truthy response", async () => {
      assessmentService.updateAssessment.mockResolvedValue({
        status: 200,
        data: { assessmentId: 1 }
      });

      const result = await store.updateAssessment([1, 2, 3, 4]);

      expect(result).toEqual({ status: 200, data: { assessmentId: 1 } });
    });

    it("returns null when the response is falsy", async () => {
      assessmentService.updateAssessment.mockResolvedValue(null);

      const result = await store.updateAssessment([1, 2, 3, 4]);

      expect(result).toBeNull();
    });

    it("returns null and logs on rejection", async () => {
      assessmentService.updateAssessment.mockRejectedValue(new Error("fail"));

      const result = await store.updateAssessment([1, 2, 3, 4]);

      expect(result).toBeNull();
      expect(consoleSpy).toHaveBeenCalled();
    });
  });

  describe("createQuestion / createQuestionAtIndex", () => {
    beforeEach(() => {
      store.setAssessment({ questions: [{ questionId: 1 }] });
    });

    it("createQuestion appends the new question via upsert", async () => {
      assessmentService.createQuestion.mockResolvedValue({
        status: 201,
        data: { questionId: 2 }
      });

      const result = await store.createQuestion([1, 2, 3, 4]);

      expect(result).toEqual({ status: 201, data: { questionId: 2 } });
      expect(store.assessment.questions.map(q => q.questionId)).toEqual([1, 2]);
    });

    it("createQuestionAtIndex splices the question at the given index", async () => {
      assessmentService.createQuestion.mockResolvedValue({
        status: 201,
        data: { questionId: 2 }
      });

      await store.createQuestionAtIndex({ payload: [1, 2, 3, 4], questionIndex: 0 });

      expect(store.assessment.questions.map(q => q.questionId)).toEqual([2, 1]);
    });

    it("returns null when the response has no questionId", async () => {
      assessmentService.createQuestion.mockResolvedValue({ status: 201, data: {} });

      const result = await store.createQuestion([1, 2, 3, 4]);

      expect(result).toBeNull();
    });

    it("returns null and logs on rejection", async () => {
      assessmentService.createQuestion.mockRejectedValue(new Error("fail"));

      const result = await store.createQuestion([1, 2, 3, 4]);

      expect(result).toBeNull();
      expect(consoleSpy).toHaveBeenCalled();
    });
  });

  describe("updateQuestion", () => {
    beforeEach(() => {
      store.setAssessment({ questions: [{ questionId: 5, html: "old" }] });
    });

    it("upserts the rebuilt question from the payload on success", async () => {
      assessmentService.updateQuestion.mockResolvedValue({ status: 200 });

      const payload = [1, 2, 3, 4, 5, "new html", 10, 1, "MC", true, [], null];
      const result = await store.updateQuestion(payload);

      expect(result).toEqual({ status: 200, data: null });
      expect(store.assessment.questions[0].html).toBe("new html");
    });

    it("returns null when the response is falsy", async () => {
      assessmentService.updateQuestion.mockResolvedValue(null);

      const result = await store.updateQuestion([1, 2, 3, 4, 5]);

      expect(result).toBeNull();
      expect(store.assessment.questions[0].html).toBe("old");
    });

    it("returns null and logs on rejection", async () => {
      assessmentService.updateQuestion.mockRejectedValue(new Error("fail"));

      const result = await store.updateQuestion([1, 2, 3, 4, 5]);

      expect(result).toBeNull();
      expect(consoleSpy).toHaveBeenCalled();
    });
  });

  describe("updateQuestionsBatch", () => {
    it("upserts every question in the batch on success", async () => {
      store.setAssessment({ questions: [{ questionId: 1 }] });
      assessmentService.updateQuestions.mockResolvedValue({ status: 200 });

      const questionList = [{ questionId: 1, html: "a" }, { questionId: 2, html: "b" }];
      const result = await store.updateQuestionsBatch([1, 2, 3, 4, questionList]);

      expect(result).toEqual({ status: 200, data: null });
      expect(store.assessment.questions.map(q => q.questionId)).toEqual([1, 2]);
    });

    it("returns null when the response is falsy", async () => {
      assessmentService.updateQuestions.mockResolvedValue(null);

      const result = await store.updateQuestionsBatch([1, 2, 3, 4, []]);

      expect(result).toBeNull();
    });

    it("returns null and logs on rejection", async () => {
      assessmentService.updateQuestions.mockRejectedValue(new Error("fail"));

      const result = await store.updateQuestionsBatch([1, 2, 3, 4, []]);

      expect(result).toBeNull();
      expect(consoleSpy).toHaveBeenCalled();
    });
  });

  describe("deleteQuestion", () => {
    beforeEach(() => {
      store.setAssessment({
        questions: [{ questionId: 1 }, { questionId: 2 }]
      });
    });

    it("removes the matching question on a 200 response", async () => {
      assessmentService.deleteQuestion.mockResolvedValue({ status: 200 });

      const result = await store.deleteQuestion([1, 2, 3, 4, 2]);

      expect(store.assessment.questions.map(q => q.questionId)).toEqual([1]);
      expect(result.status).toBe(200);
    });

    it("returns null on a non-200 response", async () => {
      assessmentService.deleteQuestion.mockResolvedValue({ status: 400 });

      const result = await store.deleteQuestion([1, 2, 3, 4, 2]);

      expect(result).toBeNull();
      expect(store.assessment.questions).toHaveLength(2);
    });

    it("returns null and logs on rejection", async () => {
      assessmentService.deleteQuestion.mockRejectedValue(new Error("fail"));

      const result = await store.deleteQuestion([1, 2, 3, 4, 2]);

      expect(result).toBeNull();
      expect(consoleSpy).toHaveBeenCalled();
    });
  });

  describe("deleteQuestions", () => {
    it("clears all questions on a 200 response", async () => {
      store.setAssessment({ questions: [{ questionId: 1 }] });
      assessmentService.deleteQuestions.mockResolvedValue({ status: 200 });

      const result = await store.deleteQuestions([1, 2, 3, 4, []]);

      expect(store.assessment.questions).toEqual([]);
      expect(result).toEqual({ status: 200, data: null });
    });

    it("returns null on a non-200 response", async () => {
      store.setAssessment({ questions: [{ questionId: 1 }] });
      assessmentService.deleteQuestions.mockResolvedValue({ status: 400 });

      const result = await store.deleteQuestions([1, 2, 3, 4, []]);

      expect(result).toBeNull();
      expect(store.assessment.questions).toHaveLength(1);
    });

    it("returns null and logs on rejection", async () => {
      assessmentService.deleteQuestions.mockRejectedValue(new Error("fail"));

      const result = await store.deleteQuestions([1, 2, 3, 4, []]);

      expect(result).toBeNull();
      expect(consoleSpy).toHaveBeenCalled();
    });
  });

  describe("createAnswer", () => {
    beforeEach(() => {
      store.setAssessment({ questions: [{ questionId: 1, answers: [] }] });
    });

    it("upserts the new answer on success", async () => {
      assessmentService.createAnswer.mockResolvedValue({
        status: 201,
        data: { answerId: 1, questionId: 1 }
      });

      const result = await store.createAnswer([1, 2, 3, 4, 1, "html", true, 1]);

      expect(result.data).toEqual({ answerId: 1, questionId: 1 });
      expect(store.assessment.questions[0].answers).toHaveLength(1);
    });

    it("returns null when the response has no answerId", async () => {
      assessmentService.createAnswer.mockResolvedValue({ status: 201, data: {} });

      const result = await store.createAnswer([1, 2, 3, 4, 1]);

      expect(result).toBeNull();
    });

    it("returns null and logs on rejection", async () => {
      assessmentService.createAnswer.mockRejectedValue(new Error("fail"));

      const result = await store.createAnswer([1, 2, 3, 4, 1]);

      expect(result).toBeNull();
      expect(consoleSpy).toHaveBeenCalled();
    });
  });

  describe("updateAnswer", () => {
    it("returns status/data null on a truthy response", async () => {
      assessmentService.updateAnswer.mockResolvedValue({ status: 200 });

      const result = await store.updateAnswer([1, 2, 3, 4, 1, 1]);

      expect(result).toEqual({ status: 200, data: null });
    });

    it("returns null when the response is falsy", async () => {
      assessmentService.updateAnswer.mockResolvedValue(null);

      const result = await store.updateAnswer([1, 2, 3, 4, 1, 1]);

      expect(result).toBeNull();
    });

    it("returns null and logs on rejection", async () => {
      assessmentService.updateAnswer.mockRejectedValue(new Error("fail"));

      const result = await store.updateAnswer([1, 2, 3, 4, 1, 1]);

      expect(result).toBeNull();
      expect(consoleSpy).toHaveBeenCalled();
    });
  });

  describe("updateAnswersBatch", () => {
    it("upserts every answer in the batch on success", async () => {
      store.setAssessment({ questions: [{ questionId: 1, answers: [] }] });
      assessmentService.updateAnswers.mockResolvedValue({ status: 200 });

      const answerList = [{ answerId: 1, questionId: 1 }];
      const result = await store.updateAnswersBatch([1, 2, 3, 4, 1, answerList]);

      expect(result).toEqual({ status: 200, data: null });
      expect(store.assessment.questions[0].answers).toHaveLength(1);
    });

    it("returns null when the response is falsy", async () => {
      assessmentService.updateAnswers.mockResolvedValue(null);

      const result = await store.updateAnswersBatch([1, 2, 3, 4, 1, []]);

      expect(result).toBeNull();
    });

    it("returns null and logs on rejection", async () => {
      assessmentService.updateAnswers.mockRejectedValue(new Error("fail"));

      const result = await store.updateAnswersBatch([1, 2, 3, 4, 1, []]);

      expect(result).toBeNull();
      expect(consoleSpy).toHaveBeenCalled();
    });
  });

  describe("deleteAnswer", () => {
    beforeEach(() => {
      store.setAssessment({
        questions: [
          { questionId: 1, answers: [{ answerId: 10 }, { answerId: 11 }] }
        ]
      });
    });

    it("removes the matching answer on a 200 response", async () => {
      assessmentService.deleteAnswer.mockResolvedValue({ status: 200 });

      const result = await store.deleteAnswer([1, 2, 3, 4, 1, 10]);

      expect(store.assessment.questions[0].answers.map(a => a.answerId)).toEqual([11]);
      expect(result).toEqual({ status: 200, data: null });
    });

    it("returns null on a non-200 response", async () => {
      assessmentService.deleteAnswer.mockResolvedValue({ status: 400 });

      const result = await store.deleteAnswer([1, 2, 3, 4, 1, 10]);

      expect(result).toBeNull();
      expect(store.assessment.questions[0].answers).toHaveLength(2);
    });

    it("returns null and logs on rejection", async () => {
      assessmentService.deleteAnswer.mockRejectedValue(new Error("fail"));

      const result = await store.deleteAnswer([1, 2, 3, 4, 1, 10]);

      expect(result).toBeNull();
      expect(consoleSpy).toHaveBeenCalled();
    });
  });

  describe("reset and upsert helpers", () => {
    it("resetAssessments clears assessment and assessments", () => {
      store.setAssessment({ assessmentId: 1 });
      store.upsertAssessment({ assessmentId: 1 });

      store.resetAssessments();

      expect(store.assessment).toBeNull();
      expect(store.assessments).toEqual([]);
    });

    it("upsertAssessment is a no-op for falsy input", () => {
      store.upsertAssessment(null);
      expect(store.assessments).toEqual([]);
    });

    it("upsertAssessment replaces an existing assessment by id", () => {
      store.upsertAssessment({ assessmentId: 1, title: "old" });
      store.upsertAssessment({ assessmentId: 1, title: "new" });

      expect(store.assessments).toEqual([{ assessmentId: 1, title: "new" }]);
    });

    it("upsertAnswer is a no-op when the question cannot be found", () => {
      store.setAssessment({ questions: [{ questionId: 1, answers: [] }] });

      store.upsertAnswer({ questionId: 999, answerId: 1 });

      expect(store.assessment.questions[0].answers).toEqual([]);
    });

    it("upsertAnswer initializes answers array if missing", () => {
      store.setAssessment({ questions: [{ questionId: 1 }] });

      store.upsertAnswer({ questionId: 1, answerId: 1, html: "a" });

      expect(store.assessment.questions[0].answers).toEqual([
        { questionId: 1, answerId: 1, html: "a" }
      ]);
    });
  });
});

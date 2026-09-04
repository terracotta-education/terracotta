import { beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";

vi.mock("@/services", () => ({
  submissionService: {
    getAll: vi.fn(),
    getSubmission: vi.fn(),
    updateSubmission: vi.fn(),
    updateSubmissions: vi.fn(),
    studentResponse: vi.fn(),
    getQuestionSubmissions: vi.fn(),
    createQuestionSubmissions: vi.fn(),
    updateQuestionSubmissions: vi.fn(),
    createAnswerSubmissions: vi.fn(),
    updateAnswerSubmission: vi.fn(),
    downloadAnswerFileSubmission: vi.fn()
  }
}));

import { submissionService } from "@/services";
import { submission } from "./submission.module";

describe("submission store", () => {
  let store;

  beforeEach(() => {
    setActivePinia(createPinia());
    store = submission();
    vi.clearAllMocks();
  });

  it("starts empty", () => {
    expect(store.hasSubmissions).toBe(false);
    expect(store.hasFiles).toBe(false);
    expect(store.submissions).toEqual([]);
    expect(store.files).toEqual([]);
  });

  describe("fetchSubmissions", () => {
    it("sets submissions from data on success", async () => {
      const data = [{ id: 1 }];
      submissionService.getAll.mockResolvedValue({ data });

      const result = await store.fetchSubmissions(["a"]);

      expect(submissionService.getAll).toHaveBeenCalledWith("a");
      expect(result).toEqual(data);
      expect(store.submissions).toEqual(data);
      expect(store.hasSubmissions).toBe(true);
    });

    it("falls back to [] when data is missing", async () => {
      submissionService.getAll.mockResolvedValue({});

      const result = await store.fetchSubmissions(["a"]);

      expect(result).toEqual([]);
      expect(store.submissions).toEqual([]);
    });

    it("returns [] on error", async () => {
      submissionService.getAll.mockRejectedValue(new Error("fail"));

      const result = await store.fetchSubmissions(["a"]);

      expect(result).toEqual([]);
    });
  });

  describe("fetchSubmission", () => {
    it("sets submission on success", async () => {
      const data = { id: 1 };
      submissionService.getSubmission.mockResolvedValue({ data });

      const result = await store.fetchSubmission(["a"]);

      expect(result).toEqual(data);
      expect(store.submission).toEqual(data);
    });

    it("returns null on error", async () => {
      submissionService.getSubmission.mockRejectedValue(new Error("fail"));

      const result = await store.fetchSubmission(["a"]);

      expect(result).toBeNull();
      expect(store.submission).toBeNull();
    });
  });

  describe("updateSubmission / updateSubmissions", () => {
    it("returns the raw service response on success", async () => {
      submissionService.updateSubmission.mockResolvedValue({ status: 200 });

      const result = await store.updateSubmission(["a"]);

      expect(result).toEqual({ status: 200 });
    });

    it("returns null on error", async () => {
      submissionService.updateSubmission.mockRejectedValue(new Error("fail"));

      const result = await store.updateSubmission(["a"]);

      expect(result).toBeNull();
    });

    it("updateSubmissions returns the raw response on success", async () => {
      submissionService.updateSubmissions.mockResolvedValue({ status: 200 });

      const result = await store.updateSubmissions(["a"]);

      expect(result).toEqual({ status: 200 });
    });

    it("updateSubmissions returns null on error", async () => {
      submissionService.updateSubmissions.mockRejectedValue(new Error("fail"));

      const result = await store.updateSubmissions(["a"]);

      expect(result).toBeNull();
    });
  });

  describe("fetchStudentResponse", () => {
    it("sets studentResponse on success", async () => {
      const data = { answer: "42" };
      submissionService.studentResponse.mockResolvedValue({ data });

      const result = await store.fetchStudentResponse(["a"]);

      expect(result).toEqual(data);
      expect(store.studentResponse).toEqual(data);
    });

    it("returns null on error", async () => {
      submissionService.studentResponse.mockRejectedValue(new Error("fail"));

      const result = await store.fetchStudentResponse(["a"]);

      expect(result).toBeNull();
    });
  });

  describe("fetchQuestionSubmissions", () => {
    it("sets questionSubmissions on success", async () => {
      const data = [{ id: 1 }];
      submissionService.getQuestionSubmissions.mockResolvedValue({ data });

      const result = await store.fetchQuestionSubmissions(["a"]);

      expect(result).toEqual(data);
      expect(store.questionSubmissions).toEqual(data);
    });

    it("falls back to [] when data missing", async () => {
      submissionService.getQuestionSubmissions.mockResolvedValue({});

      const result = await store.fetchQuestionSubmissions(["a"]);

      expect(result).toEqual([]);
      expect(store.questionSubmissions).toEqual([]);
    });

    it("returns [] on error", async () => {
      submissionService.getQuestionSubmissions.mockRejectedValue(
        new Error("fail")
      );

      const result = await store.fetchQuestionSubmissions(["a"]);

      expect(result).toEqual([]);
    });
  });

  describe("createQuestionSubmissions / updateQuestionSubmissions", () => {
    it("createQuestionSubmissions returns raw response on success", async () => {
      submissionService.createQuestionSubmissions.mockResolvedValue({
        status: 201
      });

      const result = await store.createQuestionSubmissions(["a"]);

      expect(result).toEqual({ status: 201 });
    });

    it("createQuestionSubmissions returns null on error", async () => {
      submissionService.createQuestionSubmissions.mockRejectedValue(
        new Error("fail")
      );

      const result = await store.createQuestionSubmissions(["a"]);

      expect(result).toBeNull();
    });

    it("updateQuestionSubmissions returns raw response on success", async () => {
      submissionService.updateQuestionSubmissions.mockResolvedValue({
        status: 200
      });

      const result = await store.updateQuestionSubmissions(["a"]);

      expect(result).toEqual({ status: 200 });
    });

    it("updateQuestionSubmissions returns null on error", async () => {
      submissionService.updateQuestionSubmissions.mockRejectedValue(
        new Error("fail")
      );

      const result = await store.updateQuestionSubmissions(["a"]);

      expect(result).toBeNull();
    });
  });

  describe("createAnswerSubmissions / updateAnswerSubmission", () => {
    it("createAnswerSubmissions returns raw response on success", async () => {
      submissionService.createAnswerSubmissions.mockResolvedValue({
        status: 201
      });

      const result = await store.createAnswerSubmissions(["a"]);

      expect(result).toEqual({ status: 201 });
    });

    it("createAnswerSubmissions returns null on error", async () => {
      submissionService.createAnswerSubmissions.mockRejectedValue(
        new Error("fail")
      );

      const result = await store.createAnswerSubmissions(["a"]);

      expect(result).toBeNull();
    });

    it("updateAnswerSubmission returns raw response on success", async () => {
      submissionService.updateAnswerSubmission.mockResolvedValue({
        status: 200
      });

      const result = await store.updateAnswerSubmission(["a"]);

      expect(result).toEqual({ status: 200 });
    });

    it("updateAnswerSubmission returns null on error", async () => {
      submissionService.updateAnswerSubmission.mockRejectedValue(
        new Error("fail")
      );

      const result = await store.updateAnswerSubmission(["a"]);

      expect(result).toBeNull();
    });
  });

  it("clearQuestionSubmissions empties the list and resolves []", async () => {
    store.questionSubmissions = [{ id: 1 }];

    const result = await store.clearQuestionSubmissions();

    expect(result).toEqual([]);
    expect(store.questionSubmissions).toEqual([]);
  });

  describe("downloadAnswerFileSubmission", () => {
    it("returns raw response on success", async () => {
      submissionService.downloadAnswerFileSubmission.mockResolvedValue({
        data: "blob"
      });

      const result = await store.downloadAnswerFileSubmission(["a"]);

      expect(result).toEqual({ data: "blob" });
    });

    it("returns null on error", async () => {
      submissionService.downloadAnswerFileSubmission.mockRejectedValue(
        new Error("fail")
      );

      const result = await store.downloadAnswerFileSubmission(["a"]);

      expect(result).toBeNull();
    });
  });

  describe("addFile / clearFile / clearFiles", () => {
    it("adds a new file entry", () => {
      store.addFile({
        file: "f1",
        name: "file1.txt",
        questionId: 1,
        submissionId: 2
      });

      expect(store.files).toEqual([
        { file: "f1", name: "file1.txt", questionId: 1, submissionId: 2 }
      ]);
      expect(store.hasFiles).toBe(true);
    });

    it("replaces an existing file entry for the same question/submission", () => {
      store.addFile({
        file: "f1",
        name: "file1.txt",
        questionId: 1,
        submissionId: 2
      });
      store.addFile({
        file: "f2",
        name: "file2.txt",
        questionId: 1,
        submissionId: 2
      });

      expect(store.files).toEqual([
        { file: "f2", name: "file2.txt", questionId: 1, submissionId: 2 }
      ]);
    });

    it("clearFile removes only the matching entry", () => {
      store.addFile({ file: "f1", name: "a", questionId: 1, submissionId: 2 });
      store.addFile({ file: "f2", name: "b", questionId: 3, submissionId: 4 });

      store.clearFile({ questionId: 1, submissionId: 2 });

      expect(store.files).toEqual([
        { file: "f2", name: "b", questionId: 3, submissionId: 4 }
      ]);
    });

    it("clearFiles empties the list", () => {
      store.addFile({ file: "f1", name: "a", questionId: 1, submissionId: 2 });

      store.clearFiles();

      expect(store.files).toEqual([]);
    });
  });

  it("resetSubmissions clears all state", () => {
    store.submissions = [{ id: 1 }];
    store.submission = { id: 1 };
    store.studentResponse = { answer: "x" };
    store.questionSubmissions = [{ id: 1 }];
    store.files = [{ file: "f" }];

    store.resetSubmissions();

    expect(store.submissions).toEqual([]);
    expect(store.submission).toBeNull();
    expect(store.studentResponse).toBeNull();
    expect(store.questionSubmissions).toEqual([]);
    expect(store.files).toEqual([]);
  });
});

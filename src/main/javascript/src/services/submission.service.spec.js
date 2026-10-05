import { beforeEach, afterEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";

import { submissionService } from "./submission.service";
import { api } from "@/store/api.module";

const BASE_URL = "https://example.com";
const SUBMISSION_PATH =
  "/api/experiments/1/conditions/2/treatments/3/assessments/4/submissions";

function jsonResponse(body, status = 200, ok = true) {
  return {
    ok,
    status,
    text: vi.fn().mockResolvedValue(JSON.stringify(body)),
    blob: vi.fn()
  };
}

function emptyResponse(status = 204, ok = true) {
  return {
    ok,
    status,
    text: vi.fn().mockResolvedValue(""),
    blob: vi.fn()
  };
}

function textResponse(text, status = 200, ok = true) {
  return {
    ok,
    status,
    text: vi.fn().mockResolvedValue(text),
    blob: vi.fn()
  };
}

describe("submission.service", () => {
  let consoleErrorSpy;
  let consoleWarnSpy;

  beforeEach(() => {
    setActivePinia(createPinia());
    api().aud = BASE_URL;
    api().apiToken = "test-token";

    global.fetch = vi.fn();

    consoleErrorSpy = vi.spyOn(console, "error").mockImplementation(() => {});
    consoleWarnSpy = vi.spyOn(console, "warn").mockImplementation(() => {});
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  describe("getAll", () => {
    it("fetches the submissions list and returns parsed data on success", async () => {
      const payload = [{ submissionId: 1 }, { submissionId: 2 }];
      global.fetch.mockResolvedValue(jsonResponse(payload, 200));

      const result = await submissionService.getAll(1, 2, 3, 4);

      expect(global.fetch).toHaveBeenCalledWith(
        `${BASE_URL}${SUBMISSION_PATH}`,
        {
          method: "GET",
          headers: {
            Authorization: "Bearer test-token",
            "Content-Type": "application/json"
          }
        }
      );
      expect(result).toEqual({ data: payload, status: 200 });
    });

    it("omits the Authorization header when there is no api token", async () => {
      api().apiToken = "";
      global.fetch.mockResolvedValue(jsonResponse([], 200));

      await submissionService.getAll(1, 2, 3, 4);

      expect(global.fetch).toHaveBeenCalledWith(
        `${BASE_URL}${SUBMISSION_PATH}`,
        {
          method: "GET",
          headers: {}
        }
      );
    });

    it("returns an empty data array on a 204 response", async () => {
      global.fetch.mockResolvedValue(emptyResponse(204));

      const result = await submissionService.getAll(1, 2, 3, 4);

      expect(result).toEqual({ data: [], status: 204 });
    });

    it("logs and returns an error object on a 404", async () => {
      global.fetch.mockResolvedValue(
        jsonResponse({ message: "not found" }, 404, false)
      );

      const result = await submissionService.getAll(1, 2, 3, 4);

      expect(consoleWarnSpy).toHaveBeenCalled();
      expect(result).toEqual({
        status: 404,
        error: { message: "not found" }
      });
    });

    it("logs and returns an error object on a 500", async () => {
      global.fetch.mockResolvedValue(
        jsonResponse({ message: "boom" }, 500, false)
      );

      const result = await submissionService.getAll(1, 2, 3, 4);

      expect(consoleErrorSpy).toHaveBeenCalled();
      expect(result).toEqual({ status: 500, error: { message: "boom" } });
    });

    it("returns an error object with the response as fallback on a non-json non-ok status", async () => {
      const response = textResponse("", 409, false);
      global.fetch.mockResolvedValue(response);

      const result = await submissionService.getAll(1, 2, 3, 4);

      expect(consoleWarnSpy).not.toHaveBeenCalled();
      expect(result).toEqual({ status: 409, error: response });
    });

    it("returns the raw response when ok but there is no body", async () => {
      const response = textResponse("", 200, true);
      global.fetch.mockResolvedValue(response);

      const result = await submissionService.getAll(1, 2, 3, 4);

      expect(result).toBe(response);
    });

    it("catches and reports errors thrown while reading the response", async () => {
      const failure = new Error("network exploded");
      const response = {
        ok: true,
        status: 200,
        text: vi.fn().mockRejectedValue(failure)
      };
      global.fetch.mockResolvedValue(response);

      const result = await submissionService.getAll(1, 2, 3, 4);

      expect(consoleErrorSpy).toHaveBeenCalledWith(
        "handleResponse | catch",
        { error: failure }
      );
      expect(result).toEqual({ error: failure, status: 200 });
    });
  });

  describe("getSubmission", () => {
    it("fetches a single submission by id", async () => {
      const payload = { submissionId: 5 };
      global.fetch.mockResolvedValue(jsonResponse(payload, 200));

      const result = await submissionService.getSubmission(1, 2, 3, 4, 5);

      expect(global.fetch).toHaveBeenCalledWith(
        `${BASE_URL}${SUBMISSION_PATH}/5`,
        expect.objectContaining({ method: "GET" })
      );
      expect(result).toEqual({ data: payload, status: 200 });
    });
  });

  describe("updateSubmission", () => {
    it("PUTs the altered grade fields with a JSON body", async () => {
      const payload = { submissionId: 5, alteredGrade: 90 };
      global.fetch.mockResolvedValue(jsonResponse(payload, 200));

      const result = await submissionService.updateSubmission(
        1,
        2,
        3,
        4,
        5,
        90,
        95,
        true
      );

      expect(global.fetch).toHaveBeenCalledWith(
        `${BASE_URL}${SUBMISSION_PATH}/5`,
        {
          method: "PUT",
          headers: {
            Authorization: "Bearer test-token",
            "Content-Type": "application/json"
          },
          body: JSON.stringify({
            alteredCalculatedGrade: 90,
            totalAlteredGrade: 95,
            gradeOverridden: true
          })
        }
      );
      expect(result).toEqual({ data: payload, status: 200 });
    });
  });

  describe("updateSubmissions", () => {
    it("PUTs the full submissions array", async () => {
      const submissions = [{ submissionId: 1 }, { submissionId: 2 }];
      global.fetch.mockResolvedValue(jsonResponse(submissions, 200));

      const result = await submissionService.updateSubmissions(
        1,
        2,
        3,
        4,
        submissions
      );

      expect(global.fetch).toHaveBeenCalledWith(
        `${BASE_URL}${SUBMISSION_PATH}`,
        {
          method: "PUT",
          headers: {
            Authorization: "Bearer test-token",
            "Content-Type": "application/json"
          },
          body: JSON.stringify(submissions)
        }
      );
      expect(result).toEqual({ data: submissions, status: 200 });
    });
  });

  describe("getQuestionSubmissions", () => {
    it("fetches question submissions with the answer/comment query params", async () => {
      const payload = [{ questionSubmissionId: 1 }];
      global.fetch.mockResolvedValue(jsonResponse(payload, 200));

      const result = await submissionService.getQuestionSubmissions(
        1,
        2,
        3,
        4,
        5
      );

      expect(global.fetch).toHaveBeenCalledWith(
        `${BASE_URL}${SUBMISSION_PATH}/5/question_submissions?answer_submissions=true&question_submission_comments=true`,
        expect.objectContaining({ method: "GET" })
      );
      expect(result).toEqual({ data: payload, status: 200 });
    });
  });

  describe("studentResponse", () => {
    it("fetches the student response with the answer_submissions query param", async () => {
      const payload = { questionSubmissionId: 1 };
      global.fetch.mockResolvedValue(jsonResponse(payload, 200));

      const result = await submissionService.studentResponse(1, 2, 3, 4, 5);

      expect(global.fetch).toHaveBeenCalledWith(
        `${BASE_URL}${SUBMISSION_PATH}/5/question_submissions/?answer_submissions=true`,
        expect.objectContaining({ method: "GET" })
      );
      expect(result).toEqual({ data: payload, status: 200 });
    });
  });

  describe("createQuestionSubmissions", () => {
    it("sends only the JSON request when no answers contain a file", async () => {
      const payload = { status: "ok" };
      global.fetch.mockResolvedValue(jsonResponse(payload, 200));

      const questions = [
        {
          questionId: 1,
          answerSubmissionDtoList: [{ response: "some text" }]
        }
      ];

      const result = await submissionService.createQuestionSubmissions(
        1,
        2,
        3,
        4,
        5,
        questions
      );

      expect(global.fetch).toHaveBeenCalledTimes(1);
      expect(global.fetch).toHaveBeenCalledWith(
        `${BASE_URL}${SUBMISSION_PATH}/5/question_submissions`,
        {
          method: "POST",
          headers: {
            Authorization: "Bearer test-token",
            "Content-Type": "application/json"
          },
          body: JSON.stringify(questions)
        }
      );
      expect(result).toEqual({ data: payload, status: 200 });
    });

    it("sends a multipart request per file answer and a combined json request for the rest", async () => {
      const okResponse = jsonResponse({ ok: true }, 200);
      global.fetch.mockResolvedValue(okResponse);

      const file = new File(["contents"], "answer.txt", {
        type: "text/plain"
      });

      const questions = [
        {
          questionId: 1,
          answerSubmissionDtoList: [{ response: file, type: "FILE" }]
        },
        {
          questionId: 2,
          answerSubmissionDtoList: [{ response: "plain text" }]
        }
      ];

      const result = await submissionService.createQuestionSubmissions(
        1,
        2,
        3,
        4,
        5,
        questions
      );

      expect(global.fetch).toHaveBeenCalledTimes(2);

      const fileCallArgs = global.fetch.mock.calls[0];
      expect(fileCallArgs[0]).toBe(
        `${BASE_URL}${SUBMISSION_PATH}/5/question_submissions/file`
      );
      expect(fileCallArgs[1].method).toBe("POST");
      expect(fileCallArgs[1].headers).toEqual({
        Authorization: "Bearer test-token"
      });
      expect(fileCallArgs[1].body).toBeInstanceOf(FormData);
      expect(fileCallArgs[1].body.get("file")).toBe(file);
      expect(
        JSON.parse(fileCallArgs[1].body.get("question_dto"))
      ).toMatchObject({ questionId: 1 });

      const jsonCallArgs = global.fetch.mock.calls[1];
      expect(jsonCallArgs[0]).toBe(
        `${BASE_URL}${SUBMISSION_PATH}/5/question_submissions`
      );
      expect(jsonCallArgs[1].headers).toEqual({
        Authorization: "Bearer test-token",
        "Content-Type": "application/json"
      });

      expect(result).toEqual({ data: { ok: true }, status: 200 });
    });

    it("returns the first failing response when one of several parallel requests fails", async () => {
      const failing = jsonResponse({ message: "bad" }, 400, false);
      const succeeding = jsonResponse({ ok: true }, 200, true);

      global.fetch
        .mockResolvedValueOnce(failing)
        .mockResolvedValueOnce(succeeding);

      const file = new File(["contents"], "answer.txt", {
        type: "text/plain"
      });

      const questions = [
        {
          questionId: 1,
          answerSubmissionDtoList: [{ response: file, type: "FILE" }]
        },
        {
          questionId: 2,
          answerSubmissionDtoList: [{ response: "plain text" }]
        }
      ];

      const result = await submissionService.createQuestionSubmissions(
        1,
        2,
        3,
        4,
        5,
        questions
      );

      expect(result).toEqual({ status: 400, error: { message: "bad" } });
    });

    it("catches and reports errors when a parallel request rejects", async () => {
      const failure = new Error("fetch failed");
      global.fetch.mockRejectedValue(failure);

      const questions = [
        {
          questionId: 1,
          answerSubmissionDtoList: [{ response: "plain text" }]
        }
      ];

      const result = await submissionService.createQuestionSubmissions(
        1,
        2,
        3,
        4,
        5,
        questions
      );

      expect(consoleErrorSpy).toHaveBeenCalledWith(
        "handleParallelRequests | catch",
        failure
      );
      expect(result).toEqual({ error: failure });
    });
  });

  describe("updateQuestionSubmissions", () => {
    it("PUTs a JSON body when the response is not a file", async () => {
      const payload = { questionSubmissionId: 1 };
      global.fetch.mockResolvedValue(jsonResponse(payload, 200));

      const updatedResponseBody = {
        questionSubmissionId: 1,
        response: "plain text"
      };

      const result = await submissionService.updateQuestionSubmissions(
        1,
        2,
        3,
        4,
        5,
        updatedResponseBody
      );

      expect(global.fetch).toHaveBeenCalledWith(
        `${BASE_URL}${SUBMISSION_PATH}/5/question_submissions`,
        {
          method: "PUT",
          headers: {
            Authorization: "Bearer test-token",
            "Content-Type": "application/json"
          },
          body: JSON.stringify(updatedResponseBody)
        }
      );
      expect(result).toEqual({ data: payload, status: 200 });
    });

    it("PUTs multipart form data when the response is a File", async () => {
      const payload = { questionSubmissionId: 1 };
      global.fetch.mockResolvedValue(jsonResponse(payload, 200));

      const file = new File(["contents"], "answer.txt", {
        type: "text/plain"
      });

      const updatedResponseBody = {
        questionSubmissionId: 9,
        response: file,
        answerSubmissionDtoList: [{ response: file, type: "FILE" }]
      };

      const result = await submissionService.updateQuestionSubmissions(
        1,
        2,
        3,
        4,
        5,
        updatedResponseBody
      );

      expect(global.fetch).toHaveBeenCalledTimes(1);
      const [url, options] = global.fetch.mock.calls[0];

      expect(url).toBe(
        `${BASE_URL}${SUBMISSION_PATH}/5/question_submissions/9/file`
      );
      expect(options.method).toBe("PUT");
      expect(options.headers).toEqual({ Authorization: "Bearer test-token" });
      expect(options.body).toBeInstanceOf(FormData);
      expect(options.body.get("file")).toBe(file);
      expect(result).toEqual({ data: payload, status: 200 });
    });
  });

  describe("createAnswerSubmissions", () => {
    it("sends only the JSON request when no answers are FILE type", async () => {
      const payload = { ok: true };
      global.fetch.mockResolvedValue(jsonResponse(payload, 200));

      const answerSubmissions = [
        { response: "text one", type: "TEXT" },
        { response: "text two", type: "TEXT" }
      ];

      const result = await submissionService.createAnswerSubmissions(
        1,
        2,
        3,
        4,
        5,
        answerSubmissions
      );

      expect(global.fetch).toHaveBeenCalledTimes(1);
      expect(global.fetch).toHaveBeenCalledWith(
        `${BASE_URL}${SUBMISSION_PATH}/5/answer_submissions`,
        {
          method: "POST",
          headers: {
            Authorization: "Bearer test-token",
            "Content-Type": "application/json"
          },
          body: JSON.stringify([{ response: "text one" }, { response: "text two" }])
        }
      );
      expect(result).toEqual({ data: payload, status: 200 });
    });

    it("sends a multipart request per FILE answer and a combined json request for the rest", async () => {
      global.fetch.mockResolvedValue(jsonResponse({ ok: true }, 200));

      const file = new File(["contents"], "answer.txt", {
        type: "text/plain"
      });

      const answerSubmissions = [
        { response: file, type: "FILE" },
        { response: "text", type: "TEXT" }
      ];

      const result = await submissionService.createAnswerSubmissions(
        1,
        2,
        3,
        4,
        5,
        answerSubmissions
      );

      expect(global.fetch).toHaveBeenCalledTimes(2);

      const fileCallArgs = global.fetch.mock.calls[0];
      expect(fileCallArgs[0]).toBe(
        `${BASE_URL}${SUBMISSION_PATH}/5/answer_submissions/file`
      );
      expect(fileCallArgs[1].body).toBeInstanceOf(FormData);
      expect(fileCallArgs[1].body.get("file")).toBe(file);
      expect(
        JSON.parse(fileCallArgs[1].body.get("answer_dto"))
      ).toMatchObject({ response: null });

      const jsonCallArgs = global.fetch.mock.calls[1];
      expect(jsonCallArgs[0]).toBe(
        `${BASE_URL}${SUBMISSION_PATH}/5/answer_submissions`
      );

      expect(result).toEqual({ data: { ok: true }, status: 200 });
    });
  });

  describe("updateAnswerSubmission", () => {
    it("PUTs a JSON body when the response is not a file", async () => {
      const payload = { answerSubmissionId: 7 };
      global.fetch.mockResolvedValue(jsonResponse(payload, 200));

      const answerSubmission = { response: "plain text" };

      const result = await submissionService.updateAnswerSubmission(
        1,
        2,
        3,
        4,
        5,
        6,
        7,
        answerSubmission
      );

      expect(global.fetch).toHaveBeenCalledWith(
        `${BASE_URL}${SUBMISSION_PATH}/5/question_submissions/6/answer_submissions/7`,
        {
          method: "PUT",
          headers: {
            Authorization: "Bearer test-token",
            "Content-Type": "application/json"
          },
          body: JSON.stringify(answerSubmission)
        }
      );
      expect(result).toEqual({ data: payload, status: 200 });
    });

    it("PUTs multipart form data when the response is a File", async () => {
      const payload = { answerSubmissionId: 7 };
      global.fetch.mockResolvedValue(jsonResponse(payload, 200));

      const file = new File(["contents"], "answer.txt", {
        type: "text/plain"
      });

      const answerSubmission = { response: file, type: "FILE" };

      const result = await submissionService.updateAnswerSubmission(
        1,
        2,
        3,
        4,
        5,
        6,
        7,
        answerSubmission
      );

      expect(global.fetch).toHaveBeenCalledTimes(1);
      const [url, options] = global.fetch.mock.calls[0];

      expect(url).toBe(
        `${BASE_URL}${SUBMISSION_PATH}/5/answer_submissions/7/file`
      );
      expect(options.method).toBe("PUT");
      expect(options.headers).toEqual({ Authorization: "Bearer test-token" });
      expect(options.body).toBeInstanceOf(FormData);
      expect(options.body.get("file")).toBe(file);
      expect(
        JSON.parse(options.body.get("answer_dto"))
      ).toMatchObject({ response: null });
      expect(result).toEqual({ data: payload, status: 200 });
    });
  });

  describe("downloadAnswerFileSubmission", () => {
    let createObjectURLSpy;
    let revokeObjectURLSpy;

    beforeEach(() => {
      createObjectURLSpy = vi.fn(() => "blob:mock-url");
      revokeObjectURLSpy = vi.fn();

      vi.stubGlobal("URL", {
        ...window.URL,
        createObjectURL: createObjectURLSpy,
        revokeObjectURL: revokeObjectURLSpy
      });

      vi.useFakeTimers();
    });

    afterEach(() => {
      vi.useRealTimers();
      vi.unstubAllGlobals();
    });

    it("downloads the file, triggers a click, and revokes the object URL after a delay", async () => {
      const blob = new Blob(["file contents"], { type: "text/plain" });

      global.fetch.mockResolvedValue({
        ok: true,
        status: 200,
        text: vi.fn(),
        blob: vi.fn().mockResolvedValue(blob)
      });

      const clickSpy = vi
        .spyOn(HTMLAnchorElement.prototype, "click")
        .mockImplementation(() => {});

      const result = await submissionService.downloadAnswerFileSubmission(
        1,
        2,
        3,
        4,
        5,
        6,
        7,
        "text/plain",
        "download.txt"
      );

      expect(global.fetch).toHaveBeenCalledWith(
        `${BASE_URL}${SUBMISSION_PATH}/5/question_submissions/6/answer_submissions/7/file`,
        {
          method: "GET",
          headers: {
            Authorization: "Bearer test-token",
            "Content-Type": "application/json"
          }
        }
      );
      expect(createObjectURLSpy).toHaveBeenCalled();
      expect(clickSpy).toHaveBeenCalled();
      expect(result).toBe(true);

      expect(revokeObjectURLSpy).not.toHaveBeenCalled();

      vi.advanceTimersByTime(1000);

      expect(revokeObjectURLSpy).toHaveBeenCalledWith("blob:mock-url");

      clickSpy.mockRestore();
    });
  });
});

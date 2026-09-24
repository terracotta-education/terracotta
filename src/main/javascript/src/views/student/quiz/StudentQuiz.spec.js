import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { flushPromises } from "@vue/test-utils";

// jsdom does not implement scrollIntoView; StudentQuiz calls it on page navigation.
window.HTMLElement.prototype.scrollIntoView = vi.fn();

vi.mock("@/services", () => ({
  apiService: {
    reportStep: vi.fn()
  },
  assessmentService: {
    fetchAssessmentForSubmission: vi.fn()
  },
  submissionService: {
    getQuestionSubmissions: vi.fn().mockResolvedValue({ data: [] }),
    createAnswerSubmissions: vi.fn(),
    updateAnswerSubmission: vi.fn(),
    createQuestionSubmissions: vi.fn(),
    downloadAnswerFileSubmission: vi.fn()
  },
  previewService: {
    treatmentPreview: vi.fn()
  }
}));

vi.mock("sweetalert2", () => ({
  default: {
    fire: vi.fn(async options => {
      if (typeof options.preConfirm === "function") {
        const value = await options.preConfirm();
        return { isConfirmed: true, value };
      }
      return { isConfirmed: true };
    }),
    update: vi.fn(),
    isLoading: vi.fn(() => false)
  }
}));

import Swal from "sweetalert2";
import {
  apiService,
  assessmentService,
  submissionService,
  previewService
} from "@/services";
import { mountComponent } from "@/test-utils/mount";
import { api as apiModule } from "@/store/api.module";
import StudentQuiz from "./StudentQuiz.vue";

const stubbedChildren = {
  StudentQuizRetakeBanner: true,
  StudentQuizSubmissionDetails: true,
  StudentQuizReadonlyBanner: true,
  StudentQuizIntegration: true,
  StudentQuizQuestionCard: true,
  StudentQuizPagination: true
};

const mcQuestion = {
  questionId: 10,
  questionOrder: 1,
  questionType: "MC",
  html: "<p>Pick one</p>",
  points: 5,
  answers: [{ answerId: 100, html: "A" }]
};

function mockReportStepByStep(overrides = {}) {
  const defaults = {
    view_assignment: {
      status: 200,
      data: {
        retakeDetails: { retakeAllowed: true, submissionAttemptsCount: 0 },
        submissions: [],
        maxPoints: 10,
        allowStudentViewResponses: false
      }
    },
    launch_assignment: {
      status: 200,
      data: {
        experimentId: "1",
        conditionId: 101,
        treatmentId: 102,
        assessmentId: 103,
        submissionId: 104,
        questionSubmissionDtoList: []
      }
    },
    student_submission: {
      status: 200,
      data: {}
    }
  };

  const responses = { ...defaults, ...overrides };

  apiService.reportStep.mockImplementation((experimentId, step) => {
    return Promise.resolve(responses[step] ?? { status: 200, data: {} });
  });

  return responses;
}

// Mounts StudentQuiz through the non-preview "launch a fresh attempt" flow, with the
// launched assessment flagged as an LTI/embedded integration (assessment.integration is
// truthy) and the launch_assignment response carrying the integration token fields.
// This is what StudentQuiz.vue's setupIntegration/integrationTokenCountdown/postMessage
// handling all key off of.
async function mountInIntegrationMode({
  launchUrl = "https://lms.example.com/launch",
  expirationDate,
  warningPeriod = 60000,
  checkInterval = 10000
} = {}) {
  assessmentService.fetchAssessmentForSubmission.mockResolvedValue({
    data: { assessmentId: 103, questions: [mcQuestion], integration: { launchUrl } }
  });

  mockReportStepByStep({
    launch_assignment: {
      status: 200,
      data: {
        experimentId: "1",
        conditionId: 101,
        treatmentId: 102,
        assessmentId: 103,
        submissionId: 104,
        questionSubmissionDtoList: [],
        integrationLaunchUrl: launchUrl,
        integrationTokenExpirationDate: expirationDate,
        integrationTokenWarningPeriod: warningPeriod,
        integrationTokenExpirationCheckInterval: checkInterval
      }
    }
  });

  const wrapper = mountComponent(StudentQuiz, {
    props: { experimentId: "1" },
    global: { stubs: stubbedChildren }
  });

  await flushPromises();
  await flushPromises();

  return wrapper;
}

describe("StudentQuiz", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    assessmentService.fetchAssessmentForSubmission.mockResolvedValue({
      data: { assessmentId: 103, questions: [mcQuestion] }
    });
    submissionService.getQuestionSubmissions.mockResolvedValue({ data: [] });
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it("launches a fresh attempt on mount when retakes are allowed and no attempts have been made, then renders the quiz", async () => {
    mockReportStepByStep();

    const wrapper = mountComponent(StudentQuiz, {
      props: { experimentId: "1" },
      global: { stubs: stubbedChildren }
    });

    await flushPromises();
    await flushPromises();

    expect(apiService.reportStep).toHaveBeenCalledWith("1", "view_assignment", null, false);
    expect(apiService.reportStep).toHaveBeenCalledWith("1", "launch_assignment", null, false);
    expect(assessmentService.fetchAssessmentForSubmission).toHaveBeenCalledWith(
      "1", 101, 102, 103, 104
    );

    expect(wrapper.emitted("loaded")).toBeTruthy();

    const questionCard = wrapper.findComponent({ name: "StudentQuizQuestionCard" });
    expect(questionCard.exists()).toBe(true);
    expect(questionCard.props("question")).toEqual(mcQuestion);

    const pagination = wrapper.findComponent({ name: "StudentQuizPagination" });
    expect(pagination.props("showSubmitButton")).toBe(true);
    expect(pagination.props("disableSubmitButton")).toBe(true);

    const retakeBanner = wrapper.findComponent({ name: "StudentQuizRetakeBanner" });
    expect(retakeBanner.props("canTryAgain")).toBe(false);
  });

  it("goes readonly (no new attempt) when retakes are exhausted, and lets the student browse a past submission", async () => {
    mockReportStepByStep({
      view_assignment: {
        status: 200,
        data: {
          retakeDetails: { retakeAllowed: false, submissionAttemptsCount: 2 },
          submissions: [
            {
              submissionId: 200,
              experimentId: "1",
              conditionId: 11,
              treatmentId: 12,
              assessmentId: 13,
              dateSubmitted: 1000,
              dateCreated: 500,
              totalAlteredGrade: 8
            }
          ],
          maxPoints: 10,
          allowStudentViewResponses: true
        }
      }
    });

    const wrapper = mountComponent(StudentQuiz, {
      props: { experimentId: "1" },
      global: { stubs: stubbedChildren }
    });

    await flushPromises();
    await flushPromises();

    expect(apiService.reportStep).toHaveBeenCalledTimes(1);
    expect(apiService.reportStep).not.toHaveBeenCalledWith("1", "launch_assignment", expect.anything(), expect.anything());

    const readonlyBanner = wrapper.findComponent({ name: "StudentQuizReadonlyBanner" });
    expect(readonlyBanner.exists()).toBe(true);
    expect(readonlyBanner.props("assignmentData").submissions).toHaveLength(1);

    await readonlyBanner.vm.$emit("select-submission", 200);
    await flushPromises();

    expect(assessmentService.fetchAssessmentForSubmission).toHaveBeenCalledWith(
      "1", 11, 12, 13, 200
    );
    expect(submissionService.getQuestionSubmissions).toHaveBeenCalledWith(
      "1", 11, 12, 13, 200
    );

    const submissionDetails = wrapper.findComponent({ name: "StudentQuizSubmissionDetails" });
    expect(submissionDetails.exists()).toBe(true);
    expect(submissionDetails.props("currentScore")).toBe("8 / 10");
  });

  it("starts a new attempt when the student clicks Try Again on the retake banner", async () => {
    mockReportStepByStep({
      view_assignment: {
        status: 200,
        data: {
          retakeDetails: { retakeAllowed: true, submissionAttemptsCount: 1 },
          submissions: [],
          maxPoints: 10
        }
      }
    });

    const wrapper = mountComponent(StudentQuiz, {
      props: { experimentId: "1" },
      global: { stubs: stubbedChildren }
    });

    await flushPromises();
    await flushPromises();

    expect(apiService.reportStep).toHaveBeenCalledTimes(1);

    const retakeBanner = wrapper.findComponent({ name: "StudentQuizRetakeBanner" });
    await retakeBanner.vm.$emit("try-again");
    await flushPromises();
    await flushPromises();

    expect(apiService.reportStep).toHaveBeenCalledWith("1", "launch_assignment", null, false);
    expect(assessmentService.fetchAssessmentForSubmission).toHaveBeenCalledWith(
      "1", 101, 102, 103, 104
    );
  });

  it("navigates between question pages using the pagination component's back/next events", async () => {
    const twoPageQuestions = [
      { ...mcQuestion, questionId: 10, questionOrder: 1 },
      { questionId: 11, questionOrder: 2, questionType: "PAGE_BREAK", points: 0 },
      { questionId: 12, questionOrder: 3, questionType: "MC", html: "q2", points: 5, answers: [] }
    ];

    assessmentService.fetchAssessmentForSubmission.mockResolvedValue({
      data: { assessmentId: 103, questions: twoPageQuestions }
    });
    mockReportStepByStep();

    const wrapper = mountComponent(StudentQuiz, {
      props: { experimentId: "1" },
      global: { stubs: stubbedChildren }
    });

    await flushPromises();
    await flushPromises();

    let questionCard = wrapper.findComponent({ name: "StudentQuizQuestionCard" });
    expect(questionCard.props("question").questionId).toBe(10);

    let pagination = wrapper.findComponent({ name: "StudentQuizPagination" });
    expect(pagination.props("showNextButton")).toBe(true);
    expect(pagination.props("showSubmitButton")).toBe(false);

    await pagination.vm.$emit("next");
    await flushPromises();

    questionCard = wrapper.findComponent({ name: "StudentQuizQuestionCard" });
    expect(questionCard.props("question").questionId).toBe(12);

    pagination = wrapper.findComponent({ name: "StudentQuizPagination" });
    expect(pagination.props("showSubmitButton")).toBe(true);
  });

  it("submits the quiz: saves new answers, reports the submission, and shows the submitted state", async () => {
    mockReportStepByStep();
    submissionService.createQuestionSubmissions.mockResolvedValue({ status: 201, data: {} });

    const wrapper = mountComponent(StudentQuiz, {
      props: { experimentId: "1" },
      global: { stubs: stubbedChildren }
    });

    await flushPromises();
    await flushPromises();

    const questionCard = wrapper.findComponent({ name: "StudentQuizQuestionCard" });
    await questionCard.vm.$emit("update:question-values", [
      { questionId: 10, answerId: 100, response: null }
    ]);
    await flushPromises();

    await wrapper.find("form").trigger("submit");
    await flushPromises();
    await flushPromises();
    await flushPromises();

    expect(Swal.fire).toHaveBeenCalledWith(
      expect.objectContaining({ icon: "question", text: "Are you ready to submit your answers?" })
    );

    expect(submissionService.createQuestionSubmissions).toHaveBeenCalledWith(
      "1", 101, 102, 103, 104,
      [
        expect.objectContaining({
          questionId: 10,
          answerSubmissionDtoList: [
            expect.objectContaining({ answerId: 100, response: null })
          ]
        })
      ]
    );

    expect(apiService.reportStep).toHaveBeenCalledWith(
      "1", "student_submission", { submissionIds: 104 }, false
    );

    expect(wrapper.text()).toContain("Your answers have been submitted.");
    expect(localStorage.getItem("terracotta-quiz-draft-1-103")).toBeNull();
  });

  describe("submit failure messages", () => {
    const answerAndSubmit = async () => {
      const wrapper = mountComponent(StudentQuiz, {
        props: { experimentId: "1" },
        global: { stubs: stubbedChildren }
      });

      await flushPromises();
      await flushPromises();

      await wrapper.findComponent({ name: "StudentQuizQuestionCard" }).vm.$emit("update:question-values", [
        { questionId: 10, answerId: 100, response: null }
      ]);
      await flushPromises();

      await wrapper.find("form").trigger("submit");
      await flushPromises();
      await flushPromises();
      await flushPromises();

      return wrapper;
    };

    const errorAlertText = () => Swal.fire.mock.calls
      .map(([options]) => options)
      .find(options => options?.icon === "error")?.text;

    it("tells the student their session expired when an answer save is rejected with a 401, and keeps a draft", async () => {
      mockReportStepByStep();
      // submission.service's handleResponse shape for an HTTP error - no `data` field,
      // which is what used to render as "Error submitting quiz: undefined"
      submissionService.createQuestionSubmissions.mockResolvedValue({ status: 401, error: "" });

      await answerAndSubmit();

      expect(errorAlertText()).toBe(
        "Could not submit: Your session has expired. Please close this window, open the assignment again from your course, and submit again."
      );
      expect(apiService.reportStep).not.toHaveBeenCalledWith("1", "student_submission", expect.anything(), expect.anything());
      expect(JSON.parse(localStorage.getItem("terracotta-quiz-draft-1-103")).questionValues).toEqual([
        { questionId: 10, answerId: 100, response: null }
      ]);
    });

    it("shows the server's error body and status instead of undefined", async () => {
      mockReportStepByStep();
      submissionService.createQuestionSubmissions.mockResolvedValue({
        status: 500,
        error: "Error 105: Unable to create question submissions"
      });

      await answerAndSubmit();

      expect(errorAlertText()).toBe("Could not submit: Error 105: Unable to create question submissions (error 500)");
    });

    it("says the server couldn't be reached when the request itself fails", async () => {
      mockReportStepByStep();
      // the store action catches the thrown fetch error and returns null
      submissionService.createQuestionSubmissions.mockRejectedValue(new TypeError("Failed to fetch"));

      await answerAndSubmit();

      expect(errorAlertText()).toBe(
        "Could not submit: We couldn't reach the server. Please check your internet connection and try again."
      );
    });

    it("uses a 409's message when the final submission step is rejected", async () => {
      mockReportStepByStep({
        student_submission: { status: 409, message: "This assignment has already been submitted." }
      });
      submissionService.createQuestionSubmissions.mockResolvedValue({ status: 201, data: {} });

      await answerAndSubmit();

      expect(errorAlertText()).toBe("Could not submit: This assignment has already been submitted. (error 409)");
    });
  });

  describe("data-loss safety net: draft answers", () => {
    it("saves the current answers to localStorage when the session expires", async () => {
      mockReportStepByStep();

      const wrapper = mountComponent(StudentQuiz, {
        props: { experimentId: "1" },
        global: { stubs: stubbedChildren }
      });

      await flushPromises();
      await flushPromises();

      const questionCard = wrapper.findComponent({ name: "StudentQuizQuestionCard" });
      await questionCard.vm.$emit("update:question-values", [
        { questionId: 10, answerId: 100, response: null }
      ]);
      await flushPromises();

      const apiStore = apiModule();
      apiStore.markSessionExpired();
      await flushPromises();

      const draft = JSON.parse(localStorage.getItem("terracotta-quiz-draft-1-103"));
      expect(draft.questionValues).toEqual([
        { questionId: 10, answerId: 100, response: null }
      ]);
    });

    it("offers to restore a matching draft on load and repopulates answers when confirmed", async () => {
      localStorage.setItem("terracotta-quiz-draft-1-103", JSON.stringify({
        questionValues: [{ questionId: 10, answerId: 100, response: null }],
        savedAt: Date.now()
      }));
      mockReportStepByStep();

      const wrapper = mountComponent(StudentQuiz, {
        props: { experimentId: "1" },
        global: { stubs: stubbedChildren }
      });

      await flushPromises();
      await flushPromises();

      expect(Swal.fire).toHaveBeenCalledWith(
        expect.objectContaining({ text: "We found unsaved answers from your last session. Would you like to restore them?" })
      );

      const questionCard = wrapper.findComponent({ name: "StudentQuizQuestionCard" });
      expect(questionCard.props("questionValues")).toEqual([
        { questionId: 10, answerId: 100, response: null }
      ]);
      expect(localStorage.getItem("terracotta-quiz-draft-1-103")).toBeNull();
    });

    it("leaves fresh blank answers and still clears the draft when the restore prompt is declined", async () => {
      localStorage.setItem("terracotta-quiz-draft-1-103", JSON.stringify({
        questionValues: [{ questionId: 10, answerId: 100, response: null }],
        savedAt: Date.now()
      }));
      Swal.fire.mockResolvedValueOnce({ isConfirmed: false });
      mockReportStepByStep();

      const wrapper = mountComponent(StudentQuiz, {
        props: { experimentId: "1" },
        global: { stubs: stubbedChildren }
      });

      await flushPromises();
      await flushPromises();

      const questionCard = wrapper.findComponent({ name: "StudentQuizQuestionCard" });
      expect(questionCard.props("questionValues")).toEqual([
        { questionId: 10, answerId: null, response: null }
      ]);
      expect(localStorage.getItem("terracotta-quiz-draft-1-103")).toBeNull();
    });

    it("discards a draft for a different question set without prompting", async () => {
      localStorage.setItem("terracotta-quiz-draft-1-103", JSON.stringify({
        questionValues: [{ questionId: 999, answerId: 5, response: null }],
        savedAt: Date.now()
      }));
      mockReportStepByStep();

      mountComponent(StudentQuiz, {
        props: { experimentId: "1" },
        global: { stubs: stubbedChildren }
      });

      await flushPromises();
      await flushPromises();

      expect(Swal.fire).not.toHaveBeenCalledWith(
        expect.objectContaining({ text: expect.stringContaining("unsaved answers") })
      );
      expect(localStorage.getItem("terracotta-quiz-draft-1-103")).toBeNull();
    });

    it("does not crash when localStorage access throws", async () => {
      const getItemSpy = vi.spyOn(localStorage, "getItem").mockImplementation(() => {
        throw new Error("blocked");
      });
      mockReportStepByStep();

      const wrapper = mountComponent(StudentQuiz, {
        props: { experimentId: "1" },
        global: { stubs: stubbedChildren }
      });

      await flushPromises();
      await flushPromises();

      expect(wrapper.findComponent({ name: "StudentQuizQuestionCard" }).exists()).toBe(true);
      getItemSpy.mockRestore();
    });

    it("fails silently (no crash) when localStorage.setItem throws while saving a draft on session expiry", async () => {
      const setItemSpy = vi.spyOn(localStorage, "setItem").mockImplementation(() => {
        throw new Error("storage blocked");
      });
      const consoleErrorSpy = vi.spyOn(console, "error").mockImplementation(() => {});
      mockReportStepByStep();

      const wrapper = mountComponent(StudentQuiz, {
        props: { experimentId: "1" },
        global: { stubs: stubbedChildren }
      });

      await flushPromises();
      await flushPromises();

      const apiStore = apiModule();
      apiStore.markSessionExpired();
      await flushPromises();

      expect(consoleErrorSpy).toHaveBeenCalledWith("StudentQuiz/saveDraftAnswers | catch", expect.any(Error));
      expect(wrapper.findComponent({ name: "StudentQuizQuestionCard" }).exists()).toBe(true);

      setItemSpy.mockRestore();
      consoleErrorSpy.mockRestore();
    });

    it("fails silently (no crash) when localStorage.removeItem throws while clearing the draft after a successful submit", async () => {
      const removeItemSpy = vi.spyOn(localStorage, "removeItem").mockImplementation(() => {
        throw new Error("storage blocked");
      });
      const consoleErrorSpy = vi.spyOn(console, "error").mockImplementation(() => {});
      mockReportStepByStep();
      submissionService.createQuestionSubmissions.mockResolvedValue({ status: 201, data: {} });

      const wrapper = mountComponent(StudentQuiz, {
        props: { experimentId: "1" },
        global: { stubs: stubbedChildren }
      });

      await flushPromises();
      await flushPromises();

      const questionCard = wrapper.findComponent({ name: "StudentQuizQuestionCard" });
      await questionCard.vm.$emit("update:question-values", [
        { questionId: 10, answerId: 100, response: null }
      ]);
      await flushPromises();

      await wrapper.find("form").trigger("submit");
      await flushPromises();
      await flushPromises();
      await flushPromises();

      expect(consoleErrorSpy).toHaveBeenCalledWith("StudentQuiz/clearDraftAnswers | catch", expect.any(Error));
      expect(wrapper.text()).toContain("Your answers have been submitted.");

      removeItemSpy.mockRestore();
      consoleErrorSpy.mockRestore();
    });
  });

  it("does not crash on submit when launch_assignment omits questionSubmissionDtoList (a fresh attempt)", async () => {
    // regression test: launch_assignment previously assigned this field to submissions.value
    // unchecked, so a null/missing value (as a fresh attempt with no prior question submissions
    // can return) made the later saveAnswers()'s submissions.value.find(...) throw
    // "submissions.value.find is not a function"
    mockReportStepByStep({
      launch_assignment: {
        status: 200,
        data: {
          experimentId: "1",
          conditionId: 101,
          treatmentId: 102,
          assessmentId: 103,
          submissionId: 104,
          questionSubmissionDtoList: null
        }
      }
    });
    submissionService.createQuestionSubmissions.mockResolvedValue({ status: 201, data: {} });

    const wrapper = mountComponent(StudentQuiz, {
      props: { experimentId: "1" },
      global: { stubs: stubbedChildren }
    });

    await flushPromises();
    await flushPromises();

    const questionCard = wrapper.findComponent({ name: "StudentQuizQuestionCard" });
    await questionCard.vm.$emit("update:question-values", [
      { questionId: 10, answerId: 100, response: null }
    ]);
    await flushPromises();

    await wrapper.find("form").trigger("submit");
    await flushPromises();
    await flushPromises();
    await flushPromises();

    expect(submissionService.createQuestionSubmissions).toHaveBeenCalledWith(
      "1", 101, 102, 103, 104,
      [
        expect.objectContaining({
          questionId: 10,
          answerSubmissionDtoList: [
            expect.objectContaining({ answerId: 100, response: null })
          ]
        })
      ]
    );

    expect(wrapper.text()).toContain("Your answers have been submitted.");
  });

  it("delegates file downloads from the question card to the submission store and clears the in-flight id", async () => {
    mockReportStepByStep();
    let resolveDownload;
    submissionService.downloadAnswerFileSubmission.mockImplementation(
      () => new Promise(resolve => { resolveDownload = resolve; })
    );

    const wrapper = mountComponent(StudentQuiz, {
      props: { experimentId: "1" },
      global: { stubs: stubbedChildren }
    });

    await flushPromises();
    await flushPromises();

    const questionCard = wrapper.findComponent({ name: "StudentQuizQuestionCard" });

    const payload = {
      answerSubmissionId: 55,
      conditionId: 101,
      treatmentId: 102,
      assessmentId: 103,
      submissionId: 104,
      questionSubmissionId: 900,
      mimeType: "application/pdf",
      fileName: "essay.pdf"
    };

    questionCard.vm.$emit("download-file-response", payload);
    await flushPromises();

    expect(questionCard.props("selectedDownloadId")).toBe(55);
    expect(submissionService.downloadAnswerFileSubmission).toHaveBeenCalledWith(
      "1", 101, 102, 103, 104, 900, 55, "application/pdf", "essay.pdf"
    );

    resolveDownload();
    await flushPromises();

    expect(wrapper.findComponent({ name: "StudentQuizQuestionCard" }).props("selectedDownloadId")).toBe(null);
  });

  it("loads a preview treatment instead of hitting the LMS-backed step endpoints when in preview mode", async () => {
    previewService.treatmentPreview.mockResolvedValue({
      data: {
        treatment: {
          treatmentId: 55,
          conditionId: 66,
          assessmentDto: { assessmentId: 77, questions: [mcQuestion] }
        },
        submission: { submissionId: 88 }
      }
    });

    const wrapper = mountComponent(StudentQuiz, {
      props: {
        experimentId: "1",
        preview: true,
        previewConditionId: "66",
        previewTreatmentId: "55",
        previewId: "9",
        ownerId: "owner-1"
      },
      global: { stubs: stubbedChildren }
    });

    await flushPromises();
    await flushPromises();

    expect(previewService.treatmentPreview).toHaveBeenCalledWith("1", "66", "55", "9", "owner-1");
    expect(apiService.reportStep).not.toHaveBeenCalled();
    expect(wrapper.emitted("loaded")).toBeTruthy();

    const questionCard = wrapper.findComponent({ name: "StudentQuizQuestionCard" });
    expect(questionCard.exists()).toBe(true);
    expect(questionCard.props("question")).toEqual(mcQuestion);

    expect(wrapper.findComponent({ name: "StudentQuizRetakeBanner" }).exists()).toBe(false);
  });

  it.each([
    [null, 1],
    [0, "Unlimited"],
    [7, 7]
  ])("shows the allowed-attempts count for numOfSubmissions=%s as %s", async (numOfSubmissions, expected) => {
    mockReportStepByStep({
      view_assignment: {
        status: 200,
        data: {
          retakeDetails: { retakeAllowed: false, submissionAttemptsCount: 2 },
          submissions: [],
          maxPoints: 10,
          numOfSubmissions
        }
      }
    });

    const wrapper = mountComponent(StudentQuiz, {
      props: { experimentId: "1" },
      global: { stubs: stubbedChildren }
    });

    await flushPromises();
    await flushPromises();

    const submissionDetails = wrapper.findComponent({ name: "StudentQuizSubmissionDetails" });
    expect(submissionDetails.props("allowedAttempts")).toBe(expected);
  });

  it.each([
    ["viewing responses is disabled entirely", { allowStudentViewResponses: false }, true],
    ["viewing responses is enabled with no time window", { allowStudentViewResponses: true }, false],
    ["the view window hasn't opened yet", { allowStudentViewResponses: true, studentViewResponsesAfter: Date.now() + 1000000 }, true],
    ["the view window has already closed", { allowStudentViewResponses: true, studentViewResponsesBefore: Date.now() - 1000000 }, true]
  ])("computes whether past responses are muted when %s", async (_desc, fields, expectedMuted) => {
    mockReportStepByStep({
      view_assignment: {
        status: 200,
        data: {
          retakeDetails: { retakeAllowed: false, submissionAttemptsCount: 2 },
          submissions: [],
          maxPoints: 10,
          ...fields
        }
      }
    });

    const wrapper = mountComponent(StudentQuiz, {
      props: { experimentId: "1" },
      global: { stubs: stubbedChildren }
    });

    await flushPromises();
    await flushPromises();

    const readonlyBanner = wrapper.findComponent({ name: "StudentQuizReadonlyBanner" });
    expect(readonlyBanner.props("muted")).toBe(expectedMuted);
  });

  it.each([
    ["viewing correct answers is disabled entirely", { allowStudentViewCorrectAnswers: false }, false],
    ["viewing correct answers is enabled with no time window", { allowStudentViewCorrectAnswers: true }, true],
    ["the view window hasn't opened yet", { allowStudentViewCorrectAnswers: true, studentViewCorrectAnswersAfter: Date.now() + 1000000 }, false],
    ["the view window has already closed", { allowStudentViewCorrectAnswers: true, studentViewCorrectAnswersBefore: Date.now() - 1000000 }, false]
  ])("computes whether correct answers are shown when %s", async (_desc, fields, expectedShowAnswers) => {
    // readonly mode only loads an assessment (and thus renders a question card) once a
    // past submission has been selected via the readonly banner
    mockReportStepByStep({
      view_assignment: {
        status: 200,
        data: {
          retakeDetails: { retakeAllowed: false, submissionAttemptsCount: 2 },
          submissions: [
            { submissionId: 200, experimentId: "1", conditionId: 11, treatmentId: 12, assessmentId: 13 }
          ],
          maxPoints: 10,
          ...fields
        }
      }
    });

    const wrapper = mountComponent(StudentQuiz, {
      props: { experimentId: "1" },
      global: { stubs: stubbedChildren }
    });

    await flushPromises();
    await flushPromises();

    const readonlyBanner = wrapper.findComponent({ name: "StudentQuizReadonlyBanner" });
    await readonlyBanner.vm.$emit("select-submission", 200);
    await flushPromises();

    const questionCard = wrapper.findComponent({ name: "StudentQuizQuestionCard" });
    expect(questionCard.props("showAnswers")).toBe(expectedShowAnswers);
  });

  it("requires essay and file responses (not just multiple-choice answers) before enabling submit", async () => {
    const essayFileQuestions = [
      { questionId: 20, questionOrder: 1, questionType: "ESSAY", html: "Explain", points: 5 },
      { questionId: 21, questionOrder: 2, questionType: "FILE", html: "Upload", points: 5 }
    ];
    assessmentService.fetchAssessmentForSubmission.mockResolvedValue({
      data: { assessmentId: 103, questions: essayFileQuestions }
    });
    mockReportStepByStep();

    const wrapper = mountComponent(StudentQuiz, {
      props: { experimentId: "1" },
      global: { stubs: stubbedChildren }
    });

    await flushPromises();
    await flushPromises();

    let pagination = wrapper.findComponent({ name: "StudentQuizPagination" });
    expect(pagination.props("disableSubmitButton")).toBe(true);

    const questionCard = wrapper.findComponent({ name: "StudentQuizQuestionCard" });

    // a whitespace-only essay response and a null file response both still count as unanswered
    await questionCard.vm.$emit("update:question-values", [
      { questionId: 20, answerId: null, response: "   " },
      { questionId: 21, answerId: null, response: null }
    ]);
    await flushPromises();
    pagination = wrapper.findComponent({ name: "StudentQuizPagination" });
    expect(pagination.props("disableSubmitButton")).toBe(true);

    await questionCard.vm.$emit("update:question-values", [
      { questionId: 20, answerId: null, response: "My answer" },
      { questionId: 21, answerId: null, response: "file-token" }
    ]);
    await flushPromises();
    pagination = wrapper.findComponent({ name: "StudentQuizPagination" });
    expect(pagination.props("disableSubmitButton")).toBe(false);
  });

  it("navigates backward through question pages using the pagination component's back event", async () => {
    const twoPageQuestions = [
      { ...mcQuestion, questionId: 10, questionOrder: 1 },
      { questionId: 11, questionOrder: 2, questionType: "PAGE_BREAK", points: 0 },
      { questionId: 12, questionOrder: 3, questionType: "MC", html: "q2", points: 5, answers: [] }
    ];
    previewService.treatmentPreview.mockResolvedValue({
      data: {
        treatment: { treatmentId: 55, conditionId: 66, assessmentDto: { assessmentId: 77, questions: twoPageQuestions } },
        submission: { submissionId: 88 }
      }
    });

    const wrapper = mountComponent(StudentQuiz, {
      props: {
        experimentId: "1",
        preview: true,
        previewConditionId: "66",
        previewTreatmentId: "55",
        previewId: "9",
        ownerId: "owner-1"
      },
      global: { stubs: stubbedChildren }
    });

    await flushPromises();
    await flushPromises();

    let pagination = wrapper.findComponent({ name: "StudentQuizPagination" });
    expect(pagination.props("showBackButton")).toBe(true);
    expect(pagination.props("disableBackButton")).toBe(true);

    await pagination.vm.$emit("next");
    await flushPromises();

    pagination = wrapper.findComponent({ name: "StudentQuizPagination" });
    expect(pagination.props("disableBackButton")).toBe(false);
    let questionCard = wrapper.findComponent({ name: "StudentQuizQuestionCard" });
    expect(questionCard.props("question").questionId).toBe(12);

    window.HTMLElement.prototype.scrollIntoView.mockClear();
    await pagination.vm.$emit("back");
    await flushPromises();

    questionCard = wrapper.findComponent({ name: "StudentQuizQuestionCard" });
    expect(questionCard.props("question").questionId).toBe(10);
    expect(window.HTMLElement.prototype.scrollIntoView).toHaveBeenCalled();

    pagination = wrapper.findComponent({ name: "StudentQuizPagination" });
    expect(pagination.props("disableBackButton")).toBe(true);
  });

  it("throws and surfaces an error alert when the student_submission report call itself fails", async () => {
    mockReportStepByStep({
      student_submission: { status: 500, data: "boom" }
    });
    submissionService.createQuestionSubmissions.mockResolvedValue({ status: 201, data: {} });

    const wrapper = mountComponent(StudentQuiz, {
      props: { experimentId: "1" },
      global: { stubs: stubbedChildren }
    });

    await flushPromises();
    await flushPromises();

    const questionCard = wrapper.findComponent({ name: "StudentQuizQuestionCard" });
    await questionCard.vm.$emit("update:question-values", [
      { questionId: 10, answerId: 100, response: null }
    ]);
    await flushPromises();

    await wrapper.find("form").trigger("submit");
    await flushPromises();
    await flushPromises();
    await flushPromises();

    expect(Swal.fire).toHaveBeenCalledWith(
      expect.objectContaining({ icon: "error", text: expect.stringContaining("Error submitting quiz") })
    );
    expect(wrapper.text()).not.toContain("Your answers have been submitted.");
  });

  it("shows an error alert when saving a submission fails, then recovers by re-fetching question submissions on retry", async () => {
    mockReportStepByStep();
    submissionService.createQuestionSubmissions.mockRejectedValueOnce(new Error("network down"));

    const wrapper = mountComponent(StudentQuiz, {
      props: { experimentId: "1" },
      global: { stubs: stubbedChildren }
    });

    await flushPromises();
    await flushPromises();

    const questionCard = wrapper.findComponent({ name: "StudentQuizQuestionCard" });
    await questionCard.vm.$emit("update:question-values", [
      { questionId: 10, answerId: 100, response: null }
    ]);
    await flushPromises();

    await wrapper.find("form").trigger("submit");
    await flushPromises();
    await flushPromises();
    await flushPromises();

    expect(Swal.fire).toHaveBeenCalledWith(
      expect.objectContaining({ icon: "error", text: expect.stringContaining("Could not submit") })
    );
    expect(wrapper.text()).not.toContain("Your answers have been submitted.");

    // the failed attempt reset the in-memory question submissions to null; retrying must
    // re-fetch them from the server (rather than crashing on a null .find()) before saving again
    submissionService.createQuestionSubmissions.mockResolvedValue({ status: 201, data: {} });

    await wrapper.find("form").trigger("submit");
    await flushPromises();
    await flushPromises();
    await flushPromises();

    expect(submissionService.getQuestionSubmissions).toHaveBeenCalledWith("1", 101, 102, 103, 104);
    expect(wrapper.text()).toContain("Your answers have been submitted.");
  });

  it("updates existing answer submissions and creates new ones for previously-unanswered questions on resubmit", async () => {
    const twoMcQuestions = [
      { ...mcQuestion, questionId: 10, questionOrder: 1 },
      { questionId: 30, questionOrder: 2, questionType: "MC", html: "q2", points: 5, answers: [{ answerId: 300, html: "B" }] }
    ];
    assessmentService.fetchAssessmentForSubmission.mockResolvedValue({
      data: { assessmentId: 103, questions: twoMcQuestions }
    });
    mockReportStepByStep({
      launch_assignment: {
        status: 200,
        data: {
          experimentId: "1",
          conditionId: 101,
          treatmentId: 102,
          assessmentId: 103,
          submissionId: 104,
          questionSubmissionDtoList: [
            { questionSubmissionId: 500, questionId: 10, answerSubmissionDtoList: [{ answerSubmissionId: 600, answerId: 100 }] },
            { questionSubmissionId: 501, questionId: 30, answerSubmissionDtoList: [] }
          ]
        }
      }
    });
    submissionService.updateAnswerSubmission.mockResolvedValue({ status: 200, data: {} });
    submissionService.createAnswerSubmissions.mockResolvedValue({ status: 201, data: {} });

    const wrapper = mountComponent(StudentQuiz, {
      props: { experimentId: "1" },
      global: { stubs: stubbedChildren }
    });

    await flushPromises();
    await flushPromises();

    const questionCard = wrapper.findComponent({ name: "StudentQuizQuestionCard" });
    await questionCard.vm.$emit("update:question-values", [
      { questionId: 10, answerId: 100, response: null },
      { questionId: 30, answerId: 300, response: null }
    ]);
    await flushPromises();

    await wrapper.find("form").trigger("submit");
    await flushPromises();
    await flushPromises();
    await flushPromises();

    expect(submissionService.updateAnswerSubmission).toHaveBeenCalledWith(
      "1", 101, 102, 103, 104, 500, 600,
      expect.objectContaining({ answerSubmissionId: 600, questionSubmissionId: 500, answerId: 100 })
    );
    expect(submissionService.createAnswerSubmissions).toHaveBeenCalledWith(
      "1", 101, 102, 103, 104,
      [expect.objectContaining({ questionSubmissionId: 501, answerId: 300 })]
    );
    expect(wrapper.text()).toContain("Your answers have been submitted.");
  });

  it("shows a no-attempts-available alert when launch_assignment reports the attempt limit has been reached (Error 150)", async () => {
    mockReportStepByStep({
      launch_assignment: { status: 401, data: "Error 150: no attempts left" }
    });

    mountComponent(StudentQuiz, {
      props: { experimentId: "1" },
      global: { stubs: stubbedChildren }
    });

    await flushPromises();
    await flushPromises();

    expect(Swal.fire).toHaveBeenCalledWith(
      expect.objectContaining({ icon: "error", text: "You have no more attempts available" })
    );
  });

  describe("LTI/embedded integration mode", () => {
    it("advances the token-expiration countdown through initial, warning, and expired alerts, then stops it on unmount", async () => {
      const setIntervalSpy = vi.spyOn(window, "setInterval");
      const clearIntervalSpy = vi.spyOn(window, "clearInterval");
      const baseTime = 1700000000000;

      vi.useFakeTimers({ toFake: ["Date"] });
      vi.setSystemTime(baseTime);

      try {
        const wrapper = await mountInIntegrationMode({
          expirationDate: baseTime + 100000,
          warningPeriod: 60000,
          checkInterval: 10000
        });

        expect(setIntervalSpy).toHaveBeenCalledTimes(1);
        const countdownCallback = setIntervalSpy.mock.calls[0][0];

        let alerts = wrapper.emitted("integrationsTokenAlert");
        expect(alerts.at(-1)[0]).toMatchObject({ type: "initial" });

        // 40s in: past the 10s "expired" threshold but still inside the 60s warning window
        vi.setSystemTime(baseTime + 40000);
        countdownCallback();
        await flushPromises();
        alerts = wrapper.emitted("integrationsTokenAlert");
        expect(alerts.at(-1)[0]).toMatchObject({ type: "warning" });

        // 90s in: only 10s of the token's life remains, at the "expired" threshold
        vi.setSystemTime(baseTime + 90000);
        countdownCallback();
        await flushPromises();
        alerts = wrapper.emitted("integrationsTokenAlert");
        expect(alerts.at(-1)[0]).toMatchObject({ type: "expired" });

        wrapper.unmount();
        expect(clearIntervalSpy).toHaveBeenCalled();
      } finally {
        vi.useRealTimers();
        setIntervalSpy.mockRestore();
        clearIntervalSpy.mockRestore();
      }
    });

    it("only reacts to postMessage resize events whose origin hostname matches the integration launch URL, and resizes the host iframe", async () => {
      const iframe = document.createElement("iframe");
      iframe.id = "integration-iframe";
      document.body.appendChild(iframe);

      try {
        const wrapper = await mountInIntegrationMode({ expirationDate: Date.now() + 1000000 });

        window.dispatchEvent(new MessageEvent("message", {
          origin: "https://evil.example.com",
          data: { subject: "terracotta_iframe_resize", height: 500 }
        }));
        await flushPromises();
        expect(iframe.height).toBe("");

        window.dispatchEvent(new MessageEvent("message", {
          origin: "https://lms.example.com",
          data: { subject: "terracotta_iframe_resize", height: 500 }
        }));
        await flushPromises();
        expect(iframe.height).toBe("500px");

        wrapper.unmount();
      } finally {
        iframe.remove();
      }
    });

    it("finalizes the integration submission, stops the token countdown, and selects the most recent submission when the LMS posts integrations_score", async () => {
      const wrapper = await mountInIntegrationMode({ expirationDate: Date.now() + 1000000 });

      apiService.reportStep.mockImplementation((experimentId, step) => {
        if (step === "view_assignment") {
          return Promise.resolve({
            status: 200,
            data: {
              retakeDetails: { retakeAllowed: true, submissionAttemptsCount: 1 },
              submissions: [
                { submissionId: 555, experimentId: "1", conditionId: 11, treatmentId: 12, assessmentId: 13 },
                { submissionId: 777, experimentId: "1", conditionId: 11, treatmentId: 12, assessmentId: 13 }
              ],
              maxPoints: 10
            }
          });
        }
        return Promise.resolve({ status: 200, data: {} });
      });

      window.document.dispatchEvent(new Event("integrations_score"));
      await flushPromises();
      await flushPromises();

      expect(wrapper.emitted("integrationsTokenAlert").at(-1)).toEqual([null]);
      expect(assessmentService.fetchAssessmentForSubmission).toHaveBeenCalledWith("1", 11, 12, 13, 777);

      wrapper.unmount();
    });

    it("starts a fresh attempt with LMS checks preferred when the LMS posts an integrations_reattempt event", async () => {
      const wrapper = await mountInIntegrationMode({ expirationDate: Date.now() + 1000000 });

      window.document.dispatchEvent(new Event("integrations_reattempt"));
      await flushPromises();
      await flushPromises();

      expect(apiService.reportStep).toHaveBeenCalledWith("1", "launch_assignment", null, true);

      wrapper.unmount();
    });
  });
});

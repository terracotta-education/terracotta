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
});

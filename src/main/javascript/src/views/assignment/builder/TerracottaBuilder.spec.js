import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";
import { flushPromises } from "@vue/test-utils";

const { routeParams, routerPush } = vi.hoisted(() => ({
  routeParams: { conditionId: "5", treatmentId: "10", assessmentId: "100" },
  routerPush: vi.fn()
}));

vi.mock("vue-router", () => ({
  useRoute: () => ({ params: routeParams }),
  useRouter: () => ({ push: routerPush })
}));

const swalFire = vi.fn(() => Promise.resolve({ isConfirmed: true }));
vi.mock("sweetalert2", () => ({
  default: {
    fire: (...args) => swalFire(...args),
    isLoading: () => false,
    getPopup: () => null,
    getHtmlContainer: () => null
  }
}));

vi.mock("@/services", () => ({
  assessmentService: {
    fetchAssessment: vi.fn(),
    updateAssessment: vi.fn(() => Promise.resolve({ status: 200, data: {} })),
    createQuestion: vi.fn(),
    updateQuestions: vi.fn(() => Promise.resolve({ status: 200 })),
    deleteQuestion: vi.fn(() => Promise.resolve({ status: 200 })),
    deleteQuestions: vi.fn(() => Promise.resolve({ status: 200 })),
    createAnswer: vi.fn(() => Promise.resolve({
      status: 201,
      data: { answerId: 1, questionId: 1, html: "", correct: false }
    })),
    updateAnswer: vi.fn(),
    updateAnswers: vi.fn(() => Promise.resolve({ status: 200 })),
    regradeQuestions: vi.fn(() => Promise.resolve({ status: 200 }))
  },
  submissionService: {
    getAll: vi.fn(() => Promise.resolve({ data: [] }))
  },
  exposuresService: {
    getAll: vi.fn(() => Promise.resolve([]))
  },
  treatmentService: {
    create: vi.fn(),
    update: vi.fn()
  }
}));

import { mountComponent } from "@/test-utils/mount";
import TerracottaBuilder from "./TerracottaBuilder.vue";
import BuilderHeader from "./components/BuilderHeader.vue";
import TreatmentEditorTab from "./components/TreatmentEditorTab.vue";
import TreatmentSettings from "@/views/assignment/TreatmentSettings.vue";

import { assessment as assessmentModule } from "@/store/assessment.module";
import { assignment as assignmentModule } from "@/store/assignment.module";
import { alert as alertModule } from "@/store/alert.module";

let pinia;
let assessmentStore;
let alertStore;

const baseAssessment = overrides => ({
  assessmentId: 100,
  html: "",
  questions: [],
  allowStudentViewResponses: false,
  studentViewResponsesAfter: null,
  studentViewResponsesBefore: null,
  allowStudentViewCorrectAnswers: false,
  studentViewCorrectAnswersAfter: null,
  studentViewCorrectAnswersBefore: null,
  numOfSubmissions: 1,
  multipleSubmissionScoringScheme: "MOST_RECENT",
  hoursBetweenSubmissions: 0,
  cumulativeScoringInitialPercentage: 0,
  ...overrides
});

const experimentProp = () => ({
  experimentId: 1,
  conditions: [{ conditionId: 5, name: "Condition A" }]
});

const setCurrentAssignment = assignment => {
  window.history.replaceState({ current_assignment: assignment }, "");
};

const mountBuilder = async () => {
  const wrapper = mountComponent(TerracottaBuilder, {
    pinia,
    props: { experiment: experimentProp() },
    global: {
      stubs: {
        BuilderHeader: true,
        TreatmentEditorTab: true,
        TreatmentSettings: true
      }
    }
  });

  await flushPromises();

  return wrapper;
};

describe("TerracottaBuilder", () => {
  let containerEl;

  beforeEach(async () => {
    pinia = createPinia();
    setActivePinia(pinia);
    assessmentStore = assessmentModule();
    assignmentModule();
    alertStore = alertModule();

    vi.clearAllMocks();
    swalFire.mockImplementation(() => Promise.resolve({ isConfirmed: true }));

    setCurrentAssignment({
      assignmentId: 42,
      title: "My Assignment",
      treatments: [{ treatmentId: 10 }, { treatmentId: 11 }]
    });

    const { assessmentService, submissionService, exposuresService } = await import("@/services");
    assessmentService.fetchAssessment.mockResolvedValue({ data: baseAssessment() });
    submissionService.getAll.mockResolvedValue({ data: [] });
    exposuresService.getAll.mockResolvedValue([]);

    // onMounted/onBeforeUnmount call widenContainer/shrinkContainer, which
    // assume the surrounding app shell rendered a ".steps-container-col"
    // element. Provide a stand-in so those DOM helpers don't throw.
    containerEl = document.createElement("div");
    containerEl.className = "steps-container-col col-md-6";
    document.body.appendChild(containerEl);
  });

  afterEach(() => {
    containerEl?.remove();
  });

  it("fetches the assessment, submissions, and exposures for the routed condition/treatment/assessment on mount", async () => {
    await mountBuilder();

    const { assessmentService, submissionService, exposuresService } = await import("@/services");

    expect(assessmentService.fetchAssessment).toHaveBeenCalledWith(1, 5, 10, 100);
    expect(submissionService.getAll).toHaveBeenCalledWith(1, 5, 10, 100);
    expect(exposuresService.getAll).toHaveBeenCalledWith(1);
  });

  it("renders the header, tabs, and treatment/settings panes once the assessment has loaded", async () => {
    const wrapper = await mountBuilder();

    expect(wrapper.findComponent(BuilderHeader).exists()).toBe(true);
    expect(wrapper.findComponent(BuilderHeader).props()).toMatchObject({
      assignmentTitle: "My Assignment",
      conditionName: "Condition A",
      hasSingleTreatment: false
    });

    expect(wrapper.findAllComponents({ name: "VTab" }).map(t => t.text())).toEqual([
      "Treatment",
      "Settings"
    ]);
    expect(wrapper.findComponent(TreatmentEditorTab).exists()).toBe(true);

    // VWindow only renders the active window-item, so the settings pane
    // isn't created until its tab is selected.
    wrapper.vm.tab = "settings";
    await flushPromises();

    expect(wrapper.findComponent(TreatmentSettings).exists()).toBe(true);
  });

  it("does not render the builder body until the assessment has resolved", async () => {
    const { assessmentService } = await import("@/services");
    let resolveFetch;
    assessmentService.fetchAssessment.mockReturnValue(
      new Promise(resolve => { resolveFetch = resolve; })
    );

    const wrapper = mountComponent(TerracottaBuilder, {
      pinia,
      props: { experiment: experimentProp() },
      global: {
        stubs: {
          BuilderHeader: true,
          TreatmentEditorTab: true,
          TreatmentSettings: true
        }
      }
    });

    expect(wrapper.findComponent(BuilderHeader).exists()).toBe(false);

    resolveFetch({ data: baseAssessment() });
    await flushPromises();

    expect(wrapper.findComponent(BuilderHeader).exists()).toBe(true);
  });

  it("marks treatmentOptionSelected true on mount when the assessment already has questions", async () => {
    const { assessmentService } = await import("@/services");
    assessmentService.fetchAssessment.mockResolvedValue({
      data: baseAssessment({
        questions: [{ questionId: 1, questionOrder: 0, questionType: "ESSAY", html: "Q1" }]
      })
    });

    const wrapper = await mountBuilder();

    expect(wrapper.findComponent(TreatmentEditorTab).props("treatmentOptionSelected")).toBe(true);
  });

  it("marks treatmentOptionSelected false on mount when there are no questions yet", async () => {
    const wrapper = await mountBuilder();

    expect(wrapper.findComponent(TreatmentEditorTab).props("treatmentOptionSelected")).toBe(false);
  });

  it("handleAddQuestion creates the question and, for MC, also creates two blank options", async () => {
    const { assessmentService } = await import("@/services");
    assessmentService.createQuestion.mockResolvedValue({
      status: 201,
      data: { questionId: 55, questionOrder: 0, questionType: "MC", html: "" }
    });

    const wrapper = await mountBuilder();

    await wrapper.vm.handleAddQuestion("MC");
    await flushPromises();

    expect(assessmentService.createQuestion).toHaveBeenCalledWith(
      1, 5, 10, 100, 0, "MC", 1, "", null
    );
    expect(assessmentService.createAnswer).toHaveBeenCalledTimes(2);
  });

  it("reports an error and does not report success when the underlying create-question call fails", async () => {
    const { assessmentService } = await import("@/services");
    assessmentService.createQuestion.mockRejectedValue(new Error("boom"));

    const wrapper = await mountBuilder();

    await wrapper.vm.handleAddQuestion("ESSAY");
    await flushPromises();

    expect(alertStore.type).toBe("error");
    expect(alertStore.message).toContain("An error occurred while adding the question");
  });

  it("handleClearQuestions is a no-op that resolves true when there are no questions", async () => {
    const wrapper = await mountBuilder();

    const { assessmentService } = await import("@/services");
    const result = await wrapper.vm.handleClearQuestions();

    expect(result).toBe(true);
    expect(assessmentService.deleteQuestions).not.toHaveBeenCalled();
  });

  it("handleClearQuestions deletes all questions when some exist", async () => {
    const { assessmentService } = await import("@/services");
    assessmentService.fetchAssessment.mockResolvedValue({
      data: baseAssessment({
        questions: [{ questionId: 1, questionOrder: 0, questionType: "ESSAY", html: "Q1" }]
      })
    });

    const wrapper = await mountBuilder();

    const result = await wrapper.vm.handleClearQuestions();

    expect(result).toBe(true);
    expect(assessmentService.deleteQuestions).toHaveBeenCalledWith(
      1, 5, 10, 100, [{ questionId: 1, questionOrder: 0, questionType: "ESSAY", html: "Q1" }]
    );
  });

  it("handleQuestionOrderChange reorders questions and persists the new order via a batched update", async () => {
    const { assessmentService } = await import("@/services");
    assessmentService.fetchAssessment.mockResolvedValue({
      data: baseAssessment({
        questions: [
          { questionId: 1, questionOrder: 0, questionType: "ESSAY", html: "Q1" },
          { questionId: 2, questionOrder: 1, questionType: "ESSAY", html: "Q2" }
        ]
      })
    });

    const wrapper = await mountBuilder();

    await wrapper.vm.handleQuestionOrderChange({
      moved: { element: { questionId: 1 }, newIndex: 1 }
    });
    await flushPromises();

    expect(assessmentService.updateQuestions).toHaveBeenCalled();

    const orderedIds = assessmentStore.questions.map(q => q.questionId);
    expect(orderedIds).toEqual([2, 1]);
  });

  it("handleQuestionOrderChange ignores 'removed' drag events", async () => {
    const wrapper = await mountBuilder();
    const { assessmentService } = await import("@/services");

    await wrapper.vm.handleQuestionOrderChange({ removed: {} });

    expect(assessmentService.updateQuestions).not.toHaveBeenCalled();
  });

  it("saveAll blocks with a warning and does not navigate when an answerable question has no html", async () => {
    const { assessmentService } = await import("@/services");
    assessmentService.fetchAssessment.mockResolvedValue({
      data: baseAssessment({
        questions: [{ questionId: 1, questionOrder: 0, questionType: "ESSAY", html: "" }]
      })
    });

    const wrapper = await mountBuilder();

    const result = await wrapper.vm.saveAll("ExperimentSummary");

    expect(result).toBe(false);
    expect(swalFire).toHaveBeenCalledWith("Please fill or delete empty questions.");
    expect(routerPush).not.toHaveBeenCalled();
    expect(assessmentService.updateAssessment).not.toHaveBeenCalled();
  });

  it("saveAll saves the assessment/questions/answers and navigates on success", async () => {
    const { assessmentService } = await import("@/services");
    assessmentService.fetchAssessment.mockResolvedValue({
      data: baseAssessment({
        questions: [{
          questionId: 1,
          questionOrder: 0,
          questionType: "ESSAY",
          html: "Filled in",
          answers: []
        }]
      })
    });

    const wrapper = await mountBuilder();

    const result = await wrapper.vm.saveAll("ExperimentSummary");

    expect(result).toBe(true);
    expect(assessmentService.updateAssessment).toHaveBeenCalled();
    expect(assessmentService.updateQuestions).toHaveBeenCalled();
    expect(routerPush).toHaveBeenCalledWith({
      name: "ExperimentSummary",
      params: { experimentId: 1 }
    });
  });

  it("saveExit saves and navigates to ExperimentSummary when no URL validation is in progress", async () => {
    const { assessmentService } = await import("@/services");
    assessmentService.fetchAssessment.mockResolvedValue({ data: baseAssessment({ questions: [] }) });

    const wrapper = await mountBuilder();

    const result = await wrapper.vm.saveExit();

    expect(result).toBe(true);
    expect(routerPush).toHaveBeenCalledWith({
      name: "ExperimentSummary",
      params: { experimentId: 1 }
    });
  });

  it("handleBackToTreatmentModeSelection clears questions and resets treatment mode when confirmed", async () => {
    const wrapper = await mountBuilder();

    wrapper.vm.treatmentOptionSelected = true;
    await wrapper.vm.handleBackToTreatmentModeSelection();

    expect(swalFire).toHaveBeenCalled();
    expect(wrapper.vm.treatmentOptionSelected).toBe(false);
  });

  it("handleBackToTreatmentModeSelection leaves treatment mode untouched when the user cancels", async () => {
    swalFire.mockImplementation(() => Promise.resolve({ isConfirmed: false }));

    const wrapper = await mountBuilder();
    wrapper.vm.treatmentOptionSelected = true;

    await wrapper.vm.handleBackToTreatmentModeSelection();

    expect(wrapper.vm.treatmentOptionSelected).toBe(true);
  });

  it("handleIntegrationUpdate defaults null points to 0 and mirrors feedbackEnabled onto allowStudentViewResponses", async () => {
    const wrapper = await mountBuilder();

    wrapper.vm.handleIntegrationUpdate({
      points: null,
      feedbackEnabled: true,
      launchUrlValidated: true,
      pointsValidated: true
    });

    expect(assessmentStore.assessment.allowStudentViewResponses).toBe(true);
  });

  it("duplicate() bails out without copying when the copy-from dialog is dismissed", async () => {
    swalFire.mockImplementation(() => Promise.resolve({ isDismissed: true }));

    const wrapper = await mountBuilder();
    const { treatmentService } = await import("@/services");

    await wrapper.vm.duplicate({ treatments: [] });

    expect(treatmentService.update).not.toHaveBeenCalled();
  });
});

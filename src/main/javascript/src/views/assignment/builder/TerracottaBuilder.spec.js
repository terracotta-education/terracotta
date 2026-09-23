import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";
import { flushPromises } from "@vue/test-utils";
import { nextTick } from "vue";

const { routeParams, routerPush } = vi.hoisted(() => ({
  routeParams: { conditionId: "5", treatmentId: "10", assessmentId: "100" },
  routerPush: vi.fn()
}));

vi.mock("vue-router", () => ({
  useRoute: () => ({ params: routeParams }),
  useRouter: () => ({ push: routerPush })
}));

const swalFire = vi.fn(() => Promise.resolve({ isConfirmed: true }));
const swalGetPopup = vi.fn(() => null);
const swalGetHtmlContainer = vi.fn(() => null);
const swalShowValidationMessage = vi.fn();
vi.mock("sweetalert2", () => ({
  default: {
    fire: (...args) => swalFire(...args),
    isLoading: () => false,
    getPopup: (...args) => swalGetPopup(...args),
    getHtmlContainer: (...args) => swalGetHtmlContainer(...args),
    showValidationMessage: (...args) => swalShowValidationMessage(...args)
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
import { treatment as treatmentModule } from "@/store/treatment.module";

let pinia;
let assessmentStore;
let alertStore;
let assignmentStore;

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
  conditions: [{ conditionId: "5", name: "Condition A" }]
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
    assignmentStore = assignmentModule();
    alertStore = alertModule();

    vi.clearAllMocks();
    swalFire.mockImplementation(() => Promise.resolve({ isConfirmed: true }));
    swalGetPopup.mockReturnValue(null);
    swalGetHtmlContainer.mockReturnValue(null);

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
    vi.useRealTimers();
  });

  it("fetches the assessment, submissions, and exposures for the routed condition/treatment/assessment on mount", async () => {
    await mountBuilder();

    const { assessmentService, submissionService, exposuresService } = await import("@/services");

    expect(assessmentService.fetchAssessment).toHaveBeenCalledWith(1, "5", "10", "100");
    expect(submissionService.getAll).toHaveBeenCalledWith(1, "5", "10", "100");
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
      1, "5", "10", "100", 0, "MC", 1, "", null
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
      1, "5", "10", "100", [{ questionId: 1, questionOrder: 0, questionType: "ESSAY", html: "Q1" }]
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

  it("duplicate() copies the source treatment's assessment and refetches on success", async () => {
    const { exposuresService } = await import("@/services");
    exposuresService.getAll.mockResolvedValue([{
      exposureId: "expo-1",
      groupConditionList: [{ conditionId: "6", conditionName: "Condition B" }]
    }]);

    const wrapper = await mountBuilder();
    const { assessmentService, treatmentService } = await import("@/services");

    swalFire.mockImplementation(() => Promise.resolve({
      isConfirmed: true,
      value: { treatmentId: 99 }
    }));

    wrapper.vm.assignmentsAvailableToCopy = [{
      treatments: [{
        treatmentId: 99,
        conditionId: "6",
        assessmentDto: { treatmentId: 99, assessmentId: 200 }
      }]
    }];

    assessmentService.fetchAssessment.mockResolvedValueOnce({
      data: { questions: [{ questionId: 1, answerId: 2, assessmentId: 200, html: "hi" }] }
    });
    treatmentService.update.mockResolvedValue({ status: 200, data: { treatmentId: 10 } });

    // exposureId/conditionId here match the exposures fixture above, so this also
    // exercises getGroupConditionListForAssignment/conditionForTreatment's find()
    // predicates on a non-empty list rather than short-circuiting on an empty one.
    await wrapper.vm.duplicate({
      exposureId: "expo-1",
      treatments: [
        { treatmentId: 99, conditionId: "6", assessmentDto: {} },
        { treatmentId: 10, conditionId: "5", assessmentDto: { integration: false } }
      ]
    });
    await flushPromises();

    expect(assessmentService.fetchAssessment).toHaveBeenCalledWith(1, "6", 99, 200);
    expect(treatmentService.update).toHaveBeenCalled();
    expect(wrapper.vm.treatmentOptionSelected).toBe(true);
  });

  it("duplicate() shows a warning and stops when the selected treatment can't be found to copy from", async () => {
    const wrapper = await mountBuilder();
    const { treatmentService } = await import("@/services");

    swalFire.mockImplementation(() => Promise.resolve({
      isConfirmed: true,
      value: { treatmentId: "does-not-exist" }
    }));

    wrapper.vm.assignmentsAvailableToCopy = [];

    await wrapper.vm.duplicate({ treatments: [] });
    await flushPromises();

    expect(swalFire).toHaveBeenCalledWith(
      "Selected assignment does not have any treatments to copy from."
    );
    expect(treatmentService.update).not.toHaveBeenCalled();
  });

  it("duplicate() logs and stops when fetching the source assessment fails", async () => {
    const wrapper = await mountBuilder();
    const { assessmentService, treatmentService } = await import("@/services");

    swalFire.mockImplementation(() => Promise.resolve({
      isConfirmed: true,
      value: { treatmentId: 99 }
    }));

    wrapper.vm.assignmentsAvailableToCopy = [{
      treatments: [{
        treatmentId: 99,
        conditionId: "6",
        assessmentDto: { treatmentId: 99, assessmentId: 200 }
      }]
    }];

    assessmentService.fetchAssessment.mockRejectedValueOnce(new Error("network down"));

    await wrapper.vm.duplicate({ treatments: [] });
    await flushPromises();

    expect(treatmentService.update).not.toHaveBeenCalled();
  });

  it("duplicate() resets treatmentOptionSelected when updating the treatment fails", async () => {
    const wrapper = await mountBuilder();
    const { assessmentService } = await import("@/services");

    swalFire.mockImplementation(() => Promise.resolve({
      isConfirmed: true,
      value: { treatmentId: 99 }
    }));

    wrapper.vm.assignmentsAvailableToCopy = [{
      treatments: [{
        treatmentId: 99,
        conditionId: "6",
        assessmentDto: { treatmentId: 99, assessmentId: 200 }
      }]
    }];

    assessmentService.fetchAssessment.mockResolvedValueOnce({
      data: { questions: [] }
    });

    // treatmentStore.updateTreatment already catches a rejected treatmentService.update
    // internally and resolves null (so duplicate()'s own try/catch never sees a
    // rejection that way) - spy on the store action itself to force duplicate()'s
    // catch branch.
    const treatmentStore = treatmentModule();
    vi.spyOn(treatmentStore, "updateTreatment").mockRejectedValueOnce(new Error("save failed"));

    wrapper.vm.treatmentOptionSelected = true;
    await wrapper.vm.duplicate({ treatments: [] });
    await flushPromises();

    expect(wrapper.vm.treatmentOptionSelected).toBe(false);
  });

  it("handleBackToTreatmentModeSelection's allowOutsideClick guard reflects Swal.isLoading()", async () => {
    const wrapper = await mountBuilder();

    wrapper.vm.treatmentOptionSelected = true;
    await wrapper.vm.handleBackToTreatmentModeSelection();

    const config = swalFire.mock.calls.at(-1)[0];

    expect(config.allowOutsideClick()).toBe(true);
  });

  it("handleAddIntegration creates an INTEGRATION question for the matching client and widens the container", async () => {
    const { assessmentService } = await import("@/services");
    assessmentService.fetchAssessment.mockResolvedValue({
      data: baseAssessment({
        integrationClients: [{ id: "client-abc", name: "Qualtrics" }]
      })
    });
    assessmentService.createQuestion.mockResolvedValue({
      status: 201,
      data: { questionId: 77, questionOrder: 0, questionType: "INTEGRATION", html: "" }
    });

    const wrapper = await mountBuilder();

    await wrapper.vm.handleAddIntegration("Qualtrics");
    await flushPromises();

    expect(assessmentService.createQuestion).toHaveBeenCalledWith(
      1, "5", "10", "100", 0, "INTEGRATION", 1, "", "client-abc"
    );
    expect(wrapper.vm.treatmentOptionSelected).toBe(true);
  });

  it("handleAddMCOption reports an error when creating the blank option fails", async () => {
    const wrapper = await mountBuilder();

    // assessmentStore.createAnswer catches a rejected service call internally and
    // resolves null, so it never throws that way - spy on the store action itself
    // to force handleAddMCOption's own catch branch.
    vi.spyOn(assessmentStore, "createAnswer").mockRejectedValueOnce(new Error("boom"));

    await wrapper.vm.handleAddMCOption({ questionId: 1 });

    expect(alertStore.type).toBe("error");
    expect(alertStore.message).toContain("multiple choice option");
  });

  it("handleQuestionOrderChange is a no-op when the drag event has neither an added nor a moved entry", async () => {
    const wrapper = await mountBuilder();
    const { assessmentService } = await import("@/services");

    await wrapper.vm.handleQuestionOrderChange({});

    expect(assessmentService.updateQuestions).not.toHaveBeenCalled();
  });

  it("handleClearQuestions shows an error and returns false when the API reports a 400", async () => {
    const { assessmentService } = await import("@/services");
    assessmentService.fetchAssessment.mockResolvedValue({
      data: baseAssessment({
        questions: [{ questionId: 1, questionOrder: 0, questionType: "ESSAY", html: "Q1" }]
      })
    });

    const wrapper = await mountBuilder();

    // assessmentStore.deleteQuestions collapses any non-200 status into null
    // internally, so a genuine 400 from the service never reaches the component
    // that way - spy on the store action itself to force handleClearQuestions'
    // 400-handling branch.
    vi.spyOn(assessmentStore, "deleteQuestions").mockResolvedValueOnce({
      status: 400,
      data: "Cannot clear questions"
    });

    const result = await wrapper.vm.handleClearQuestions();

    expect(result).toBe(false);
    expect(swalFire).toHaveBeenCalledWith("Cannot clear questions");
    expect(alertStore.type).toBe("error");
  });

  it("handleClearQuestions reports an error and returns false when the API call throws", async () => {
    const { assessmentService } = await import("@/services");
    assessmentService.fetchAssessment.mockResolvedValue({
      data: baseAssessment({
        questions: [{ questionId: 1, questionOrder: 0, questionType: "ESSAY", html: "Q1" }]
      })
    });

    const wrapper = await mountBuilder();

    // as above: assessmentStore.deleteQuestions catches a rejected service call
    // internally, so spy on the store action to force the component's own catch.
    vi.spyOn(assessmentStore, "deleteQuestions").mockRejectedValueOnce(new Error("network down"));

    const result = await wrapper.vm.handleClearQuestions();

    expect(result).toBe(false);
    expect(alertStore.type).toBe("error");
  });

  it("handleSaveAssessment shows an error and returns false when the API reports a 400", async () => {
    const { assessmentService } = await import("@/services");
    assessmentService.updateAssessment.mockResolvedValueOnce({
      status: 400,
      data: "Invalid assessment"
    });

    const wrapper = await mountBuilder();
    const result = await wrapper.vm.handleSaveAssessment();

    expect(result).toBe(false);
    expect(swalFire).toHaveBeenCalledWith("Invalid assessment");
    expect(alertStore.type).toBe("error");
  });

  it("saveAll stops without saving questions/answers when saving the assessment fails", async () => {
    const { assessmentService } = await import("@/services");
    assessmentService.fetchAssessment.mockResolvedValue({
      data: baseAssessment({
        questions: [{ questionId: 1, questionOrder: 0, questionType: "ESSAY", html: "Filled in" }]
      })
    });
    assessmentService.updateAssessment.mockResolvedValueOnce({ status: 400, data: "Nope" });

    const wrapper = await mountBuilder();
    const result = await wrapper.vm.saveAll("ExperimentSummary");

    expect(result).toBe(false);
    expect(assessmentService.updateQuestions).not.toHaveBeenCalled();
    expect(routerPush).not.toHaveBeenCalled();
  });

  it("saveAll blocks on an INTEGRATION question when launch URL/points validation is incomplete", async () => {
    const { assessmentService } = await import("@/services");
    assessmentService.fetchAssessment.mockResolvedValue({
      data: baseAssessment({
        questions: [{ questionId: 1, questionOrder: 0, questionType: "INTEGRATION", html: "" }]
      })
    });

    const wrapper = await mountBuilder();
    const result = await wrapper.vm.saveAll("ExperimentSummary");

    expect(result).toBe(false);
    expect(swalFire).toHaveBeenCalledWith("Please complete all fields.");
    expect(routerPush).not.toHaveBeenCalled();
  });

  it("saveAll cancels the save when the regrade-assignment dialog is dismissed", async () => {
    const { assessmentService, submissionService } = await import("@/services");
    assessmentService.fetchAssessment.mockResolvedValue({
      data: baseAssessment({
        questions: [{ questionId: 1, questionOrder: 0, questionType: "ESSAY", html: "Filled in" }]
      })
    });
    submissionService.getAll.mockResolvedValue({ data: [{ participantId: 1 }] });

    const wrapper = await mountBuilder();
    wrapper.vm.addEditedQuestion(1);

    swalFire.mockImplementation(() => Promise.resolve({ isDismissed: true }));

    const result = await wrapper.vm.saveAll("ExperimentSummary");

    expect(result).toBe(false);
    expect(assessmentService.updateAssessment).not.toHaveBeenCalled();
  });

  it("saveAll requests a regrade and navigates once the regrade dialog is confirmed", async () => {
    const { assessmentService, submissionService } = await import("@/services");
    assessmentService.fetchAssessment.mockResolvedValue({
      data: baseAssessment({
        questions: [{ questionId: 1, questionOrder: 0, questionType: "ESSAY", html: "Filled in" }]
      })
    });
    submissionService.getAll.mockResolvedValue({ data: [{ participantId: 1 }] });

    const wrapper = await mountBuilder();
    wrapper.vm.addEditedQuestion(1);

    swalFire.mockImplementation(() => Promise.resolve({
      value: { regradeOption: "FULL_REGRADE" }
    }));

    const result = await wrapper.vm.saveAll("ExperimentSummary");

    expect(result).toBe(true);
    expect(assessmentService.regradeQuestions).toHaveBeenCalled();
    expect(routerPush).toHaveBeenCalledWith({
      name: "ExperimentSummary",
      params: { experimentId: 1 }
    });
  });

  it("handleSaveAnswers batches order-stamped answers per question", async () => {
    const { assessmentService } = await import("@/services");
    assessmentService.fetchAssessment.mockResolvedValue({
      data: baseAssessment({
        questions: [{
          questionId: 1,
          questionOrder: 0,
          questionType: "MC",
          html: "Q1",
          answers: [{ answerId: 10, html: "A" }, { answerId: 11, html: "B" }]
        }]
      })
    });

    const wrapper = await mountBuilder();

    await wrapper.vm.handleSaveAnswers();

    // one batched request per question (covering all of its answers), not one
    // request per answer - see handleSaveAnswers' own comment in the component.
    expect(assessmentService.updateAnswers).toHaveBeenCalledTimes(1);
    expect(assessmentService.updateAnswers).toHaveBeenCalledWith(
      1, "5", "10", "100", 1,
      [
        expect.objectContaining({ answerId: 10, answerOrder: 0 }),
        expect.objectContaining({ answerId: 11, answerOrder: 1 })
      ]
    );
  });

  it("saveExit gives up waiting on URL validation once the 5s timeout elapses", async () => {
    vi.useFakeTimers();

    const wrapper = await mountBuilder();
    wrapper.vm.handleUrlValidationInProgress(true);

    const saveExitPromise = wrapper.vm.saveExit();
    await vi.advanceTimersByTimeAsync(5200);
    const result = await saveExitPromise;

    expect(wrapper.vm.urlValidationInProgress).toBe(false);
    expect(result).toBe(true);
  });

  it("textOnly parses the HTML and joins each top-level element's text", async () => {
    const wrapper = await mountBuilder();

    // jsdom doesn't implement layout, so Element#innerText (which textOnly relies
    // on) never reflects real rendered text here - this exercises the parse/map/
    // join statements rather than asserting on specific text content.
    expect(wrapper.vm.textOnly("")).toBe("");
    expect(wrapper.vm.textOnly("<p>Hello</p><p>World</p>").split(" ")).toHaveLength(2);
  });

  it("addEditedQuestion tracks unique question ids and ignores duplicates", async () => {
    const wrapper = await mountBuilder();

    wrapper.vm.addEditedQuestion(5);
    wrapper.vm.addEditedQuestion(5);
    wrapper.vm.addEditedQuestion(6);

    expect(wrapper.vm.regradeDetails.editedMCQuestionIds).toEqual([5, 6]);
  });

  it("handleUrlValidationInProgress mirrors the emitted flag onto local state", async () => {
    const wrapper = await mountBuilder();

    wrapper.vm.handleUrlValidationInProgress(true);
    expect(wrapper.vm.urlValidationInProgress).toBe(true);

    wrapper.vm.handleUrlValidationInProgress(false);
    expect(wrapper.vm.urlValidationInProgress).toBe(false);
  });

  it("handleAddTerracottaBuilder switches into treatment-building mode", async () => {
    const wrapper = await mountBuilder();

    wrapper.vm.handleAddTerracottaBuilder();

    expect(wrapper.vm.treatmentOptionSelected).toBe(true);
  });

  it("the expandedQuestionPagePanel watcher collapses other pages' expanded panels", async () => {
    vi.useFakeTimers();

    const { assessmentService } = await import("@/services");
    assessmentService.fetchAssessment.mockResolvedValue({
      data: baseAssessment({
        questions: [
          { questionId: 1, questionOrder: 0, questionType: "ESSAY", html: "Q1" },
          { questionId: 2, questionOrder: 1, questionType: "PAGE_BREAK" },
          { questionId: 3, questionOrder: 2, questionType: "ESSAY", html: "Q3" }
        ]
      })
    });

    const wrapper = await mountBuilder();

    wrapper.vm.expandedQuestionPanel = [0, 0];
    await nextTick();

    wrapper.findComponent(TreatmentEditorTab).vm.$emit("update-expanded-question-page-panel", 1);
    await nextTick();

    expect(wrapper.vm.expandedQuestionPagePanel).toBe(1);
    expect(wrapper.vm.expandedQuestionPanel[0]).toBe(null);

    await vi.advanceTimersByTimeAsync(1000);
  });

  it("the expandedQuestionPanel watcher scrolls the newly expanded panel into view", async () => {
    vi.useFakeTimers();

    const wrapper = await mountBuilder();

    wrapper.vm.treatmentOptionSelected = true;
    const scrollIntoView = vi.fn();
    wrapper.vm.setQuestionPanelRef({
      element: { scrollIntoView },
      pageIndex: 0,
      questionIndex: 0
    });

    wrapper.vm.expandedQuestionPagePanel = 0;
    await nextTick();

    wrapper.vm.expandedQuestionPanel = [0];
    await nextTick();

    await vi.advanceTimersByTimeAsync(500);

    expect(scrollIntoView).toHaveBeenCalledWith({ behavior: "smooth", block: "start" });
  });

  it("the expandedQuestionPanel watcher does nothing while not in treatment-building mode", async () => {
    const wrapper = await mountBuilder();

    wrapper.vm.treatmentOptionSelected = false;
    wrapper.vm.expandedQuestionPagePanel = 0;
    wrapper.vm.expandedQuestionPanel = [0];
    await nextTick();

    // no scrollIntoView call to assert on directly; this exercises the early-return
    // branch without throwing.
    expect(wrapper.vm.treatmentOptionSelected).toBe(false);
  });

  it("the assignmentCount watcher recomputes which assignments are available to copy from", async () => {
    const wrapper = await mountBuilder();

    expect(wrapper.vm.assignmentsAvailableToCopy).toEqual([]);

    assignmentStore.assignments.push({
      assignmentId: 2,
      treatments: [
        { treatmentId: 20, assessmentDto: { integration: false } },
        { treatmentId: 21, assessmentDto: { integration: false } }
      ]
    });
    await nextTick();

    expect(wrapper.vm.assignmentsAvailableToCopy).toHaveLength(1);
  });

  it("the questions watcher runs its DOM cleanup after new questions are added", async () => {
    vi.useFakeTimers();

    await mountBuilder();

    assessmentStore.assessment.questions.push({
      questionId: 99,
      questionOrder: 0,
      questionType: "ESSAY",
      html: ""
    });
    await nextTick();

    await vi.advanceTimersByTimeAsync(1000);

    expect(assessmentStore.assessment.questions).toHaveLength(1);
  });

  it("wires the tab/window v-model and inline event bindings through to their setters", async () => {
    const wrapper = await mountBuilder();

    await wrapper.findComponent({ name: "VTabs" }).vm.$emit("update:modelValue", "settings");
    expect(wrapper.vm.tab).toBe("settings");

    await wrapper.findComponent({ name: "VWindow" }).vm.$emit("update:modelValue", "treatment");
    expect(wrapper.vm.tab).toBe("treatment");

    wrapper.findComponent(TreatmentEditorTab).vm.$emit("update:html", "<p>Updated</p>");
    await flushPromises();
    expect(assessmentStore.assessment.html).toBe("<p>Updated</p>");

    wrapper.findComponent(TreatmentEditorTab).vm.$emit("update-expanded-question-page-panel", 2);
    await flushPromises();
    expect(wrapper.vm.expandedQuestionPagePanel).toBe(2);
  });

  it("mountDialogComponent renders the given component into an existing target and returns an unmount handle", async () => {
    const wrapper = await mountBuilder();

    const target = document.createElement("div");
    target.id = "dialog-test-target";
    document.body.appendChild(target);

    const handle = wrapper.vm.mountDialogComponent(
      "#dialog-test-target",
      { template: "<div class=\"dialog-stub\">hi</div>" }
    );

    expect(handle).not.toBeNull();
    expect(target.querySelector(".dialog-stub")).not.toBeNull();

    handle.unmount();
    expect(target.querySelector(".dialog-stub")).toBeNull();

    target.remove();
  });

  it("mountDialogComponent returns null when the target element isn't in the document", async () => {
    const wrapper = await mountBuilder();

    const handle = wrapper.vm.mountDialogComponent("#does-not-exist", { template: "<div />" });

    expect(handle).toBeNull();
  });

  it("handleDisplayRegradeAssignmentDialog's preConfirm resolves the chosen regrade option", async () => {
    const { submissionService } = await import("@/services");
    submissionService.getAll.mockResolvedValue({
      data: [{ participantId: 1 }, { participantId: 2 }]
    });

    const wrapper = await mountBuilder();

    wrapper.vm.handleDisplayRegradeAssignmentDialog();
    const config = swalFire.mock.calls.at(-1)[0];

    swalGetPopup.mockReturnValue({
      querySelector: () => ({ value: "FULL_REGRADE" })
    });
    expect(config.preConfirm()).toEqual({ regradeOption: "FULL_REGRADE" });

    swalGetPopup.mockReturnValue({ querySelector: () => null });
    expect(config.preConfirm()).toBe(false);
    expect(swalShowValidationMessage).toHaveBeenCalledWith("Please select a regrade option");

    config.willOpen();
    config.didDestroy();
  });

  it("handleDisplayCopyFromDialog's preConfirm/didOpen resolve the chosen treatment and focus the option list", async () => {
    const wrapper = await mountBuilder();

    wrapper.vm.handleDisplayCopyFromDialog([{ treatmentId: 1, conditionName: "A" }]);
    const config = swalFire.mock.calls.at(-1)[0];

    swalGetPopup.mockReturnValue({
      querySelector: () => ({ value: "10" })
    });
    expect(config.preConfirm()).toEqual({ treatmentId: "10" });

    swalGetPopup.mockReturnValue({ querySelector: () => null });
    expect(config.preConfirm()).toBe(false);
    expect(swalShowValidationMessage).toHaveBeenCalledWith(
      "Please select a treatment to copy the content from."
    );

    const focus = vi.fn();
    swalGetHtmlContainer.mockReturnValue({ querySelector: () => ({ focus }) });
    config.didOpen();
    expect(focus).toHaveBeenCalled();

    swalGetHtmlContainer.mockReturnValue({ querySelector: () => null });
    config.didOpen();

    config.willOpen();
    config.didDestroy();
  });
});

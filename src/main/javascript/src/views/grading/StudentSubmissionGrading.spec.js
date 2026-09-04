import { beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";

vi.mock("vue-router", () => ({
  useRoute: () => ({
    params: {
      experimentId: "1",
      conditionId: "10",
      treatmentId: "100",
      assessmentId: "500",
      participantId: "1"
    }
  })
}));

vi.mock("@/services", () => ({
  assessmentService: {
    fetchAssessment: vi.fn()
  },
  submissionService: {
    studentResponse: vi.fn(),
    updateSubmissions: vi.fn(),
    updateQuestionSubmissions: vi.fn(),
    downloadAnswerFileSubmission: vi.fn()
  },
  apiService: {
    reportStep: vi.fn()
  }
}));

import {
  assessmentService,
  submissionService
} from "@/services";
import { experiment as experimentModule } from "@/store/experiment.module";
import { participants as participantsModule } from "@/store/participants.module";
import { api as apiModule } from "@/store/api.module";
import { mountComponent } from "@/test-utils/mount";
import StudentSubmissionGrading from "./StudentSubmissionGrading.vue";

function flushPromises() {
  return new Promise(resolve => setTimeout(resolve));
}

const assessmentFixture = {
  assessmentId: 500,
  maxPoints: 10,
  questions: [
    {
      questionId: 1,
      questionType: "MC",
      points: 5,
      questionOrder: 1,
      html: "Pick the right one",
      answers: [
        { answerId: 11, correct: true, html: "Correct answer" },
        { answerId: 12, correct: false, html: "Wrong answer" }
      ]
    },
    {
      questionId: 2,
      questionType: "ESSAY",
      points: 5,
      questionOrder: 2,
      html: "Explain yourself"
    }
  ],
  submissions: [
    {
      submissionId: 900,
      participantId: 1,
      dateSubmitted: 2000,
      gradeOverridden: false,
      totalAlteredGrade: 8,
      alteredCalculatedGrade: 8,
      assessmentId: 500,
      conditionId: 10,
      treatmentId: 100,
      experimentId: 1
    },
    {
      submissionId: 901,
      participantId: 1,
      dateSubmitted: 1000,
      gradeOverridden: false,
      totalAlteredGrade: 5,
      alteredCalculatedGrade: 5,
      assessmentId: 500,
      conditionId: 10,
      treatmentId: 100,
      experimentId: 1
    }
  ]
};

const studentResponseFixture = [
  {
    questionId: 1,
    questionSubmissionId: 21,
    answerSubmissionDtoList: [{ answerId: 12, answerSubmissionId: 31 }],
    alteredGrade: 0,
    calculatedPoints: 0
  },
  {
    questionId: 2,
    questionSubmissionId: 22,
    answerSubmissionDtoList: [
      { response: "My essay answer", answerSubmissionId: 32 }
    ],
    alteredGrade: null,
    calculatedPoints: 0
  }
];

describe("StudentSubmissionGrading", () => {
  let pinia;

  beforeEach(() => {
    pinia = createPinia();
    setActivePinia(pinia);

    experimentModule().setExperiment({ experimentId: 1 });
    participantsModule().participants = [
      { participantId: 1, user: { displayName: "Alice Smith" } }
    ];

    vi.clearAllMocks();

    assessmentService.fetchAssessment.mockResolvedValue({
      data: assessmentFixture
    });
    submissionService.studentResponse.mockResolvedValue({
      data: studentResponseFixture
    });
    submissionService.updateSubmissions.mockResolvedValue({ status: 200 });
    submissionService.updateQuestionSubmissions.mockResolvedValue({
      status: 200
    });
  });

  function mount() {
    return mountComponent(StudentSubmissionGrading, { pinia });
  }

  it("fetches the assessment for the routed experiment/condition/treatment on mount", async () => {
    mount();
    await flushPromises();

    expect(assessmentService.fetchAssessment).toHaveBeenCalledWith(
      1, 10, 100, 500
    );
  });

  it("shows the participant's name in the header", async () => {
    const wrapper = mount();
    await flushPromises();
    await wrapper.vm.$nextTick();

    expect(wrapper.text()).toContain("Alice Smith's response");
  });

  it("auto-selects the latest submission and loads its student response", async () => {
    const wrapper = mount();
    await flushPromises();
    await wrapper.vm.$nextTick();
    await flushPromises();

    expect(submissionService.studentResponse).toHaveBeenCalledWith(
      1, 10, 100, 500, 900
    );
    expect(wrapper.vm.selectedSubmissionId).toBe(900);
  });

  it("marks the correct answer and the (incorrect) student response for an MC question", async () => {
    const wrapper = mount();
    await flushPromises();
    await wrapper.vm.$nextTick();
    await flushPromises();
    await wrapper.vm.$nextTick();

    expect(wrapper.text()).toContain("Correct Response");
    expect(wrapper.text()).toContain("Student Response");
    expect(wrapper.find(".wrong-answer").exists()).toBe(true);

    const radios = wrapper.findAll(".radio-button input[type=radio]");
    const checkedValues = radios.filter(r => r.element.checked).map(r => r.element.value);
    expect(checkedValues).toEqual(["12"]);
  });

  it("does not mark the correct answer as wrong when the student selected it", async () => {
    submissionService.studentResponse.mockResolvedValue({ data: [
      {
        questionId: 1,
        questionSubmissionId: 21,
        answerSubmissionDtoList: [{ answerId: 11, answerSubmissionId: 31 }],
        alteredGrade: 5,
        calculatedPoints: 5
      },
      studentResponseFixture[1]
    ] });

    const wrapper = mount();
    await flushPromises();
    await wrapper.vm.$nextTick();
    await flushPromises();
    await wrapper.vm.$nextTick();

    expect(wrapper.find(".correct-answer").exists()).toBe(true);
    expect(wrapper.find(".wrong-answer").exists()).toBe(false);
  });

  it("flags the ungraded essay question with a manual-grade chip and notice", async () => {
    const wrapper = mount();
    await flushPromises();
    await wrapper.vm.$nextTick();
    await flushPromises();
    await wrapper.vm.$nextTick();

    expect(wrapper.find(".ungraded-essay-question-chip").exists()).toBe(true);
    expect(wrapper.text()).toContain("Please grade");
    expect(wrapper.text()).toContain("short answer responses (2)");
  });

  it("shows the calculated score by default and switches to override on click", async () => {
    const wrapper = mount();
    await flushPromises();
    await wrapper.vm.$nextTick();
    await flushPromises();
    await wrapper.vm.$nextTick();

    expect(wrapper.text()).toContain("Calculated Score");
    expect(wrapper.text()).toContain("0/10");

    await wrapper.find(".col-score-toggle a").trigger("click");
    await wrapper.vm.$nextTick();

    expect(wrapper.text()).toContain("Override Score");
    expect(
      wrapper.find(".input-override-grade input").exists()
    ).toBe(true);
  });

  it("editing a question's points updates the calculated grade and shows an unsaved-changes warning", async () => {
    const wrapper = mount();
    await flushPromises();
    await wrapper.vm.$nextTick();
    await flushPromises();
    await wrapper.vm.$nextTick();

    const pointsInput = wrapper.find('input[name="questionPoints"]');
    await pointsInput.setValue(3);

    expect(wrapper.vm.currentAttempt.calculatedGrade.grade).toBe(3);
    expect(wrapper.text()).toContain("Unsaved Changes");
  });

  it("saveExit persists submission + question scores and reports the step", async () => {
    const wrapper = mount();
    await flushPromises();
    await wrapper.vm.$nextTick();
    await flushPromises();
    await wrapper.vm.$nextTick();

    const reportStepSpy = vi.spyOn(apiModule(), "reportStep");

    await wrapper.vm.saveExit();
    await flushPromises();

    expect(submissionService.updateSubmissions).toHaveBeenCalledWith(
      1, 10, 100, 500,
      expect.arrayContaining([
        expect.objectContaining({
          submissionId: 900,
          gradeOverridden: false
        }),
        expect.objectContaining({
          submissionId: 901,
          gradeOverridden: false
        })
      ])
    );
    expect(submissionService.updateQuestionSubmissions).toHaveBeenCalledWith(
      1, 10, 100, 500, 900,
      expect.any(Array)
    );
    expect(reportStepSpy).toHaveBeenCalledWith({
      experimentId: 1,
      step: "student_submission",
      parameters: { submissionIds: "900" }
    });
  });
});

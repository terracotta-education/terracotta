import { beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";
import { flushPromises } from "@vue/test-utils";

vi.mock("vue-router", () => ({
  useRoute: () => ({
    params: {
      experimentId: "1",
      treatmentId: "2",
      assessmentId: "3",
      conditionId: "4"
    }
  })
}));

const swalFire = vi.fn(() => Promise.resolve({ isConfirmed: true }));
vi.mock("sweetalert2", () => ({
  default: {
    fire: (...args) => swalFire(...args)
  }
}));

vi.mock("@/services", () => ({
  assessmentService: {
    createAnswer: vi.fn(() => Promise.resolve({
      status: 201,
      data: { answerId: 999, questionId: 1, html: "", correct: false }
    })),
    deleteAnswer: vi.fn(() => Promise.resolve({ status: 200 })),
    createQuestion: vi.fn(),
    updateQuestions: vi.fn(),
    deleteQuestion: vi.fn()
  }
}));

import { mountComponent } from "@/test-utils/mount";
import MultipleChoiceQuestionEditor from "./MultipleChoiceQuestionEditor.vue";
import { assessment as assessmentModule } from "@/store/assessment.module";
import { alert as alertModule } from "@/store/alert.module";

let pinia;
let assessmentStore;

const buildQuestion = overrides => ({
  questionId: 1,
  questionOrder: 0,
  html: "Pick one",
  points: 5,
  questionType: "MC",
  randomizeAnswers: false,
  answers: [
    { answerId: 10, questionId: 1, html: "Option A", correct: false },
    { answerId: 11, questionId: 1, html: "Option B", correct: true }
  ],
  ...overrides
});

const mountEditor = async question => {
  assessmentStore.setAssessment({ questions: [question] });

  const wrapper = mountComponent(MultipleChoiceQuestionEditor, {
    pinia,
    props: { question },
    global: {
      stubs: {
        TipTapEditor: true
      }
    }
  });

  await flushPromises();

  return wrapper;
};

describe("MultipleChoiceQuestionEditor", () => {
  beforeEach(() => {
    pinia = createPinia();
    setActivePinia(pinia);
    assessmentStore = assessmentModule();
    alertModule();
    vi.clearAllMocks();
    swalFire.mockImplementation(() => Promise.resolve({ isConfirmed: true }));
  });

  it("renders a text field for each answer, labeled by option position", async () => {
    const question = buildQuestion();
    const wrapper = await mountEditor(question);

    const fields = wrapper.findAllComponents({ name: "VTextField" })
      .filter(field => field.props("label")?.startsWith("Option"));

    expect(fields.length).toBe(2);
    expect(fields[0].props("label")).toBe("Option 1");
    expect(fields[1].props("label")).toBe("Option 2");
  });

  it("marks the correct answer's toggle button as active", async () => {
    const question = buildQuestion();
    const wrapper = await mountEditor(question);

    const correctButton = wrapper.find("[aria-label='Mark option Option B as correct']");
    expect(correctButton.classes()).toContain("correct-answer");

    const incorrectButton = wrapper.find("[aria-label='Mark option Option A as correct']");
    expect(incorrectButton.classes()).not.toContain("correct-answer");
  });

  it("toggles an answer's correct flag and emits edited when its button is clicked", async () => {
    const question = buildQuestion();
    const wrapper = await mountEditor(question);

    await wrapper.find("[aria-label='Mark option Option A as correct']").trigger("click");

    const updatedAnswer = assessmentStore.assessment.questions[0].answers
      .find(answer => answer.answerId === 10);

    expect(updatedAnswer.correct).toBe(true);
    expect(wrapper.emitted("edited")).toBeTruthy();
  });

  it("updates an answer's html when its text field changes", async () => {
    const question = buildQuestion();
    const wrapper = await mountEditor(question);

    const fields = wrapper.findAllComponents({ name: "VTextField" })
      .filter(field => field.props("label")?.startsWith("Option"));

    await fields[0].vm.$emit("update:modelValue", "New option text");

    const updatedAnswer = assessmentStore.assessment.questions[0].answers
      .find(answer => answer.answerId === 10);

    expect(updatedAnswer.html).toBe("New option text");
    expect(wrapper.emitted("edited")).toBeTruthy();
  });

  it("adds a new (blank, incorrect) option via the store when 'Add Option' is clicked", async () => {
    const question = buildQuestion();
    const wrapper = await mountEditor(question);

    const addButton = wrapper.findAll("button").find(
      button => button.text() === "Add Option"
    );

    await addButton.trigger("click");
    await flushPromises();

    const { assessmentService } = await import("@/services");
    expect(assessmentService.createAnswer).toHaveBeenCalledWith(
      1, 4, 2, 3, question.questionId, "", false, 0
    );
    expect(wrapper.emitted("edited")).toBeTruthy();
  });

  it("deletes an option and emits edited on success", async () => {
    const question = buildQuestion();
    const wrapper = await mountEditor(question);

    await wrapper.find("[aria-label='Delete option Option A']").trigger("click");
    await flushPromises();

    const { assessmentService } = await import("@/services");
    expect(assessmentService.deleteAnswer).toHaveBeenCalledWith(
      1, 4, 2, 3, question.questionId, 10
    );
    expect(wrapper.emitted("edited")).toBeTruthy();
  });

  it("shows an error dialog and does not emit edited when the underlying delete call fails", async () => {
    const { assessmentService } = await import("@/services");
    assessmentService.deleteAnswer.mockImplementationOnce(() => Promise.reject(new Error("boom")));

    const question = buildQuestion();
    const wrapper = await mountEditor(question);

    await wrapper.find("[aria-label='Delete option Option A']").trigger("click");
    await flushPromises();

    expect(swalFire).toHaveBeenCalledWith("there was a problem deleting the answer");
    expect(wrapper.emitted("edited")).toBeFalsy();
  });

  it("toggles randomizeAnswers on the question and emits edited via the switch in the actions slot", async () => {
    const question = buildQuestion({ randomizeAnswers: false });
    const wrapper = await mountEditor(question);

    const randomizeSwitch = wrapper.findComponent({ name: "VSwitch" });
    await randomizeSwitch.setValue(true);

    expect(assessmentStore.assessment.questions[0].randomizeAnswers).toBe(true);
    expect(wrapper.emitted("edited")).toBeTruthy();
  });
});

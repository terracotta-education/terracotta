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
    createQuestion: vi.fn(() => Promise.resolve({
      status: 201,
      data: { questionId: 99, questionOrder: 2, questionType: "PAGE_BREAK", html: "" }
    })),
    updateQuestions: vi.fn(() => Promise.resolve({ status: 200 })),
    deleteQuestion: vi.fn(() => Promise.resolve({ status: 200 }))
  }
}));

import { mountComponent } from "@/test-utils/mount";
import QuestionEditor from "./QuestionEditor.vue";
import TipTapEditor from "@/components/editor/TipTapEditor.vue";
import { assessment as assessmentModule } from "@/store/assessment.module";
import { alert as alertModule } from "@/store/alert.module";

let pinia;
let assessmentStore;

const buildQuestion = overrides => ({
  questionId: 1,
  questionOrder: 0,
  html: "What is 2+2?",
  points: 5,
  questionType: "ESSAY",
  ...overrides
});

const mountEditor = async (question, questions, extraProps) => {
  assessmentStore.setAssessment({
    questions: questions || [question]
  });

  const wrapper = mountComponent(QuestionEditor, {
    pinia,
    props: { question, ...extraProps },
    global: {
      stubs: {
        TipTapEditor: true
      }
    }
  });

  // isLoaded flips true inside onMounted, but the DOM update is only
  // visible after Vue flushes its render job on the microtask queue.
  await flushPromises();

  return wrapper;
};

const openQuestionMenu = async wrapper => {
  const activator = wrapper.find("[aria-label^='Open question menu for']");

  await activator.trigger("click");
  await flushPromises();

  return Array.from(document.querySelectorAll(".v-list-item"));
};

describe("QuestionEditor", () => {
  beforeEach(() => {
    pinia = createPinia();
    setActivePinia(pinia);
    assessmentStore = assessmentModule();
    alertModule();
    vi.clearAllMocks();
    swalFire.mockImplementation(() => Promise.resolve({ isConfirmed: true }));
  });

  it("renders the editor and points field once loaded onMounted", async () => {
    const question = buildQuestion();
    const wrapper = await mountEditor(question);

    expect(wrapper.findComponent(TipTapEditor).exists()).toBe(true);
    expect(wrapper.find(".question-points").exists()).toBe(true);
  });

  it("passes the question's existing html to TipTapEditor as initial content", async () => {
    const question = buildQuestion({ html: "Existing content" });
    const wrapper = await mountEditor(question);

    expect(wrapper.findComponent(TipTapEditor).props("content")).toBe("Existing content");
  });

  it("updates the question's html in the store when the editor emits edited", async () => {
    const question = buildQuestion({ html: "" });
    const wrapper = await mountEditor(question);

    await wrapper.findComponent(TipTapEditor).vm.$emit("edited", "<p>New</p>");

    expect(assessmentStore.assessment.questions[0].html).toBe("<p>New</p>");
  });

  it("updates the question's points in the store when the points field changes", async () => {
    const question = buildQuestion({ points: 1 });
    const wrapper = await mountEditor(question);

    await wrapper.find(".question-points input").setValue("10");

    expect(assessmentStore.assessment.questions[0].points).toBe("10");
  });

  it("shows 'Add page break after question' when the next question is not a page break", async () => {
    const question = buildQuestion({ questionOrder: 0 });
    const other = buildQuestion({ questionId: 2, questionOrder: 1, questionType: "ESSAY" });
    const wrapper = await mountEditor(question, [question, other]);
    const items = await openQuestionMenu(wrapper);
    const itemsText = items.map(item => item.textContent).join(" | ");

    expect(itemsText).toContain("Add page break after question");
    expect(itemsText).not.toContain("Remove page break after question");
  });

  it("shows 'Remove page break after question' when the next question is a page break", async () => {
    const question = buildQuestion({ questionOrder: 0 });
    const pageBreak = buildQuestion({
      questionId: 2,
      questionOrder: 1,
      questionType: "PAGE_BREAK"
    });
    const wrapper = await mountEditor(question, [question, pageBreak]);
    const items = await openQuestionMenu(wrapper);
    const itemsText = items.map(item => item.textContent).join(" | ");

    expect(itemsText).toContain("Remove page break after question");
  });

  it("adds a page break after the question and re-saves the question order", async () => {
    const question = buildQuestion({ questionOrder: 0 });
    const wrapper = await mountEditor(question, [question]);
    const items = await openQuestionMenu(wrapper);

    const addPageBreakItem = items.find(
      item => item.textContent.includes("Add page break after question")
    );

    expect(addPageBreakItem).toBeTruthy();

    addPageBreakItem.dispatchEvent(new MouseEvent("click", { bubbles: true }));
    await flushPromises();

    const { assessmentService } = await import("@/services");
    expect(assessmentService.createQuestion).toHaveBeenCalled();
    expect(assessmentService.updateQuestions).toHaveBeenCalled();
  });

  it("prompts for confirmation and deletes the question when confirmed", async () => {
    const question = buildQuestion();
    const wrapper = await mountEditor(question, [question]);
    const items = await openQuestionMenu(wrapper);

    const deleteItem = items.find(
      item => item.textContent.includes("Delete question")
    );

    expect(deleteItem).toBeTruthy();

    deleteItem.dispatchEvent(new MouseEvent("click", { bubbles: true }));
    await flushPromises();

    const { assessmentService } = await import("@/services");
    expect(swalFire).toHaveBeenCalled();
    expect(assessmentService.deleteQuestion).toHaveBeenCalledWith(
      1, 4, 2, 3, question.questionId
    );
  });

  it("does not delete the question when the confirmation is dismissed", async () => {
    swalFire.mockImplementation(() => Promise.resolve({ isConfirmed: false }));

    const question = buildQuestion();
    const wrapper = await mountEditor(question, [question]);
    const items = await openQuestionMenu(wrapper);

    const deleteItem = items.find(
      item => item.textContent.includes("Delete question")
    );

    deleteItem.dispatchEvent(new MouseEvent("click", { bubbles: true }));
    await flushPromises();

    const { assessmentService } = await import("@/services");
    expect(assessmentService.deleteQuestion).not.toHaveBeenCalled();
  });

  it("emits edited immediately when isMC is true and the html changes", async () => {
    const question = buildQuestion({ html: "" });
    const wrapper = await mountEditor(question, [question], { isMC: true });

    await wrapper.findComponent(TipTapEditor).vm.$emit("edited", "<p>MC edit</p>");

    expect(wrapper.emitted("edited")).toBeTruthy();
  });

  it("does not emit edited for a plain (non-MC) html change", async () => {
    const question = buildQuestion({ html: "" });
    const wrapper = await mountEditor(question, [question]);

    await wrapper.findComponent(TipTapEditor).vm.$emit("edited", "<p>plain edit</p>");

    expect(wrapper.emitted("edited")).toBeFalsy();
  });
});

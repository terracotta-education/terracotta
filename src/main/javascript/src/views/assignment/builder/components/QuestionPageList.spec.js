import { describe, expect, it, vi } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import QuestionPageList from "./QuestionPageList.vue";

vi.mock("@/views/integrations/ExternalIntegrationEditor.vue", () => ({
  default: {
    name: "ExternalIntegrationEditor",
    template: "<div class=\"external-integration-editor-stub\" />"
  }
}));

vi.mock("@/views/assignment/FileUploadQuestionEditor.vue", () => ({
  default: {
    name: "FileUploadQuestionEditor",
    props: ["question"],
    emits: ["edited"],
    template: "<div class=\"file-upload-question-editor-stub\" />"
  }
}));

vi.mock("@/views/assignment/MultipleChoiceQuestionEditor.vue", () => ({
  default: {
    name: "MultipleChoiceQuestionEditor",
    props: ["question"],
    emits: ["edited"],
    template: "<div class=\"multiple-choice-question-editor-stub\" />"
  }
}));

vi.mock("@/views/assignment/PageBreak.vue", () => ({
  default: {
    name: "PageBreak",
    template: "<div class=\"page-break-stub\" />"
  }
}));

vi.mock("@/views/assignment/QuestionEditor.vue", () => ({
  default: {
    name: "QuestionEditor",
    props: ["question"],
    emits: ["edited"],
    template: "<div class=\"question-editor-stub\" />"
  }
}));

const textOnly = html => (html || "").replace(/<[^>]+>/g, "");

const buildQuestionPages = () => ([
  {
    key: "page-1",
    questionStartIndex: 0,
    pageBreakAfter: true,
    questions: [
      { questionId: 1, questionType: "MC", html: "<p>First question</p>" },
      { questionId: 2, questionType: "ESSAY", html: "<p>Second question</p>" }
    ]
  },
  {
    key: "page-2",
    questionStartIndex: 2,
    pageBreakAfter: false,
    questions: [
      { questionId: 3, questionType: "FILE", html: "<p>Third question</p>" }
    ]
  }
]);

const mountList = (overrides = {}) => mountComponent(QuestionPageList, {
  props: {
    questionPages: buildQuestionPages(),
    expandedQuestionPanel: [null, null],
    textOnly,
    ...overrides
  }
});

describe("QuestionPageList", () => {
  it("renders one question-page block per page and one panel per question", () => {
    const wrapper = mountList();

    expect(wrapper.findAll(".question-page")).toHaveLength(2);
    expect(wrapper.findAllComponents({ name: "VExpansionPanel" })).toHaveLength(3);
  });

  it("renders the stripped question text and numbers questions using questionStartIndex", () => {
    const wrapper = mountList();

    const titles = wrapper.findAllComponents({ name: "VExpansionPanelTitle" });
    expect(titles[0].text()).toContain("1");
    expect(titles[0].text()).toContain("First question");
    expect(titles[2].text()).toContain("3");
    expect(titles[2].text()).toContain("Third question");
  });

  it("renders a page break after pages flagged with pageBreakAfter", () => {
    const wrapper = mountList();

    expect(wrapper.findComponent({ name: "PageBreak" }).exists()).toBe(true);
  });

  it("resolves the correct editor component per question type (in the expanded panel of each page)", () => {
    // v-expansion-panels only renders a panel's content once it is the expanded one,
    // so exercise each question type by expanding the panel that holds it.
    const mcWrapper = mountList({ expandedQuestionPanel: [0, null] });
    expect(mcWrapper.findComponent({ name: "MultipleChoiceQuestionEditor" }).exists()).toBe(true);

    const essayWrapper = mountList({ expandedQuestionPanel: [1, null] });
    expect(essayWrapper.findComponent({ name: "QuestionEditor" }).exists()).toBe(true);

    const fileWrapper = mountList({ expandedQuestionPanel: [null, 0] });
    expect(fileWrapper.findComponent({ name: "FileUploadQuestionEditor" }).exists()).toBe(true);
  });

  it("emits question-order-change when the draggable list reports a change", () => {
    const wrapper = mountList();

    const draggable = wrapper.findComponent({ name: "draggable" });
    draggable.vm.$emit("change", { moved: { newIndex: 1, oldIndex: 0 } });

    expect(wrapper.emitted("question-order-change")).toBeTruthy();
    expect(wrapper.emitted("question-order-change")[0]).toEqual([{ moved: { newIndex: 1, oldIndex: 0 } }]);
  });

  it("emits update-expanded-question-page-panel when a panel is clicked", async () => {
    const wrapper = mountList();

    await wrapper.findAllComponents({ name: "VExpansionPanel" })[0].trigger("click");

    expect(wrapper.emitted("update-expanded-question-page-panel")).toBeTruthy();
    expect(wrapper.emitted("update-expanded-question-page-panel")[0]).toEqual([0]);
  });

  it("emits edited-question with the questionId when a child editor emits edited", () => {
    const wrapper = mountList({ expandedQuestionPanel: [0, null] });

    wrapper.findComponent({ name: "MultipleChoiceQuestionEditor" }).vm.$emit("edited");

    expect(wrapper.emitted("edited-question")).toBeTruthy();
    expect(wrapper.emitted("edited-question")[0]).toEqual([1]);
  });
});

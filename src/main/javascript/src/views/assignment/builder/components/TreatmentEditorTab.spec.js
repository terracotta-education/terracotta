import { describe, expect, it } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import TreatmentEditorTab from "./TreatmentEditorTab.vue";

const textOnly = html => (html || "").replace(/<[^>]+>/g, "");

const stubs = {
  QuestionPageList: true,
  TreatmentModeSelector: true,
  ExternalIntegrationEditor: true
};

const baseProps = {
  assessment: { assessmentId: 1 },
  expandedQuestionPanel: [],
  textOnly
};

const mountTab = (overrides = {}) => mountComponent(TreatmentEditorTab, {
  props: { ...baseProps, ...overrides },
  global: { stubs }
});

describe("TreatmentEditorTab", () => {
  it("prompts to select a treatment mode when none is selected yet", () => {
    const wrapper = mountTab({ treatmentOptionSelected: false });

    expect(wrapper.text()).toContain("Select a treatment mode");
    expect(wrapper.findComponent({ name: "QuestionPageList" }).exists()).toBe(false);
    expect(wrapper.findComponent({ name: "TreatmentModeSelector" }).exists()).toBe(true);
  });

  it("shows the empty-questions message when a builder treatment has no question pages yet", () => {
    const wrapper = mountTab({
      treatmentOptionSelected: true,
      isIntegrationType: false,
      questionPages: []
    });

    expect(wrapper.text()).toContain("Add questions to continue");
    expect(wrapper.findComponent({ name: "QuestionPageList" }).exists()).toBe(false);
  });

  it("shows the question header and list once question pages exist", () => {
    const wrapper = mountTab({
      treatmentOptionSelected: true,
      isIntegrationType: false,
      questionPages: [{ key: "page-1", questions: [{ questionId: 1 }] }]
    });

    expect(wrapper.text()).toContain("Questions");
    expect(wrapper.findComponent({ name: "QuestionPageList" }).exists()).toBe(true);
  });

  it("shows the external integration editor for integration-type treatments instead of the question list", () => {
    const wrapper = mountTab({
      treatmentOptionSelected: true,
      isIntegrationType: true,
      questions: [{ questionId: 5 }]
    });

    expect(wrapper.findComponent({ name: "ExternalIntegrationEditor" }).exists()).toBe(true);
    expect(wrapper.findComponent({ name: "QuestionPageList" }).exists()).toBe(false);
    expect(wrapper.text()).not.toContain("Add questions to continue");
  });

  it("disables Clear All when canClearAll is false and emits clear-questions when enabled and clicked", async () => {
    const disabledWrapper = mountTab({
      treatmentOptionSelected: true,
      questionPages: [{ key: "page-1", questions: [{ questionId: 1 }] }],
      canClearAll: false
    });

    expect(disabledWrapper.find(".saveButton").attributes("disabled")).not.toBeUndefined();

    const wrapper = mountTab({
      treatmentOptionSelected: true,
      questionPages: [{ key: "page-1", questions: [{ questionId: 1 }] }],
      canClearAll: true
    });

    await wrapper.find(".saveButton").trigger("click");

    expect(wrapper.emitted("clear-questions")).toBeTruthy();
  });

  it("emits update:html when the description textarea changes", async () => {
    const wrapper = mountTab({ treatmentOptionSelected: false });

    await wrapper.findComponent({ name: "VTextarea" }).setValue("New instructions");

    expect(wrapper.emitted("update:html")).toBeTruthy();
    expect(wrapper.emitted("update:html")[0]).toEqual(["New instructions"]);
  });

  it("emits save-all on form submit", async () => {
    const wrapper = mountTab({ treatmentOptionSelected: false });

    await wrapper.find("form").trigger("submit");

    expect(wrapper.emitted("save-all")).toBeTruthy();
    expect(wrapper.emitted("save-all")[0]).toEqual(["AssignmentYourAssignments"]);
  });

  it("forwards events emitted by QuestionPageList", () => {
    const wrapper = mountTab({
      treatmentOptionSelected: true,
      questionPages: [{ key: "page-1", questions: [{ questionId: 1 }] }]
    });

    const list = wrapper.findComponent({ name: "QuestionPageList" });
    list.vm.$emit("question-order-change", { moved: {} });
    list.vm.$emit("edited-question", 7);
    list.vm.$emit("update-expanded-question-page-panel", 0);
    list.vm.$emit("panel-ref", { pageIndex: 0 });

    expect(wrapper.emitted("question-order-change")[0]).toEqual([{ moved: {} }]);
    expect(wrapper.emitted("edited-question")[0]).toEqual([7]);
    expect(wrapper.emitted("update-expanded-question-page-panel")[0]).toEqual([0]);
    expect(wrapper.emitted("panel-ref")[0]).toEqual([{ pageIndex: 0 }]);
  });

  it("forwards events emitted by TreatmentModeSelector", () => {
    const wrapper = mountTab({ treatmentOptionSelected: false });

    const selector = wrapper.findComponent({ name: "TreatmentModeSelector" });
    selector.vm.$emit("add-terracotta-builder");
    selector.vm.$emit("add-integration", "Qualtrics");
    selector.vm.$emit("add-question", "MC");
    selector.vm.$emit("duplicate", { assignmentId: 1 });
    selector.vm.$emit("back-to-treatment-mode-selection");

    expect(wrapper.emitted("add-terracotta-builder")).toBeTruthy();
    expect(wrapper.emitted("add-integration")[0]).toEqual(["Qualtrics"]);
    expect(wrapper.emitted("add-question")[0]).toEqual(["MC"]);
    expect(wrapper.emitted("duplicate")[0]).toEqual([{ assignmentId: 1 }]);
    expect(wrapper.emitted("back-to-treatment-mode-selection")).toBeTruthy();
  });
});

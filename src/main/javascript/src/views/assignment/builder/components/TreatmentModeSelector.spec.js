import { describe, expect, it } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import TreatmentModeSelector from "./TreatmentModeSelector.vue";

const integrationClients = [
  { name: "Qualtrics" },
  { name: "Custom Web Activity" }
];

describe("TreatmentModeSelector", () => {
  it("shows the mode-selection buttons when no treatment option is selected yet", () => {
    const wrapper = mountComponent(TreatmentModeSelector, {
      props: {
        treatmentOptionSelected: false,
        integrationClients
      }
    });

    expect(wrapper.text()).toContain("Use Terracotta Builder");
    expect(wrapper.text()).toContain("Add External Integration");
    expect(wrapper.findComponent({ name: "QuestionTypeMenu" }).exists()).toBe(false);
    expect(wrapper.findComponent({ name: "CopyTreatmentMenu" }).exists()).toBe(false);
  });

  it("emits add-terracotta-builder when the builder button is clicked", async () => {
    const wrapper = mountComponent(TreatmentModeSelector, {
      props: { treatmentOptionSelected: false, integrationClients }
    });

    await wrapper.find(".add-treatment-type").trigger("click");

    expect(wrapper.emitted("add-terracotta-builder")).toBeTruthy();
  });

  it("emits add-integration with the client name when an integration option is clicked", async () => {
    const wrapper = mountComponent(TreatmentModeSelector, {
      props: { treatmentOptionSelected: false, integrationClients }
    });

    const buttons = wrapper.findAllComponents({ name: "VBtn" });
    await buttons[1].trigger("click");

    const items = wrapper.findAllComponents({ name: "VListItem" });
    expect(items).toHaveLength(2);

    await items[0].trigger("click");

    expect(wrapper.emitted("add-integration")).toBeTruthy();
    expect(wrapper.emitted("add-integration")[0]).toEqual(["Qualtrics"]);
  });

  it("shows the builder menus (not the integration ones) once a Terracotta-builder treatment is selected", () => {
    const wrapper = mountComponent(TreatmentModeSelector, {
      props: {
        treatmentOptionSelected: true,
        isIntegrationType: false,
        assignmentsAvailableToCopy: []
      }
    });

    expect(wrapper.findComponent({ name: "QuestionTypeMenu" }).exists()).toBe(true);
    expect(wrapper.findComponent({ name: "CopyTreatmentMenu" }).exists()).toBe(true);
  });

  it("hides the builder menus when the selected treatment is an integration type", () => {
    const wrapper = mountComponent(TreatmentModeSelector, {
      props: {
        treatmentOptionSelected: true,
        isIntegrationType: true
      }
    });

    expect(wrapper.findComponent({ name: "QuestionTypeMenu" }).exists()).toBe(false);
    expect(wrapper.findComponent({ name: "CopyTreatmentMenu" }).exists()).toBe(false);
  });

  it("forwards add-question and duplicate events from its child menus", async () => {
    const wrapper = mountComponent(TreatmentModeSelector, {
      props: {
        treatmentOptionSelected: true,
        isIntegrationType: false,
        assignmentsAvailableToCopy: [{ assignmentId: 1, title: "Pre-test" }]
      }
    });

    wrapper.findComponent({ name: "QuestionTypeMenu" }).vm.$emit("add-question", "MC");
    wrapper.findComponent({ name: "CopyTreatmentMenu" }).vm.$emit("duplicate", { assignmentId: 1 });

    expect(wrapper.emitted("add-question")[0]).toEqual(["MC"]);
    expect(wrapper.emitted("duplicate")[0]).toEqual([{ assignmentId: 1 }]);
  });

  it("shows the back-to-selection button only when display-back-to-treatment-mode-selection is true", async () => {
    const wrapper = mountComponent(TreatmentModeSelector, {
      props: {
        treatmentOptionSelected: true,
        displayBackToTreatmentModeSelection: true
      }
    });

    const buttons = wrapper.findAllComponents({ name: "VBtn" });
    const backButton = buttons.find(btn => btn.text().includes("BACK TO TREATMENT MODE SELECTION"));
    expect(backButton).toBeTruthy();

    await backButton.trigger("click");
    expect(wrapper.emitted("back-to-treatment-mode-selection")).toBeTruthy();
  });
});

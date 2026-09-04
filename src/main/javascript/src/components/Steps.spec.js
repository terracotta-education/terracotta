import { describe, expect, it, vi } from "vitest";

vi.mock("vue-router", () => ({
  useRoute: vi.fn()
}));

import { useRoute } from "vue-router";
import { mountComponent } from "@/test-utils/mount";
import Steps from "./Steps.vue";

describe("Steps", () => {
  it("renders the design section steps and highlights the current step's section as active", () => {
    useRoute.mockReturnValue({
      name: "ExperimentDesign",
      meta: { currentSection: "design" }
    });

    const wrapper = mountComponent(Steps, {
      props: {
        currentSection: "design",
        currentStep: "design_conditions",
        participationType: "CONSENT"
      }
    });

    expect(wrapper.text()).toContain("Section 1: Design");
    expect(wrapper.text()).toContain("Section 2: Participation");
    expect(wrapper.find("strong").text()).toBe("Section 1: Design");

    const stepLabels = wrapper
      .findAll(".steps-list__label")
      .map(l => l.text());

    expect(stepLabels).toEqual([
      "Title",
      "Description",
      "Conditions",
      "Experiment Type"
    ]);
  });

  it("marks steps up to the current step as complete", () => {
    useRoute.mockReturnValue({
      name: "ExperimentDesign",
      meta: { currentSection: "design" }
    });

    const wrapper = mountComponent(Steps, {
      props: {
        currentSection: "design",
        currentStep: "design_conditions",
        participationType: "CONSENT"
      }
    });

    const steps = wrapper.findAll(".steps-list__step");

    // Title, Description, Conditions (index 0-2) complete; Experiment Type (3) not
    expect(steps[0].classes()).toContain("complete");
    expect(steps[2].classes()).toContain("complete");
    expect(steps[3].classes()).not.toContain("complete");
  });

  it("builds CONSENT participation steps including the consent-specific steps", () => {
    useRoute.mockReturnValue({
      name: "ParticipationSelection",
      meta: { currentSection: "participation", selectionType: "consent" }
    });

    const wrapper = mountComponent(Steps, {
      props: {
        currentSection: "participation",
        currentStep: "participation_selection_method",
        participationType: "CONSENT"
      }
    });

    const stepLabels = wrapper
      .findAll(".steps-list__label")
      .map(l => l.text());

    expect(stepLabels).toEqual([
      "Selection Method",
      "Assignment Title",
      "Informed Consent",
      "Distribution"
    ]);
  });

  it("builds MANUAL participation steps including the select-participants step", () => {
    useRoute.mockReturnValue({
      name: "ParticipationSelection",
      meta: { currentSection: "participation", selectionType: "manual" }
    });

    const wrapper = mountComponent(Steps, {
      props: {
        currentSection: "participation",
        currentStep: "participation_selection_method",
        participationType: "MANUAL"
      }
    });

    const stepLabels = wrapper
      .findAll(".steps-list__label")
      .map(l => l.text());

    expect(stepLabels).toEqual([
      "Selection Method",
      "Select Participants",
      "Distribution"
    ]);
  });

  it("marks the design section as green/complete when viewing the participation section", () => {
    useRoute.mockReturnValue({
      name: "ParticipationSelection",
      meta: { currentSection: "participation", selectionType: "any" }
    });

    const wrapper = mountComponent(Steps, {
      props: {
        currentSection: "participation",
        currentStep: "participation_selection_method",
        participationType: "AUTO"
      }
    });

    const spans = wrapper.findAll("li span");
    const designSpan = spans.find(s => s.text() === "Section 1: Design");

    expect(designSpan.classes()).toContain("text-green");
  });

  it("includes the Distribution step on ParticipationSummary despite its route having no selectionType meta", () => {
    // matches router/index.js's actual ParticipationSummary route meta - no selectionType,
    // unlike every other participation-flow route
    useRoute.mockReturnValue({
      name: "ParticipationSummary",
      meta: { currentSection: "participation", currentStep: "select_participants" }
    });

    const wrapper = mountComponent(Steps, {
      props: {
        currentSection: "participation",
        currentStep: "select_participants",
        participationType: "AUTO"
      }
    });

    const stepLabels = wrapper
      .findAll(".steps-list__label")
      .map(l => l.text());

    expect(stepLabels).toContain("Distribution");
  });

  it("applies the finished/text-green treatment on summary routes", () => {
    useRoute.mockReturnValue({
      name: "ExperimentDesignSummary",
      meta: { currentSection: "design" }
    });

    const wrapper = mountComponent(Steps, {
      props: {
        currentSection: "design",
        currentStep: "design_type",
        participationType: "CONSENT"
      }
    });

    expect(wrapper.find("strong").classes()).toContain("text-green");
    expect(wrapper.find(".steps-list").classes()).toContain("finished");
  });
});

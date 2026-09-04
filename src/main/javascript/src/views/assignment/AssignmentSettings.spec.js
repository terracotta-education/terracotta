import { describe, expect, it } from "vitest";
import { createPinia, setActivePinia } from "pinia";

import { mountComponent } from "@/test-utils/mount";
import AssignmentSettings from "./AssignmentSettings.vue";
import { assignment as assignmentModule } from "@/store/assignment.module";

const MultipleAttemptsSettingStub = {
  name: "MultipleAttemptsSetting",
  props: ["modelValue"],
  template: "<div class=\"multiple-attempts-stub\" />"
};

const RevealResponsesSettingStub = {
  name: "RevealResponsesSetting",
  props: ["modelValue"],
  template: "<div class=\"reveal-responses-stub\" />"
};

function mountSettings(assignment) {
  const pinia = createPinia();
  setActivePinia(pinia);

  const assignmentStore = assignmentModule();

  if (assignment) {
    assignmentStore.setAssignment(assignment);
  }

  const wrapper = mountComponent(AssignmentSettings, {
    pinia,
    global: {
      stubs: {
        MultipleAttemptsSetting: MultipleAttemptsSettingStub,
        RevealResponsesSetting: RevealResponsesSettingStub
      }
    }
  });

  return { wrapper, assignmentStore };
}

describe("AssignmentSettings", () => {
  it("passes null to both child settings when there is no assignment loaded", () => {
    const { wrapper } = mountSettings();

    const multipleAttempts = wrapper.findComponent(MultipleAttemptsSettingStub);
    const revealResponses = wrapper.findComponent(RevealResponsesSettingStub);

    expect(multipleAttempts.props("modelValue")).toBeNull();
    expect(revealResponses.props("modelValue")).toBeNull();
  });

  it("derives the multiple-attempts subset from the assignment in the store", () => {
    const { wrapper } = mountSettings({
      title: "Quiz 1",
      allowMultipleAttempts: true,
      numOfSubmissions: 3,
      hoursBetweenSubmissions: 2,
      multipleSubmissionScoringScheme: "HIGHEST",
      cumulativeScoringInitialPercentage: null
    });

    const multipleAttempts = wrapper.findComponent(MultipleAttemptsSettingStub);

    expect(multipleAttempts.props("modelValue")).toEqual({
      allowMultipleAttempts: true,
      numOfSubmissions: 3,
      hoursBetweenSubmissions: 2,
      multipleSubmissionScoringScheme: "HIGHEST",
      cumulativeScoringInitialPercentage: null
    });
  });

  it("derives the reveal-responses subset from the assignment in the store", () => {
    const { wrapper } = mountSettings({
      title: "Quiz 1",
      allowStudentViewResponses: true,
      studentViewResponsesAfter: "2024-01-01",
      studentViewResponsesBefore: "2024-02-01",
      allowStudentViewCorrectAnswers: false,
      studentViewCorrectAnswersAfter: null,
      studentViewCorrectAnswersBefore: null
    });

    const revealResponses = wrapper.findComponent(RevealResponsesSettingStub);

    expect(revealResponses.props("modelValue")).toEqual({
      allowStudentViewResponses: true,
      studentViewResponsesAfter: "2024-01-01",
      studentViewResponsesBefore: "2024-02-01",
      allowStudentViewCorrectAnswers: false,
      studentViewCorrectAnswersAfter: null,
      studentViewCorrectAnswersBefore: null
    });
  });

  it("merges multiple-attempts updates into the assignment store, preserving other fields", async () => {
    const { wrapper, assignmentStore } = mountSettings({
      title: "Quiz 1",
      numOfSubmissions: null
    });

    const multipleAttempts = wrapper.findComponent(MultipleAttemptsSettingStub);

    await multipleAttempts.vm.$emit("update:modelValue", { numOfSubmissions: 5 });

    expect(assignmentStore.assignment).toEqual({
      title: "Quiz 1",
      numOfSubmissions: 5
    });
  });

  it("merges reveal-responses updates into the assignment store, preserving other fields", async () => {
    const { wrapper, assignmentStore } = mountSettings({
      title: "Quiz 1",
      allowStudentViewResponses: false
    });

    const revealResponses = wrapper.findComponent(RevealResponsesSettingStub);

    await revealResponses.vm.$emit("update:modelValue", { allowStudentViewResponses: true });

    expect(assignmentStore.assignment).toEqual({
      title: "Quiz 1",
      allowStudentViewResponses: true
    });
  });
});

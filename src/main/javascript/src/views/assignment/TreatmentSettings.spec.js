import { beforeEach, describe, expect, it } from "vitest";
import { createPinia, setActivePinia } from "pinia";

import { mountComponent } from "@/test-utils/mount";
import TreatmentSettings from "./TreatmentSettings.vue";
import MultipleAttemptsSetting from "./MultipleAttemptsSetting.vue";
import RevealResponsesSetting from "./RevealResponsesSetting.vue";

import { assessment as assessmentModule } from "@/store/assessment.module";

let pinia;

const mountSettings = () => {
  return mountComponent(TreatmentSettings, {
    pinia,
    global: {
      stubs: {
        MultipleAttemptsSetting: true,
        RevealResponsesSetting: true
      }
    }
  });
};

describe("TreatmentSettings", () => {
  let assessmentStore;

  beforeEach(() => {
    pinia = createPinia();
    setActivePinia(pinia);
    assessmentStore = assessmentModule();
  });

  it("renders neither child setting when there is no assessment", () => {
    const wrapper = mountSettings();

    expect(wrapper.findComponent(MultipleAttemptsSetting).exists()).toBe(false);
    expect(wrapper.findComponent(RevealResponsesSetting).exists()).toBe(false);
  });

  it("passes the multiple-attempts fields from the assessment down as a v-model", () => {
    assessmentStore.setAssessment({
      allowMultipleAttempts: true,
      numOfSubmissions: 3,
      hoursBetweenSubmissions: 12,
      multipleSubmissionScoringScheme: "MOST_RECENT",
      cumulativeScoringInitialPercentage: 50,
      allowStudentViewResponses: false,
      questions: []
    });

    const wrapper = mountSettings();

    expect(wrapper.findComponent(MultipleAttemptsSetting).props("modelValue")).toEqual({
      allowMultipleAttempts: true,
      numOfSubmissions: 3,
      hoursBetweenSubmissions: 12,
      multipleSubmissionScoringScheme: "MOST_RECENT",
      cumulativeScoringInitialPercentage: 50
    });
  });

  it("passes the reveal-response fields, including the first question's integration, down as a v-model", () => {
    assessmentStore.setAssessment({
      allowStudentViewResponses: true,
      studentViewResponsesAfter: "2024-01-01",
      studentViewResponsesBefore: "2024-02-01",
      allowStudentViewCorrectAnswers: false,
      studentViewCorrectAnswersAfter: null,
      studentViewCorrectAnswersBefore: null,
      questions: [
        { questionId: 1, integration: { id: "abc" } }
      ]
    });

    const wrapper = mountSettings();

    expect(wrapper.findComponent(RevealResponsesSetting).props("modelValue")).toMatchObject({
      allowStudentViewResponses: true,
      studentViewResponsesAfter: "2024-01-01",
      studentViewResponsesBefore: "2024-02-01",
      integration: { id: "abc" }
    });
  });

  it("writes multiple-attempts changes back onto the assessment via setAssessment", async () => {
    assessmentStore.setAssessment({
      allowMultipleAttempts: false,
      numOfSubmissions: 1,
      hoursBetweenSubmissions: 0,
      multipleSubmissionScoringScheme: "MOST_RECENT",
      cumulativeScoringInitialPercentage: 0,
      allowStudentViewResponses: false,
      questions: []
    });

    const wrapper = mountSettings();

    await wrapper.findComponent(MultipleAttemptsSetting).vm.$emit("update:modelValue", {
      allowMultipleAttempts: true,
      numOfSubmissions: 5,
      hoursBetweenSubmissions: 24,
      multipleSubmissionScoringScheme: "AVERAGE",
      cumulativeScoringInitialPercentage: 0
    });

    expect(assessmentStore.assessment).toMatchObject({
      allowMultipleAttempts: true,
      numOfSubmissions: 5,
      hoursBetweenSubmissions: 24,
      multipleSubmissionScoringScheme: "AVERAGE"
    });
  });

  it("writes reveal-response changes back onto the assessment via setAssessment", async () => {
    assessmentStore.setAssessment({
      allowStudentViewResponses: false,
      questions: []
    });

    const wrapper = mountSettings();

    await wrapper.findComponent(RevealResponsesSetting).vm.$emit("update:modelValue", {
      allowStudentViewResponses: true,
      studentViewResponsesAfter: "2024-03-01"
    });

    expect(assessmentStore.assessment).toMatchObject({
      allowStudentViewResponses: true,
      studentViewResponsesAfter: "2024-03-01"
    });
  });

  it("resolves integration from the first question only, ignoring subsequent questions", () => {
    assessmentStore.setAssessment({
      allowStudentViewResponses: false,
      questions: [
        { questionId: 1, integration: null },
        { questionId: 2, integration: { id: "should-be-ignored" } }
      ]
    });

    const wrapper = mountSettings();

    expect(wrapper.findComponent(RevealResponsesSetting).props("modelValue").integration).toBe(null);
  });
});

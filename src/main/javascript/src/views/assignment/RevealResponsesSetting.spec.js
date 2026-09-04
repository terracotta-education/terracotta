import { describe, expect, it } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import RevealResponsesSetting from "./RevealResponsesSetting.vue";
import DateTimePicker from "@/components/picker/DateTimePicker.vue";

const baseModelValue = () => ({
  allowStudentViewResponses: false,
  studentViewResponsesAfter: null,
  studentViewResponsesBefore: null,
  allowStudentViewCorrectAnswers: false,
  studentViewCorrectAnswersAfter: null,
  studentViewCorrectAnswersBefore: null,
  integration: null
});

const mountSetting = modelValue => {
  return mountComponent(RevealResponsesSetting, {
    props: {
      modelValue: modelValue || baseModelValue()
    },
    global: {
      stubs: {
        DateTimePicker: true
      }
    }
  });
};

describe("RevealResponsesSetting", () => {
  it("shows the default (non-integration) header text", () => {
    const wrapper = mountSetting();

    expect(wrapper.text()).toContain("Reveal treatment responses");
    expect(wrapper.text()).toContain(
      "Allow students to see their treatment responses and points earned for each response"
    );
  });

  it("does not show the date controls until responses are allowed", () => {
    const wrapper = mountSetting();

    expect(wrapper.findComponent({ name: "VCardText" }).exists()).toBe(false);
    expect(wrapper.findAllComponents(DateTimePicker).length).toBe(0);
  });

  it("emits an update with allowStudentViewResponses set when the checkbox is toggled", async () => {
    const wrapper = mountSetting();

    const checkbox = wrapper.findComponent({ name: "VCheckbox" });
    await checkbox.setValue(true);

    const emitted = wrapper.emitted("update:modelValue");
    expect(emitted).toBeTruthy();
    expect(emitted.at(-1)[0]).toMatchObject({
      allowStudentViewResponses: true,
      allowStudentViewCorrectAnswers: false
    });
  });

  it("reveals the response-date pickers and correct-answers checkbox once allowed", () => {
    const wrapper = mountSetting({
      ...baseModelValue(),
      allowStudentViewResponses: true
    });

    expect(wrapper.findAllComponents(DateTimePicker).length).toBe(2);
    expect(wrapper.find(".allow-students-view-correct-answers").exists()).toBe(true);
  });

  it("reveals the correct-answer date pickers once that checkbox is also allowed", () => {
    const wrapper = mountSetting({
      ...baseModelValue(),
      allowStudentViewResponses: true,
      allowStudentViewCorrectAnswers: true
    });

    expect(wrapper.findAllComponents(DateTimePicker).length).toBe(4);
    expect(wrapper.find(".correct-answer-date-controls").exists()).toBe(true);
  });

  it("hides the correct-answers checkbox for non-Custom-Web-Activity integrations and swaps messaging", () => {
    const wrapper = mountSetting({
      ...baseModelValue(),
      allowStudentViewResponses: true,
      integration: {
        configuration: {
          client: { name: "Some LTI Tool" }
        }
      }
    });

    expect(wrapper.text()).toContain("Reveal treatment scores");
    expect(wrapper.text()).toContain("Show points on");
    expect(wrapper.find(".allow-students-view-correct-answers").exists()).toBe(false);
  });

  it("keeps the default messaging for Custom Web Activity integrations", () => {
    const wrapper = mountSetting({
      ...baseModelValue(),
      integration: {
        configuration: {
          client: { name: "Custom Web Activity" }
        }
      }
    });

    expect(wrapper.text()).toContain("Reveal treatment responses");
  });

  it("clears allowStudentViewCorrectAnswers when responses are turned off", async () => {
    const wrapper = mountSetting({
      ...baseModelValue(),
      allowStudentViewResponses: true,
      allowStudentViewCorrectAnswers: true
    });

    const checkbox = wrapper.findComponent({ name: "VCheckbox" });
    await checkbox.setValue(false);

    const emitted = wrapper.emitted("update:modelValue");
    expect(emitted.at(-1)[0]).toMatchObject({
      allowStudentViewResponses: false,
      allowStudentViewCorrectAnswers: false
    });
  });

  it("updates the response-window dates through the DateTimePicker handlers", async () => {
    const wrapper = mountSetting({
      ...baseModelValue(),
      allowStudentViewResponses: true
    });

    const [afterPicker] = wrapper.findAllComponents(DateTimePicker);
    await afterPicker.vm.$emit("update:modelValue", "2024-01-01");

    const emitted = wrapper.emitted("update:modelValue");
    expect(emitted.at(-1)[0]).toMatchObject({
      studentViewResponsesAfter: "2024-01-01"
    });
  });
});

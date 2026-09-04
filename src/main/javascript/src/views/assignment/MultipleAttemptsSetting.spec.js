import { describe, expect, it } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import MultipleAttemptsSetting from "./MultipleAttemptsSetting.vue";

describe("MultipleAttemptsSetting", () => {
  it("hides the settings card body when multiple attempts are not allowed", () => {
    const wrapper = mountComponent(MultipleAttemptsSetting, {
      props: {
        modelValue: { numOfSubmissions: null }
      }
    });

    expect(wrapper.find(".v-card-text").exists()).toBe(false);
    expect(wrapper.findComponent({ name: "VCheckbox" }).props("modelValue")).toBe(false);
  });

  it("shows the settings card body when numOfSubmissions is set", () => {
    const wrapper = mountComponent(MultipleAttemptsSetting, {
      props: {
        modelValue: {
          numOfSubmissions: 3,
          multipleSubmissionScoringScheme: "MOST_RECENT"
        }
      }
    });

    expect(wrapper.find(".v-card-text").exists()).toBe(true);
  });

  it("emits numOfSubmissions: 0 when the 'allow multiple attempts' checkbox is checked", async () => {
    const wrapper = mountComponent(MultipleAttemptsSetting, {
      props: {
        modelValue: { numOfSubmissions: null }
      }
    });

    const checkbox = wrapper.findComponent({ name: "VCheckbox" });
    await checkbox.find("input").setValue(true);

    const emitted = wrapper.emitted("update:modelValue");
    expect(emitted).toBeTruthy();
    expect(emitted.at(-1)[0]).toEqual({ numOfSubmissions: 0 });
  });

  it("emits numOfSubmissions: null when unchecking 'allow multiple attempts'", async () => {
    const wrapper = mountComponent(MultipleAttemptsSetting, {
      props: {
        modelValue: { numOfSubmissions: 3 }
      }
    });

    const checkbox = wrapper.findComponent({ name: "VCheckbox" });
    await checkbox.find("input").setValue(false);

    const emitted = wrapper.emitted("update:modelValue");
    expect(emitted.at(-1)[0]).toEqual({ numOfSubmissions: null });
  });

  it("treats numOfSubmissions of 0 as infinite attempts selected", () => {
    const wrapper = mountComponent(MultipleAttemptsSetting, {
      props: {
        modelValue: { numOfSubmissions: 0 }
      }
    });

    const radioGroup = wrapper.findComponent({ name: "VRadioGroup" });
    expect(radioGroup.props("modelValue")).toBe(true);
  });

  it("reveals the cumulative percentage field only when scheme is CUMULATIVE", async () => {
    const wrapper = mountComponent(MultipleAttemptsSetting, {
      props: {
        modelValue: {
          numOfSubmissions: 4,
          multipleSubmissionScoringScheme: "MOST_RECENT"
        }
      }
    });

    expect(wrapper.find(".v-card-text").text()).not.toContain("Proportion earned on first attempt");

    await wrapper.setProps({
      modelValue: {
        numOfSubmissions: 4,
        multipleSubmissionScoringScheme: "CUMULATIVE",
        cumulativeScoringInitialPercentage: 40
      }
    });

    expect(wrapper.text()).toContain("Proportion earned on first attempt");
    // remaining 60% distributed among the other 3 attempts = 20 per attempt
    expect(wrapper.text()).toContain("60% will be distributed");
    expect(wrapper.text()).toContain("20.00% per attempt");
  });

  it("filters CUMULATIVE out of scoring options when numOfSubmissions is 0 (infinite)", () => {
    const wrapper = mountComponent(MultipleAttemptsSetting, {
      props: {
        modelValue: {
          numOfSubmissions: 0,
          multipleSubmissionScoringScheme: "MOST_RECENT"
        }
      }
    });

    const select = wrapper.findComponent({ name: "VSelect" });
    const items = select.props("items");

    expect(items.some(item => item.value === "CUMULATIVE")).toBe(false);
  });

  it("emits a parsed float for hoursBetweenSubmissions text input", async () => {
    const wrapper = mountComponent(MultipleAttemptsSetting, {
      props: {
        modelValue: { numOfSubmissions: 3 }
      }
    });

    const hoursField = wrapper.find('input[aria-label="assignment multiple submission minimum time between submissions"]');
    await hoursField.setValue("2.5");

    const emitted = wrapper.emitted("update:modelValue");
    expect(emitted.at(-1)[0]).toEqual({
      numOfSubmissions: 3,
      hoursBetweenSubmissions: 2.5
    });
  });
});

import { afterEach, describe, expect, it, vi } from "vitest";

vi.mock("@/helpers/ui-utils.js", () => ({
  deleteAttributesFromElement: vi.fn(),
  addAttributesToElement: vi.fn(),
  getAttributeFromElement: vi.fn(() => "some-listbox-id")
}));

import {
  deleteAttributesFromElement,
  addAttributesToElement,
  getAttributeFromElement
} from "@/helpers/ui-utils.js";
import { mountComponent } from "@/test-utils/mount";
import MultipleAttemptsSetting from "./MultipleAttemptsSetting.vue";

describe("MultipleAttemptsSetting", () => {
  afterEach(() => {
    vi.useRealTimers();
    vi.clearAllMocks();
  });

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

  it("emits a parsed integer for the numOfSubmissions attempts field, and null when cleared", async () => {
    const wrapper = mountComponent(MultipleAttemptsSetting, {
      props: {
        modelValue: { numOfSubmissions: 3 }
      }
    });

    const attemptsField = wrapper.find(
      'input[aria-label="Number of attempts a student is allowed"]'
    );

    await attemptsField.setValue("5");

    let emitted = wrapper.emitted("update:modelValue");
    expect(emitted.at(-1)[0]).toEqual({ numOfSubmissions: 5 });

    await attemptsField.setValue("");

    emitted = wrapper.emitted("update:modelValue");
    expect(emitted.at(-1)[0]).toEqual({ numOfSubmissions: null });
  });

  it("emits the selected multipleSubmissionScoringScheme when the select changes", async () => {
    const wrapper = mountComponent(MultipleAttemptsSetting, {
      props: {
        modelValue: { numOfSubmissions: 3, multipleSubmissionScoringScheme: "MOST_RECENT" }
      }
    });

    const select = wrapper.findComponent({ name: "VSelect" });
    await select.vm.$emit("update:modelValue", "AVERAGE");

    const emitted = wrapper.emitted("update:modelValue");
    expect(emitted.at(-1)[0]).toEqual({
      numOfSubmissions: 3,
      multipleSubmissionScoringScheme: "AVERAGE"
    });
  });

  it("emits a parsed float for cumulativeScoringInitialPercentage, and null when cleared", async () => {
    const wrapper = mountComponent(MultipleAttemptsSetting, {
      props: {
        modelValue: {
          numOfSubmissions: 4,
          multipleSubmissionScoringScheme: "CUMULATIVE",
          cumulativeScoringInitialPercentage: 40
        }
      }
    });

    const percentField = wrapper.find(
      'input[aria-label="Proportion earned on first attempt, percent"]'
    );

    await percentField.setValue("25");

    let emitted = wrapper.emitted("update:modelValue");
    expect(emitted.at(-1)[0]).toEqual({
      numOfSubmissions: 4,
      multipleSubmissionScoringScheme: "CUMULATIVE",
      cumulativeScoringInitialPercentage: 25
    });

    await percentField.setValue("");

    emitted = wrapper.emitted("update:modelValue");
    expect(emitted.at(-1)[0]).toEqual({
      numOfSubmissions: 4,
      multipleSubmissionScoringScheme: "CUMULATIVE",
      cumulativeScoringInitialPercentage: null
    });
  });

  it("shows only the 'to the other attempt' wording when exactly 2 attempts are allowed", async () => {
    const wrapper = mountComponent(MultipleAttemptsSetting, {
      props: {
        modelValue: {
          numOfSubmissions: 2,
          multipleSubmissionScoringScheme: "CUMULATIVE",
          cumulativeScoringInitialPercentage: 50
        }
      }
    });

    expect(wrapper.text()).toContain("to the other attempt.");
    expect(wrapper.text()).not.toContain("evenly among the other");
  });

  it("emits numOfSubmissions: 0 when the infinite-attempts radio is selected, and 2 when the limited radio is selected", async () => {
    const wrapper = mountComponent(MultipleAttemptsSetting, {
      props: {
        modelValue: { numOfSubmissions: 3 }
      }
    });

    const radioGroup = wrapper.findComponent({ name: "VRadioGroup" });

    await radioGroup.vm.$emit("update:modelValue", true);
    let emitted = wrapper.emitted("update:modelValue");
    expect(emitted.at(-1)[0]).toEqual({ numOfSubmissions: 0 });

    await radioGroup.vm.$emit("update:modelValue", false);
    emitted = wrapper.emitted("update:modelValue");
    expect(emitted.at(-1)[0]).toEqual({ numOfSubmissions: 2 });
  });

  it("resets an CUMULATIVE scoring scheme back to MOST_RECENT when switching to infinite attempts", async () => {
    const wrapper = mountComponent(MultipleAttemptsSetting, {
      props: {
        modelValue: {
          numOfSubmissions: 3,
          multipleSubmissionScoringScheme: "CUMULATIVE",
          cumulativeScoringInitialPercentage: 40
        }
      }
    });

    // Simulate the parent applying the emitted numOfSubmissions: 0 (infinite attempts) update,
    // which flips the allowInfiniteSubmissions computed and should trigger its watcher.
    await wrapper.setProps({
      modelValue: {
        numOfSubmissions: 0,
        multipleSubmissionScoringScheme: "CUMULATIVE",
        cumulativeScoringInitialPercentage: 40
      }
    });
    await wrapper.vm.$nextTick();

    const emitted = wrapper.emitted("update:modelValue");
    expect(emitted.at(-1)[0]).toEqual({
      numOfSubmissions: 0,
      multipleSubmissionScoringScheme: "MOST_RECENT",
      cumulativeScoringInitialPercentage: null
    });
  });

  it("does not touch the scoring scheme when switching to infinite attempts if it wasn't CUMULATIVE", async () => {
    const wrapper = mountComponent(MultipleAttemptsSetting, {
      props: {
        modelValue: {
          numOfSubmissions: 3,
          multipleSubmissionScoringScheme: "MOST_RECENT"
        }
      }
    });

    await wrapper.setProps({
      modelValue: {
        numOfSubmissions: 0,
        multipleSubmissionScoringScheme: "MOST_RECENT"
      }
    });
    await wrapper.vm.$nextTick();

    expect(wrapper.emitted("update:modelValue")).toBeFalsy();
  });

  it("re-applies combobox accessibility attributes to the scoring-scheme select once multiple attempts are enabled", async () => {
    vi.useFakeTimers();

    const wrapper = mountComponent(MultipleAttemptsSetting, {
      props: {
        modelValue: { numOfSubmissions: null }
      }
    });

    await wrapper.setProps({
      modelValue: { numOfSubmissions: 0, multipleSubmissionScoringScheme: "MOST_RECENT" }
    });
    await wrapper.vm.$nextTick();

    await vi.advanceTimersByTimeAsync(1000);

    expect(getAttributeFromElement).toHaveBeenCalledWith(
      ".keep-treatment-score-select .v-field:first-of-type",
      "aria-owns"
    );
    expect(deleteAttributesFromElement).toHaveBeenCalledWith(
      ".keep-treatment-score-select .v-field",
      ["role"]
    );
    expect(addAttributesToElement).toHaveBeenCalledWith(
      ".keep-treatment-score-select .v-field",
      [
        { name: "role", value: "combobox" },
        { name: "aria-controls", value: "some-listbox-id" }
      ]
    );
  });

  it("does not re-apply accessibility attributes when multiple attempts are turned off", async () => {
    vi.useFakeTimers();

    const wrapper = mountComponent(MultipleAttemptsSetting, {
      props: {
        modelValue: { numOfSubmissions: 0, multipleSubmissionScoringScheme: "MOST_RECENT" }
      }
    });

    await wrapper.setProps({ modelValue: { numOfSubmissions: null } });
    await wrapper.vm.$nextTick();

    await vi.advanceTimersByTimeAsync(1000);

    expect(addAttributesToElement).not.toHaveBeenCalled();
  });

  it("does not re-apply accessibility attributes when multiple attempts stay disabled", async () => {
    vi.useFakeTimers();

    mountComponent(MultipleAttemptsSetting, {
      props: {
        modelValue: { numOfSubmissions: null }
      }
    });

    await vi.advanceTimersByTimeAsync(1000);

    expect(addAttributesToElement).not.toHaveBeenCalled();
  });
});

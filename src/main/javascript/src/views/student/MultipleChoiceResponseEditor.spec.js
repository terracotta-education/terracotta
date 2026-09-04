import { describe, expect, it } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import MultipleChoiceResponseEditor from "./MultipleChoiceResponseEditor.vue";

const answers = [
  { answerId: 1, html: "Answer one" },
  { answerId: 2, html: "Answer two" }
];

describe("MultipleChoiceResponseEditor", () => {
  it("renders one ResponseRow per answer", () => {
    const wrapper = mountComponent(MultipleChoiceResponseEditor, {
      props: { answers }
    });

    const rows = wrapper.findAllComponents({ name: "ResponseRow" });

    expect(rows).toHaveLength(2);
    expect(wrapper.text()).toContain("Answer one");
    expect(wrapper.text()).toContain("Answer two");
  });

  it("emits update:modelValue with the selected answerId when editable", async () => {
    const wrapper = mountComponent(MultipleChoiceResponseEditor, {
      props: {
        answers,
        modelValue: null,
        readonly: false
      }
    });

    const radioInputs = wrapper.findAll("input[type=\"radio\"]");

    expect(radioInputs).toHaveLength(2);

    await radioInputs[1].setValue(true);

    const emitted = wrapper.emitted("update:modelValue");

    expect(emitted).toBeTruthy();
    expect(emitted.at(-1)).toEqual([2]);
  });

  it("updates the internal selection when modelValue prop changes", async () => {
    const wrapper = mountComponent(MultipleChoiceResponseEditor, {
      props: {
        answers,
        modelValue: 1,
        readonly: false
      }
    });

    let radioGroups = wrapper.findAllComponents({ name: "VRadioGroup" });
    expect(radioGroups[0].props("modelValue")).toBe(1);

    await wrapper.setProps({ modelValue: 2 });

    radioGroups = wrapper.findAllComponents({ name: "VRadioGroup" });
    expect(radioGroups[1].props("modelValue")).toBe(2);
  });

  it("disables the radio group and binds to studentResponse when readonly", () => {
    const wrapper = mountComponent(MultipleChoiceResponseEditor, {
      props: {
        answers: [
          { answerId: 1, html: "Answer one", correct: true, studentResponse: 1 },
          { answerId: 2, html: "Answer two", correct: false, studentResponse: false }
        ],
        readonly: true
      }
    });

    const radioGroups = wrapper.findAllComponents({ name: "VRadioGroup" });

    expect(radioGroups[0].props("disabled")).toBe(true);
    expect(radioGroups[0].props("modelValue")).toBe(1);
  });

  it("shows 'Student Response' decorator with a green border for a correct, chosen answer", () => {
    const wrapper = mountComponent(MultipleChoiceResponseEditor, {
      props: {
        answers: [
          { answerId: 1, html: "Answer one", correct: true, studentResponse: 1 }
        ],
        readonly: true
      }
    });

    expect(wrapper.text()).toContain("Student Response");
    expect(wrapper.findComponent({ name: "ResponseRow" }).props("correct")).toBe(true);
  });

  it("shows 'Student Response' decorator with a red border for an incorrect, chosen answer", () => {
    const wrapper = mountComponent(MultipleChoiceResponseEditor, {
      props: {
        answers: [
          { answerId: 1, html: "Answer one", correct: false, studentResponse: 1 }
        ],
        readonly: true
      }
    });

    expect(wrapper.text()).toContain("Student Response");
    expect(wrapper.findComponent({ name: "ResponseRow" }).props("correct")).toBe(false);
  });

  it("shows 'Correct Response' only when showAnswers is true and the student did not pick the correct answer", () => {
    const withoutShowAnswers = mountComponent(MultipleChoiceResponseEditor, {
      props: {
        answers: [
          { answerId: 1, html: "Answer one", correct: true, studentResponse: false }
        ],
        readonly: true,
        showAnswers: false
      }
    });

    expect(withoutShowAnswers.text()).not.toContain("Correct Response");
    expect(withoutShowAnswers.findComponent({ name: "ResponseRow" }).props("correct")).toBe(null);

    const withShowAnswers = mountComponent(MultipleChoiceResponseEditor, {
      props: {
        answers: [
          { answerId: 1, html: "Answer one", correct: true, studentResponse: false }
        ],
        readonly: true,
        showAnswers: true
      }
    });

    expect(withShowAnswers.text()).toContain("Correct Response");
    expect(withShowAnswers.findComponent({ name: "ResponseRow" }).props("correct")).toBe(true);
  });

  it("shows no decorator or border for an unselected, incorrect answer", () => {
    const wrapper = mountComponent(MultipleChoiceResponseEditor, {
      props: {
        answers: [
          { answerId: 1, html: "Answer one", correct: false, studentResponse: false }
        ],
        readonly: true,
        showAnswers: true
      }
    });

    expect(wrapper.text()).not.toContain("Student Response");
    expect(wrapper.text()).not.toContain("Correct Response");
    expect(wrapper.findComponent({ name: "ResponseRow" }).props("correct")).toBe(null);
  });
});

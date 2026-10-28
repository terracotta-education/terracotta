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

  // screen readers: one group of options named by the question, so they're read as "1 of 2" and
  // the arrow keys move the selection between them
  it("puts all the answers in one radio group named by the question", () => {
    const wrapper = mountComponent(MultipleChoiceResponseEditor, {
      props: { answers, modelValue: 2, labelledby: "question-5-text" }
    });

    const groups = wrapper.findAll("[role=\"radiogroup\"]");
    expect(groups).toHaveLength(1);
    expect(groups[0].attributes("aria-labelledby")).toBe("question-5-text");

    const radios = wrapper.findAll("input[type=\"radio\"]");
    expect(radios).toHaveLength(2);
    expect(radios[0].attributes("name")).toBeTruthy();
    expect(radios[1].attributes("name")).toBe(radios[0].attributes("name"));
    expect(radios[1].element.checked).toBe(true);
  });

  it("names each radio by its answer's text, without markup", () => {
    const wrapper = mountComponent(MultipleChoiceResponseEditor, {
      props: { answers: [{ answerId: 1, html: "<p>A <strong>big</strong> dog</p>" }] }
    });

    expect(wrapper.find("input[type=\"radio\"]").attributes("aria-label")).toBe("A big dog");
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

    expect(wrapper.findComponent({ name: "VRadioGroup" }).props("modelValue")).toBe(1);

    await wrapper.setProps({ modelValue: 2 });

    expect(wrapper.findComponent({ name: "VRadioGroup" }).props("modelValue")).toBe(2);
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

    const radioGroup = wrapper.findComponent({ name: "VRadioGroup" });

    expect(radioGroup.props("disabled")).toBe(true);
    expect(radioGroup.props("modelValue")).toBe(1);
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

  it("selects an answer when its text is clicked", async () => {
    const wrapper = mountComponent(MultipleChoiceResponseEditor, {
      props: { answers, modelValue: null, readonly: false }
    });

    await wrapper.findAll(".selectable-row")[1].find("span").trigger("click");

    expect(wrapper.emitted("update:modelValue")).toEqual([[2]]);
    expect(wrapper.findAll("input[type=\"radio\"]")[1].element.checked).toBe(true);
  });

  it("selects an answer when anywhere on its row is clicked", async () => {
    const wrapper = mountComponent(MultipleChoiceResponseEditor, {
      props: { answers, modelValue: null, readonly: false }
    });

    await wrapper.findAll(".selectable-row")[0].trigger("click");

    expect(wrapper.emitted("update:modelValue")).toEqual([[1]]);
  });

  it("reports one change when the radio itself is clicked", async () => {
    const wrapper = mountComponent(MultipleChoiceResponseEditor, {
      props: { answers, modelValue: null, readonly: false }
    });

    // a real click: the click event bubbles to the row, then the radio's change fires
    await wrapper.findAll("input[type=\"radio\"]")[1].trigger("click");

    expect(wrapper.emitted("update:modelValue")).toEqual([[2]]);
  });

  it("doesn't report a change when the selected answer's row is clicked again", async () => {
    const wrapper = mountComponent(MultipleChoiceResponseEditor, {
      props: { answers, modelValue: 2, readonly: false }
    });

    await wrapper.findAll(".selectable-row")[1].trigger("click");

    expect(wrapper.emitted("update:modelValue")).toBeUndefined();
  });

  it("doesn't report a change from the group when readonly", async () => {
    const wrapper = mountComponent(MultipleChoiceResponseEditor, {
      props: {
        answers: [
          { answerId: 1, html: "Answer one", studentResponse: 1 },
          { answerId: 2, html: "Answer two", studentResponse: false }
        ],
        readonly: true
      }
    });

    await wrapper.findComponent({ name: "VRadioGroup" }).vm.$emit("update:modelValue", 2);

    expect(wrapper.emitted("update:modelValue")).toBeFalsy();
  });

  it("ignores row clicks when readonly", async () => {
    const wrapper = mountComponent(MultipleChoiceResponseEditor, {
      props: { answers: answers.map(answer => ({ ...answer, studentResponse: false })), readonly: true }
    });

    expect(wrapper.find(".selectable-row").exists()).toBe(false);

    await wrapper.findAll("span")[0].trigger("click");

    expect(wrapper.emitted("update:modelValue")).toBeUndefined();
  });
});

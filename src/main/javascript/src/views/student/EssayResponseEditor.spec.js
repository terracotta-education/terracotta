import { describe, expect, it } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import EssayResponseEditor from "./EssayResponseEditor.vue";

describe("EssayResponseEditor", () => {
  it("mounts without throwing when given a non-empty initial modelValue", () => {
    const wrapper = mountComponent(EssayResponseEditor, {
      props: { modelValue: "a pre-filled essay response" }
    });

    expect(wrapper.findComponent({ name: "VTextarea" }).props("modelValue")).toBe(
      "a pre-filled essay response"
    );
    expect(wrapper.text()).toContain("4 words");
  });

  it("renders an editable textarea and reflects modelValue updates", async () => {
    const wrapper = mountComponent(EssayResponseEditor, {
      props: { modelValue: "" }
    });

    const textarea = wrapper.findComponent({ name: "VTextarea" });
    expect(textarea.exists()).toBe(true);
    expect(textarea.props("modelValue")).toBe("");

    await wrapper.setProps({ modelValue: "Hello world" });

    expect(wrapper.findComponent({ name: "VTextarea" }).props("modelValue")).toBe(
      "Hello world"
    );
  });

  it("shows the word count once a modelValue is set", async () => {
    const wrapper = mountComponent(EssayResponseEditor, {
      props: { modelValue: "" }
    });

    await wrapper.setProps({ modelValue: "one two three" });

    expect(wrapper.text()).toContain("3 words");
  });

  it("pluralizes the word count correctly for a single word", async () => {
    const wrapper = mountComponent(EssayResponseEditor, {
      props: { modelValue: "" }
    });

    await wrapper.setProps({ modelValue: "hello" });

    expect(wrapper.text()).toContain("1 word");
    expect(wrapper.text()).not.toContain("1 words");
  });

  it("shows 0 words when there is no modelValue", () => {
    const wrapper = mountComponent(EssayResponseEditor, {
      props: { modelValue: null }
    });

    expect(wrapper.text()).toContain("0 words");
  });

  it("emits update:modelValue and updates the word count as the user types", async () => {
    const wrapper = mountComponent(EssayResponseEditor, {
      props: { modelValue: "" }
    });

    const textarea = wrapper.findComponent({ name: "VTextarea" });
    await textarea.setValue("four score and seven");

    const emitted = wrapper.emitted("update:modelValue");
    expect(emitted).toBeTruthy();
    expect(emitted.at(-1)[0]).toBe("four score and seven");
    expect(wrapper.text()).toContain("4 words");
  });

  it("recalculates the word count as the modelValue prop keeps changing externally", async () => {
    const wrapper = mountComponent(EssayResponseEditor, {
      props: { modelValue: "" }
    });

    await wrapper.setProps({ modelValue: "one" });
    expect(wrapper.text()).toContain("1 word");

    await wrapper.setProps({ modelValue: "one two three four" });
    expect(wrapper.text()).toContain("4 words");

    await wrapper.setProps({ modelValue: "" });
    expect(wrapper.text()).toContain("0 words");
  });

  it("renders a readonly textarea bound to the answer's response when readonly", () => {
    const wrapper = mountComponent(EssayResponseEditor, {
      props: {
        modelValue: null,
        readonly: true,
        answer: { response: "Submitted answer text" }
      }
    });

    const textareas = wrapper.findAllComponents({ name: "VTextarea" });
    expect(textareas).toHaveLength(1);
    expect(textareas[0].props("modelValue")).toBe("Submitted answer text");
    expect(textareas[0].props("readonly")).toBe(true);
  });

  it("shows an empty readonly textarea when there is no answer", () => {
    const wrapper = mountComponent(EssayResponseEditor, {
      props: {
        modelValue: null,
        readonly: true,
        answer: null
      }
    });

    const textarea = wrapper.findComponent({ name: "VTextarea" });
    expect(textarea.props("modelValue")).toBe("");
  });
});

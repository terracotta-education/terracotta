import { afterEach, describe, expect, it } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import EssayResponseEditor from "./EssayResponseEditor.vue";

// ProseMirror renders its DOM after a macrotask under jsdom (see TipTapEditor.spec.js)
const settle = async () => {
  await Promise.resolve();
  await new Promise(resolve => setTimeout(resolve, 20));
  await Promise.resolve();
};

describe("EssayResponseEditor", () => {
  let wrapper;

  afterEach(() => {
    wrapper?.unmount();
  });

  const mount = props => {
    wrapper = mountComponent(EssayResponseEditor, { props, attachTo: document.body });

    return wrapper;
  };

  const editorComponent = () => wrapper.findComponent({ name: "TipTapEditor" });

  // the Tiptap editor itself, to type into it the way the toolbar and keyboard do
  const tiptap = () => editorComponent().vm.$.setupState.editor;

  it("shows the response in a rich text editor with the essay toolbar", async () => {
    mount({ modelValue: "<p>a pre-filled <strong>essay</strong> response</p>" });
    await settle();

    expect(editorComponent().props("editorType")).toBe("html");
    expect(wrapper.find(".ProseMirror").html()).toContain("<strong>essay</strong>");
    expect(wrapper.find(".ProseMirror").attributes("aria-label")).toBe("Your response");

    const titles = wrapper.findAll(".toolbar button").map(button => button.attributes("title") || button.attributes("aria-label"));

    expect(wrapper.findAll(".toolbar button").length).toBe(7);
    expect(titles.join(" ")).not.toMatch(/Heading/);
    expect(wrapper.text()).toContain("4 words");
  });

  // responses saved before essays took rich text
  it("shows a plain text response as paragraphs, keeping its characters", async () => {
    mount({ modelValue: "first line\n\nsecond < third" });
    await settle();

    const paragraphs = wrapper.findAll(".ProseMirror p").map(p => p.text());

    expect(paragraphs).toEqual(["first line", "second < third"]);
    expect(wrapper.text()).toContain("5 words");
  });

  it("emits the HTML as the student types and counts its words", async () => {
    mount({ modelValue: "" });
    await settle();

    tiptap().commands.insertContent("four score and seven");
    await settle();

    expect(wrapper.emitted("update:modelValue").at(-1)[0]).toBe("<p>four score and seven</p>");
    expect(wrapper.text()).toContain("4 words");
  });

  it("counts words across paragraphs and list items", async () => {
    mount({ modelValue: "<p>one</p><ul><li><p>two</p></li><li><p>three</p></li></ul>" });
    await settle();

    expect(wrapper.text()).toContain("3 words");
  });

  it("pluralizes the word count for a single word and shows 0 words when empty", async () => {
    mount({ modelValue: "<p>hello</p>" });
    await settle();

    expect(wrapper.text()).toContain("1 word");
    expect(wrapper.text()).not.toContain("1 words");

    await wrapper.setProps({ modelValue: null });

    expect(wrapper.text()).toContain("0 words");
  });

  it("loads a response that changes from outside the editor", async () => {
    mount({ modelValue: "<p>first attempt</p>" });
    await settle();

    await wrapper.setProps({ modelValue: "<p>second attempt here</p>" });
    await settle();

    expect(wrapper.find(".ProseMirror").text()).toBe("second attempt here");
    expect(wrapper.text()).toContain("3 words");
  });

  it("drops formatting an essay doesn't offer, such as headings", async () => {
    mount({ modelValue: "<h1>Title</h1><p>body</p>" });
    await settle();

    expect(wrapper.find(".ProseMirror h1").exists()).toBe(false);
    expect(wrapper.find(".ProseMirror").text()).toContain("Title");
  });

  it("shows the submitted response read only, without a toolbar, when readonly", async () => {
    mount({ modelValue: null, readonly: true, answer: { response: "<p>Submitted <em>answer</em></p>" } });
    await settle();

    expect(editorComponent().props("readOnly")).toBe(true);
    expect(wrapper.find(".ProseMirror").attributes("contenteditable")).toBe("false");
    expect(wrapper.find(".ProseMirror").html()).toContain("<em>answer</em>");
    expect(wrapper.find(".toolbar").exists()).toBe(false);
  });

  it("shows an empty read only response when there is no answer", async () => {
    mount({ modelValue: null, readonly: true, answer: null });
    await settle();

    expect(wrapper.find(".ProseMirror").text()).toBe("");
  });
});

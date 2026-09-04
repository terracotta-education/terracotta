import { afterEach, describe, expect, it } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import TipTapEditor from "./TipTapEditor.vue";

// TipTap/ProseMirror creates its EditorView lazily and needs the component's
// element attached to a real document plus a macrotask tick before the
// ProseMirror DOM is fully rendered under jsdom, so every test mounts to an
// attached container and waits a beat after the initial nextTick.
const waitForEditor = async wrapper => {
  await wrapper.vm.$nextTick();
  await new Promise(resolve => setTimeout(resolve, 20));
  await wrapper.vm.$nextTick();
};

const mountEditor = async options => {
  const container = document.createElement("div");
  document.body.appendChild(container);

  const wrapper = mountComponent(TipTapEditor, {
    attachTo: container,
    ...options
  });

  await waitForEditor(wrapper);

  return wrapper;
};

describe("TipTapEditor", () => {
  let wrapper;

  afterEach(() => {
    wrapper?.unmount();
  });

  it("renders the provided content inside a ProseMirror editor for the basic editor type", async () => {
    wrapper = await mountEditor({
      props: {
        content: "Hello world",
        editorType: "basic"
      }
    });

    const content = wrapper.find(".tiptap.ProseMirror");

    expect(content.exists()).toBe(true);
    expect(content.text()).toContain("Hello world");
  });

  it("does not render the toolbar for the basic editor type", async () => {
    wrapper = await mountEditor({
      props: {
        content: "Hello world",
        editorType: "basic"
      }
    });

    expect(wrapper.findComponent({ name: "ToolBar" }).exists()).toBe(false);
  });

  it("renders the toolbar for the html editor type when not read-only", async () => {
    wrapper = await mountEditor({
      props: {
        content: "<p>Hello world</p>",
        editorType: "html"
      }
    });

    expect(wrapper.findComponent({ name: "ToolBar" }).exists()).toBe(true);
  });

  it("hides the toolbar when readOnly is true even for the html editor type", async () => {
    wrapper = await mountEditor({
      props: {
        content: "<p>Hello world</p>",
        editorType: "html",
        readOnly: true
      }
    });

    expect(wrapper.findComponent({ name: "ToolBar" }).exists()).toBe(false);
  });

  it("makes the editor content non-editable when readOnly is true", async () => {
    wrapper = await mountEditor({
      props: {
        content: "Hello world",
        editorType: "basic",
        readOnly: true
      }
    });

    const content = wrapper.find(".tiptap.ProseMirror");

    expect(content.attributes("contenteditable")).toBe("false");
  });

  it("leaves the editor content editable by default", async () => {
    wrapper = await mountEditor({
      props: {
        content: "Hello world",
        editorType: "basic"
      }
    });

    const content = wrapper.find(".tiptap.ProseMirror");

    expect(content.attributes("contenteditable")).toBe("true");
  });

  it("emits edited with the plain text content on mount for the basic editor", async () => {
    wrapper = await mountEditor({
      props: {
        content: "Hello world",
        editorType: "basic"
      }
    });

    const editedEvents = wrapper.emitted("edited");

    expect(editedEvents).toBeTruthy();
    expect(editedEvents[0]).toEqual(["Hello world"]);
  });

  it("emits edited with HTML content on mount for the html editor", async () => {
    wrapper = await mountEditor({
      props: {
        content: "<p>Hello world</p>",
        editorType: "html"
      }
    });

    const editedEvents = wrapper.emitted("edited");

    expect(editedEvents).toBeTruthy();
    expect(editedEvents[0][0]).toContain("Hello world");
  });

  it("emits cursor on mount", async () => {
    wrapper = await mountEditor({
      props: {
        content: "Hello world",
        editorType: "basic"
      }
    });

    expect(wrapper.emitted("cursor")).toBeTruthy();
  });

  it("inserts a conditional-text mention when conditionalTextToPlace changes", async () => {
    wrapper = await mountEditor({
      props: {
        content: "Hello world",
        editorType: "basic"
      }
    });

    await wrapper.setProps({
      conditionalTextToPlace: {
        id: 5,
        label: "My Label",
        status: "create",
        cursorPosition: null
      }
    });

    await waitForEditor(wrapper);

    const mention = wrapper.find("conditional-text");

    expect(mention.exists()).toBe(true);
    expect(mention.text()).toContain("conditional text: My Label");
  });

  it("inserts a piped-text mention when pipedTextToPlace changes", async () => {
    wrapper = await mountEditor({
      props: {
        content: "Hello world",
        editorType: "basic"
      }
    });

    await wrapper.setProps({
      pipedTextToPlace: {
        id: 7,
        key: "student_name",
        cursorPosition: null
      }
    });

    await waitForEditor(wrapper);

    const mention = wrapper.find("piped-text");

    expect(mention.exists()).toBe(true);
    expect(mention.text()).toContain("piped text: student_name");
  });

  it("recreates the editor and shows the toolbar when switching from basic to html", async () => {
    wrapper = await mountEditor({
      props: {
        content: "Hello world",
        editorType: "basic"
      }
    });

    expect(wrapper.findComponent({ name: "ToolBar" }).exists()).toBe(false);

    await wrapper.setProps({ editorType: "html" });
    await waitForEditor(wrapper);

    expect(wrapper.findComponent({ name: "ToolBar" }).exists()).toBe(true);
  });

  it("replaces the editor content when the content prop changes", async () => {
    wrapper = await mountEditor({
      props: {
        content: "Hello world",
        editorType: "basic"
      }
    });

    await wrapper.setProps({ content: "Goodbye world" });
    await waitForEditor(wrapper);

    const content = wrapper.find(".tiptap.ProseMirror");

    expect(content.text()).toContain("Goodbye world");
    expect(content.text()).not.toContain("Hello world");
  });
});

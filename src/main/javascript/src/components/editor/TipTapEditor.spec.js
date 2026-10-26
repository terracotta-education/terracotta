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

  const lastEdited = wrapper => {
    const edits = wrapper.emitted("edited");

    return edits[edits.length - 1][0];
  };

  const video = '<div data-youtube-video=""><iframe width="560" height="315" src="https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ"></iframe></div>';

  it("keeps a video in a block of its own rather than inside the heading it follows", async () => {
    // saved before videos were block elements: the video inside the heading
    wrapper = await mountEditor({
      props: {
        content: `<h1>Do your best${video}</h1>`,
        editorType: "html"
      }
    });

    expect(lastEdited(wrapper)).toMatch(/^<h1>Do your best<\/h1><div data-youtube-video="">/);
  });

  it("leaves the trailing empty paragraph the editor adds out of the HTML", async () => {
    wrapper = await mountEditor({
      props: {
        content: "<p>Read this</p><ul><li><p>one</p></li></ul>",
        editorType: "html"
      }
    });

    expect(lastEdited(wrapper)).toBe("<p>Read this</p><ul><li><p>one</p></li></ul>");
  });

  it("keeps empty paragraphs the author put between content", async () => {
    wrapper = await mountEditor({
      props: {
        content: "<p>one</p><p></p><p>two</p>",
        editorType: "html"
      }
    });

    expect(lastEdited(wrapper)).toBe("<p>one</p><p></p><p>two</p>");
  });

  it("keeps a video that is the only content", async () => {
    wrapper = await mountEditor({
      props: {
        content: video,
        editorType: "html"
      }
    });

    expect(lastEdited(wrapper)).toMatch(/^<div data-youtube-video=""><iframe .*dQw4w9WgXcQ/);
  });

  it("emits an empty string for an empty html editor", async () => {
    wrapper = await mountEditor({
      props: {
        content: "",
        editorType: "html"
      }
    });

    expect(lastEdited(wrapper)).toBe("");
  });

  it("shows only the toolbar actions it's given, in that order", async () => {
    wrapper = await mountEditor({
      props: {
        editorType: "html",
        features: ["bold", "bulletList", "heading"]
      }
    });

    const toolbarItems = wrapper.findAllComponents({ name: "ToolbarItem" });

    expect(toolbarItems.map(item => item.props("title"))).toEqual(["Bold", "Bulleted List", "Heading 1", "Heading 2", "Heading 3"]);
  });

  // or a heading pasted into an essay would still be a heading
  it("keeps formatting it isn't given out of the content too", async () => {
    wrapper = await mountEditor({
      props: {
        content: "<h1>Title</h1><p><strong>bold</strong> <em>italic</em> <a href=\"https://example.com\">link</a></p><blockquote><p>quote</p></blockquote>",
        editorType: "html",
        features: ["bold"]
      }
    });

    const html = wrapper.emitted("edited").at(-1)[0];

    expect(html).toBe("<p>Title</p><p><strong>bold</strong> italic link</p><p>quote</p>");
  });

  it("offers every toolbar action when no features are given", async () => {
    wrapper = await mountEditor({
      props: {
        editorType: "html"
      }
    });

    expect(wrapper.findAllComponents({ name: "ToolbarItem" }).length).toBe(17);
  });

  it("labels its own editable area, so editors sharing a page keep their own names", async () => {
    wrapper = await mountEditor({
      props: {
        editorType: "html",
        label: "Your response"
      }
    });

    const other = await mountEditor({
      props: {
        editorType: "html"
      }
    });

    expect(wrapper.find(".ProseMirror").attributes("aria-label")).toBe("Your response");
    expect(other.find(".ProseMirror").attributes("aria-label")).toBe("message editor content");

    other.unmount();
  });

  describe("adding a link", () => {
    const submitLink = async (wrapper, url) => {
      wrapper.findComponent({ name: "ToolBar" }).vm.$.setupState.handleLinkDialogSubmit(url);
      await waitForEditor(wrapper);

      return wrapper.emitted("edited").at(-1)[0];
    };

    const tiptap = wrapper => wrapper.vm.$.setupState.editor;

    // nothing selected: the address itself becomes the link's text
    it("inserts the address as linked text when nothing is selected", async () => {
      wrapper = await mountEditor({ props: { editorType: "html", content: "<p>See </p>" } });
      tiptap(wrapper).commands.focus("end");

      const html = await submitLink(wrapper, "https://example.com");

      expect(html).toContain('<a target="_blank" rel="noopener noreferrer nofollow" href="https://example.com">https://example.com</a>');
    });

    it("keeps what's typed after an inserted link out of it", async () => {
      wrapper = await mountEditor({ props: { editorType: "html", content: "<p>See </p>" } });
      tiptap(wrapper).commands.focus("end");

      await submitLink(wrapper, "https://example.com");
      tiptap(wrapper).commands.insertContent(" now");
      await waitForEditor(wrapper);

      expect(wrapper.emitted("edited").at(-1)[0]).toContain("https://example.com</a> now</p>");
    });

    it("turns the selected text into the link", async () => {
      wrapper = await mountEditor({ props: { editorType: "html", content: "<p>Google</p>" } });
      tiptap(wrapper).commands.selectAll();

      const html = await submitLink(wrapper, "https://www.google.com");

      expect(html).toContain('href="https://www.google.com">Google</a>');
      expect(html).not.toContain(">https://www.google.com</a>");
    });
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

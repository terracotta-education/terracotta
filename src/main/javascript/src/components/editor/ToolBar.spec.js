import { describe, expect, it, vi } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import ToolBar from "./ToolBar.vue";

// Builds a fake TipTap editor whose chainable command API records every
// call so we can assert exactly which commands ToolBar dispatched, without
// needing a real ProseMirror instance wired to a selection.
const createMockEditor = ({ isActive = false, linkHref = "", youtubeSrc = "" } = {}) => {
  const calls = [];

  const chainable = new Proxy(
    {},
    {
      get(_target, prop) {
        if (prop === "then") {
          return undefined;
        }

        return (...args) => {
          calls.push({ method: prop, args });
          return chainable;
        };
      }
    }
  );

  return {
    calls,
    chain: vi.fn(() => chainable),
    isActive: vi.fn(() => isActive),
    getAttributes: vi.fn(attrType => {
      if (attrType === "link") {
        return { href: linkHref };
      }

      if (attrType === "youtube") {
        return { src: youtubeSrc };
      }

      return {};
    }),
    commands: {
      setYoutubeVideo: vi.fn()
    }
  };
};

const findItemByTitle = (wrapper, title) => {
  return wrapper
    .findAllComponents({ name: "ToolbarItem" })
    .find(item => item.props("title") === title);
};

const clickItem = async (wrapper, title) => {
  const item = findItemByTitle(wrapper, title);
  await item.findComponent({ name: "VBtn" }).trigger("click");
};

describe("ToolBar", () => {
  it("renders a ToolbarItem for every toolbar action", () => {
    const wrapper = mountComponent(ToolBar, {
      props: {
        editor: createMockEditor()
      }
    });

    expect(wrapper.findAllComponents({ name: "ToolbarItem" }).length).toBe(17);
  });

  it("does not render the link or YouTube dialogs by default", () => {
    const wrapper = mountComponent(ToolBar, {
      props: {
        editor: createMockEditor()
      }
    });

    expect(wrapper.findComponent({ name: "LinkDialog" }).exists()).toBe(false);
    expect(wrapper.findComponent({ name: "YouTubeDialog" }).exists()).toBe(false);
  });

  it("runs undo/redo through the editor's chain when those buttons are clicked", async () => {
    const editor = createMockEditor();

    const wrapper = mountComponent(ToolBar, {
      props: { editor }
    });

    await clickItem(wrapper, "Undo");
    expect(editor.calls.map(call => call.method)).toEqual([
      "focus",
      "undo",
      "run"
    ]);

    editor.calls.length = 0;
    await clickItem(wrapper, "Redo");
    expect(editor.calls.map(call => call.method)).toEqual([
      "focus",
      "redo",
      "run"
    ]);
  });

  it("toggles bold through the editor's chain when the Bold button is clicked", async () => {
    const editor = createMockEditor();

    const wrapper = mountComponent(ToolBar, {
      props: { editor }
    });

    await clickItem(wrapper, "Bold");

    expect(editor.calls.map(call => call.method)).toEqual([
      "focus",
      "toggleBold",
      "run"
    ]);
  });

  it("toggles a heading level through the editor's chain when a heading button is clicked", async () => {
    const editor = createMockEditor({ isActive: false });

    const wrapper = mountComponent(ToolBar, {
      props: { editor }
    });

    await clickItem(wrapper, "Heading 2");

    const toggleHeadingCall = editor.calls.find(
      call => call.method === "toggleHeading"
    );

    expect(toggleHeadingCall.args).toEqual([{ level: 2 }]);
  });

  it("toggles an already-active heading off, without immediately toggling it back on", async () => {
    const editor = createMockEditor();
    editor.isActive = vi.fn(
      (type, attrs) => type === "heading" && attrs?.level === 2
    );

    const wrapper = mountComponent(ToolBar, {
      props: { editor }
    });

    await clickItem(wrapper, "Heading 2");

    const toggleHeadingCalls = editor.calls.filter(
      call => call.method === "toggleHeading" && call.args[0]?.level === 2
    );

    expect(toggleHeadingCalls).toHaveLength(1);
  });

  it("opens the link dialog with the current link href when Add link is clicked", async () => {
    const editor = createMockEditor({ linkHref: "https://example.com" });

    const wrapper = mountComponent(ToolBar, {
      props: { editor }
    });

    await clickItem(wrapper, "Add link");

    const linkDialog = wrapper.findComponent({ name: "LinkDialog" });

    expect(linkDialog.exists()).toBe(true);
    expect(linkDialog.props("href")).toBe("https://example.com");
  });

  it("sets a link through the editor's chain when the link dialog submits a URL", async () => {
    const editor = createMockEditor();

    const wrapper = mountComponent(ToolBar, {
      props: { editor }
    });

    await clickItem(wrapper, "Add link");

    const linkDialog = wrapper.findComponent({ name: "LinkDialog" });
    linkDialog.vm.$emit("submit", "https://example.com");
    await wrapper.vm.$nextTick();

    expect(wrapper.findComponent({ name: "LinkDialog" }).exists()).toBe(false);

    const setLinkCall = editor.calls.find(call => call.method === "setLink");
    expect(setLinkCall.args).toEqual([{ href: "https://example.com" }]);
  });

  it("unsets a link through the editor's chain when the link dialog submits an empty URL", async () => {
    const editor = createMockEditor({ linkHref: "https://example.com" });

    const wrapper = mountComponent(ToolBar, {
      props: { editor }
    });

    await clickItem(wrapper, "Add link");

    const linkDialog = wrapper.findComponent({ name: "LinkDialog" });
    linkDialog.vm.$emit("submit", "");
    await wrapper.vm.$nextTick();

    expect(editor.calls.map(call => call.method)).toContain("unsetLink");
  });

  it("closes the link dialog without changing the editor when it is dismissed", async () => {
    const editor = createMockEditor();

    const wrapper = mountComponent(ToolBar, {
      props: { editor }
    });

    await clickItem(wrapper, "Add link");
    editor.calls.length = 0;

    const linkDialog = wrapper.findComponent({ name: "LinkDialog" });
    linkDialog.vm.$emit("close");
    await wrapper.vm.$nextTick();

    expect(wrapper.findComponent({ name: "LinkDialog" }).exists()).toBe(false);
    expect(editor.calls.length).toBe(0);
  });

  it("opens the YouTube dialog with the current embed src when YouTube is clicked", async () => {
    const editor = createMockEditor({ youtubeSrc: "https://youtu.be/abc123" });

    const wrapper = mountComponent(ToolBar, {
      props: { editor }
    });

    await clickItem(wrapper, "YouTube");

    const youTubeDialog = wrapper.findComponent({ name: "YouTubeDialog" });

    expect(youTubeDialog.exists()).toBe(true);
    expect(youTubeDialog.props("embedCode")).toBe("https://youtu.be/abc123");
  });

  it("sets a YouTube video via editor commands when the dialog submits a result", async () => {
    const editor = createMockEditor();

    const wrapper = mountComponent(ToolBar, {
      props: { editor }
    });

    await clickItem(wrapper, "YouTube");

    const youTubeDialog = wrapper.findComponent({ name: "YouTubeDialog" });
    const result = { src: "https://youtu.be/abc123", height: 315, width: 560 };
    youTubeDialog.vm.$emit("submit", result);
    await wrapper.vm.$nextTick();

    expect(editor.commands.setYoutubeVideo).toHaveBeenCalledWith(result);
    expect(wrapper.findComponent({ name: "YouTubeDialog" }).exists()).toBe(false);
  });

  it("marks the matching toolbar items active based on the activeItems prop", async () => {
    const editor = createMockEditor();

    const wrapper = mountComponent(ToolBar, {
      props: {
        editor,
        activeItems: {}
      }
    });

    await wrapper.setProps({
      activeItems: {
        marks: ["bold"],
        nodes: [
          {
            name: "heading",
            attributes: { level: 2 }
          }
        ]
      }
    });

    expect(findItemByTitle(wrapper, "Bold").props("activate")).toBe(true);
    expect(findItemByTitle(wrapper, "Heading 2").props("activate")).toBe(true);
    expect(findItemByTitle(wrapper, "Heading 1").props("activate")).toBe(false);
    expect(findItemByTitle(wrapper, "Italic").props("activate")).toBe(false);
  });
});

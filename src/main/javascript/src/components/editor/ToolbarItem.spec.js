import { describe, expect, it, vi } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import ToolbarItem from "./ToolbarItem.vue";

const makeEditor = (isActive = false) => ({
  isActive: vi.fn(() => isActive)
});

describe("ToolbarItem", () => {
  it("renders the ToolTip activator with the given icon and title", () => {
    const wrapper = mountComponent(ToolbarItem, {
      props: {
        editor: makeEditor(),
        icon: "mdi-format-bold",
        title: "Bold",
        action: "bold"
      }
    });

    const toolTip = wrapper.findComponent({ name: "ToolTip" });

    expect(toolTip.exists()).toBe(true);
    expect(toolTip.props("icon")).toBe("mdi-format-bold");
    expect(toolTip.props("content")).toBe("Bold");
  });

  it("emits clicked with the action and attributes when the button is clicked", async () => {
    const wrapper = mountComponent(ToolbarItem, {
      props: {
        editor: makeEditor(),
        icon: "mdi-format-header-1",
        title: "Heading 1",
        action: "heading",
        attributes: { level: 1 }
      }
    });

    await wrapper.findComponent({ name: "VBtn" }).trigger("click");

    expect(wrapper.emitted("clicked")).toBeTruthy();
    expect(wrapper.emitted("clicked")[0]).toEqual([
      "heading",
      { level: 1 }
    ]);
  });

  it("emits is-hovered when the activator is hovered", async () => {
    const wrapper = mountComponent(ToolbarItem, {
      props: {
        editor: makeEditor(),
        icon: "mdi-format-bold",
        title: "Bold",
        action: "bold"
      }
    });

    await wrapper.findComponent({ name: "VBtn" }).trigger("mouseenter");

    expect(wrapper.emitted("is-hovered")).toBeTruthy();
  });

  it("toggles the button on when activatable, active, and the editor mark is applied", async () => {
    const wrapper = mountComponent(ToolbarItem, {
      props: {
        editor: makeEditor(true),
        icon: "mdi-format-bold",
        title: "Bold",
        action: "bold",
        activatable: true,
        activate: false
      }
    });

    expect(wrapper.vm.toggled).toBe(null);

    await wrapper.setProps({ activate: true });

    expect(wrapper.vm.toggled).toBe(0);
  });

  it("does not toggle on when not activatable, even if activate becomes true", async () => {
    const wrapper = mountComponent(ToolbarItem, {
      props: {
        editor: makeEditor(true),
        icon: "mdi-minus",
        title: "Horizontal line",
        action: "horizontalRule",
        activatable: false,
        activate: false
      }
    });

    await wrapper.setProps({ activate: true });

    expect(wrapper.vm.toggled).toBe(null);
  });

  it("clears the toggle state when the editor prop changes and the mark is no longer active", async () => {
    const activeEditor = makeEditor(true);

    const wrapper = mountComponent(ToolbarItem, {
      props: {
        editor: activeEditor,
        icon: "mdi-format-bold",
        title: "Bold",
        action: "bold",
        activatable: true,
        activate: false
      }
    });

    await wrapper.setProps({ activate: true });
    expect(wrapper.vm.toggled).toBe(0);

    const inactiveEditor = makeEditor(false);
    await wrapper.setProps({ editor: inactiveEditor });

    expect(wrapper.vm.toggled).toBe(null);
  });
});

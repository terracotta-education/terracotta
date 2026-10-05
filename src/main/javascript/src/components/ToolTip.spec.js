import { afterEach, describe, expect, it, vi } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import ToolTip from "./ToolTip.vue";

describe("ToolTip", () => {
  it("renders a button activator with the icon by default", () => {
    const wrapper = mountComponent(ToolTip, {
      props: {
        content: "Helpful info",
        icon: "mdi-information"
      }
    });

    expect(wrapper.findComponent({ name: "VBtn" }).exists()).toBe(true);
  });

  it("opens on activator mouseenter and emits is-opened", async () => {
    const wrapper = mountComponent(ToolTip, {
      props: {
        content: "Helpful info",
        icon: "mdi-information"
      }
    });

    await wrapper.findComponent({ name: "VBtn" }).trigger("mouseenter");

    expect(wrapper.emitted("is-opened")).toBeTruthy();
  });

  it("renders a link activator when activatorType is link", () => {
    const wrapper = mountComponent(ToolTip, {
      props: {
        content: "Helpful info",
        activatorType: "link",
        activatorContent: "Learn more"
      }
    });

    expect(wrapper.find("a.has-tooltip, a").exists()).toBe(true);
  });

  it("emits clicked when the button activator is clicked", async () => {
    const wrapper = mountComponent(ToolTip, {
      props: {
        content: "Helpful info"
      }
    });

    await wrapper.findComponent({ name: "VBtn" }).trigger("click");

    expect(wrapper.emitted("clicked")).toBeTruthy();
  });

  it("closes the button activator's tooltip on mouseleave and blur", async () => {
    const wrapper = mountComponent(ToolTip, {
      props: {
        content: "Helpful info",
        icon: "mdi-information"
      }
    });
    const button = wrapper.findComponent({ name: "VBtn" });

    await button.trigger("mouseenter");
    expect(wrapper.vm.showToolTip).toBe(true);

    await button.trigger("mouseleave");
    expect(wrapper.vm.showToolTip).toBe(true);

    await button.trigger("blur");
    expect(wrapper.vm.showToolTip).toBe(true);
  });

  it("renders a link activator's icon and closes it on mouseleave and blur", async () => {
    const wrapper = mountComponent(ToolTip, {
      props: {
        content: "Helpful info",
        activatorType: "link",
        activatorContent: "Learn more",
        icon: "mdi-information"
      }
    });
    const link = wrapper.find("a");

    expect(link.find(".mdi-information").exists()).toBe(true);

    await link.trigger("mouseenter");
    expect(wrapper.vm.showToolTip).toBe(true);

    await link.trigger("mouseleave");
    await link.trigger("blur");
    expect(wrapper.vm.showToolTip).toBe(true);
  });

  it("renders a paragraph activator with an icon and responds to hover/focus", async () => {
    const wrapper = mountComponent(ToolTip, {
      props: {
        content: "Helpful info",
        activatorType: "paragraph",
        activatorContent: "Paragraph label",
        icon: "mdi-information"
      }
    });
    const paragraph = wrapper.find("p.has-tooltip");

    expect(paragraph.exists()).toBe(true);
    expect(paragraph.find(".mdi-information").exists()).toBe(true);

    await paragraph.trigger("mouseenter");
    expect(wrapper.vm.showToolTip).toBe(true);

    await paragraph.trigger("focus");
    await paragraph.trigger("mouseleave");
    await paragraph.trigger("blur");
    expect(wrapper.vm.showToolTip).toBe(true);
  });

  it("renders an icon-only activator and responds to hover/focus", async () => {
    const wrapper = mountComponent(ToolTip, {
      props: {
        content: "Helpful info",
        activatorType: "icon",
        icon: "mdi-information"
      }
    });
    const icon = wrapper.findComponent({ name: "VIcon" });

    expect(icon.exists()).toBe(true);

    await icon.trigger("mouseenter");
    expect(wrapper.vm.showToolTip).toBe(true);

    await icon.trigger("focus");
    await icon.trigger("mouseleave");
    await icon.trigger("blur");
    expect(wrapper.vm.showToolTip).toBe(true);
  });

  it("renders the header and closes when another tooltip opens", async () => {
    const other = mountComponent(ToolTip, {
      props: {
        content: "Other tooltip",
        icon: "mdi-alert"
      },
      attachTo: document.body
    });
    const viewer = mountComponent(ToolTip, {
      props: {
        content: "Helpful info",
        header: "More info",
        icon: "mdi-information"
      },
      attachTo: document.body
    });

    await viewer.findComponent({ name: "VBtn" }).trigger("mouseenter");
    expect(viewer.vm.showToolTip).toBe(true);
    expect(document.body.textContent).toContain("More info");

    await other.findComponent({ name: "VBtn" }).trigger("mouseenter");

    expect(viewer.vm.showToolTip).toBe(false);

    other.unmount();
    viewer.unmount();
  });

  describe("with fake timers", () => {
    afterEach(() => {
      vi.useRealTimers();
    });

    it("closes automatically after leaving the activator without entering the content", async () => {
      vi.useFakeTimers();

      const wrapper = mountComponent(ToolTip, {
        props: {
          content: "Helpful info",
          icon: "mdi-information"
        },
        attachTo: document.body
      });
      const button = wrapper.findComponent({ name: "VBtn" });

      await button.trigger("mouseenter");
      expect(wrapper.vm.showToolTip).toBe(true);

      await button.trigger("mouseleave");
      await vi.advanceTimersByTimeAsync(1000);

      expect(wrapper.vm.showToolTip).toBe(false);

      wrapper.unmount();
    });

    it("cancels the pending close when re-entering before the delay elapses, then closes after leaving the content", async () => {
      vi.useFakeTimers();

      const wrapper = mountComponent(ToolTip, {
        props: {
          content: "Helpful info",
          icon: "mdi-information"
        },
        attachTo: document.body
      });
      const button = wrapper.findComponent({ name: "VBtn" });

      await button.trigger("mouseenter");
      await button.trigger("mouseleave");
      await vi.advanceTimersByTimeAsync(500);
      expect(wrapper.vm.showToolTip).toBe(true);

      const content = document.querySelector(".tool-tip-content-body");

      content.dispatchEvent(new Event("mouseenter"));
      await vi.advanceTimersByTimeAsync(0);
      expect(wrapper.vm.showToolTip).toBe(true);

      content.dispatchEvent(new Event("mouseleave"));
      await vi.advanceTimersByTimeAsync(500);

      expect(wrapper.vm.showToolTip).toBe(false);

      wrapper.unmount();
    });

    it("closes on focus loss from the tooltip content", async () => {
      vi.useFakeTimers();

      const wrapper = mountComponent(ToolTip, {
        props: {
          content: "Helpful info",
          icon: "mdi-information"
        },
        attachTo: document.body
      });
      const button = wrapper.findComponent({ name: "VBtn" });

      await button.trigger("mouseenter");
      const content = document.querySelector(".tool-tip-content-body");

      content.dispatchEvent(new Event("focus"));
      content.dispatchEvent(new Event("blur"));
      await vi.advanceTimersByTimeAsync(500);

      expect(wrapper.vm.showToolTip).toBe(false);

      wrapper.unmount();
    });
  });

  it("closes when the Escape key is pressed", async () => {
    const wrapper = mountComponent(ToolTip, {
      props: {
        content: "Helpful info",
        icon: "mdi-information"
      }
    });

    await wrapper.findComponent({ name: "VBtn" }).trigger("mouseenter");
    expect(wrapper.vm.showToolTip).toBe(true);

    window.dispatchEvent(new KeyboardEvent("keydown", { key: "Escape" }));
    await wrapper.vm.$nextTick();

    expect(wrapper.vm.showToolTip).toBe(false);
  });
});

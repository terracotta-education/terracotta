import { describe, expect, it } from "vitest";

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
});

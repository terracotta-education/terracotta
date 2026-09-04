import { describe, expect, it } from "vitest";
import { createPinia, setActivePinia } from "pinia";

import { mountComponent } from "@/test-utils/mount";
import { configuration } from "@/store/configuration.module";
import Help from "./Help.vue";

describe("Help", () => {
  it("renders nothing when the configuration store has no helpUrl", () => {
    const pinia = createPinia();
    setActivePinia(pinia);
    configuration().configurations = {};

    const wrapper = mountComponent(Help, { pinia });

    expect(wrapper.findComponent({ name: "ToolTip" }).exists()).toBe(false);
  });

  it("renders a ToolTip pointed at the configured help url", () => {
    const pinia = createPinia();
    setActivePinia(pinia);
    configuration().configurations = {
      helpUrl: "https://example.com/help"
    };

    const wrapper = mountComponent(Help, { pinia });

    const tooltip = wrapper.findComponent({ name: "ToolTip" });

    expect(tooltip.exists()).toBe(true);
    expect(tooltip.props("url")).toBe("https://example.com/help");
    expect(tooltip.props("icon")).toBe("mdi-help-circle-outline");
  });
});

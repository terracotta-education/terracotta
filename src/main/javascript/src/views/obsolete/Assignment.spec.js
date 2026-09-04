import { describe, expect, it } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import Assignment from "./Assignment.vue";
import { configuration as configurationModule } from "@/store/configuration.module";

describe("InactiveExperimentNotice (obsolete/Assignment)", () => {
  it("defaults the LMS title to 'LMS' when no configuration is loaded", () => {
    const wrapper = mountComponent(Assignment);

    expect(wrapper.text()).toContain(
      "linked to an inactive Terracotta experiment"
    );
    expect(wrapper.text()).toContain(
      "click the Terracotta link from within"
    );
    expect(wrapper.text()).toContain("this LMS course site");
  });

  it("uses the configured lmsTitle when available", () => {
    const wrapper = mountComponent(Assignment);

    const configurationStore = configurationModule();
    configurationStore.configurations = { lmsTitle: "Canvas" };

    return wrapper.vm.$nextTick().then(() => {
      expect(wrapper.text()).toContain("this Canvas course site");
      expect(wrapper.text()).toContain(
        "copied your Canvas course"
      );
    });
  });

  it("renders the warning icon", () => {
    const wrapper = mountComponent(Assignment);

    expect(wrapper.find(".icon-circle-invalid").exists()).toBe(true);
    expect(wrapper.find(".icon-circle-invalid .mdi-exclamation").exists()).toBe(
      true
    );
  });

  it("renders the Terracotta logo image", () => {
    const wrapper = mountComponent(Assignment);

    const img = wrapper.findComponent({ name: "VImg" });
    expect(img.exists()).toBe(true);
    expect(img.props("alt")).toBe("Terracotta Logo");
  });
});

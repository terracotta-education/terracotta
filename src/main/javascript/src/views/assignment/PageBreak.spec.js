import { describe, expect, it } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import PageBreak from "./PageBreak.vue";

describe("PageBreak", () => {
  it("renders the 'Page break' label", () => {
    const wrapper = mountComponent(PageBreak);

    expect(wrapper.text()).toContain("Page break");
  });

  it("renders a divider on either side of the label", () => {
    const wrapper = mountComponent(PageBreak);

    expect(wrapper.findAllComponents({ name: "VDivider" }).length).toBe(2);
  });
});

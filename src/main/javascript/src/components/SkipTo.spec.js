import { describe, expect, it } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import SkipTo from "./SkipTo.vue";

describe("SkipTo", () => {
  it("renders a skip link pointing at the main content anchor", () => {
    const wrapper = mountComponent(SkipTo);

    const link = wrapper.find("a.skip-to-content-link");

    expect(link.exists()).toBe(true);
    expect(link.attributes("href")).toBe("#terracotta-main");
    expect(link.attributes("tabindex")).toBe("0");
    expect(link.text()).toBe("Skip to main content");
  });
});

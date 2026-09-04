import { describe, expect, it } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import TreatmentPreviewComplete from "./TreatmentPreviewComplete.vue";

describe("TreatmentPreviewComplete", () => {
  it("renders the preview complete heading and instructions", () => {
    const wrapper = mountComponent(TreatmentPreviewComplete);

    expect(wrapper.text()).toContain("Preview complete!");
    expect(wrapper.text()).toContain(
      "Please close your tab to return to Terracotta."
    );
  });

  it("renders a success check icon", () => {
    const wrapper = mountComponent(TreatmentPreviewComplete);

    const icon = wrapper.findComponent({ name: "VIcon" });
    expect(icon.exists()).toBe(true);
    expect(icon.classes()).toContain("mdi-check");
    expect(wrapper.find(".icon-circle-success").exists()).toBe(true);
  });

  it("renders the Terracotta logo image", () => {
    const wrapper = mountComponent(TreatmentPreviewComplete);

    const logo = wrapper.findComponent({ name: "VImg" });
    expect(logo.exists()).toBe(true);
    expect(logo.props("alt")).toBe("Terracotta Logo");
  });
});

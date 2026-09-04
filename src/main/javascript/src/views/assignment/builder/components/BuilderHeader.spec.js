import { describe, expect, it } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import BuilderHeader from "./BuilderHeader.vue";

describe("BuilderHeader", () => {
  it("renders the assignment title", () => {
    const wrapper = mountComponent(BuilderHeader, {
      props: {
        assignmentTitle: "My Assignment",
        conditionName: "Condition A"
      }
    });

    expect(wrapper.find("h1").text()).toBe("My Assignment");
  });

  it("renders the treatment condition chip when there is more than one treatment", () => {
    const wrapper = mountComponent(BuilderHeader, {
      props: {
        assignmentTitle: "My Assignment",
        conditionName: "Condition A",
        conditionColor: "#FFCCBC",
        hasSingleTreatment: false
      }
    });

    expect(wrapper.text()).toContain("Treatment");
    expect(wrapper.text()).toContain("Condition A");

    const chip = wrapper.findComponent({ name: "VChip" });
    expect(chip.exists()).toBe(true);
    expect(chip.props("color")).toBe("#FFCCBC");
  });

  it("hides the treatment condition chip when there is a single treatment", () => {
    const wrapper = mountComponent(BuilderHeader, {
      props: {
        assignmentTitle: "My Assignment",
        conditionName: "Condition A",
        hasSingleTreatment: true
      }
    });

    expect(wrapper.findComponent({ name: "VChip" }).exists()).toBe(false);
    expect(wrapper.text()).not.toContain("Treatment");
  });

  it("defaults conditionColor to null and hasSingleTreatment to false", () => {
    const wrapper = mountComponent(BuilderHeader, {
      props: {
        assignmentTitle: "My Assignment",
        conditionName: "Condition A"
      }
    });

    expect(wrapper.findComponent({ name: "VChip" }).props("color")).toBe(null);
  });
});

import { describe, expect, it } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import CopyCandidatesDialog from "./CopyCandidatesDialog.vue";

const candidates = [
  {
    id: "c1",
    experimentTitle: "Reading Study",
    sourceCourseTitle: "Course A",
    conditionCount: 2,
    assignmentCount: 3
  },
  {
    id: "c2",
    experimentTitle: "Writing Study",
    sourceCourseTitle: "Course A",
    conditionCount: 1,
    assignmentCount: 1
  }
];

describe("CopyCandidatesDialog", () => {
  it("renders one option per candidate, with its counts", () => {
    const wrapper = mountComponent(CopyCandidatesDialog, { props: { candidates } });

    expect(wrapper.text()).toContain("Reading Study");
    expect(wrapper.text()).toContain("Writing Study");
    expect(wrapper.text()).toContain("2 conditions");
    expect(wrapper.text()).toContain("1 assignment");
  });

  it("names the source course once in the intro, not per-candidate", () => {
    const wrapper = mountComponent(CopyCandidatesDialog, { props: { candidates } });

    expect(wrapper.find(".copy-candidates-intro").text()).toContain("Course A");
    expect(wrapper.find(".copy-candidate-meta").text()).not.toContain("Course A");
    expect(wrapper.find(".copy-candidate-meta").text()).not.toContain("From course");
  });

  it("starts with no candidate selected", () => {
    const wrapper = mountComponent(CopyCandidatesDialog, { props: { candidates } });

    const hiddenInput = wrapper.find("#copy-candidates-selected");
    expect(JSON.parse(hiddenInput.element.value)).toEqual([]);
  });

  it("updates the hidden input when a candidate is checked", async () => {
    const wrapper = mountComponent(CopyCandidatesDialog, { props: { candidates } });

    const checkboxes = wrapper.findAllComponents({ name: "VCheckbox" });
    await checkboxes[0].find("input").setValue(true);
    await wrapper.vm.$nextTick();

    const hiddenInput = wrapper.find("#copy-candidates-selected");
    expect(JSON.parse(hiddenInput.element.value)).toEqual(["c1"]);
  });

  it("selects every candidate when 'Select All' is clicked", async () => {
    const wrapper = mountComponent(CopyCandidatesDialog, { props: { candidates } });

    const buttons = wrapper.findAll(".copy-candidates-select-all-link");
    await buttons[0].trigger("click");

    const hiddenInput = wrapper.find("#copy-candidates-selected");
    expect(JSON.parse(hiddenInput.element.value)).toEqual(["c1", "c2"]);
  });

  it("unselects every candidate when 'Unselect All' is clicked", async () => {
    const wrapper = mountComponent(CopyCandidatesDialog, { props: { candidates } });

    const buttons = wrapper.findAll(".copy-candidates-select-all-link");
    await buttons[0].trigger("click");
    await buttons[1].trigger("click");

    const hiddenInput = wrapper.find("#copy-candidates-selected");
    expect(JSON.parse(hiddenInput.element.value)).toEqual([]);
  });
});

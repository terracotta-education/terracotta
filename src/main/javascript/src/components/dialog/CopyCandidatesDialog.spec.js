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

  it("updates the hidden input when a candidate option is clicked", async () => {
    const wrapper = mountComponent(CopyCandidatesDialog, { props: { candidates } });

    const options = wrapper.findAll(".copy-candidate-option");
    await options[0].trigger("click");

    const hiddenInput = wrapper.find("#copy-candidates-selected");
    expect(JSON.parse(hiddenInput.element.value)).toEqual(["c1"]);
  });

  it("toggles a candidate via the keyboard (Space or Enter), matching a native checkbox", async () => {
    const wrapper = mountComponent(CopyCandidatesDialog, { props: { candidates } });

    const options = wrapper.findAll(".copy-candidate-option");
    await options[0].trigger("keydown.space");
    expect(JSON.parse(wrapper.find("#copy-candidates-selected").element.value)).toEqual(["c1"]);

    await options[0].trigger("keydown.enter");
    expect(JSON.parse(wrapper.find("#copy-candidates-selected").element.value)).toEqual([]);
  });

  it("exposes each option as an accessible checkbox with the experiment title as its name", () => {
    const wrapper = mountComponent(CopyCandidatesDialog, { props: { candidates } });

    const options = wrapper.findAll(".copy-candidate-option");
    expect(options[0].attributes("role")).toBe("checkbox");
    expect(options[0].attributes("aria-checked")).toBe("false");
    expect(options[0].attributes("aria-label")).toBe("Reading Study");
    expect(options[0].attributes("tabindex")).toBe("0");
  });

  it("marks a selected option both visually (a class hook for the selected color treatment) and via aria-checked - not by color alone", async () => {
    const wrapper = mountComponent(CopyCandidatesDialog, { props: { candidates } });

    const option = wrapper.findAll(".copy-candidate-option")[0];
    await option.trigger("click");

    expect(option.classes()).toContain("copy-candidate-option--selected");
    expect(option.attributes("aria-checked")).toBe("true");
    // the checkmark icon is the non-color signal alongside the border/background change
    expect(option.findComponent({ name: "VIcon" }).exists()).toBe(true);
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

  it("disables 'Unselect All' while nothing is selected, and 'Select All' once everything is", async () => {
    const wrapper = mountComponent(CopyCandidatesDialog, { props: { candidates } });

    const buttons = wrapper.findAll(".copy-candidates-select-all-link");
    expect(buttons[0].attributes("disabled")).toBeUndefined();
    expect(buttons[1].attributes("disabled")).toBeDefined();

    await buttons[0].trigger("click");

    expect(buttons[0].attributes("disabled")).toBeDefined();
    expect(buttons[1].attributes("disabled")).toBeUndefined();
  });

  it("emits selectionChange with the current selection, including immediately on mount", async () => {
    const wrapper = mountComponent(CopyCandidatesDialog, { props: { candidates } });

    expect(wrapper.emitted("selectionChange")[0]).toEqual([[]]);

    await wrapper.findAll(".copy-candidate-option")[0].trigger("click");

    expect(wrapper.emitted("selectionChange").at(-1)).toEqual([["c1"]]);
  });
});

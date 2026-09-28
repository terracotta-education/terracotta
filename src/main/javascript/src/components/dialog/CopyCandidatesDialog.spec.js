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

  it("updates the hidden input when a candidate's checkbox is checked", async () => {
    const wrapper = mountComponent(CopyCandidatesDialog, { props: { candidates } });

    // jsdom doesn't fire a "change" event as a side effect of a synthetic click on a checkbox
    // (confirmed: neither trigger("click") nor calling .click() on the element triggers it,
    // only setValue() does), so these tests drive the checkbox via setValue() rather than
    // clicking it - a real browser fires "change" on click, which is what the component relies on
    const checkboxes = wrapper.findAll(".copy-candidate-checkbox");
    await checkboxes[0].setValue(true);

    const hiddenInput = wrapper.find("#copy-candidates-selected");
    expect(JSON.parse(hiddenInput.element.value)).toEqual(["c1"]);
  });

  // Space is now a real <input type="checkbox">'s native activation key (the browser toggles
  // it and fires "change" - nothing in this component needs to implement that itself, so
  // there's nothing here to unit test beyond trusting the platform). Enter still needs an
  // explicit handler, since it isn't a native checkbox activation key.
  it("also toggles a candidate on Enter, since a native checkbox alone wouldn't", async () => {
    const wrapper = mountComponent(CopyCandidatesDialog, { props: { candidates } });

    const checkbox = wrapper.findAll(".copy-candidate-checkbox")[0];
    await checkbox.trigger("keydown.enter");

    expect(JSON.parse(wrapper.find("#copy-candidates-selected").element.value)).toEqual(["c1"]);
  });

  it("exposes each option as a real checkbox with the experiment title as its accessible name", () => {
    const wrapper = mountComponent(CopyCandidatesDialog, { props: { candidates } });

    const checkbox = wrapper.findAll(".copy-candidate-checkbox")[0];
    expect(checkbox.element.type).toBe("checkbox");
    expect(checkbox.element.checked).toBe(false);
    // an explicit aria-label, not just the wrapping <label>'s own text, so the name stays just
    // the title instead of picking up the meta line ("2 conditions · 3 assignments") too
    expect(checkbox.attributes("aria-label")).toBe("Reading Study");
  });

  it("marks a selected option both visually (a class hook for the selected color treatment) and via the checkbox's checked state - not by color alone", async () => {
    const wrapper = mountComponent(CopyCandidatesDialog, { props: { candidates } });

    const option = wrapper.findAll(".copy-candidate-option")[0];
    await wrapper.findAll(".copy-candidate-checkbox")[0].setValue(true);

    expect(option.classes()).toContain("copy-candidate-option--selected");
    expect(wrapper.findAll(".copy-candidate-checkbox")[0].element.checked).toBe(true);
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

  it("disables 'Create selected' until at least one candidate is selected", async () => {
    const wrapper = mountComponent(CopyCandidatesDialog, { props: { candidates } });

    const createButton = wrapper.findAll(".copy-candidates-btn--primary")[0];
    expect(createButton.attributes("disabled")).toBeDefined();

    await wrapper.findAll(".copy-candidate-checkbox")[0].setValue(true);

    expect(createButton.attributes("disabled")).toBeUndefined();
  });

  describe("confirmation overlay", () => {
    it("shows a confirmation overlay over the (still-visible) grid instead of hiding it, for each of the three actions", async () => {
      const wrapper = mountComponent(CopyCandidatesDialog, { props: { candidates } });

      await wrapper.findAll(".copy-candidate-checkbox")[0].setValue(true);

      expect(wrapper.find(".copy-candidates-confirm-overlay").exists()).toBe(false);
      expect(wrapper.find(".copy-candidates-content").attributes("inert")).toBeUndefined();

      const [deferButton, declineButton, createButton] = wrapper.findAll(".copy-candidates-btn").filter(
        button => !button.element.closest(".copy-candidates-confirm-panel")
      );

      await deferButton.trigger("click");
      expect(wrapper.find(".copy-candidates-confirm-overlay").text()).toContain(
        "Experiment selection will be available"
      );
      // the grid is still in the DOM (present, just inert/dimmed) underneath the overlay
      expect(wrapper.find(".copy-candidates-grid").exists()).toBe(true);
      expect(wrapper.find(".copy-candidates-content").attributes("inert")).toBeDefined();

      await wrapper.find(".copy-candidates-confirm-buttons .copy-candidates-btn--tertiary").trigger("click");
      expect(wrapper.find(".copy-candidates-confirm-overlay").exists()).toBe(false);

      await declineButton.trigger("click");
      expect(wrapper.find(".copy-candidates-confirm-overlay").text()).toContain(
        "You will not be able to return to this screen"
      );
      await wrapper.find(".copy-candidates-confirm-buttons .copy-candidates-btn--tertiary").trigger("click");

      await createButton.trigger("click");
      expect(wrapper.find(".copy-candidates-confirm-overlay").text()).toContain(
        "Ensure you've selected all experiments"
      );
    });

    it("emits 'defer' only once the overlay confirmation is accepted", async () => {
      const wrapper = mountComponent(CopyCandidatesDialog, { props: { candidates } });
      const [deferButton] = wrapper.findAll(".copy-candidates-btn");

      await deferButton.trigger("click");
      expect(wrapper.emitted("defer")).toBeUndefined();

      await wrapper.find(".copy-candidates-confirm-buttons .copy-candidates-btn--primary").trigger("click");
      expect(wrapper.emitted("defer")).toHaveLength(1);
    });

    it("emits 'decline' only once the overlay confirmation is accepted", async () => {
      const wrapper = mountComponent(CopyCandidatesDialog, { props: { candidates } });
      const [, declineButton] = wrapper.findAll(".copy-candidates-btn");

      await declineButton.trigger("click");
      await wrapper.find(".copy-candidates-confirm-buttons .copy-candidates-btn--primary").trigger("click");

      expect(wrapper.emitted("decline")).toHaveLength(1);
    });

    it("emits 'create' with the current selection only once the overlay confirmation is accepted", async () => {
      const wrapper = mountComponent(CopyCandidatesDialog, { props: { candidates } });

      await wrapper.findAll(".copy-candidate-checkbox")[0].setValue(true);

      const [, , createButton] = wrapper.findAll(".copy-candidates-btn");
      await createButton.trigger("click");
      await wrapper.find(".copy-candidates-confirm-buttons .copy-candidates-btn--primary").trigger("click");

      expect(wrapper.emitted("create")).toEqual([[["c1"]]]);
    });

    it("does not emit anything when 'Go back to selection' is chosen, and lets the selection keep changing", async () => {
      const wrapper = mountComponent(CopyCandidatesDialog, { props: { candidates } });
      const [deferButton] = wrapper.findAll(".copy-candidates-btn");

      await deferButton.trigger("click");
      await wrapper.find(".copy-candidates-confirm-buttons .copy-candidates-btn--tertiary").trigger("click");

      expect(wrapper.emitted("defer")).toBeUndefined();
      expect(wrapper.find(".copy-candidates-confirm-overlay").exists()).toBe(false);

      await wrapper.findAll(".copy-candidate-checkbox")[0].setValue(true);
      const hiddenInput = wrapper.find("#copy-candidates-selected");
      expect(JSON.parse(hiddenInput.element.value)).toEqual(["c1"]);
    });
  });
});

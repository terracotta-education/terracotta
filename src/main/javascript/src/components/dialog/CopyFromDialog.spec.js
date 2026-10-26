import { describe, expect, it } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import CopyFromDialog from "./CopyFromDialog.vue";

const treatments = [
  {
    treatmentId: 1,
    conditionName: "Condition A",
    conditionColor: "blue"
  },
  {
    treatmentId: 2,
    conditionName: "Condition B",
    conditionColor: "red"
  }
];

describe("CopyFromDialog", () => {
  it("names the treatment being copied to, and lists a radio option per treatment", () => {
    const wrapper = mountComponent(CopyFromDialog, {
      props: {
        assignmentName: "Quiz 1",
        targetConditionName: "Condition C",
        treatments
      }
    });

    expect(wrapper.find(".copy-radio-label p").text().replace(/\s+/g, " ")).toBe(
      "Choose the treatment from which you wish to copy content to Quiz 1 - Treatment Condition C."
    );
    expect(wrapper.find(".copy-target-treatment b").text()).toBe("Quiz 1 - Treatment");
    expect(wrapper.findAllComponents({ name: "VRadio" }).length).toBe(2);
    expect(wrapper.text()).toContain("Condition A");
    expect(wrapper.text()).toContain("Condition B");
  });

  it("shows a condition chip per treatment when there is more than one treatment", () => {
    const wrapper = mountComponent(CopyFromDialog, {
      props: {
        targetConditionName: "Condition C",
        treatments
      }
    });

    expect(wrapper.find(".v-chip--only-one").exists()).toBe(false);
  });

  it("shows an Only One Version chip and auto-selects it for a single-version assignment", async () => {
    const wrapper = mountComponent(CopyFromDialog, {
      props: {
        targetConditionName: "Condition C",
        treatments: [treatments[0]],
        singleVersion: true
      }
    });

    await wrapper.vm.$nextTick();

    expect(wrapper.text()).toContain("Only One Version");
    expect(wrapper.find("#treatment-option-selected").element.value).toBe("1");
  });

  // e.g. a three-condition assignment where only one other treatment has content yet
  it("names the condition when only one treatment of a multi-version assignment can be copied", async () => {
    const wrapper = mountComponent(CopyFromDialog, {
      props: {
        targetConditionName: "Condition C",
        treatments: [treatments[1]]
      }
    });

    await wrapper.vm.$nextTick();

    expect(wrapper.text()).toContain("Condition B");
    expect(wrapper.text()).not.toContain("Only One Version");
    expect(wrapper.find("#treatment-option-selected").element.value).toBe("2");
  });

  it("shows condition chips as solid color, like the components table", () => {
    const wrapper = mountComponent(CopyFromDialog, {
      props: { targetConditionName: "Condition C", treatments }
    });

    // tonal chips use the pale condition colors as their text color, which was unreadable
    // the treatment being copied to, then one per option
    expect(wrapper.findAllComponents({ name: "VChip" }).map(chip => chip.props("variant"))).toEqual(["flat", "flat", "flat"]);
  });

  it("calls the treatment being copied to the only version when its assignment has one", () => {
    const wrapper = mountComponent(CopyFromDialog, {
      props: { assignmentName: "Quiz 1", targetSingleVersion: true, treatments }
    });

    expect(wrapper.find(".copy-radio-label p").text().replace(/\s+/g, " ")).toBe(
      "Choose the treatment from which you wish to copy content to Quiz 1 - Treatment Only One Version."
    );
  });

  describe("treatments with no content", () => {
    const withEmpty = [
      { treatmentId: 1, conditionName: "Condition A", conditionColor: "blue" },
      { treatmentId: 2, conditionName: "Condition B", conditionColor: "red", disabled: true },
      { treatmentId: 3, conditionName: "Condition C", conditionColor: "green", disabled: true }
    ];

    it("lists them disabled, with a tooltip saying why", () => {
      const wrapper = mountComponent(CopyFromDialog, {
        props: { targetConditionName: "Condition C", treatments: withEmpty }
      });

      const radios = wrapper.findAllComponents({ name: "VRadio" });
      expect(radios.map(radio => radio.props("disabled"))).toEqual([false, true, true]);

      const tooltips = wrapper.findAllComponents({ name: "VTooltip" });
      expect(tooltips.map(tooltip => tooltip.props("disabled"))).toEqual([true, false, false]);
      // beside the treatment it's about, not over the option above it
      expect(tooltips[1].props("location")).toBe("end");
      expect(tooltips[1].props("contentClass")).toBe("tool-tip-content");
      // and for screen readers, which don't get the tooltip on a disabled radio
      expect(wrapper.findAll(".d-sr-only").map(el => el.text())).toEqual(["This treatment has no content.", "This treatment has no content."]);
    });

    it("selects the one treatment that has content", async () => {
      const wrapper = mountComponent(CopyFromDialog, {
        props: { targetConditionName: "Condition C", treatments: withEmpty }
      });

      await wrapper.vm.$nextTick();

      expect(wrapper.find("#treatment-option-selected").element.value).toBe("1");
    });
  });

  it("leaves no treatment selected by default when there are multiple treatments", () => {
    const wrapper = mountComponent(CopyFromDialog, {
      props: {
        targetConditionName: "Condition C",
        treatments
      }
    });

    expect(wrapper.find("#treatment-option-selected").element.value).toBe("");
  });

  it("updates the hidden input when a radio option is selected", async () => {
    const wrapper = mountComponent(CopyFromDialog, {
      props: {
        targetConditionName: "Condition C",
        treatments
      }
    });

    const radioGroup = wrapper.findComponent({ name: "VRadioGroup" });
    await radioGroup.setValue(2);

    expect(wrapper.find("#treatment-option-selected").element.value).toBe("2");
  });
});

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
  it("renders the assignment name and a radio option per treatment", () => {
    const wrapper = mountComponent(CopyFromDialog, {
      props: {
        assignmentName: "Quiz 1",
        treatments
      }
    });

    expect(wrapper.text()).toContain("Quiz 1");
    expect(wrapper.findAllComponents({ name: "VRadio" }).length).toBe(2);
    expect(wrapper.text()).toContain("Condition A");
    expect(wrapper.text()).toContain("Condition B");
  });

  it("shows a condition chip per treatment when there is more than one treatment", () => {
    const wrapper = mountComponent(CopyFromDialog, {
      props: {
        assignmentName: "Quiz 1",
        treatments
      }
    });

    expect(wrapper.find(".v-chip--only-one").exists()).toBe(false);
  });

  it("shows a single Only One Version chip and auto-selects it when there is a single treatment", async () => {
    const wrapper = mountComponent(CopyFromDialog, {
      props: {
        assignmentName: "Quiz 1",
        treatments: [treatments[0]]
      }
    });

    await wrapper.vm.$nextTick();

    expect(wrapper.text()).toContain("Only One Version");
    expect(wrapper.find("#treatment-option-selected").element.value).toBe("1");
  });

  it("leaves no treatment selected by default when there are multiple treatments", () => {
    const wrapper = mountComponent(CopyFromDialog, {
      props: {
        assignmentName: "Quiz 1",
        treatments
      }
    });

    expect(wrapper.find("#treatment-option-selected").element.value).toBe("");
  });

  it("updates the hidden input when a radio option is selected", async () => {
    const wrapper = mountComponent(CopyFromDialog, {
      props: {
        assignmentName: "Quiz 1",
        treatments
      }
    });

    const radioGroup = wrapper.findComponent({ name: "VRadioGroup" });
    await radioGroup.setValue(2);

    expect(wrapper.find("#treatment-option-selected").element.value).toBe("2");
  });
});

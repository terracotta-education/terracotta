import { describe, expect, it } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import MoveAssignmentDialog from "./MoveAssignmentDialog.vue";

const exposures = [
  {
    exposureId: 1,
    title: "Exposure A"
  },
  {
    exposureId: 2,
    title: "Exposure B"
  }
];

describe("MoveAssignmentDialog", () => {
  it("renders the assignment name and a radio option per exposure", () => {
    const wrapper = mountComponent(MoveAssignmentDialog, {
      props: {
        assignmentName: "Quiz 1",
        exposures
      }
    });

    expect(wrapper.text()).toContain("Quiz 1");
    expect(wrapper.findAllComponents({ name: "VRadio" }).length).toBe(2);
    expect(wrapper.text()).toContain("Exposure A");
    expect(wrapper.text()).toContain("Exposure B");
  });

  it("leaves no exposure selected by default when there are multiple exposures", () => {
    const wrapper = mountComponent(MoveAssignmentDialog, {
      props: {
        assignmentName: "Quiz 1",
        exposures
      }
    });

    expect(wrapper.find("#exposure-option-selected").element.value).toBe("");
  });

  it("auto-selects the exposure when there is only a single exposure", async () => {
    const wrapper = mountComponent(MoveAssignmentDialog, {
      props: {
        assignmentName: "Quiz 1",
        exposures: [exposures[0]]
      }
    });

    await wrapper.vm.$nextTick();

    expect(wrapper.find("#exposure-option-selected").element.value).toBe("1");
  });

  it("updates the hidden input when a radio option is selected", async () => {
    const wrapper = mountComponent(MoveAssignmentDialog, {
      props: {
        assignmentName: "Quiz 1",
        exposures
      }
    });

    const radioGroup = wrapper.findComponent({ name: "VRadioGroup" });
    await radioGroup.setValue(2);

    expect(wrapper.find("#exposure-option-selected").element.value).toBe("2");
  });

  it("disables the confirm button in the DOM when nothing is selected", () => {
    const confirmButton = document.createElement("button");
    confirmButton.className = "response-option-confirm";
    document.body.appendChild(confirmButton);

    const wrapper = mountComponent(MoveAssignmentDialog, {
      props: {
        assignmentName: "Quiz 1",
        exposures
      }
    });

    expect(confirmButton.disabled).toBe(true);

    wrapper.unmount();
    confirmButton.remove();
  });

  it("enables the confirm button in the DOM once an exposure is selected", async () => {
    const confirmButton = document.createElement("button");
    confirmButton.className = "response-option-confirm";
    document.body.appendChild(confirmButton);

    const wrapper = mountComponent(MoveAssignmentDialog, {
      props: {
        assignmentName: "Quiz 1",
        exposures
      }
    });

    const radioGroup = wrapper.findComponent({ name: "VRadioGroup" });
    await radioGroup.setValue(1);

    expect(confirmButton.disabled).toBe(false);

    wrapper.unmount();
    confirmButton.remove();
  });
});

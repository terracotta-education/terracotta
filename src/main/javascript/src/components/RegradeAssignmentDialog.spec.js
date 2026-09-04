import { afterEach, describe, expect, it } from "vitest";
import { createPinia, setActivePinia } from "pinia";

import { mountComponent } from "@/test-utils/mount";
import { configuration } from "@/store/configuration.module";
import RegradeAssignmentDialog from "./RegradeAssignmentDialog.vue";

describe("RegradeAssignmentDialog", () => {
  afterEach(() => {
    document.body.innerHTML = "";
  });

  it("renders singular student count copy for a single affected student", () => {
    const wrapper = mountComponent(RegradeAssignmentDialog, {
      props: {
        assignmentName: "Quiz 1",
        conditionName: "Treatment A",
        studentCount: 1,
        editedQuestionCount: 1
      }
    });

    expect(wrapper.text()).toContain("1 student");
    expect(wrapper.text()).toContain("has already completed");
    expect(wrapper.text()).toContain("Treatment A version");
    expect(wrapper.text()).toContain("Quiz 1 assignment");
  });

  it("renders plural student count copy for multiple affected students", () => {
    const wrapper = mountComponent(RegradeAssignmentDialog, {
      props: {
        assignmentName: "Quiz 1",
        conditionName: "Treatment A",
        studentCount: 5,
        editedQuestionCount: 2
      }
    });

    expect(wrapper.text()).toContain("5 students");
    expect(wrapper.text()).toContain("have already completed");
  });

  it("uses 'this question' phrasing for a single edited question and 'LMS' fallback with no configured lmsTitle", () => {
    const wrapper = mountComponent(RegradeAssignmentDialog, {
      props: {
        studentCount: 1,
        editedQuestionCount: 1
      }
    });

    expect(wrapper.text()).toContain("Give everyone full credit for this question");
    expect(wrapper.text()).toContain("Update this question without regrading");
    expect(wrapper.text()).toContain("LMS will regrade");
  });

  it("uses 'the questions you've changed' phrasing for multiple edited questions and the configured lmsTitle", () => {
    const pinia = createPinia();
    setActivePinia(pinia);
    configuration().configurations = { lmsTitle: "Canvas" };

    const wrapper = mountComponent(RegradeAssignmentDialog, {
      pinia,
      props: {
        studentCount: 2,
        editedQuestionCount: 3
      }
    });

    expect(wrapper.text()).toContain(
      "Give everyone full credit for the questions you've changed"
    );
    expect(wrapper.text()).toContain("Canvas will regrade");
  });

  it("renders all four regrade radio options", () => {
    const wrapper = mountComponent(RegradeAssignmentDialog, {
      props: { studentCount: 1, editedQuestionCount: 1 }
    });

    const radios = wrapper.findAllComponents({ name: "VRadio" });

    expect(radios).toHaveLength(4);
  });

  it("updates the hidden input and enables an external confirm button when an option is selected", async () => {
    const confirmButton = document.createElement("button");
    confirmButton.className = "response-option-confirm";
    confirmButton.disabled = true;
    document.body.appendChild(confirmButton);

    const wrapper = mountComponent(RegradeAssignmentDialog, {
      props: { studentCount: 1, editedQuestionCount: 1 },
      attachTo: document.body
    });

    expect(document.getElementById("regrade-option-selected").value).toBe("");
    expect(confirmButton.disabled).toBe(true);

    const radios = wrapper.findAllComponents({ name: "VRadio" });
    await radios[0].find("input").trigger("click");
    await wrapper.vm.$nextTick();

    expect(document.getElementById("regrade-option-selected").value).toBe("BOTH");
    expect(confirmButton.disabled).toBe(false);

    wrapper.unmount();
  });
});

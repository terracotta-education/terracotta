import { describe, expect, it } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import StudentQuizPagination from "./StudentQuizPagination.vue";

const baseProps = {
  experimentId: "1",
  conditionId: "2",
  treatmentId: "3",
  ownerId: "4"
};

describe("StudentQuizPagination", () => {
  it("hides all buttons when none of the show flags are set", () => {
    const wrapper = mountComponent(StudentQuizPagination, {
      props: { ...baseProps }
    });

    expect(wrapper.findAllComponents({ name: "VBtn" })).toHaveLength(0);
  });

  it("renders a disabled Back button that does not emit 'back' when clicked", async () => {
    const wrapper = mountComponent(StudentQuizPagination, {
      props: {
        ...baseProps,
        showBackButton: true,
        disableBackButton: true
      }
    });

    const buttons = wrapper.findAllComponents({ name: "VBtn" });
    expect(buttons).toHaveLength(1);
    expect(buttons[0].text()).toBe("Back");
    expect(buttons[0].props("disabled")).toBe(true);

    await buttons[0].trigger("click");
    expect(wrapper.emitted("back")).toBeFalsy();
  });

  it("emits 'back' when an enabled Back button is clicked", async () => {
    const wrapper = mountComponent(StudentQuizPagination, {
      props: {
        ...baseProps,
        showBackButton: true,
        disableBackButton: false
      }
    });

    const button = wrapper.findComponent({ name: "VBtn" });
    await button.trigger("click");

    expect(wrapper.emitted("back")).toBeTruthy();
  });

  it("renders an enabled Next button and emits 'next' when clicked", async () => {
    const wrapper = mountComponent(StudentQuizPagination, {
      props: {
        ...baseProps,
        showNextButton: true,
        disableNextButton: false
      }
    });

    const button = wrapper.findComponent({ name: "VBtn" });
    expect(button.text()).toBe("Next");
    expect(button.props("disabled")).toBe(false);

    await button.trigger("click");
    expect(wrapper.emitted("next")).toBeTruthy();
  });

  it("renders a type=submit Submit button when not in preview mode", () => {
    const wrapper = mountComponent(StudentQuizPagination, {
      props: {
        ...baseProps,
        preview: false,
        showSubmitButton: true,
        disableSubmitButton: true
      }
    });

    const button = wrapper.findComponent({ name: "VBtn" });
    expect(button.text()).toBe("Submit");
    expect(button.attributes("type")).toBe("submit");
    expect(button.props("disabled")).toBe(true);
  });

  it("renders an anchor-style Submit button with a preview completion href when in preview mode", () => {
    const wrapper = mountComponent(StudentQuizPagination, {
      props: {
        ...baseProps,
        preview: true,
        showSubmitButton: true,
        disableSubmitButton: false
      }
    });

    const button = wrapper.findComponent({ name: "VBtn" });
    expect(button.text()).toBe("Submit");
    expect(button.props("href")).toBe(
      "/preview/experiments/1/conditions/2/treatments/3/complete?ownerId=4"
    );
  });

  it("does not render the non-preview submit button while in preview mode", () => {
    const wrapper = mountComponent(StudentQuizPagination, {
      props: {
        ...baseProps,
        preview: true,
        showSubmitButton: true
      }
    });

    const buttons = wrapper.findAllComponents({ name: "VBtn" });
    expect(buttons).toHaveLength(1);
    expect(buttons[0].attributes("type")).not.toBe("submit");
  });
});

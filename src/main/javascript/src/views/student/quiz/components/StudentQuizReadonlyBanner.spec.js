import { describe, expect, it } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import StudentQuizReadonlyBanner from "./StudentQuizReadonlyBanner.vue";

describe("StudentQuizReadonlyBanner", () => {
  it("shows the muted message and no submission selector when muted", () => {
    const wrapper = mountComponent(StudentQuizReadonlyBanner, {
      props: {
        muted: true,
        assignmentData: { submissions: [{ submissionId: 1, dateSubmitted: 100 }] }
      }
    });

    expect(wrapper.text()).toContain("Your assignment is muted");
    expect(wrapper.findComponent({ name: "SubmissionSelector" }).exists()).toBe(false);
  });

  it("shows nothing when not muted but there are no submissions", () => {
    const wrapper = mountComponent(StudentQuizReadonlyBanner, {
      props: { muted: false, assignmentData: null }
    });

    expect(wrapper.text()).not.toContain("Your assignment is muted");
    expect(wrapper.findComponent({ name: "SubmissionSelector" }).exists()).toBe(false);
  });

  it("renders a SubmissionSelector with the assignment's submissions when not muted", () => {
    const submissions = [
      { submissionId: 1, dateSubmitted: 100 },
      { submissionId: 2, dateSubmitted: 200 }
    ];

    const wrapper = mountComponent(StudentQuizReadonlyBanner, {
      props: { muted: false, assignmentData: { submissions } }
    });

    const selector = wrapper.findComponent({ name: "SubmissionSelector" });
    expect(selector.exists()).toBe(true);
    expect(selector.props("submissions")).toEqual(submissions);
  });

  it("re-emits 'select-submission' when the SubmissionSelector emits 'select'", async () => {
    const submissions = [
      { submissionId: 1, dateSubmitted: 100 },
      { submissionId: 2, dateSubmitted: 200 }
    ];

    const wrapper = mountComponent(StudentQuizReadonlyBanner, {
      props: { muted: false, assignmentData: { submissions } }
    });

    const selector = wrapper.findComponent({ name: "SubmissionSelector" });
    await selector.findComponent({ name: "VSelect" }).vm.$emit("update:modelValue", 2);

    expect(wrapper.emitted("select-submission")).toBeTruthy();
    expect(wrapper.emitted("select-submission").at(-1)).toEqual([2]);
  });
});

import { describe, expect, it } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import StudentQuizSubmissionDetails from "./StudentQuizSubmissionDetails.vue";

describe("StudentQuizSubmissionDetails", () => {
  it("renders default placeholder values when no props are given", () => {
    const wrapper = mountComponent(StudentQuizSubmissionDetails);

    expect(wrapper.text()).toContain("Submission Details");
    expect(wrapper.text()).toContain("-");
  });

  it("renders all provided submission detail fields", () => {
    const wrapper = mountComponent(StudentQuizSubmissionDetails, {
      props: {
        timeBeforeSubmission: "5 minutes",
        allowedAttempts: 3,
        dateSubmitted: "June 1st 2024 10:00am",
        currentScore: "8 / 10",
        keptScore: "9 / 10"
      }
    });

    const text = wrapper.text();

    expect(text).toContain("Time");
    expect(text).toContain("5 minutes");
    expect(text).toContain("Allowed Attempts");
    expect(text).toContain("3");
    expect(text).toContain("Submitted");
    expect(text).toContain("June 1st 2024 10:00am");
    expect(text).toContain("Current Score");
    expect(text).toContain("8 / 10");
    expect(text).toContain("Kept Score");
    expect(text).toContain("9 / 10");
  });

  it("renders the string 'Unlimited' for allowedAttempts when passed through as a string", () => {
    const wrapper = mountComponent(StudentQuizSubmissionDetails, {
      props: { allowedAttempts: "Unlimited" }
    });

    expect(wrapper.text()).toContain("Unlimited");
  });
});

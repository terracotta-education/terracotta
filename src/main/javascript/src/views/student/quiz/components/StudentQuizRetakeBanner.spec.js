import { describe, expect, it } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import StudentQuizRetakeBanner from "./StudentQuizRetakeBanner.vue";

describe("StudentQuizRetakeBanner", () => {
  it("renders nothing when the student cannot try again and there is no message", () => {
    const wrapper = mountComponent(StudentQuizRetakeBanner, {
      props: { canTryAgain: false, cantTryAgainMessage: null }
    });

    expect(wrapper.findComponent({ name: "VBtn" }).exists()).toBe(false);
    expect(wrapper.text()).toBe("");
  });

  it("shows the Try Again button and emits 'try-again' when clicked", async () => {
    const wrapper = mountComponent(StudentQuizRetakeBanner, {
      props: { canTryAgain: true, scoringScheme: "HIGHEST" }
    });

    const button = wrapper.findComponent({ name: "VBtn" });
    expect(button.text()).toBe("Try Again");

    await button.trigger("click");
    expect(wrapper.emitted("try-again")).toBeTruthy();
  });

  it.each([
    ["HIGHEST", "The highest"],
    ["MOST_RECENT", "The most recent"],
    ["AVERAGE", "The average"],
    ["CUMULATIVE", "A cumulative"]
  ])("describes the %s scoring scheme as '%s'", (scoringScheme, expectedText) => {
    const wrapper = mountComponent(StudentQuizRetakeBanner, {
      props: { canTryAgain: true, scoringScheme }
    });

    expect(wrapper.text()).toContain(`${expectedText} score will be kept`);
  });

  it("shows the max-attempts message", () => {
    const wrapper = mountComponent(StudentQuizRetakeBanner, {
      props: { canTryAgain: false, cantTryAgainMessage: "MAX_NUMBER_ATTEMPTS_REACHED" }
    });

    expect(wrapper.text()).toContain("You have reached the maximum number of attempts");
    expect(wrapper.findComponent({ name: "VBtn" }).exists()).toBe(false);
  });

  it("shows the wait-time message", () => {
    const wrapper = mountComponent(StudentQuizRetakeBanner, {
      props: { canTryAgain: false, cantTryAgainMessage: "WAIT_TIME_NOT_REACHED" }
    });

    expect(wrapper.text()).toContain("You must wait a period of time before submitting again");
  });

  it("can show both the try-again button and a cant-try-again message together", () => {
    const wrapper = mountComponent(StudentQuizRetakeBanner, {
      props: {
        canTryAgain: true,
        scoringScheme: "AVERAGE",
        cantTryAgainMessage: "MAX_NUMBER_ATTEMPTS_REACHED"
      }
    });

    expect(wrapper.findComponent({ name: "VBtn" }).exists()).toBe(true);
    expect(wrapper.text()).toContain("maximum number of attempts");
  });
});

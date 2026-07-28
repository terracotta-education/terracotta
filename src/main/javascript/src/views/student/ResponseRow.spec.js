import { describe, expect, it } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import ResponseRow from "./ResponseRow.vue";

describe("ResponseRow", () => {
  it("renders slot content inside the card title", () => {
    const wrapper = mountComponent(ResponseRow, {
      slots: {
        default: "<span class=\"answer-text\">An answer</span>"
      }
    });

    expect(wrapper.find(".answer-text").exists()).toBe(true);
    expect(wrapper.text()).toContain("An answer");
  });

  it("applies no border class when correct is null (default)", () => {
    const wrapper = mountComponent(ResponseRow);

    const card = wrapper.findComponent({ name: "VCard" });

    expect(card.classes()).not.toContain("green-border");
    expect(card.classes()).not.toContain("red-border");
  });

  it("applies the green-border class when correct is true", () => {
    const wrapper = mountComponent(ResponseRow, {
      props: { correct: true }
    });

    const card = wrapper.findComponent({ name: "VCard" });

    expect(card.classes()).toContain("green-border");
  });

  it("applies the red-border class when correct is false", () => {
    const wrapper = mountComponent(ResponseRow, {
      props: { correct: false }
    });

    const card = wrapper.findComponent({ name: "VCard" });

    expect(card.classes()).toContain("red-border");
  });
});

import { afterEach, describe, expect, it } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import Toggle from "./Toggle.vue";

describe("Toggle", () => {
  let wrapper;

  afterEach(() => {
    wrapper?.unmount();
  });

  it("renders a button for each of the two options", () => {
    wrapper = mountComponent(Toggle, {
      props: { selectedOption: "AND", options: ["AND", "OR"] }
    });

    const buttons = wrapper.findAllComponents({ name: "VBtn" });

    expect(buttons).toHaveLength(2);
    expect(buttons[0].text()).toBe("AND");
    expect(buttons[1].text()).toBe("OR");
  });

  it("defaults the toggle group to the selectedOption prop", () => {
    wrapper = mountComponent(Toggle, {
      props: { selectedOption: "OR", options: ["AND", "OR"] }
    });

    expect(wrapper.findComponent({ name: "VBtnToggle" }).props("modelValue")).toBe(
      "OR"
    );
  });

  it("does not emit update on initial mount", () => {
    wrapper = mountComponent(Toggle, {
      props: { selectedOption: "AND", options: ["AND", "OR"] }
    });

    expect(wrapper.emitted("update")).toBeFalsy();
  });

  it("emits update with the newly selected option when a button is clicked", async () => {
    wrapper = mountComponent(Toggle, {
      props: { selectedOption: "INCLUDE", options: ["INCLUDE", "EXCLUDE"] }
    });

    await wrapper.findComponent({ name: "VBtnToggle" }).setValue("EXCLUDE");

    expect(wrapper.emitted("update")).toEqual([["EXCLUDE"]]);
  });

  it("disables both buttons when readOnly is true", () => {
    wrapper = mountComponent(Toggle, {
      props: { selectedOption: "AND", options: ["AND", "OR"], readOnly: true }
    });

    const buttons = wrapper.findAllComponents({ name: "VBtn" });

    expect(buttons[0].props("disabled")).toBe(true);
    expect(buttons[1].props("disabled")).toBe(true);
  });

  it("re-syncs and emits update when the selectedOption prop changes externally", async () => {
    wrapper = mountComponent(Toggle, {
      props: { selectedOption: "AND", options: ["AND", "OR"] }
    });

    await wrapper.setProps({ selectedOption: "OR" });

    expect(wrapper.findComponent({ name: "VBtnToggle" }).props("modelValue")).toBe(
      "OR"
    );
    expect(wrapper.emitted("update").at(-1)).toEqual(["OR"]);
  });
});

import { describe, expect, it } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import Outcomes from "./Outcomes.vue";

const stubs = {
  SectionInput: true,
  SectionOutput: true
};

describe("Outcomes", () => {
  it("renders the input and output sections, with the output panel hidden by default", () => {
    const wrapper = mountComponent(Outcomes, { global: { stubs } });

    expect(wrapper.findComponent({ name: "SectionInput" }).exists()).toBe(true);

    const output = wrapper.findComponent({ name: "SectionOutput" });
    expect(output.exists()).toBe(true);
    expect(output.props("showOutputPanel")).toBe(false);
  });

  it("shows the output panel once the input section reports a selection", async () => {
    const wrapper = mountComponent(Outcomes, { global: { stubs } });

    wrapper.findComponent({ name: "SectionInput" }).vm.$emit("hasSelection", true);
    await wrapper.vm.$nextTick();

    expect(wrapper.findComponent({ name: "SectionOutput" }).props("showOutputPanel")).toBe(true);
  });

  it("hides the output panel again when the input section reports the selection was cleared", async () => {
    const wrapper = mountComponent(Outcomes, { global: { stubs } });

    const input = wrapper.findComponent({ name: "SectionInput" });
    input.vm.$emit("hasSelection", true);
    await wrapper.vm.$nextTick();
    input.vm.$emit("hasSelection", false);
    await wrapper.vm.$nextTick();

    expect(wrapper.findComponent({ name: "SectionOutput" }).props("showOutputPanel")).toBe(false);
  });
});

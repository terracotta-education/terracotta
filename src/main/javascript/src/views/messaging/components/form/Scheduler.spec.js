import { afterEach, describe, expect, it } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import Scheduler from "./Scheduler.vue";

const stubs = { DateTimePicker: true };

describe("Scheduler", () => {
  let wrapper;

  afterEach(() => {
    wrapper?.unmount();
  });

  it("renders the header and the provided label", () => {
    wrapper = mountComponent(Scheduler, {
      props: { sendAt: null, label: "Decide when to send this message" },
      global: { stubs }
    });

    expect(wrapper.text()).toContain("Scheduler");
    expect(wrapper.text()).toContain("Decide when to send this message");
  });

  it("passes sendAt and readOnly through to the DateTimePicker", () => {
    wrapper = mountComponent(Scheduler, {
      props: { sendAt: "2026-08-01T10:00:00Z", readOnly: true },
      global: { stubs }
    });

    const picker = wrapper.findComponent({ name: "DateTimePicker" });

    expect(picker.props("modelValue")).toBe("2026-08-01T10:00:00Z");
    expect(picker.props("disabled")).toBe(true);
  });

  it("emits updated with the new date when the picker changes", async () => {
    wrapper = mountComponent(Scheduler, {
      props: { sendAt: null },
      global: { stubs }
    });

    const picker = wrapper.findComponent({ name: "DateTimePicker" });
    await picker.vm.$emit("update:modelValue", "2026-09-01T12:00:00Z");

    expect(wrapper.emitted("updated")).toEqual([["2026-09-01T12:00:00Z"]]);
  });

  it("updates the picker value reactively when the sendAt prop changes", async () => {
    wrapper = mountComponent(Scheduler, {
      props: { sendAt: "2026-08-01T10:00:00Z" },
      global: { stubs }
    });

    await wrapper.setProps({ sendAt: "2026-10-05T09:30:00Z" });

    const picker = wrapper.findComponent({ name: "DateTimePicker" });
    expect(picker.props("modelValue")).toBe("2026-10-05T09:30:00Z");
  });
});

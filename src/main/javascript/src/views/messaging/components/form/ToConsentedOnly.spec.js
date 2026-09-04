import { afterEach, describe, expect, it } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import ToConsentedOnly from "./ToConsentedOnly.vue";

describe("ToConsentedOnly", () => {
  let wrapper;

  afterEach(() => {
    wrapper?.unmount();
  });

  it("enables and checks the switch when the experiment participation type is CONSENT and selected is true", () => {
    wrapper = mountComponent(ToConsentedOnly, {
      props: {
        selected: true,
        experiment: { participationType: "CONSENT" }
      }
    });

    const swtch = wrapper.findComponent({ name: "VSwitch" });
    expect(swtch.props("disabled")).toBe(false);
    expect(swtch.props("modelValue")).toBe(true);
  });

  it("disables and forces the switch off when participation type is not CONSENT, even if selected is true", () => {
    wrapper = mountComponent(ToConsentedOnly, {
      props: {
        selected: true,
        experiment: { participationType: "OPT_OUT" }
      }
    });

    const swtch = wrapper.findComponent({ name: "VSwitch" });
    expect(swtch.props("disabled")).toBe(true);
    expect(swtch.props("modelValue")).toBe(false);
  });

  it("disables the switch when readOnly is true", () => {
    wrapper = mountComponent(ToConsentedOnly, {
      props: {
        selected: true,
        experiment: { participationType: "CONSENT" },
        readOnly: true
      }
    });

    expect(wrapper.findComponent({ name: "VSwitch" }).props("disabled")).toBe(true);
  });

  it("emits updated with the new value when toggled", async () => {
    wrapper = mountComponent(ToConsentedOnly, {
      props: {
        selected: false,
        experiment: { participationType: "CONSENT" }
      }
    });

    await wrapper.findComponent({ name: "VSwitch" }).setValue(true);

    expect(wrapper.emitted("updated")).toEqual([[true]]);
  });

  it("forces the selection off and emits updated(false) when the experiment stops being consent-based", async () => {
    wrapper = mountComponent(ToConsentedOnly, {
      props: {
        selected: true,
        experiment: { participationType: "CONSENT" }
      }
    });

    await wrapper.setProps({ experiment: { participationType: "OPT_OUT" } });

    expect(wrapper.findComponent({ name: "VSwitch" }).props("modelValue")).toBe(false);
    expect(wrapper.emitted("updated").at(-1)).toEqual([false]);
  });
});
